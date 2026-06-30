// frontend/components/instructor/curriculum-builder.tsx
'use client'

import { useState, useEffect, useMemo } from 'react'
import { FileVideo, GripVertical, ImageUp, Plus, Trash2, Save, Loader2, AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import { toast } from 'sonner'
import { cn } from '@/lib/utils'
import { api } from '@/lib/api'
import type { Course } from '@/lib/types'

interface LectureDraft {
  id: string
  title: string
  description: string
  durationSeconds: number
  videoName?: string
  hasVideo?: boolean
  videoFile: File | null
  isFreePreview?: boolean
}

interface ChapterDraft {
  id: string
  title: string
  lectures: LectureDraft[]
}

/** 백엔드 Lecture 엔티티는 durationSeconds > 0 을 요구한다. */
const toBackendDurationSeconds = (seconds: number) => {
  const n = Number(seconds)
  return Number.isFinite(n) && n > 0 ? n : 1
}

export function CurriculumBuilder({
  courseId,
  backHref,
}: {
  courseId?: string
  backHref?: string
}) {
  const [course, setCourse] = useState<Course | null>(null)
  const [chapters, setChapters] = useState<ChapterDraft[]>([])
  const [activeId, setActiveId] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [uploadingId, setUploadingId] = useState<string | null>(null)

  const isReadOnly = useMemo(() => {
    return course?.status === 'ON_SALE' || course?.status === 'SUSPENDED'
  }, [course])

  const loadCurriculum = async (
    shouldShowToastError = true,
    preferredActiveIndex?: number,
    uploadedVideoNames?: Record<string, string>,
  ) => {
    if (!courseId) {
      setLoading(false)
      return
    }
    try {
      const course = await api.getInstructorCourse(courseId)
      setCourse(course ?? null)
      if (course) {
        const mappedChapters = (course.chapters ?? []).map((ch) => ({
          id: ch.id.toString(),
          title: ch.title,
          lectures: ch.lectures.map((lec) => {
            const videoName =
              uploadedVideoNames?.[lec.id] ??
              (lec.hasVideo ? '업로드된 영상' : undefined)
            return {
              id: lec.id.toString(),
              title: lec.title,
              description: lec.summary ?? '',
              durationSeconds: lec.durationSeconds || 0,
              videoName,
              hasVideo: lec.hasVideo ?? !!videoName,
              videoFile: null,
              isFreePreview: lec.isFreePreview ?? false,
            }
          }),
        }))
        setChapters((prev) => {
          if (mappedChapters.length === 0 && prev.length > 0) {
            console.warn('[CurriculumBuilder] 서버에서 빈 커리큘럼이 반환되어 로컬 상태를 유지합니다.')
            return prev
          }
          return mappedChapters
        })
        if (mappedChapters.length > 0) {
          if (
            preferredActiveIndex !== undefined &&
            preferredActiveIndex >= 0 &&
            preferredActiveIndex < mappedChapters.length
          ) {
            setActiveId(mappedChapters[preferredActiveIndex].id)
          } else {
            setActiveId(mappedChapters[0].id)
          }
        } else {
          setActiveId('')
        }
      }
    } catch (e) {
      if (shouldShowToastError) {
        toast.error('커리큘럼 정보를 불러오는데 실패했습니다.')
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCurriculum()
  }, [courseId])

  const active = chapters.find((c) => c.id === activeId) ?? chapters[0]

  const nextId = () => `tmp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

  const addChapter = () => {
    if (isReadOnly) return
    const id = nextId()
    setChapters((prev) => [
      ...prev,
      { id, title: `섹션 ${prev.length + 1}.`, lectures: [] },
    ])
    setActiveId(id)
  }

  const removeChapter = (cid: string) => {
    if (isReadOnly) return
    setChapters((prev) => {
      const next = prev.filter((c) => c.id !== cid)
      if (cid === activeId) setActiveId(next[0]?.id ?? '')
      return next
    })
  }

  const updateChapterTitle = (cid: string, title: string) =>
    setChapters((prev) => prev.map((c) => (c.id === cid ? { ...c, title } : c)))

  const addLecture = (cid: string) => {
    if (isReadOnly) return
    setChapters((prev) =>
      prev.map((c) =>
        c.id === cid
          ? {
              ...c,
              lectures: [
                ...c.lectures,
                { id: nextId(), title: '새 강의', description: '', durationSeconds: 0, videoFile: null, hasVideo: false, isFreePreview: false },
              ],
            }
          : c,
      ),
    )
  }

  const removeLecture = (cid: string, lid: string) => {
    if (isReadOnly) return
    setChapters((prev) =>
      prev.map((c) =>
        c.id === cid ? { ...c, lectures: c.lectures.filter((l) => l.id !== lid) } : c,
      ),
    )
  }

  const updateLecture = (cid: string, lid: string, patch: Partial<LectureDraft>) =>
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

  const handleVideoSelect = (lectureId: string, file: File) => {
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      window.URL.revokeObjectURL(video.src)
      const duration = Math.floor(video.duration)
      updateLecture(active.id, lectureId, { 
        videoFile: file, 
        videoName: file.name,
        durationSeconds: duration,
        hasVideo: false,
      })
      toast.info(`${file.name} 영상이 대기열에 추가되었습니다. 재생 시간(${duration}초) 감지 완료.`)
    }
    video.onerror = () => {
      updateLecture(active.id, lectureId, { 
        videoFile: file, 
        videoName: file.name,
        hasVideo: false,
      })
      toast.info(`${file.name} 영상이 대기열에 추가되었습니다.`)
    }
    video.src = URL.createObjectURL(file)
  }

  const handleSave = async () => {
    if (!courseId || !course) {
      toast.error('강좌 정보를 먼저 불러와야 커리큘럼을 저장할 수 있습니다.')
      return
    }

    const invalidChapter = chapters.find((chapter) => !chapter.title.trim())
    if (invalidChapter) {
      toast.error('챕터 제목을 입력해주세요.')
      return
    }

    const invalidLecture = chapters
      .flatMap((chapter) => chapter.lectures)
      .find((lecture) => !lecture.title.trim())
    if (invalidLecture) {
      toast.error('강의 제목을 입력해주세요.')
      return
    }

    const pendingUploads = chapters.flatMap((chapter, chapterIndex) =>
      chapter.lectures
        .map((lecture, lectureIndex) => ({
          chapterIndex,
          lectureOrderNo: lectureIndex + 1,
          lectureTitle: lecture.title,
          file: lecture.videoFile,
        }))
        .filter((target) => target.file !== null),
    )

    const activeIndex = chapters.findIndex((c) => c.id === activeId)
    setSaving(true)
    try {
      const formData = new FormData()

      const rawCat = (course as any).categoryId || (course as any).categoryIdResponse || course.category;
      const parsedCategoryId = Number(rawCat);
      const safeCategoryId = Number.isFinite(parsedCategoryId) && parsedCategoryId > 0 ? parsedCategoryId : 1;

      const requestData = {
        title: course.title || '임시 강좌 제목',
        description: course.description || '강좌 세부 커리큘럼 명세 및 강의 구성 정보 단락입니다.',
        categoryId: safeCategoryId,
        price: Number(course.price) || 0,
        thumbnail: course.thumbnailUrl || (course as any).thumbnail || 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?q=80&w=800&auto=format&fit=crop',
        chapters: chapters.map((chapter, chapterIndex) => ({
          id: chapter.id.startsWith('tmp_') ? null : Number(chapter.id),
          title: chapter.title.trim(),
          orderNo: chapterIndex + 1,
          lectures: chapter.lectures.map((lecture, lectureIndex) => ({
            id: lecture.id.startsWith('tmp_') ? null : Number(lecture.id),
            title: lecture.title.trim(),
            summary: lecture.description.trim(),
            durationSeconds: toBackendDurationSeconds(lecture.durationSeconds),
            orderNo: lectureIndex + 1,
            isFreePreview: lecture.isFreePreview ?? false,
          })),
        })),
      }

      const requestBlob = new Blob(
        [JSON.stringify(requestData)], 
        { type: 'application/json' }
      )
      
      formData.append('request', requestBlob)

      await api.updateCourse(courseId, formData)

      const refreshedCourse = await api.getInstructorCourse(courseId)
      
      let videoUploadFailed = false
      const uploadedVideoNames: Record<string, string> = {}
      if (refreshedCourse && refreshedCourse.chapters) {
        for (const target of pendingUploads) {
          const matchedChapter = refreshedCourse.chapters[target.chapterIndex]
          const matchedLecture = matchedChapter?.lectures?.[target.lectureOrderNo - 1]

          if (matchedLecture && matchedLecture.id) {
            const lecIdStr = matchedLecture.id.toString()
            setUploadingId(lecIdStr)
            try {
              toast.info(`'${target.lectureTitle}' 강의 영상 업로드를 시작합니다...`)
              await api.uploadLectureVideo(lecIdStr, target.file!)
              uploadedVideoNames[lecIdStr] = target.file!.name
              toast.success(`'${target.lectureTitle}' 영상 업로드 완료. 인코딩이 시작됩니다.`)
            } catch (videoError: unknown) {
              videoUploadFailed = true
              const message = videoError instanceof Error ? videoError.message : String(videoError)
              toast.error(`'${target.lectureTitle}' 영상 업로드 실패: ${message}`)
            }
          }
        }
        setUploadingId(null)
      }

      if (videoUploadFailed) {
        toast.warning('커리큘럼은 저장되었으나 일부 영상 업로드에 실패했습니다.')
      } else if (pendingUploads.length > 0) {
        toast.success('커리큘럼과 영상 업로드가 완료되었습니다.')
      } else {
        toast.success('커리큘럼이 저장되었습니다.')
      }
      await loadCurriculum(false, activeIndex, uploadedVideoNames)
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : String(error)
      toast.error(`커리큘럼 저장에 실패했습니다: ${message}`)
    } finally {
      setSaving(false)
    }
  }

  const totalLectures = chapters.reduce((acc, c) => acc + c.lectures.length, 0)

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {isReadOnly && (
        <div className="flex items-center gap-3 rounded-xl bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-900/50 p-4 text-sm text-amber-800 dark:text-amber-300">
          <AlertTriangle className="size-5 shrink-0 text-amber-600 dark:text-amber-400" />
          <div>
            <p className="font-semibold text-[13px]">공개 판매 중인 강좌 알림</p>
            <p className="text-xs text-muted-foreground mt-0.5">
              이미 판매 중인 강좌는 수강생 보호를 위해 챕터 및 강의의 추가/삭제가 제한됩니다. 
              기존 강의의 영상 교체나 텍스트 수정은 아래의 강의 상세 항목에서 정상적으로 수정하여 저장하실 수 있습니다.
            </p>
          </div>
        </div>
      )}
      <div className="grid gap-6 lg:grid-cols-[300px_1fr]">
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
            disabled={isReadOnly}
            className="w-full gap-1.5"
          >
            <Plus className="size-4" /> 새 챕터 추가
          </Button>

          <Button
            type="button"
            onClick={handleSave}
            disabled={saving}
            className="mt-6 w-full gap-1.5"
          >
            {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
            커리큘럼 저장
          </Button>
        </aside>

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
                  disabled={isReadOnly}
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
                        disabled={isReadOnly}
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
                        <div className="relative">
                          <input
                            type="file"
                            id={`video-input-${lecture.id}`}
                            accept="video/*"
                            className="hidden"
                            onChange={(e) => {
                              const file = e.target.files?.[0]
                              if (file) handleVideoSelect(lecture.id, file)
                            }}
                            disabled={saving || uploadingId !== null}
                          />
                          <button
                            type="button"
                            onClick={() => {
                              document.getElementById(`video-input-${lecture.id}`)?.click()
                            }}
                            disabled={saving || uploadingId !== null}
                            className="flex flex-col items-center justify-center gap-1 rounded-lg border border-dashed py-6 text-muted-foreground transition-colors hover:bg-secondary w-full"
                          >
                            {uploadingId === lecture.id ? (
                              <span className="flex items-center gap-1.5 text-sm font-medium text-primary">
                                <Loader2 className="size-4 animate-spin" />
                                업로드 중...
                              </span>
                            ) : (
                              <>
                                <span className="flex items-center gap-1.5 text-sm font-medium">
                                  <FileVideo className="size-4" />
                                  {lecture.videoName ??
                                    (lecture.hasVideo ? '업로드된 영상' : '영상 파일 업로드')}
                                </span>
                                <span className="text-[11px]">MP4, MOV 지원 · 최대 2GB · 언제든 파일 미리 대기 가능</span>
                              </>
                            )}
                          </button>
                        </div>
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

                      <div className="flex items-center gap-2 py-1">
                        <Checkbox
                          id={`free-preview-${lecture.id}`}
                          checked={lecture.isFreePreview ?? false}
                          onCheckedChange={(checked) =>
                            updateLecture(active.id, lecture.id, { isFreePreview: !!checked })
                          }
                        />
                        <Label
                          htmlFor={`free-preview-${lecture.id}`}
                          className="text-sm font-medium leading-none cursor-pointer"
                        >
                          무료 미리보기 강의로 설정
                        </Label>
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
                disabled={isReadOnly}
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
    </div>
  )
}
