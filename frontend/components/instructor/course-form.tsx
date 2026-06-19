// frontend/components/instructor/course-form.tsx
'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
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
import type { Course } from '@/lib/types'

const subCategories = ['백엔드', '프론트엔드', '데브옵스', '프로그래밍 언어', '업무 생산성']

export function CourseForm({ course }: { course?: Course }) {
  const router = useRouter()
  const editing = Boolean(course)
  
  const [title, setTitle] = useState(course?.title ?? '')
  const [mainCat, setMainCat] = useState(course?.category ?? '')
  const [subCat, setSubCat] = useState(course?.subCategory ?? '')
  const [price, setPrice] = useState(course ? String(course.price) : '')
  const [description, setDescription] = useState(course?.description ?? '')
  const [tags, setTags] = useState<string[]>(course?.tags ?? [])
  const [tagInput, setTagInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [categoryOptions, setCategoryOptions] = useState<string[]>(
    course?.category ? [course.category] : [],
  )

  const isInReview = course && (course.status === 'IN_REVIEW' || course.status === 'REVIEW')
  const isOnSale = course && (course.status === 'ON_SALE' || course.status === 'PUBLISHED')
  const isDraft = !course || course.status === 'DRAFT'
  const isReadOnly = isInReview

  useEffect(() => {
    let active = true
    api.getCourses(0, 100)
      .then((response) => {
        if (!active) return
        const next = Array.from(
          new Set(
            response.content
              .map((item) => item.category)
              .filter((value): value is string => Boolean(value)),
          ),
        )
        setCategoryOptions(
          Array.from(new Set([course?.category, ...next].filter(Boolean) as string[])),
        )
      })
      .catch(() => {
        if (course?.category) setCategoryOptions([course.category])
      })
    return () => {
      active = false
    }
  }, [course?.category])

  const addTag = () => {
    const v = tagInput.trim()
    if (v && !tags.includes(v)) setTags((p) => [...p, v])
    setTagInput('')
  }

  const handleDelete = async () => {
    if (!course || !confirm('정말 이 강의를 삭제하시겠습니까?')) return
    setLoading(true)
    try {
      await api.deleteCourse(course.id)
      toast.success('강의가 성공적으로 삭제되었습니다.')
      router.push('/instructor')
    } catch (error: any) {
      toast.error(`강의 삭제 실패: ${error.message || error}`)
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
    const categoryId = Number(mainCat)
    if (!title.trim() || !price || !Number.isFinite(categoryId)) {
      toast.error('필수 항목(제목, 가격, 대분류)을 입력해주세요.')
      return
    }

    setLoading(true)
    try {
      const payload = {
        title,
        description,
        categoryId,
        price: Number(price),
        thumbnail: course?.thumbnailUrl || 'https://via.placeholder.com/800x450',
      }

      if (editing && course) {
        await api.updateCourse(course.id, {
          ...payload,
          chapters: course.chapters.map((chapter, chapterIndex) => ({
            id: Number(chapter.id),
            title: chapter.title,
            orderNo: chapterIndex + 1,
            lectures: chapter.lectures.map((lecture, lectureIndex) => ({
              id: Number(lecture.id),
              title: lecture.title,
              durationSeconds: 0,
              orderNo: lectureIndex + 1,
              isFreePreview: false,
            })),
          })),
        })
        if (status === 'REVIEW') {
          await api.requestCourseReview(course.id)
          toast.success('검수 요청이 접수되었습니다.')
          router.push('/instructor')
        } else {
          toast.success('강의가 수정되었습니다.')
          router.refresh()
        }
      } else {
        const courseId = await api.createCourse(payload)
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

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label>대분류</Label>
                <Select value={mainCat} onValueChange={(value) => setMainCat(value ?? '')} disabled={isReadOnly || loading}>
                  <SelectTrigger>
                    <SelectValue placeholder="대분류 선택" />
                  </SelectTrigger>
                  <SelectContent>
                    {categoryOptions.map((c) => (
                        <SelectItem key={c} value={c}>
                          카테고리 {c}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label>소분류</Label>
                <Select value={subCat} onValueChange={(value) => setSubCat(value ?? '')} disabled={isReadOnly || loading}>
                  <SelectTrigger>
                    <SelectValue placeholder="소분류 선택" />
                  </SelectTrigger>
                  <SelectContent>
                    {subCategories.map((c) => (
                      <SelectItem key={c} value={c}>
                        {c}
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

            <div className="grid gap-2">
              <Label htmlFor="tags">태그 / 키워드</Label>
              <div className="flex flex-wrap gap-2">
                {tags.map((t) => (
                  <Badge key={t} variant="secondary" className="gap-1">
                    {t}
                    <button
                      type="button"
                      onClick={() => setTags((p) => p.filter((x) => x !== t))}
                      aria-label={`${t} 삭제`}
                      disabled={isReadOnly || loading}
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </Badge>
                ))}
              </div>
              <div className="flex gap-2">
                <Input
                  id="tags"
                  value={tagInput}
                  onChange={(e) => setTagInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault()
                      addTag()
                    }
                  }}
                  placeholder="태그 입력 후 Enter"
                  disabled={isReadOnly || loading}
                />
                <Button type="button" variant="outline" onClick={addTag} disabled={isReadOnly || loading}>
                  추가
                </Button>
              </div>
            </div>
          </section>

          <section className="space-y-3 rounded-xl border bg-card p-5">
            <div className="flex items-center justify-between">
              <Label htmlFor="desc">상세 설명</Label>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="text-xs text-muted-foreground"
                onClick={() => toast.info('AI 설명 생성 기능은 곧 제공될 예정입니다.')}
                disabled={isReadOnly || loading}
              >
                <Sparkles className="mr-1 h-3.5 w-3.5" /> AI로 생성
              </Button>
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
            <button
              type="button"
              onClick={() => {
                if (isReadOnly) return
                toast.info('이미지 업로드는 데모에서 비활성화되어 있습니다.')
              }}
              disabled={isReadOnly || loading}
              className="mt-2 flex aspect-video w-full flex-col items-center justify-center gap-2 rounded-lg border border-dashed text-muted-foreground transition-colors hover:bg-secondary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ImagePlus className="h-7 w-7" />
              <span className="text-xs">
                파일을 드래그하거나 클릭해 업로드
              </span>
              <span className="text-[11px]">최대 10MB · JPG, PNG</span>
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
