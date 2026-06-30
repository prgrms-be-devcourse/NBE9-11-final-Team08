// frontend/components/instructor/course-form.tsx
'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useEffect, useState, useRef } from 'react'
import { ArrowLeft, ImagePlus, ListChecks, Sparkles, X, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { api } from '@/lib/api'
import type { CategoryResponse, Chapter, Course } from '@/lib/types'

const allowedCourseCategoryNames = ['DevOps', '백엔드', '마케팅', '프론트엔드', 'UI/UX']
const categoryId = (category: CategoryResponse) => String(category.id)

const buildChapterUpdatePayload = (chapters: Chapter[]) =>
  chapters.map((chapter, chapterIndex) => ({
    id: Number(chapter.id),
    title: chapter.title,
    orderNo: chapterIndex + 1,
    lectures: chapter.lectures.map((lecture, lectureIndex) => ({
      id: Number(lecture.id),
      title: lecture.title,
      summary: lecture.summary ?? '',
      durationSeconds: Number(lecture.durationSeconds) > 0 ? Number(lecture.durationSeconds) : 1,
      orderNo: lectureIndex + 1,
      isFreePreview: lecture.isFreePreview || false,
    })),
  }))

export function CourseForm({ course }: { course?: Course }) {
  const router = useRouter()
  const editing = Boolean(course)

  const [title, setTitle] = useState(course?.title ?? '')
  const [mainCat, setMainCat] = useState(course?.category ?? '')
  const [price, setPrice] = useState(course ? String(course.price) : '')
  const [description, setDescription] = useState(course?.description ?? '')
  const [tags, setTags] = useState<string[]>(course?.tags ?? [])
  const [tagInput, setTagInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [categories, setCategories] = useState<CategoryResponse[]>([])

  const fileInputRef = useRef<HTMLInputElement>(null)
  const [thumbnailFile, setThumbnailFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string>(course?.thumbnailUrl || '')
  const [courseChapters, setCourseChapters] = useState<Chapter[]>(course?.chapters ?? [])

  const isInReview = course && (course.status === 'IN_REVIEW' || course.status === 'REVIEW')
  const isOnSale = course && (course.status === 'ON_SALE' || course.status === 'PUBLISHED')
  const isDraft = !course || course.status === 'DRAFT'
  const isReadOnly = isInReview

  useEffect(() => {
    let active = true
    api.getCategories()
      .then((response) => {
        if (!active) return
        setCategories(response)
      })
      .catch(() => setCategories([]))
    return () => {
      active = false
    }
  }, [course?.category])

  const mainCategories = categories.filter((category) => allowedCourseCategoryNames.includes(category.name))

  useEffect(() => {
    if (!course?.id) return
    let active = true
    api.getInstructorCourse(course.id).then((fresh) => {
      if (!active || !fresh?.chapters?.length) return
      setCourseChapters(fresh.chapters)
    })
    return () => {
      active = false
    }
  }, [course?.id])

  const addTag = () => {
    const v = tagInput.trim()
    if (v && !tags.includes(v)) setTags((p) => [...p, v])
    setTagInput('')
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      if (file.size > 10 * 1024 * 1024) {
        toast.error('파일 크기는 최대 10MB를 초과할 수 없습니다.')
        return
      }
      setThumbnailFile(file)
      setPreviewUrl(URL.createObjectURL(file))
    }
  }

  const handleDelete = async () => {
    if (!course || !confirm('정말 이 강의를 삭제하시겠습니까?')) return
    setLoading(true)
    try {
      await api.deleteCourse(course.id)
      toast.success('강의가 성공적으로 삭제되었습니다.')
      router.push('/instructor')
    } catch (error: any) {
      const message = error.message || String(error)
      toast.error(message.includes('수강생이 존재') ? '수강생이 존재하므로 삭제할 수 없습니다' : `강의 삭제 실패: ${message}`)
    } finally {
      setLoading(false)
    }
  }

  const handleCancelReview = async () => {
    if (!course || !confirm('심사 요청을 취소하시겠습니까?')) return
    setLoading(true)
    try {
      await api.cancelCourseReview(course.id)
      toast.success('심사 요청이 취소되었습니다.')
      router.refresh()
    } catch (error: any) {
      toast.error(`심사 취소 실패: ${error.message || error}`)
    } finally {
      setLoading(false)
    }
  }

  const handleCloseCourse = async () => {
    if (!course || !confirm('정말 이 강의의 판매를 종료하시겠습니까?')) return
    setLoading(true)
    try {
      await api.closeCourse(course.id)
      toast.success('강의 판매가 종료되었습니다.')
      router.refresh()
    } catch (error: any) {
      toast.error(`판매 종료 실패: ${error.message || error}`)
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (status: 'DRAFT' | 'REVIEW') => {
    const selectedCategoryId = mainCat
    const selectedCategoryNumber = Number(selectedCategoryId)
    if (!title.trim() || !price || !selectedCategoryId || !Number.isFinite(selectedCategoryNumber)) {
      toast.error('필수 항목(제목, 가격, 대분류)을 입력해주세요.')
      return
    }

    setLoading(true)
    try {
      if (editing && course) {
        const freshCourse = await api.getInstructorCourse(course.id)
        const chaptersForUpdate =
          freshCourse?.chapters?.length ? freshCourse.chapters : courseChapters

        if (freshCourse?.chapters?.length) {
          setCourseChapters(freshCourse.chapters)
        }

        const hasCurriculum =
          chaptersForUpdate.length > 0 &&
          chaptersForUpdate.every((chapter) => chapter.lectures.length > 0)

        if (status === 'REVIEW' && !hasCurriculum) {
          toast.error('커리큘럼을 먼저 등록해주세요. 챕터와 강의를 각각 1개 이상 추가해야 합니다.')
          router.push(`/instructor/courses/${course.id}/curriculum`)
          return
        }

        const formData = new FormData()

        const requestData = {
          title,
          description,
          categoryId: selectedCategoryNumber,
          price: Number(price),
          thumbnail: previewUrl || 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?q=80&w=800&auto=format&fit=crop',
          chapters: buildChapterUpdatePayload(chaptersForUpdate),
        }

        const requestBlob = new Blob(
          [JSON.stringify(requestData)],
          { type: 'application/json' }
        )

        formData.append('request', requestBlob)

        if (thumbnailFile) {
          formData.append('thumbnail', thumbnailFile)
        }

        // 💡 기존 객체 전송 방식에서 FormData 파이프라인 구조로 전환 호출
        await api.updateCourse(course.id, formData)

        if (status === 'REVIEW') {
          await api.requestCourseReview(course.id)
          toast.success('검수 요청이 접수되었습니다.')
          router.push('/instructor')
        } else {
          toast.success('강의가 수정되었습니다.')
          router.refresh()
        }
      } else {
        const formData = new FormData()

        const requestData = {
          title,
          description,
          categoryId: selectedCategoryNumber,
          price: Number(price),
          thumbnail: previewUrl || 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?q=80&w=800&auto=format&fit=crop',
        }

        const requestBlob = new Blob(
          [JSON.stringify(requestData)],
          { type: 'application/json' }
        )

        formData.append('request', requestBlob)

        if (thumbnailFile) {
          formData.append('thumbnail', thumbnailFile)
        }

        const courseId = await api.createCourse(formData)
        if (status === 'REVIEW') {
          toast.success('강의 기본정보가 저장되었습니다. 커리큘럼 등록 후 검수 요청해주세요.')
          router.push(`/instructor/courses/${courseId}/curriculum`)
        } else {
          toast.success('임시저장되었습니다.')
          router.push(`/instructor/courses/${courseId}`)
        }
      }
    } catch (error: any) {
      console.error('강의 저장 실패:', error)
      toast.error(`강의 저장에 실패했습니다: ${error.message || error}`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <Button asChild variant="ghost" size="icon" aria-label="목록으로">
          <Link href="/instructor">
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <h1 className="text-2xl font-bold">
          {editing ? '강의 수정' : '새 강의 등록'}
        </h1>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <section className="space-y-4 rounded-xl border bg-card p-5">
            <div className="grid gap-2">
              <Label htmlFor="title">강의 제목</Label>
              <Input
                id="title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="예: 자바 ORM 표준기술 JPA for beginner"
                disabled={isReadOnly || loading}
              />
            </div>

            <div className="grid gap-2">
              <div className="grid gap-2">
                <Label>대분류</Label>
                <Select value={mainCat} onValueChange={(value) => setMainCat(value ?? '')} disabled={isReadOnly || loading}>
                  <SelectTrigger>
                    <SelectValue placeholder="대분류 선택" />
                  </SelectTrigger>
                  <SelectContent>
                    {mainCategories.map((c) => (
                      <SelectItem key={c.id} value={categoryId(c)}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="price">가격 (KRW)</Label>
              <Input
                id="price"
                type="number"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                placeholder="0"
                disabled={isReadOnly || loading}
              />
            </div>
          </section>

          <section className="space-y-3 rounded-xl border bg-card p-5">
            <div className="flex items-center justify-between">
              <Label htmlFor="desc">상세 설명</Label>
            </div>
            <Textarea
              id="desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="강의 소개, 학습 목표, 수강 대상 등을 작성하세요."
              className="min-h-44"
              disabled={isReadOnly || loading}
            />
            <Separator />
            {course ? (
              <Button asChild variant="outline" className="w-full" disabled={isReadOnly || loading}>
                <Link href={`/instructor/courses/${course.id}/curriculum`}>
                  <ListChecks className="mr-1 h-4 w-4" /> 커리큘럼 등록 / 수정
                </Link>
              </Button>
            ) : (
              <Button
                type="button"
                variant="outline"
                className="w-full"
                onClick={() => toast.info('강좌 기본 정보를 먼저 임시저장한 뒤 커리큘럼을 등록해주세요.')}
                disabled={isReadOnly || loading}
              >
                <ListChecks className="mr-1 h-4 w-4" /> 커리큘럼 등록 / 수정
              </Button>
            )}
          </section>
        </div>

        {/* Side: cover + actions */}
        <div className="space-y-6">
          <section className="rounded-xl border bg-card p-5">
            <Label>커버 이미지</Label>
            <input
              type="file"
              ref={fileInputRef}
              className="hidden"
              accept="image/jpeg, image/png"
              onChange={handleFileChange}
              disabled={isReadOnly || loading}
            />
            <button
              type="button"
              onClick={() => {
                if (isReadOnly) return
                fileInputRef.current?.click()
              }}
              disabled={isReadOnly || loading}
              className="mt-2 flex aspect-video w-full flex-col items-center justify-center gap-2 rounded-lg border border-dashed text-muted-foreground transition-colors hover:bg-secondary disabled:opacity-50 disabled:cursor-not-allowed overflow-hidden relative bg-muted"
            >
              {previewUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={previewUrl} alt="Thumbnail preview" className="w-full h-full object-cover" />
              ) : (
                <>
                  <ImagePlus className="h-7 w-7" />
                  <span className="text-xs">
                    파일을 드래그하거나 클릭해 업로드
                  </span>
                  <span className="text-[11px]">최대 10MB · JPG, PNG</span>
                </>
              )}
            </button>
          </section>

          <section className="space-y-2 rounded-xl border bg-card p-5">
            {isInReview ? (
              <div className="space-y-2">
                <p className="text-xs text-muted-foreground text-center">
                  현재 심사 대기 중입니다. 정보를 수정하려면 심사를 취소해 주세요.
                </p>
                <Button
                  type="button"
                  variant="outline"
                  className="w-full gap-1 border-destructive text-destructive hover:bg-destructive/10"
                  disabled={loading}
                  onClick={handleCancelReview}
                >
                  {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  심사 취소
                </Button>
              </div>
            ) : (
              <>
                <Button
                  className="w-full"
                  onClick={() => handleSubmit('REVIEW')}
                  disabled={loading}
                >
                  {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  강의 신청
                </Button>
                <Button
                  variant="outline"
                  className="w-full"
                  onClick={() => handleSubmit('DRAFT')}
                  disabled={loading}
                >
                  {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  {editing ? '수정 완료' : '임시저장'}
                </Button>

                {isOnSale && (
                  <Button
                    type="button"
                    variant="outline"
                    className="w-full gap-1 border-amber-600 text-amber-600 hover:bg-amber-500/10"
                    disabled={loading}
                    onClick={handleCloseCourse}
                  >
                    {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                    판매 종료
                  </Button>
                )}

                {isDraft && editing && (
                  <Button
                    type="button"
                    variant="destructive"
                    className="w-full gap-1"
                    disabled={loading}
                    onClick={handleDelete}
                  >
                    {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                    강의 삭제
                  </Button>
                )}
              </>
            )}
          </section>
        </div>
      </div>
    </div>
  )
}
