'use client'

import { useState } from 'react'
import { FileVideo, GripVertical, ImageUp, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'

interface LectureDraft {
  id: string
  title: string
  description: string
  videoName?: string
  thumbnailName?: string
}

interface ChapterDraft {
  id: string
  title: string
  lectures: LectureDraft[]
}

let idCounter = 100
const nextId = () => `c${idCounter++}`

const initialChapters: ChapterDraft[] = [
  {
    id: 's1',
    title: '섹션 1. 오리엔테이션',
    lectures: [
      {
        id: 'l1',
        title: '강의 소개와 학습 목표',
        description: '강의의 전체 구성과 학습 목표를 안내합니다.',
      },
      { id: 'l2', title: '개발 환경 세팅', description: '실습에 필요한 개발 환경을 설정합니다.' },
    ],
  },
  {
    id: 's2',
    title: '섹션 2. 핵심 개념',
    lectures: [
      { id: 'l3', title: '영속성 컨텍스트의 이해', description: 'JPA의 핵심 개념을 설명합니다.' },
    ],
  },
]

export function CurriculumBuilder() {
  const [chapters, setChapters] = useState<ChapterDraft[]>(initialChapters)
  const [activeId, setActiveId] = useState<string>(initialChapters[0]?.id ?? '')

  const active = chapters.find((c) => c.id === activeId) ?? chapters[0]

  const addChapter = () => {
    const id = nextId()
    setChapters((prev) => [
      ...prev,
      { id, title: `섹션 ${prev.length + 1}.`, lectures: [] },
    ])
    setActiveId(id)
  }

  const removeChapter = (cid: string) => {
    setChapters((prev) => {
      const next = prev.filter((c) => c.id !== cid)
      if (cid === activeId) setActiveId(next[0]?.id ?? '')
      return next
    })
  }

  const updateChapterTitle = (cid: string, title: string) =>
    setChapters((prev) => prev.map((c) => (c.id === cid ? { ...c, title } : c)))

  const addLecture = (cid: string) =>
    setChapters((prev) =>
      prev.map((c) =>
        c.id === cid
          ? {
              ...c,
              lectures: [
                ...c.lectures,
                { id: nextId(), title: '새 강의', description: '' },
              ],
            }
          : c,
      ),
    )

  const removeLecture = (cid: string, lid: string) =>
    setChapters((prev) =>
      prev.map((c) =>
        c.id === cid ? { ...c, lectures: c.lectures.filter((l) => l.id !== lid) } : c,
      ),
    )

  const updateLecture = (
    cid: string,
    lid: string,
    patch: Partial<LectureDraft>,
  ) =>
    setChapters((prev) =>
      prev.map((c) =>
        c.id === cid
          ? {
              ...c,
              lectures: c.lectures.map((l) => (l.id === lid ? { ...l, ...patch } : l)),
            }
          : c,
      ),
    )

  const totalLectures = chapters.reduce((acc, c) => acc + c.lectures.length, 0)

  return (
    <div className="grid gap-6 lg:grid-cols-[300px_1fr]">
      {/* Chapter list */}
      <aside className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold">챕터 목록</h2>
          <span className="text-xs text-muted-foreground">{chapters.length}개</span>
        </div>

        <ul className="space-y-2">
          {chapters.map((c) => {
            const isActive = c.id === active?.id
            return (
              <li key={c.id}>
                <button
                  type="button"
                  onClick={() => setActiveId(c.id)}
                  className={cn(
                    'flex w-full items-center gap-2 rounded-lg border bg-card px-3 py-3 text-left text-sm transition-colors hover:bg-secondary',
                    isActive && 'border-primary bg-secondary font-medium',
                  )}
                >
                  <GripVertical className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                  <span className="line-clamp-1 flex-1">{c.title}</span>
                  <span className="shrink-0 text-xs text-muted-foreground">
                    {c.lectures.length}
                  </span>
                </button>
              </li>
            )
          })}
        </ul>

        <Button
          type="button"
          variant="outline"
          onClick={addChapter}
          className="w-full gap-1.5"
        >
          <Plus className="size-4" /> 새 챕터 추가
        </Button>
      </aside>

      {/* Lecture list for active chapter */}
      <section className="space-y-4">
        {active ? (
          <>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex-1 space-y-1">
                <Label htmlFor="chapter-title" className="text-xs text-muted-foreground">
                  챕터명
                </Label>
                <Input
                  id="chapter-title"
                  value={active.title}
                  onChange={(e) => updateChapterTitle(active.id, e.target.value)}
                  className="h-9 font-medium"
                />
              </div>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => removeChapter(active.id)}
                className="self-end text-muted-foreground hover:text-destructive"
              >
                <Trash2 className="mr-1 size-4" /> 챕터 삭제
              </Button>
            </div>

            <p className="text-sm text-muted-foreground">
              강의 목록 · 총 {totalLectures}개 강의 중 {active.lectures.length}개
            </p>

            <div className="space-y-4">
              {active.lectures.map((lecture, index) => (
                <article key={lecture.id} className="rounded-xl border bg-card p-4">
                  <div className="flex items-center gap-2">
                    <GripVertical className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                    <span className="text-sm font-semibold">강의 {index + 1}</span>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="ml-auto size-8 text-muted-foreground hover:text-destructive"
                      onClick={() => removeLecture(active.id, lecture.id)}
                      aria-label="강의 삭제"
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </div>

                  <div className="mt-3 grid gap-4">
                    <div className="grid gap-1.5">
                      <Label htmlFor={`title-${lecture.id}`}>강의 제목</Label>
                      <Input
                        id={`title-${lecture.id}`}
                        value={lecture.title}
                        onChange={(e) =>
                          updateLecture(active.id, lecture.id, { title: e.target.value })
                        }
                        placeholder="예: What is OOP?"
                      />
                    </div>

                    <div className="grid gap-1.5">
                      <Label>강의 영상</Label>
                      <button
                        type="button"
                        onClick={() =>
                          toast.info('영상 업로드는 데모에서 비활성화되어 있습니다.')
                        }
                        className="flex flex-col items-center justify-center gap-1 rounded-lg border border-dashed py-6 text-muted-foreground transition-colors hover:bg-secondary"
                      >
                        <span className="flex items-center gap-1.5 text-sm font-medium">
                          <FileVideo className="size-4" />
                          {lecture.videoName ?? '영상 파일 업로드'}
                        </span>
                        <span className="text-[11px]">MP4, MOV 지원 · 최대 2GB</span>
                      </button>
                    </div>

                    <div className="grid gap-1.5">
                      <Label htmlFor={`desc-${lecture.id}`}>설명</Label>
                      <Textarea
                        id={`desc-${lecture.id}`}
                        value={lecture.description}
                        onChange={(e) =>
                          updateLecture(active.id, lecture.id, {
                            description: e.target.value,
                          })
                        }
                        placeholder="강의 내용을 간략히 설명하세요."
                        className="min-h-20"
                      />
                    </div>

                    <div className="grid gap-1.5">
                      <Label>썸네일</Label>
                      <button
                        type="button"
                        onClick={() =>
                          toast.info('썸네일 업로드는 데모에서 비활성화되어 있습니다.')
                        }
                        className="flex aspect-video w-32 flex-col items-center justify-center gap-1 rounded-lg border border-dashed text-muted-foreground transition-colors hover:bg-secondary"
                      >
                        <ImageUp className="size-5" />
                        <span className="text-[11px]">썸네일 업로드</span>
                      </button>
                    </div>
                  </div>
                </article>
              ))}

              {active.lectures.length === 0 ? (
                <div className="rounded-xl border border-dashed py-12 text-center text-sm text-muted-foreground">
                  아직 강의가 없습니다. 새 강의를 추가하세요.
                </div>
              ) : null}
            </div>

            <Button
              type="button"
              variant="outline"
              onClick={() => addLecture(active.id)}
              className="w-full gap-1.5"
            >
              <Plus className="size-4" /> 새 강의 추가
            </Button>
          </>
        ) : (
          <div className="rounded-xl border border-dashed py-20 text-center text-sm text-muted-foreground">
            챕터를 추가해 커리큘럼 구성을 시작하세요.
          </div>
        )}
      </section>
    </div>
  )
}
