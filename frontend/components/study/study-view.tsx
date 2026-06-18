// frontend/components/study/study-view.tsx
'use client'

import Link from 'next/link'
import { useMemo, useState, useEffect } from 'react'
import {
  ArrowLeft,
  CheckCircle2,
  ChevronDown,
  Clock,
  MessageCircleQuestion,
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
import type { Course, Lecture, QnaQuestionResponse } from '@/lib/types'

interface StudyViewProps {
  course: Course
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

export function StudyView({ course }: StudyViewProps) {
  const lectures = useMemo(
    () => course.chapters.flatMap((c) => c.lectures),
    [course],
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
  const totalProgress = Math.round(
    lectures.reduce((s, l) => s + l.progress, 0) / Math.max(lectures.length, 1),
  )

  useEffect(() => {
    if (active?.id) {
      api.getQna(active.id).then((res) => {
        setPosts(res as unknown as QnaQuestionResponse[])
      }).catch(() => setPosts([]))
    }
  }, [active?.id])

  const toggleChapter = (id: string) =>
    setOpenChapters((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )

  const submitQuestion = async () => {
    if (!question.trim()) return
    
    try {
      const newQuestion = {
        id: Date.now(),
        lectureId: Number(active.id),
        authorId: 1,
        authorName: '현재 사용자',
        title: question.trim().substring(0, 20),
        content: question.trim(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        answers: []
      }
      
      setPosts((prev) => [newQuestion, ...prev])
      setQuestion('')
      toast.success('질문이 게시되었습니다.')
    } catch (err) {
      toast.error('질문 게시에 실패했습니다.')
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="sticky top-0 z-40 flex h-14 items-center gap-3 border-b bg-card px-4">
        <Button asChild variant="ghost" size="icon" aria-label="스터디로 돌아가기">
          <Link href={`/study/${course.id}`}>
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
          <Badge variant="secondary">스터디 완료 D-28</Badge>
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
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 text-background">
              <PlayCircle className="h-16 w-16 opacity-90" />
              <p className="text-sm text-background/70">{active?.title}</p>
            </div>
            <span className="absolute bottom-3 right-3 rounded bg-background/15 px-2 py-0.5 text-xs text-background">
              {active?.duration}
            </span>
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

            <ReflectionSection lectureId={active?.id} lectureTitle={active?.title ?? ''} />
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
  active,
  onSelect,
}: {
  lecture: Lecture
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
        {lecture.completed ? (
          <CheckCircle2 className="h-4 w-4 shrink-0 text-primary" />
        ) : (
          <PlayCircle className="h-4 w-4 shrink-0 text-muted-foreground" />
        )}
        <span className="min-w-0 flex-1 truncate">{lecture.title}</span>
        <span className="flex items-center gap-1 text-xs text-muted-foreground">
          {lecture.completed ? (
            <span className="font-semibold text-primary">100%</span>
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
  return (
    <li className="rounded-lg border p-3">
      <div className="flex items-center gap-2">
        <Avatar className="h-7 w-7">
          <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
            {post.authorName ? post.authorName.charAt(0) : '?'}
          </AvatarFallback>
        </Avatar>
        <div className="min-w-0">
          <p className="text-xs font-semibold">
            Q. {post.authorName}
          </p>
          <p className="text-[11px] text-muted-foreground">{new Date(post.createdAt).toLocaleString()}</p>
        </div>
      </div>
      <p className="mt-2 text-sm font-semibold">{post.title}</p>
      <p className="mt-1 text-sm leading-relaxed">{post.content}</p>

      {post.answers?.map((ans) => (
        <div key={ans.id} className="mt-3 rounded-md bg-secondary/60 p-3">
          <p className="text-xs font-semibold text-primary">
            A. {ans.authorName}
          </p>
          <p className="mt-1 text-sm leading-relaxed">{ans.content}</p>
          <p className="mt-1 text-[11px] text-muted-foreground">{new Date(ans.createdAt).toLocaleString()}</p>
        </div>
      ))}
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

function ReflectionSection({ lectureId, lectureTitle }: { lectureId?: string, lectureTitle: string }) {
  const [reflection, setReflection] = useState('')
  const [requestAi, setRequestAi] = useState(true)
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState('')

  const hasReflection = !isEmptyHtml(reflection)

  useEffect(() => {
    if (lectureId) {
      setReflection('')
      setDraft('')
      setEditing(false)
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

  const saveEdit = () => {
    if (isEmptyHtml(draft)) {
      toast.error('회고 내용을 입력해주세요.')
      return
    }
    setReflection(draft)
    setEditing(false)
    setDraft('')
    toast.success(
      requestAi
        ? '회고가 저장되었습니다. AI 피드백을 요청했어요.'
        : '회고가 저장되었습니다.',
    )
  }

  return (
    <section className="mt-8 rounded-lg border bg-card">
      <div className="flex items-center gap-2 border-b px-4 py-3">
        <Sparkles className="h-4 w-4 text-primary" />
        <h2 className="text-sm font-semibold">학습 회고</h2>
        <span className="hidden text-xs text-muted-foreground sm:inline">
          · {lectureTitle}
        </span>
        {!editing && (
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
              아직 작성한 회고가 없어요. 학습한 내용을 정리하고 AI 피드백을 받아보세요.
            </p>
          )}
        </div>
      )}
    </section>
  )
}
