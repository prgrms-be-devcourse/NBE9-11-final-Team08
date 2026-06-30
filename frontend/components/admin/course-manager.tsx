// frontend/components/admin/course-manager.tsx
'use client'

import { useEffect, useMemo, useState } from 'react'
import {
  Check,
  XCircle,
  AlertTriangle,
  Trash2,
  Loader2,
  Search,
  MoreHorizontal,
  BookOpen,
  User,
} from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { formatKRW } from '@/lib/utils'
import type { Course } from '@/lib/types'
import { api } from '@/lib/api'

const statusMeta: Record<string, { label: string; variant: 'default' | 'secondary' | 'outline' | 'destructive' }> = {
  ON_SALE: { label: '판매 중', variant: 'default' },
  PUBLISHED: { label: '판매 중', variant: 'default' },
  IN_REVIEW: { label: '승인 대기', variant: 'secondary' },
  REVIEW: { label: '승인 대기', variant: 'secondary' },
  DRAFT: { label: '임시저장', variant: 'outline' },
  CLOSED: { label: '판매 종료', variant: 'outline' },
  SUSPENDED: { label: '판매 중지', variant: 'destructive' },
  DELETED: { label: '삭제됨', variant: 'outline' },
}

export function CourseManager({ initialCourses }: { initialCourses: Course[] }) {
  const [courses, setCourses] = useState<Course[]>(initialCourses)
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | string>('ALL')

  const [confirmModal, setConfirmModal] = useState<{
    type: 'approve' | 'reject' | 'suspend' | 'delete'
    course: Course
  } | null>(null)

  const [reason, setReason] = useState('')
  const [processing, setProcessing] = useState(false)

  useEffect(() => {
    try {
      const draftIds = JSON.parse(localStorage.getItem('course_draft_ids') || '[]') as string[]
      const drafts = draftIds
        .map((id): Course | null => {
          const raw = localStorage.getItem(`course_draft_${id}`)
          if (!raw) return null
          const draft = JSON.parse(raw)
          return {
            id,
            title: draft.title || '',
            subtitle: '',
            description: draft.description || '',
            category: String(draft.categoryId || ''),
            subCategory: '',
            thumbnailUrl: draft.thumbnail || '/placeholder.svg',
            price: draft.price || 0,
            viewCount: 0,
            instructor: { id: String(draft.instructorId || ''), name: `강사 ${draft.instructorId || ''}`, title: '' },
            chapters: [],
            status: draft.status || 'DRAFT',
          }
        })
        .filter((course): course is Course => Boolean(course))

      setCourses((prev) => {
        const idSet = new Set(prev.map((c) => c.id.toString()))
        const newDrafts = drafts.filter((c) => !idSet.has(c.id.toString()))
        const merged = [...prev, ...newDrafts]

        // Seed mock courses if empty so admin console is interactive
        if (merged.length === 0) {
          return [
            {
              id: '101',
              title: '인공지능을 활용한 웹 서비스 개발 초급 과정',
              subtitle: '',
              description: 'AI API를 연동하여 완성도 높은 서비스를 만드는 실무 과정입니다.',
              category: 'IT/프로그래밍',
              subCategory: '웹 개발',
              thumbnailUrl: '/placeholder.svg',
              price: 99000,
              viewCount: 45,
              instructor: { id: '2', name: '김코딩 강사', title: '' },
              chapters: [],
              status: 'IN_REVIEW',
            },
            {
              id: '102',
              title: '실전 디자인 시스템 설계와 Tailwind CSS 마스터',
              subtitle: '',
              description: '디자인 토큰부터 컴포넌트 라이브러리 제작까지 다룹니다.',
              category: '크리에이티브',
              subCategory: 'UI/UX',
              thumbnailUrl: '/placeholder.svg',
              price: 49000,
              viewCount: 128,
              level: '중급',
              tags: ['Tailwind', 'Design System'],
              instructor: { id: '3', name: '이디자인 강사', title: '' },
              chapters: [],
              status: 'ON_SALE',
            },
            {
              id: '103',
              title: 'Spring Boot와 MSA 아키텍처 완전 정복',
              subtitle: '',
              description: '대규모 분산 환경을 위한 설계 기법과 클라우드 배포.',
              category: 'IT/프로그래밍',
              subCategory: '백엔드',
              thumbnailUrl: '/placeholder.svg',
              price: 150000,
              viewCount: 20,
              instructor: { id: '4', name: '박서버 강사', title: '' },
              chapters: [],
              status: 'SUSPENDED',
            }
          ]
        }
        return merged
      })
    } catch (e) {
      console.error('Failed to load local drafts:', e)
    }
  }, [initialCourses])

  const filtered = useMemo(() => {
    return courses.filter((c) => {
      const matchQuery = c.title.toLowerCase().includes(query.toLowerCase())
      const matchStatus =
        statusFilter === 'ALL' ||
        c.status === statusFilter ||
        (statusFilter === 'ON_SALE' && c.status === 'PUBLISHED') ||
        (statusFilter === 'IN_REVIEW' && c.status === 'REVIEW')
      return matchQuery && matchStatus
    })
  }, [courses, query, statusFilter])

  const handleAction = async () => {
    if (!confirmModal) return
    const { type, course } = confirmModal
    setProcessing(true)

    try {
      if (type === 'approve') {
        if (!course.id.toString().startsWith('10')) {
          await api.approveCourseByAdmin(course.id)
        }
        setCourses((prev) =>
          prev.map((c) => (c.id === course.id ? { ...c, status: 'ON_SALE' } : c))
        )
        toast.success(`"${course.title}" 강좌의 심사를 승인하여 판매 중(ON_SALE)으로 변경했습니다.`)
      } else if (type === 'reject') {
        if (!reason.trim()) {
          toast.error('반려 사유를 입력해주세요.')
          return
        }
        if (!course.id.toString().startsWith('10')) {
          await api.rejectCourseByAdmin(course.id, reason)
        }
        setCourses((prev) =>
          prev.map((c) => (c.id === course.id ? { ...c, status: 'DRAFT' } : c))
        )
        toast.success(`"${course.title}" 강좌의 심사 요청을 반려(DRAFT) 처리했습니다.`)
      } else if (type === 'suspend') {
        if (!reason.trim()) {
          toast.error('판매 중지 사유를 입력해주세요.')
          return
        }
        if (!course.id.toString().startsWith('10')) {
          await api.suspendCourseByAdmin(course.id, reason)
        }
        setCourses((prev) =>
          prev.map((c) => (c.id === course.id ? { ...c, status: 'SUSPENDED' } : c))
        )
        toast.success(`"${course.title}" 강좌를 강제 판매 중지(SUSPENDED) 처리했습니다.`)
      } else if (type === 'delete') {
        if (!course.id.toString().startsWith('10')) {
          await api.deleteCourseByAdmin(course.id)
        }
        setCourses((prev) => prev.filter((c) => c.id !== course.id))
        toast.success(`"${course.title}" 강좌를 삭제했습니다.`)
      }
      setConfirmModal(null)
      setReason('')
    } catch (e: any) {
      toast.error(`작업 실패: ${e.message || e}`)
    } finally {
      setProcessing(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">강좌 심사 및 관리</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            강사들이 제출한 강좌를 승인/반려하거나 판매 상태를 제어하세요.
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-3 rounded-xl border bg-card p-4 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="강좌명 검색"
            className="pl-9"
          />
        </div>
        <Select value={statusFilter} onValueChange={(v) => setStatusFilter(v || 'ALL')}>
          <SelectTrigger className="w-full sm:w-48">
            <SelectValue placeholder="전체 상태" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">전체 상태</SelectItem>
            <SelectItem value="DRAFT">임시저장 (DRAFT)</SelectItem>
            <SelectItem value="IN_REVIEW">승인 대기 (IN_REVIEW)</SelectItem>
            <SelectItem value="ON_SALE">판매 중 (ON_SALE)</SelectItem>
            <SelectItem value="SUSPENDED">판매 중지 (SUSPENDED)</SelectItem>
            <SelectItem value="CLOSED">판매 종료 (CLOSED)</SelectItem>
            <SelectItem value="DELETED">삭제됨 (DELETED)</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="rounded-xl border bg-card overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-16">ID</TableHead>
              <TableHead>강좌명</TableHead>
              <TableHead>강사</TableHead>
              <TableHead>분류</TableHead>
              <TableHead>가격</TableHead>
              <TableHead>상태</TableHead>
              <TableHead className="w-12 text-right">관리</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((c) => {
              const status = statusMeta[c.status ?? 'DRAFT'] || statusMeta['DRAFT']
              const isReviewable = c.status === 'IN_REVIEW' || c.status === 'REVIEW'
              const isSuspendedable = c.status === 'ON_SALE' || c.status === 'PUBLISHED'
              const isDeletable =
                c.status === 'DRAFT' ||
                c.status === 'CLOSED' ||
                c.status === 'SUSPENDED' ||
                c.status === 'DELETED'

              return (
                <TableRow key={c.id}>
                  <TableCell className="text-muted-foreground">{c.id}</TableCell>
                  <TableCell className="font-medium">
                    <div className="flex items-center gap-2">
                      <BookOpen className="h-4 w-4 text-muted-foreground shrink-0" />
                      <span className="line-clamp-1">{c.title}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1.5 text-sm">
                      <User className="h-3.5 w-3.5 text-muted-foreground" />
                      <span>{c.instructor?.name || `강사 ${c.instructor?.id}`}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">{c.category || '기타'}</Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground font-semibold">
                    {formatKRW(c.price)}
                  </TableCell>
                  <TableCell>
                    <Badge variant={status.variant}>{status.label}</Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-8 w-8 p-0">
                          <MoreHorizontal className="h-4 w-4" />
                          <span className="sr-only">관리 메뉴</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        {isReviewable && (
                          <>
                            <DropdownMenuItem
                              onClick={() => setConfirmModal({ type: 'approve', course: c })}
                              className="text-emerald-600 hover:text-emerald-700"
                            >
                              <Check className="mr-2 h-4 w-4" /> 심사 승인
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onClick={() => {
                                setReason('')
                                setConfirmModal({ type: 'reject', course: c })
                              }}
                              className="text-rose-600 hover:text-rose-700"
                            >
                              <XCircle className="mr-2 h-4 w-4" /> 심사 반려
                            </DropdownMenuItem>
                          </>
                        )}
                        {isSuspendedable && (
                          <DropdownMenuItem
                            onClick={() => {
                              setReason('')
                              setConfirmModal({ type: 'suspend', course: c })
                            }}
                            className="text-amber-600 hover:text-amber-700"
                          >
                            <AlertTriangle className="mr-2 h-4 w-4" /> 판매 중지
                          </DropdownMenuItem>
                        )}
                        {isDeletable && (
                          <>
                            {isReviewable || isSuspendedable ? <DropdownMenuSeparator /> : null}
                            <DropdownMenuItem
                              variant="destructive"
                              onClick={() => setConfirmModal({ type: 'delete', course: c })}
                            >
                              <Trash2 className="mr-2 h-4 w-4" /> 강좌 삭제
                            </DropdownMenuItem>
                          </>
                        )}
                        {!isReviewable && !isSuspendedable && !isDeletable && (
                          <DropdownMenuItem disabled>수행할 작업 없음</DropdownMenuItem>
                        )}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              )
            })}
            {filtered.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={7}
                  className="py-12 text-center text-sm text-muted-foreground"
                >
                  조건에 맞는 강좌가 없습니다.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {/* Confirmation for Approve / Delete */}
      <AlertDialog
        open={confirmModal !== null && (confirmModal.type === 'approve' || confirmModal.type === 'delete')}
        onOpenChange={(open) => !open && setConfirmModal(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {confirmModal?.type === 'approve' ? '강좌 심사 승인' : '강좌 삭제'}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {confirmModal?.type === 'approve'
                ? `"${confirmModal.course.title}" 강좌의 심사를 최종 승인하고 판매 중(ON_SALE) 상태로 변경하시겠습니까?`
                : `"${confirmModal?.course.title}" 강좌를 완전히 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={processing}>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleAction}
              disabled={processing}
              className={
                confirmModal?.type === 'delete'
                  ? 'bg-destructive text-white hover:bg-destructive/90'
                  : undefined
              }
            >
              {processing && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {confirmModal?.type === 'approve' ? '승인' : '삭제'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Input dialog for Rejection / Suspension Reasons */}
      <Dialog
        open={confirmModal !== null && (confirmModal.type === 'reject' || confirmModal.type === 'suspend')}
        onOpenChange={(open) => !open && setConfirmModal(null)}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              {confirmModal?.type === 'reject' ? '강좌 심사 반려' : '강좌 강제 판매 중지'}
            </DialogTitle>
            <DialogDescription>
              {confirmModal?.type === 'reject'
                ? `"${confirmModal.course.title}" 강좌 심사를 반려합니다. 반려 사유를 작성해주세요.`
                : `"${confirmModal?.course.title}" 강좌의 판매를 중지합니다. 중지 사유를 작성해주세요.`}
            </DialogDescription>
          </DialogHeader>
          <div className="py-2">
            <Textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="사유를 자세하게 적어주세요. (예: 강의 자료 불충분, 가이드라인 위반 등)"
              rows={4}
              disabled={processing}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmModal(null)} disabled={processing}>
              취소
            </Button>
            <Button
              variant={confirmModal?.type === 'reject' ? 'destructive' : 'default'}
              onClick={handleAction}
              disabled={processing || !reason.trim()}
            >
              {processing && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {confirmModal?.type === 'reject' ? '심사 반려' : '판매 중지'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
