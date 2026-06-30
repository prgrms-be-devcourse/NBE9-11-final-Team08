// frontend/components/study/study-view.tsx
'use client'

import Link from 'next/link'
import { usePathname, useSearchParams } from 'next/navigation'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  ArrowLeft,
  CheckCircle2,
  ChevronDown,
  Clock,
  Lock,
  MessageCircleQuestion,
  Pause,
  Play,
  PlayCircle,
  Pencil,
  Send,
  Sparkles,
} from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { Separator } from '@/components/ui/separator'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { ReflectionEditor } from '@/components/study/reflection-editor'
import { StudyReportDialog } from '@/components/study/study-report-dialog'
import { cn, formatDateTime } from '@/lib/utils'
import { api } from '@/lib/api'
import { useLearningSession } from '@/lib/hooks/use-learning-session'
import type { Course, Lecture, QnaQuestionResponse, QnaAnswerSummary } from '@/lib/types'
import Hls from 'hls.js'

interface StudyViewProps {
  course: Course
  studyId?: string
  // 스터디가 읽기 전용/비활성 상태이면 QnA·회고 등 쓰기 작업을 막는다.
  readOnly?: boolean
  // 현재 로그인 사용자 id. 강좌 강사(instructor.id)와 같으면 QnA 답변 권한이 있다.
  viewerId?: string
  qna?: any[]
}

const LESSON_NOTE = `영속성(Persistence)이란 프로그램이 종료되거나 시스템이 재시작되더라도 데이터가 사라지지 않고 저장되는 특성을 의미한다.

예를 들어,
· 변수에 저장된 데이터 — 프로그램 종료 시 사라짐
· 데이터베이스에 저장된 데이터 — 프로그램 종료 후에도 유지됨

따라서 영속성은 데이터를 장기적으로 보관하기 위한 개념이다.

2. 영속성의 필요성
만약 모든 데이터를 메모리에만 저장한다면 다음과 같은 문제가 발생한다.
예시: 쇼핑몰 회원 정보가 서버 재시작 시 모두 사라진다면 서비스를 운영할 수 없다.`

// 하트비트 주기(초). 이 간격마다 진행 시간을 누적해 서버에 보고한다.
// NEXT_PUBLIC_HEARTBEAT_INTERVAL_SECONDS 로 환경별 조절(미설정/비정상 값이면 5초).
const HEARTBEAT_SECONDS = (() => {
  const raw = Number(process.env.NEXT_PUBLIC_HEARTBEAT_INTERVAL_SECONDS)
  return Number.isFinite(raw) && raw > 0 ? raw : 5
})()

const formatClock = (totalSeconds: number) => {
  const s = Math.max(0, Math.floor(totalSeconds))
  return `${Math.floor(s / 60)}:${(s % 60).toString().padStart(2, '0')}`
}

export function StudyView({ course, studyId, readOnly = false, viewerId }: StudyViewProps) {
  const lectures = useMemo(
    () => course.chapters.flatMap((c) => c.lectures),
    [course],
  )
  const courseId = Number(course.id)
  // 이 강좌의 강사(판매자)인지 여부. 강사는 답변만, 일반 수강생은 질문만 작성한다.
  const isCourseSeller = !!viewerId && String(viewerId) === String(course.instructor.id)

  // 강의별 진행 상태(서버 동기화). 진행률/완료 표시에 사용.
  const [progressMap, setProgressMap] = useState<
    Record<string, { progress: number; completed: boolean }>
  >(() =>
    Object.fromEntries(
      lectures.map((l) => [l.id, { progress: l.progress, completed: l.completed }]),
    ),
  )

  const pathname = usePathname()
  const searchParams = useSearchParams()
  // 최초 렌더 시점의 URL lecture 파라미터. 있으면 그 강의로 시작하고, 이어보기로 덮지 않는다.
  const initialLectureRef = useRef(searchParams?.get('lecture') ?? null)

  const [activeId, setActiveId] = useState<string>(() => {
    const fromUrl = initialLectureRef.current
    if (fromUrl && lectures.some((l) => l.id === fromUrl)) return fromUrl
    return lectures.find((l) => !l.completed)?.id ?? lectures[0]?.id
  })
  const [openChapters, setOpenChapters] = useState<string[]>(
    course.chapters.map((c) => c.id),
  )
  const [posts, setPosts] = useState<QnaQuestionResponse[]>([])
  const [questionTitle, setQuestionTitle] = useState('')
  const [question, setQuestion] = useState('')

  const active = lectures.find((l) => l.id === activeId) ?? lectures[0]
  const durationSeconds = active?.durationSeconds ?? 0;

  const [lectureInfo, setLectureInfo] = useState<any>(null);
  const [streamUrl, setStreamUrl] = useState<string | null>(null);
  const [streamLoading, setStreamLoading] = useState(false);

  // 재생 시뮬레이션 상태
  const [position, setPosition] = useState(0)
  const [playing, setPlaying] = useState(false)
  const positionRef = useRef(0)
  positionRef.current = position
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const lastBeatPositionRef = useRef(0)
  const pendingStartPosRef = useRef<number | null>(null)

  // 강의 입장 권한 상태: enterLecture(수강권 검사) 실패 시 true → 재생/작성 차단
  const [accessDenied, setAccessDenied] = useState(false)
  const [entering, setEntering] = useState(true)
  // 마지막으로 LECTURE_ENTER 를 적재한 강의 ID. 클린업에서 리셋하지 않아 StrictMode
  // 이중 마운트에서도 살아남아, 같은 강의 입장 ENTER 가 중복 적재되는 것을 막는다.
  const enterRecordedRef = useRef<string | null>(null)
  // duplicate lectureInfo removed

  // HLS 비디오 스트리밍 로드 및 해제
  useEffect(() => {
    const video = videoRef.current;
    if (!video || !streamUrl) return;

    let hls: Hls | null = null;

    if (Hls.isSupported()) {
      hls = new Hls({
        xhrSetup: (xhr) => {
          xhr.withCredentials = true
        }
      });
      hls.loadSource(streamUrl);
      hls.attachMedia(video);
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = streamUrl;
    }

    return () => {
      if (hls) {
        hls.destroy();
      }
    };
  }, [streamUrl]);

  const chapterIdOf = useCallback(
    (lectureId?: string) => lectures.find((l) => l.id === lectureId)?.chapterId,
    [lectures],
  )

  // 학습 세션 오케스트레이션(입장/재생/하트비트/완료/퇴장 시 어떤 API를 어떤 순서로 호출할지)은 훅이 소유.
  const session = useLearningSession({ courseId, chapterIdOf })

  const lectureProgress = (l: Lecture) => progressMap[l.id]?.progress ?? l.progress
  const lectureCompleted = (l: Lecture) => progressMap[l.id]?.completed ?? l.completed

  const totalProgress = Math.round(
    lectures.reduce((s, l) => s + lectureProgress(l), 0) / Math.max(lectures.length, 1),
  )

  // 강좌 진입 시 마지막으로 본 강의로 이어보기 (URL 이 강의를 지정한 경우는 그쪽 우선)
  useEffect(() => {
    if (initialLectureRef.current) return
    if (!Number.isFinite(courseId)) return
    api
      .getLastWatched(courseId)
      .then((res) => {
        if (res && res.lectureId) {
          const id = res.lectureId.toString()
          if (lectures.some((l) => l.id === id)) setActiveId(id)
        }
      })
      .catch(() => { })
    // 최초 1회만 이어보기 위치를 결정한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 현재 강의(chapter/lecture)를 URL 쿼리에 반영 → 새로고침/공유 시 위치 유지.
  // 페이지 네비게이션을 일으키지 않도록 history.replaceState 로 주소만 갱신한다.
  useEffect(() => {
    if (!active?.id || !pathname) return
    const params = new URLSearchParams()
    const chapterId = chapterIdOf(active.id)
    if (chapterId) params.set('chapter', chapterId)
    params.set('lecture', active.id)
    window.history.replaceState(null, '', `${pathname}?${params.toString()}`)
  }, [active?.id, pathname, chapterIdOf])

  // 강의 입장: 메타데이터/진행 정보 로드 + LECTURE_ENTER 기록, 퇴장 시 LECTURE_EXIT 기록
  useEffect(() => {
    const lectureId = active?.id
    if (!lectureId) return

    let cancelled = false
    let entered = false
    setPlaying(false)
    setPosition(0)
    setAccessDenied(false)
    setEntering(true)
    pendingStartPosRef.current = null

    // 같은 강의로의 즉시 재마운트(StrictMode)에서는 ENTER 를 다시 적재하지 않는다.
    // 메타/진행 정보 GET 은 매 마운트마다 다시 읽어야 하므로 enter 호출 자체는 유지한다.
    const recordEnter = enterRecordedRef.current !== lectureId
    if (recordEnter) enterRecordedRef.current = lectureId

    session
      .enter(lectureId, recordEnter)
      .then((res) => {
        if (cancelled) return
        setEntering(false)
        // enter 가 null → 수강권 없음/입장 불가. 재생·작성 UI를 잠근다.
        if (!res) {
          setAccessDenied(true)
          return
        }
        entered = true
        setLectureInfo(res)
        const startPos = res.progress?.lastPositionSeconds ?? 0
        setPosition(startPos)
        positionRef.current = startPos
        lastBeatPositionRef.current = startPos
        pendingStartPosRef.current = startPos

        if (res.progress) {
          setProgressMap((m) => ({
            ...m,
            [lectureId]: {
              progress: res.progress!.progressRate,
              completed: res.progress!.completed,
            },
          }))
        }
      })
      .catch(() => {
        if (!cancelled) {
          setEntering(false)
          setAccessDenied(true)
        }
      })

    return () => {
      cancelled = true
      if (!entered) return // 입장하지 못한 강의는 퇴장/진행 기록을 남기지 않는다.
      // 마지막 시청 위치 flush + LECTURE_EXIT (순서는 세션 훅이 소유).
      session.exit(lectureId, positionRef.current)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active?.id])

  // 강의가 변경되거나 입장 성공 후, 실제 스트리밍 주소(및 쿠키)를 백엔드로부터 가져온다.
  useEffect(() => {
    const lectureId = active?.id
    if (!lectureId || accessDenied || entering) {
      setStreamUrl(null)
      return
    }

    let cancelled = false
    setStreamLoading(true)

    const fetchStream = async () => {
      try {
        const chapterId = chapterIdOf(lectureId) || ''
        const urlText = await api.getVideoStreamUrl(courseId, chapterId, lectureId)
        if (cancelled) return

        if (urlText) {
          const formatted = urlText.startsWith('http')
            ? urlText
            : `http://localhost:8080/videos-local/${urlText}`
          setStreamUrl(formatted)
        } else {
          setStreamUrl(null)
          toast.error('동영상 스트리밍 주소를 가져오지 못했습니다.')
        }
      } catch (err) {
        if (!cancelled) {
          setStreamUrl(null)
          toast.error('동영상 정보를 로드하는 중 에러가 발생했습니다.')
        }
      } finally {
        if (!cancelled) {
          setStreamLoading(false)
        }
      }
    }

    fetchStream()

    return () => {
      cancelled = true
    }
  }, [active?.id, entering, accessDenied, courseId, chapterIdOf])

  // 재생 시작 및 강의 변경 시 lastBeatPositionRef 초기화
  useEffect(() => {
    lastBeatPositionRef.current = position
  }, [playing, active?.id])

  // 재생 중 하트비트: 일정 간격(HEARTBEAT_SECONDS)으로 실제 시청한 진행분을 누적해 보고
  useEffect(() => {
    const lectureId = active?.id
    if (!playing || !lectureId) return

    const beat = () => {
      const current = positionRef.current
      const prev = lastBeatPositionRef.current
      const delta = Math.max(0, current - prev)
      if (delta <= 0) return

      lastBeatPositionRef.current = current
      session.beat(lectureId, current, delta).then((res) => {
        if (res) {
          setProgressMap((m) => ({
            ...m,
            [lectureId]: { progress: res.progressRate, completed: res.completed },
          }))
        }
      })
    }

    beat()
    const timer = setInterval(beat, HEARTBEAT_SECONDS * 1000)

    return () => clearInterval(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [playing, active?.id])

  const handlePlay = () => {
    if (!active?.id) return
    setPlaying(true)
    // 재생 시작/재개 이벤트는 더 이상 수집하지 않는다(멈춤 위치만 분석에 사용).
  }

  const handlePause = () => {
    if (!active?.id) return
    setPlaying(false)
    session.pause(active.id, positionRef.current)
  }

  const handleEnded = () => {
    if (!active?.id) return
    setPlaying(false)
    session.pause(active.id, positionRef.current)
    markComplete()
  }

  const handleTimeUpdate = () => {
    if (videoRef.current) {
      const current = videoRef.current.currentTime
      setPosition(current)
      positionRef.current = current
    }
  }

  const handleSeeked = () => {
    if (videoRef.current) {
      const current = videoRef.current.currentTime
      lastBeatPositionRef.current = current
    }
  }

  const handleLoadedMetadata = () => {
    if (videoRef.current && pendingStartPosRef.current !== null) {
      videoRef.current.currentTime = pendingStartPosRef.current
      lastBeatPositionRef.current = pendingStartPosRef.current
      pendingStartPosRef.current = null
    }
  }

  const togglePlay = () => {
    if (!active?.id) return
    if (accessDenied) {
      toast.error('수강권이 없어 이 강의를 재생할 수 없습니다.')
      return
    }
    const video = videoRef.current
    if (!video) return

    if (video.paused) {
      if (durationSeconds && video.currentTime >= durationSeconds) {
        video.currentTime = 0
      }
      video.play().catch(() => { })
    } else {
      video.pause()
    }
  }

  const markComplete = async () => {
    if (!active?.id) return
    if (accessDenied) return
    setPlaying(false)
    const end = durationSeconds || positionRef.current
    const delta = Math.max(0, end - positionRef.current)
    try {
      const res = await session.complete(active.id, end, delta)
      if (res) {
        setProgressMap((m) => ({
          ...m,
          [active.id]: { progress: res.progressRate, completed: res.completed },
        }))
      }
      setPosition(end)
      toast.success('수강 완료로 표시했어요.')
    } catch {
      toast.error('수강 완료 처리에 실패했습니다.')
    }
  }

  useEffect(() => {
    if (active?.id) {
      api
        .getQna(active.id)
        .then((res: any) => {
          if (Array.isArray(res)) {
            setPosts(res)
          } else if (res && Array.isArray(res.content)) {
            setPosts(res.content)
          } else {
            setPosts([])
          }
        })
        .catch(() => setPosts([]))
    }
  }, [active?.id])

  const toggleChapter = (id: string) =>
    setOpenChapters((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )

  const submitQuestion = async () => {
    if (qnaDisabled) {
      toast.error(
        accessDenied
          ? '입장 권한이 없어 질문을 작성할 수 없습니다.'
          : '읽기 전용 스터디에서는 질문을 작성할 수 없습니다.',
      )
      return
    }
    if (!questionTitle.trim()) {
      toast.error('제목을 입력해주세요.')
      return
    }
    if (!question.trim()) return
    if (!active?.id) {
      toast.error('강의를 선택해주세요.')
      return
    }

    try {
      const res = await api.createQuestion(active.id, questionTitle.trim(), question.trim())
      if (res && res.id) {
        setPosts((prev) => [res, ...prev])
        setQuestionTitle('')
        setQuestion('')
        toast.success('질문이 게시되었습니다.')
      } else {
        throw new Error('Failed to create question')
      }
    } catch (err) {
      toast.error('질문 게시에 실패했습니다.')
    }
  }

  const activeProgress = active ? lectureProgress(active) : 0
  // QnA 작성은 읽기 전용 스터디이거나 입장 권한이 없을 때 막는다.
  // 회고는 읽기 전용에서도 작성 가능하며, 입장 권한이 없을 때만 막는다.
  const qnaDisabled = readOnly || accessDenied

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="sticky top-0 z-40 flex h-14 items-center gap-3 border-b bg-card px-4">
        <Button asChild variant="ghost" size="icon" aria-label="스터디로 돌아가기">
          <Link href={`/study/${studyId ?? course.id}/course`}>
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold">{course.title}</p>
          <p className="text-xs text-muted-foreground">강의 학습</p>
        </div>
        <div className="ml-auto flex items-center gap-4">
          <div className="hidden items-center gap-2 sm:flex">
            <span className="text-xs text-muted-foreground">진행도</span>
            <Progress value={totalProgress} className="h-2 w-28" />
            <span className="text-xs font-semibold">{totalProgress}%</span>
          </div>
          <StudyReportDialog studyId={studyId ?? course.id} />
        </div>
      </header>

      <div className="grid flex-1 lg:grid-cols-[300px_1fr_340px]">
        <aside className="order-2 border-t lg:order-1 lg:border-r lg:border-t-0">
          <div className="flex items-center justify-between px-4 py-3">
            <h2 className="text-sm font-semibold">커리큘럼</h2>
            <span className="text-xs text-muted-foreground">
              {lectures.length}개 강의
            </span>
          </div>
          <Separator />
          <nav className="lg:max-h-[calc(100vh-7rem)] lg:overflow-y-auto">
            {course.chapters.map((ch) => {
              const open = openChapters.includes(ch.id)
              return (
                <div key={ch.id} className="border-b">
                  <button
                    type="button"
                    onClick={() => toggleChapter(ch.id)}
                    className="flex w-full items-center justify-between px-4 py-3 text-left text-sm font-semibold hover:bg-secondary"
                  >
                    {ch.title}
                    <ChevronDown
                      className={cn(
                        'h-4 w-4 text-muted-foreground transition-transform',
                        open && 'rotate-180',
                      )}
                    />
                  </button>
                  {open && (
                    <ul className="pb-2">
                      {ch.lectures.map((lec) => (
                        <LectureRow
                          key={lec.id}
                          lecture={lec}
                          progress={lectureProgress(lec)}
                          completed={lectureCompleted(lec)}
                          active={lec.id === active?.id}
                          onSelect={() => setActiveId(lec.id)}
                        />
                      ))}
                    </ul>
                  )}
                </div>
              )
            })}
          </nav>
        </aside>

        <main className="order-1 lg:order-2">
          <div className="relative aspect-video w-full bg-black flex items-center justify-center">
            {accessDenied ? (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 text-white bg-black/80 z-20">
                <Lock className="h-12 w-12 opacity-80" />
                <p className="text-sm font-medium">이 강의에 입장할 수 없습니다</p>
                <p className="text-xs text-white/60">
                  수강권이 없거나 만료되었어요. 강좌를 수강 신청한 뒤 다시 시도해주세요.
                </p>
              </div>
            ) : (
              <>
                <video
                  ref={videoRef}
                  className="w-full h-full object-contain"
                  controls
                  onPlay={handlePlay}
                  onPause={handlePause}
                  onEnded={handleEnded}
                  onTimeUpdate={handleTimeUpdate}
                  onSeeked={handleSeeked}
                  onLoadedMetadata={handleLoadedMetadata}
                />
                {entering && (
                  <div className="absolute inset-0 flex items-center justify-center bg-black/60 z-10 text-white text-sm">
                    동영상 정보를 불러오는 중입니다...
                  </div>
                )}
              </>
            )}
          </div>

          {/* 재생 컨트롤 + 진행 바 (실제 영상 없이 진행/이벤트를 시뮬레이션) */}
          <div className="border-b bg-card px-5 py-3">
            <div className="flex items-center gap-3">
              <Button
                size="sm"
                variant="secondary"
                onClick={togglePlay}
                disabled={accessDenied || entering}
              >
                {playing ? (
                  <>
                    <Pause className="mr-1 h-4 w-4" /> 일시정지
                  </>
                ) : (
                  <>
                    <Play className="mr-1 h-4 w-4" /> 재생
                  </>
                )}
              </Button>
              <div className="flex flex-1 items-center gap-2">
                <Progress value={activeProgress} className="h-2 flex-1" />
                <span className="w-10 text-right text-xs font-semibold">
                  {activeProgress}%
                </span>
                {active && lectureCompleted(active) && (
                  <span className="flex items-center gap-1 text-xs font-semibold text-primary">
                    <CheckCircle2 className="h-4 w-4" />
                    완료됨
                  </span>
                )}
              </div>
            </div>
          </div>

          <div className="mx-auto max-w-3xl px-5 py-6">
            <div>
              <p className="text-xs font-medium text-muted-foreground">
                현재 강의
              </p>
              <h1 className="mt-1 text-xl font-bold">{active?.title}</h1>
            </div>

            <Separator className="my-5" />

            <article className="space-y-4 text-sm leading-relaxed text-foreground/90">
              {LESSON_NOTE.split('\n\n').map((para, i) => (
                <p key={i} className="whitespace-pre-line">
                  {para}
                </p>
              ))}
            </article>

            <ReflectionSection
              lectureId={active?.id}
              lectureTitle={active?.title ?? ''}
              disabled={accessDenied}
            />
          </div>
        </main>

        <aside className="order-3 border-t lg:border-l lg:border-t-0">
          <div className="flex items-center gap-2 px-4 py-3">
            <MessageCircleQuestion className="h-4 w-4" />
            <h2 className="text-sm font-semibold">QnA</h2>
            <Badge variant="secondary" className="ml-auto">
              {posts.length}
            </Badge>
          </div>
          <Separator />
          {isCourseSeller ? (
            <p className="px-4 py-3 text-xs text-muted-foreground">
              이 강좌의 강사입니다. 수강생의 질문에 답변할 수 있어요.
            </p>
          ) : qnaDisabled ? (
            <p className="px-4 py-3 text-xs text-muted-foreground">
              {accessDenied
                ? '입장 권한이 없어 질문은 열람만 가능해요.'
                : '읽기 전용 스터디입니다. 질문은 열람만 가능해요.'}
            </p>
          ) : (
            <div className="space-y-2 p-4">
              <Input
                value={questionTitle}
                onChange={(e) => setQuestionTitle(e.target.value)}
                placeholder="제목"
                maxLength={50}
              />
              <Textarea
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder="이 강의에 대해 궁금한 점을 질문해보세요."
                className="min-h-20 resize-none"
              />
              <Button onClick={submitQuestion} size="sm" className="w-full">
                <Send className="mr-1 h-4 w-4" /> 질문 게시
              </Button>
            </div>
          )}
          <Separator />
          <ul className="space-y-4 p-4 lg:max-h-[calc(100vh-20rem)] lg:overflow-y-auto">
            {posts.map((post) => (
              <QnaThread
                key={post.id}
                post={post}
                canAnswer={isCourseSeller && !readOnly}
                onAnswered={(questionId, answer) =>
                  setPosts((prev) =>
                    prev.map((p) => (p.id === questionId ? { ...p, answer } : p)),
                  )
                }
              />
            ))}
          </ul>
        </aside>
      </div>
    </div>
  )
}

function LectureRow({
  lecture,
  progress,
  completed,
  active,
  onSelect,
}: {
  lecture: Lecture
  progress: number
  completed: boolean
  active: boolean
  onSelect: () => void
}) {
  return (
    <li>
      <button
        type="button"
        onClick={onSelect}
        className={cn(
          'flex w-full items-center gap-2 px-4 py-2 text-left text-sm hover:bg-secondary',
          active && 'bg-secondary font-medium',
        )}
      >
        {completed ? (
          <CheckCircle2 className="h-4 w-4 shrink-0 text-primary" />
        ) : (
          <PlayCircle className="h-4 w-4 shrink-0 text-muted-foreground" />
        )}
        <span className="min-w-0 flex-1 truncate">{lecture.title}</span>
        <span className="flex items-center gap-1 text-xs text-muted-foreground">
          {completed ? (
            <span className="font-semibold text-primary">100%</span>
          ) : progress > 0 ? (
            <span className="font-semibold">{progress}%</span>
          ) : (
            <>
              <Clock className="h-3 w-3" />
              {lecture.duration}
            </>
          )}
        </span>
      </button>
    </li>
  )
}

function QnaThread({
  post,
  canAnswer = false,
  onAnswered,
}: {
  post: QnaQuestionResponse
  canAnswer?: boolean
  onAnswered?: (questionId: number, answer: QnaAnswerSummary) => void
}) {
  const askerLabel = post.nickname || '수강생'
  const [answerText, setAnswerText] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const submitAnswer = async () => {
    if (!answerText.trim()) return
    setSubmitting(true)
    try {
      const res = await api.createAnswer(post.id, answerText.trim())
      if (res && res.id) {
        onAnswered?.(post.id, {
          id: res.id,
          content: res.content,
          createdAt: res.createdAt,
        })
        setAnswerText('')
        toast.success('답변이 등록되었습니다.')
      } else {
        throw new Error('Failed to create answer')
      }
    } catch (err) {
      toast.error('답변 등록에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <li className="rounded-lg border p-3">
      <div className="flex items-center gap-2">
        <Avatar className="h-7 w-7">
          <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
            {askerLabel.charAt(0)}
          </AvatarFallback>
        </Avatar>
        <div className="min-w-0">
          <p className="text-xs font-semibold">Q. {askerLabel}</p>
          <p className="text-[11px] text-muted-foreground">
            {formatDateTime(post.createdAt)}
          </p>
        </div>
      </div>
      <p className="mt-2 text-sm font-semibold">{post.title}</p>
      {post.content.trim() !== post.title.trim() && (
        <p className="mt-1 text-sm leading-relaxed">{post.content}</p>
      )}

      {post.answer ? (
        <div className="mt-3 rounded-md bg-secondary/60 p-3">
          <p className="text-xs font-semibold text-primary">A. 강사</p>
          <p className="mt-1 text-sm leading-relaxed">{post.answer.content}</p>
          <p className="mt-1 text-[11px] text-muted-foreground">
            {formatDateTime(post.answer.createdAt)}
          </p>
        </div>
      ) : canAnswer ? (
        <div className="mt-3 space-y-2">
          <Textarea
            value={answerText}
            onChange={(e) => setAnswerText(e.target.value)}
            placeholder="이 질문에 답변을 작성해주세요."
            className="min-h-16 resize-none"
          />
          <Button
            onClick={submitAnswer}
            size="sm"
            className="w-full"
            disabled={submitting || !answerText.trim()}
          >
            <Send className="mr-1 h-4 w-4" /> 답변 등록
          </Button>
        </div>
      ) : null}
    </li>
  )
}

function isEmptyHtml(html: string) {
  const stripped = html
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, '')
    .trim()
  return stripped.length === 0
}

function ReflectionSection({ lectureId, lectureTitle, disabled = false }: { lectureId?: string, lectureTitle: string, disabled?: boolean }) {
  const [reflection, setReflection] = useState('')
  const [reflectionId, setReflectionId] = useState<number | null>(null)
  const [requestAi, setRequestAi] = useState(true)
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState('')

  const hasReflection = !isEmptyHtml(reflection)

  useEffect(() => {
    if (lectureId) {
      setReflection('')
      setDraft('')
      setEditing(false)
      setReflectionId(null)

      api.getReflection(lectureId)
        .then((res) => {
          if (res && res.content) {
            setReflection(res.content)
            setReflectionId(res.id)
          }
        })
        .catch(() => {
          // Ignore error, keep empty
        })
    }
  }, [lectureId])

  const startEdit = () => {
    setDraft(reflection)
    setEditing(true)
  }

  const cancelEdit = () => {
    setEditing(false)
    setDraft('')
  }

  const saveEdit = async () => {
    if (disabled) {
      toast.error('입장 권한이 없어 회고를 저장할 수 없습니다.')
      return
    }
    if (isEmptyHtml(draft)) {
      toast.error('회고 내용을 입력해주세요.')
      return
    }
    if (!lectureId) {
      toast.error('강의가 지정되지 않았습니다.')
      return
    }

    try {
      let res
      if (reflectionId) {
        res = await api.updateReflection(lectureId, reflectionId, draft)
      } else {
        res = await api.createReflection(lectureId, draft)
      }

      if (res && res.id) {
        setReflection(res.content)
        setReflectionId(res.id)
        setEditing(false)
        setDraft('')
        toast.success('회고가 저장되었습니다.')
      } else {
        throw new Error('Failed to save reflection')
      }
    } catch (err) {
      toast.error('회고 저장에 실패했습니다.')
    }
  }

  return (
    <section className="mt-8 rounded-lg border bg-card">
      <div className="flex items-center gap-2 border-b px-4 py-3">
        <Sparkles className="h-4 w-4 text-primary" />
        <h2 className="text-sm font-semibold">학습 회고</h2>
        <span className="hidden text-xs text-muted-foreground sm:inline">
          · {lectureTitle}
        </span>
        {!editing && !disabled && (
          <Button
            size="sm"
            variant="secondary"
            className="ml-auto shrink-0"
            onClick={startEdit}
          >
            <Pencil className="mr-1 h-4 w-4" />
            {hasReflection ? '수정' : '회고 작성'}
          </Button>
        )}
      </div>

      {editing ? (
        <div className="space-y-4 p-4">
          <p className="text-xs text-muted-foreground">
            {'"# 제목", "- 목록", "**굵게**" 처럼 입력하면 노션처럼 바로 스타일이 적용돼요.'}
          </p>
          <div className="rounded-md border bg-background px-3 py-2 focus-within:ring-1 focus-within:ring-ring">
            <ReflectionEditor
              content={draft}
              editable
              onChange={setDraft}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={cancelEdit}>
              취소
            </Button>
            <Button onClick={saveEdit}>완료</Button>
          </div>
        </div>
      ) : (
        <div className="p-4">
          {hasReflection ? (
            <ReflectionEditor content={reflection} editable={false} />
          ) : (
            <p className="text-sm text-muted-foreground">
              {disabled
                ? '입장 권한이 없어 회고를 작성할 수 없어요.'
                : '아직 작성한 회고가 없어요.'}
            </p>
          )}
        </div>
      )}
    </section>
  )
}
