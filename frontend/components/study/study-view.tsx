// frontend/components/study/study-view.tsx
'use client'

import Link from 'next/link'
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
import { Textarea } from '@/components/ui/textarea'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { ReflectionEditor } from '@/components/study/reflection-editor'
import { cn } from '@/lib/utils'
import { api } from '@/lib/api'
import type { Course, Lecture, LearningEventType, QnaQuestionResponse } from '@/lib/types'

interface StudyViewProps {
  course: Course
  studyId?: string
  // 스터디가 읽기 전용/비활성 상태이면 QnA·회고 등 쓰기 작업을 막는다.
  readOnly?: boolean
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

// 하트비트 주기(초). 실제 영상 재생 대신 이 간격마다 진행 시간을 누적해 서버에 보고한다.
const HEARTBEAT_SECONDS = 5

const formatClock = (totalSeconds: number) => {
  const s = Math.max(0, Math.floor(totalSeconds))
  return `${Math.floor(s / 60)}:${(s % 60).toString().padStart(2, '0')}`
}

export function StudyView({ course, studyId, readOnly = false }: StudyViewProps) {
  const reportHref = `/study/${studyId ?? course.id}/report`
  const lectures = useMemo(
    () => course.chapters.flatMap((c) => c.lectures),
    [course],
  )
  const courseId = Number(course.id)

  // 강의별 진행 상태(서버 동기화). 진행률/완료 표시에 사용.
  const [progressMap, setProgressMap] = useState<
    Record<string, { progress: number; completed: boolean }>
  >(() =>
    Object.fromEntries(
      lectures.map((l) => [l.id, { progress: l.progress, completed: l.completed }]),
    ),
  )

  const [activeId, setActiveId] = useState<string>(
    lectures.find((l) => !l.completed)?.id ?? lectures[0]?.id,
  )
  const [openChapters, setOpenChapters] = useState<string[]>(
    course.chapters.map((c) => c.id),
  )
  const [posts, setPosts] = useState<QnaQuestionResponse[]>([])
  const [question, setQuestion] = useState('')

  const active = lectures.find((l) => l.id === activeId) ?? lectures[0]
  const durationSeconds = active?.durationSeconds ?? 0

  // 재생 시뮬레이션 상태
  const [position, setPosition] = useState(0)
  const [playing, setPlaying] = useState(false)
  const positionRef = useRef(0)
  positionRef.current = position

  // 강의 입장 권한 상태: enterLecture(수강권 검사) 실패 시 true → 재생/작성 차단
  const [accessDenied, setAccessDenied] = useState(false)
  const [entering, setEntering] = useState(true)

  const chapterIdOf = useCallback(
    (lectureId?: string) => lectures.find((l) => l.id === lectureId)?.chapterId,
    [lectures],
  )

  const recordEvent = useCallback(
    (lectureId: string, eventType: LearningEventType, positionSeconds: number) => {
      const chapterId = chapterIdOf(lectureId)
      api
        .recordLearningEvent({
          eventType,
          lectureId: Number(lectureId),
          courseId: Number.isFinite(courseId) ? courseId : undefined,
          chapterId: chapterId ? Number(chapterId) : undefined,
          positionSeconds,
        })
        .catch(() => {
          /* 로그 적재 실패는 학습 흐름을 막지 않는다 */
        })
    },
    [chapterIdOf, courseId],
  )

  const lectureProgress = (l: Lecture) => progressMap[l.id]?.progress ?? l.progress
  const lectureCompleted = (l: Lecture) => progressMap[l.id]?.completed ?? l.completed

  const totalProgress = Math.round(
    lectures.reduce((s, l) => s + lectureProgress(l), 0) / Math.max(lectures.length, 1),
  )

  // 강좌 진입 시 마지막으로 본 강의로 이어보기
  useEffect(() => {
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

    api
      .enterLecture(lectureId)
      .then((res) => {
        if (cancelled) return
        setEntering(false)
        // enterLecture 가 null → 수강권 없음/입장 불가. 재생·작성 UI를 잠근다.
        if (!res) {
          setAccessDenied(true)
          return
        }
        entered = true
        const startPos = res.progress?.lastPositionSeconds ?? 0
        setPosition(startPos)
        if (res.progress) {
          setProgressMap((m) => ({
            ...m,
            [lectureId]: {
              progress: res.progress!.progressRate,
              completed: res.progress!.completed,
            },
          }))
        }
        recordEvent(lectureId, 'LECTURE_ENTER', 0)
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
      const pos = positionRef.current
      // 마지막 시청 위치를 진행 정보에 반영하고 퇴장 이벤트를 남긴다.
      api.updateLectureProgress(lectureId, pos, 0).catch(() => { })
      recordEvent(lectureId, 'LECTURE_EXIT', pos)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active?.id])

  // 재생 중 하트비트: 일정 간격으로 진행 시간을 누적해 서버에 보고(PATCH) + POSITION_SAVE 기록
  useEffect(() => {
    const lectureId = active?.id
    if (!playing || !lectureId) return

    const timer = setInterval(() => {
      setPosition((prev) => {
        const cap = durationSeconds || prev + HEARTBEAT_SECONDS
        const next = Math.min(prev + HEARTBEAT_SECONDS, cap)
        const delta = next - prev
        if (delta > 0) {
          api
            .updateLectureProgress(lectureId, next, delta)
            .then((res) => {
              if (res) {
                setProgressMap((m) => ({
                  ...m,
                  [lectureId]: { progress: res.progressRate, completed: res.completed },
                }))
              }
            })
            .catch(() => { })
          recordEvent(lectureId, 'POSITION_SAVE', next)
        }
        if (durationSeconds && next >= durationSeconds) {
          setPlaying(false)
        }
        return next
      })
    }, HEARTBEAT_SECONDS * 1000)

    return () => clearInterval(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [playing, active?.id, durationSeconds])

  const togglePlay = () => {
    if (!active?.id) return
    if (accessDenied) {
      toast.error('수강권이 없어 이 강의를 재생할 수 없습니다.')
      return
    }
    const next = !playing
    setPlaying(next)
    recordEvent(active.id, next ? 'VIDEO_START' : 'VIDEO_END', positionRef.current)
  }

  const markComplete = async () => {
    if (!active?.id) return
    if (accessDenied) return
    setPlaying(false)
    const end = durationSeconds || positionRef.current
    const delta = Math.max(0, end - positionRef.current)
    try {
      const res = await api.updateLectureProgress(active.id, end, delta)
      if (res) {
        setProgressMap((m) => ({
          ...m,
          [active.id]: { progress: res.progressRate, completed: res.completed },
        }))
      }
      setPosition(end)
      recordEvent(active.id, 'LECTURE_COMPLETE', end)
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
    if (!question.trim()) return
    if (!active?.id) {
      toast.error('강의를 선택해주세요.')
      return
    }

    try {
      const title = question.trim().substring(0, 50)
      const res = await api.createQuestion(active.id, title, question.trim())
      if (res && res.id) {
        setPosts((prev) => [res, ...prev])
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
          <Link href={`/study/${studyId}`}>
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
          <Button asChild variant="outline" size="sm">
            <Link href={reportHref}>학습 리포트</Link>
          </Button>
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
          <div className="relative aspect-video w-full bg-foreground">
            {accessDenied ? (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 text-background">
                <Lock className="h-12 w-12 opacity-80" />
                <p className="text-sm font-medium">이 강의에 입장할 수 없습니다</p>
                <p className="text-xs text-background/60">
                  수강권이 없거나 만료되었어요. 강좌를 수강 신청한 뒤 다시 시도해주세요.
                </p>
              </div>
            ) : (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 text-background">
                <button
                  type="button"
                  onClick={togglePlay}
                  disabled={entering}
                  aria-label={playing ? '일시정지' : '재생'}
                  className="rounded-full bg-background/10 p-3 transition hover:bg-background/20 disabled:opacity-50"
                >
                  {playing ? (
                    <Pause className="h-12 w-12 opacity-90" />
                  ) : (
                    <PlayCircle className="h-12 w-12 opacity-90" />
                  )}
                </button>
                <p className="text-sm text-background/70">{active?.title}</p>
                {playing && (
                  <span className="text-xs text-background/60">시청 시뮬레이션 진행 중…</span>
                )}
              </div>
            )}
            <span className="absolute bottom-3 right-3 rounded bg-background/15 px-2 py-0.5 text-xs text-background">
              {formatClock(position)} / {active?.duration}
            </span>
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
              </div>
              <Button
                size="sm"
                onClick={markComplete}
                disabled={accessDenied || entering || (active ? lectureCompleted(active) : true)}
              >
                <CheckCircle2 className="mr-1 h-4 w-4" />
                {active && lectureCompleted(active) ? '완료됨' : '수강 완료'}
              </Button>
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
          {qnaDisabled ? (
            <p className="px-4 py-3 text-xs text-muted-foreground">
              {accessDenied
                ? '입장 권한이 없어 질문은 열람만 가능해요.'
                : '읽기 전용 스터디입니다. 질문은 열람만 가능해요.'}
            </p>
          ) : (
            <div className="space-y-2 p-4">
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
              <QnaThread key={post.id} post={post} />
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

function QnaThread({ post }: { post: QnaQuestionResponse }) {
  const askerLabel = post.userId ? `수강생 #${post.userId}` : '수강생'
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
            {new Date(post.createdAt).toLocaleString()}
          </p>
        </div>
      </div>
      <p className="mt-2 text-sm font-semibold">{post.title}</p>
      <p className="mt-1 text-sm leading-relaxed">{post.content}</p>

      {post.answer && (
        <div className="mt-3 rounded-md bg-secondary/60 p-3">
          <p className="text-xs font-semibold text-primary">A. 강사</p>
          <p className="mt-1 text-sm leading-relaxed">{post.answer.content}</p>
          <p className="mt-1 text-[11px] text-muted-foreground">
            {new Date(post.answer.createdAt).toLocaleString()}
          </p>
        </div>
      )}
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
        toast.success(
          requestAi
            ? '회고가 저장되었습니다. AI 피드백을 요청했어요.'
            : '회고가 저장되었습니다.',
        )
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
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={requestAi}
              onChange={(e) => setRequestAi(e.target.checked)}
              className="h-4 w-4 rounded border-input accent-primary"
            />
            AI 피드백 요청하기
          </label>
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
                : '아직 작성한 회고가 없어요. 학습한 내용을 정리하고 AI 피드백을 받아보세요.'}
            </p>
          )}
        </div>
      )}
    </section>
  )
}
