// frontend/components/instructor/product-manager.tsx
'use client'

import { useEffect, useMemo, useState } from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { Pencil, Plus, Trash2, Ban, RotateCcw, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { formatKRW } from '@/lib/utils'
import type { CategoryResponse, Course } from '@/lib/types'
import { useRouter } from 'next/navigation'
import { api } from '@/lib/api'
import { toast } from 'sonner'

const statusLabel: Record<string, { label: string; variant: 'default' | 'secondary' | 'outline' }> = {
  PUBLISHED: { label: '게시중', variant: 'default' },
  REVIEW: { label: '승인 대기', variant: 'secondary' },
  ON_SALE: { label: '게시중', variant: 'default' },
  IN_REVIEW: { label: '승인 대기', variant: 'secondary' },
  DRAFT: { label: '임시저장', variant: 'outline' },
  CLOSED: { label: '비공개', variant: 'outline' },
  SUSPENDED: { label: '판매 중지', variant: 'outline' },
  DELETED: { label: '삭제됨', variant: 'outline' },
}

export function ProductManager({ courses }: { courses: Course[] }) {
  const [loadingId, setLoadingId] = useState<string | null>(null)
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const router = useRouter()

  useEffect(() => {
    api.getCategories()
      .then(setCategories)
      .catch(() => setCategories([]))
  }, [])

  const categoryLabelById = useMemo(() => {
    const byId = new Map(categories.map((category) => [String(category.id), category]))
    return Object.fromEntries(categories.map((category) => {
      const parent = category.parentCategoryId ? byId.get(String(category.parentCategoryId)) : null
      const label = parent ? `${parent.name} > ${category.name}` : category.name
      return [String(category.id), label]
    }))
  }, [categories])

  const handleDelete = async (courseId: string) => {
    if (!confirm('정말 이 강의를 삭제하시겠습니까?')) return
    setLoadingId(courseId)
    try {
      await api.deleteCourse(courseId)
      toast.success('강의가 성공적으로 삭제되었습니다.')
      router.refresh()
    } catch (e: any) {
      const message = e.message || String(e)
      toast.error(message.includes('수강생이 존재') ? '수강생이 존재하므로 삭제할 수 없습니다' : `강의 삭제 실패: ${message}`)
    } finally {
      setLoadingId(null)
    }
  }

  const handleCancelReview = async (courseId: string) => {
    if (!confirm('심사 요청을 취소하시겠습니까?')) return
    setLoadingId(courseId)
    try {
      await api.cancelCourseReview(courseId)
      toast.success('심사 요청이 취소되었습니다.')
      router.refresh()
    } catch (e: any) {
      toast.error(`심사 취소 실패: ${e.message || e}`)
    } finally {
      setLoadingId(null)
    }
  }

  const handleCloseCourse = async (courseId: string) => {
    if (!confirm('정말 이 강의의 판매를 종료하시겠습니까?')) return
    setLoadingId(courseId)
    try {
      await api.closeCourse(courseId)
      toast.success('강의 판매가 종료되었습니다.')
      router.refresh()
    } catch (e: any) {
      toast.error(`판매 종료 실패: ${e.message || e}`)
    } finally {
      setLoadingId(null)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">내가 등록한 강의 관리</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            강의를 등록하고 승인 상태를 관리하세요.
          </p>
        </div>
        <Button asChild>
          <Link href="/instructor/courses/new">
            <Plus className="mr-1 h-4 w-4" /> 새 강의 등록
          </Link>
        </Button>
      </div>

      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {courses.map((c) => {
          const status = statusLabel[c.status ?? 'DRAFT'] || statusLabel['DRAFT']
          const isInReview = c.status === 'IN_REVIEW' || c.status === 'REVIEW'
          const isOnSale = c.status === 'ON_SALE' || c.status === 'PUBLISHED'

          return (
            <li key={c.id} className="overflow-hidden rounded-xl border bg-card flex flex-col justify-between">
              <div>
                <div className="relative aspect-video bg-muted">
                  <Image
                    src={c.thumbnailUrl || '/placeholder.svg'}
                    alt={`${c.title} 표지`}
                    fill
                    sizes="(min-width:1024px) 320px, 100vw"
                    className="object-cover"
                  />
                  <Badge
                    variant={status.variant}
                    className="absolute left-2 top-2"
                  >
                    {status.label}
                  </Badge>
                </div>
                <div className="p-4 space-y-1">
                  <p className="line-clamp-2 text-sm font-semibold h-10">{c.title}</p>
                  <p className="text-xs text-muted-foreground">
                    카테고리: {categoryLabelById[c.category] ?? c.category}
                  </p>
                  <p className="text-sm font-bold">{formatKRW(c.price)}</p>
                  {c.status === 'DRAFT' && c.statusReason && (
                    <p className="rounded-md bg-destructive/10 px-2 py-1 text-xs text-destructive">
                      심사 반려 사유: {c.statusReason}
                    </p>
                  )}
                  {c.status === 'SUSPENDED' && c.statusReason && (
                    <p className="rounded-md bg-destructive/10 px-2 py-1 text-xs text-destructive">
                      판매 중지 사유: {c.statusReason}
                    </p>
                  )}
                </div>
              </div>

              <div className="p-4 pt-0 space-y-2">
                <div className="flex gap-2">
                  <Button
                    asChild={!isInReview}
                    size="sm"
                    variant="outline"
                    className="flex-1"
                    disabled={isInReview}
                  >
                    {isInReview ? (
                      <span><Pencil className="mr-1 h-3.5 w-3.5" /> 수정 불가</span>
                    ) : (
                      <Link href={`/instructor/courses/${c.id}`}>
                        <Pencil className="mr-1 h-3.5 w-3.5" /> 수정하기
                      </Link>
                    )}
                  </Button>
                  <Button
                    asChild={!isInReview}
                    size="sm"
                    variant="ghost"
                    className="flex-1"
                    disabled={isInReview}
                  >
                    {isInReview ? (
                      <span>커리큘럼</span>
                    ) : (
                      <Link href={`/instructor/courses/${c.id}/curriculum`}>
                        커리큘럼
                      </Link>
                    )}
                  </Button>
                </div>

                <div className="flex gap-2">
                  {(c.status === 'DRAFT' || c.status === 'CLOSED' || c.status === 'SUSPENDED' || c.status === 'DELETED') && (
                    <Button
                      size="sm"
                      variant="destructive"
                      className="w-full gap-1"
                      disabled={loadingId !== null}
                      onClick={() => handleDelete(c.id)}
                    >
                      {loadingId === c.id ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Trash2 className="h-3.5 w-3.5" />}
                      강의 삭제
                    </Button>
                  )}

                  {isInReview && (
                    <Button
                      size="sm"
                      variant="outline"
                      className="w-full gap-1 border-destructive text-destructive hover:bg-destructive/10"
                      disabled={loadingId !== null}
                      onClick={() => handleCancelReview(c.id)}
                    >
                      {loadingId === c.id ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <RotateCcw className="h-3.5 w-3.5" />}
                      심사 취소
                    </Button>
                  )}

                  {isOnSale && (
                    <Button
                      size="sm"
                      variant="outline"
                      className="w-full gap-1 border-amber-600 text-amber-600 hover:bg-amber-500/10"
                      disabled={loadingId !== null}
                      onClick={() => handleCloseCourse(c.id)}
                    >
                      {loadingId === c.id ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Ban className="h-3.5 w-3.5" />}
                      판매 종료
                    </Button>
                  )}
                </div>
              </div>
            </li>
          )
        })}
      </ul>

      {courses.length === 0 && (
        <div className="rounded-xl border border-dashed py-16 text-center text-sm text-muted-foreground">
          등록한 강의가 없습니다. 첫 강의를 등록해보세요!
        </div>
      )}
    </div>
  )
}
