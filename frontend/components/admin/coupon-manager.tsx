// frontend/components/admin/coupon-manager.tsx
'use client'

import { useMemo, useState } from 'react'
import { Eye, MoreHorizontal, Pencil, Plus, Search, StopCircle, Trash2, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
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
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { CouponFormSheet } from '@/components/admin/coupon-form-sheet'
import { CouponDetailDialog } from '@/components/admin/coupon-detail-dialog'
import { formatKRW } from '@/lib/utils'
import {
  couponStatusMeta,
  couponTargetLabel,
  couponTypeLabel,
} from '@/lib/coupon-labels'
import type { AdminCoupon, CouponPolicyType, CouponStatus } from '@/lib/types'
import { api } from '@/lib/api'

type ConfirmAction = { type: 'end' | 'delete'; coupon: AdminCoupon } | null

export function CouponManager({
  initialCoupons,
}: {
  initialCoupons: AdminCoupon[]
}) {
  const [coupons, setCoupons] = useState<AdminCoupon[]>(initialCoupons)
  const [query, setQuery] = useState('')
  const [typeFilter, setTypeFilter] = useState<'ALL' | CouponPolicyType>('ALL')
  const [statusFilter, setStatusFilter] = useState<'ALL' | CouponStatus>('ALL')

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<AdminCoupon | null>(null)
  const [detail, setDetail] = useState<AdminCoupon | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [confirm, setConfirm] = useState<ConfirmAction>(null)
  const [processing, setProcessing] = useState(false)

  const filtered = useMemo(
    () =>
      coupons.filter((c) => {
        const matchQuery = c.name.toLowerCase().includes(query.toLowerCase())
        const matchType = typeFilter === 'ALL' || c.type === typeFilter
        const matchStatus = statusFilter === 'ALL' || c.status === statusFilter
        return matchQuery && matchType && matchStatus
      }),
    [coupons, query, typeFilter, statusFilter],
  )

  const openCreate = () => {
    setEditing(null)
    setFormOpen(true)
  }

  const openEdit = (coupon: AdminCoupon) => {
    setEditing(coupon)
    setFormOpen(true)
  }

  const openDetail = (coupon: AdminCoupon) => {
    setDetail(coupon)
    setDetailOpen(true)
  }

  const handleSave = async (coupon: AdminCoupon) => {
    const saved = editing
      ? await api.updateAdminCoupon(coupon.id, coupon)
      : await api.createAdminCoupon(coupon)

    setCoupons((prev) => {
      const exists = prev.some((c) => c.id === saved.id)
      return exists
        ? prev.map((c) => (c.id === saved.id ? saved : c))
        : [saved, ...prev]
    })
  }

  const handleConfirm = async () => {
    if (!confirm) return
    setProcessing(true)
    try {
      if (confirm.type === 'delete') {
        await api.deleteAdminCoupon(confirm.coupon.id)
        setCoupons((prev) => prev.filter((c) => c.id !== confirm.coupon.id))
        toast.success('쿠폰이 삭제되었습니다.')
      } else {
        await api.terminateAdminCoupon(confirm.coupon.id)
        setCoupons((prev) =>
          prev.map((c) =>
            c.id === confirm.coupon.id ? { ...c, status: 'ENDED' } : c,
          ),
        )
        toast.success('쿠폰이 조기 종료되었습니다.')
      }
    } catch (error) {
      toast.error('작업 처리에 실패했습니다.')
    } finally {
      setProcessing(false)
      setConfirm(null)
    }
  }

  const discountText = (c: AdminCoupon) =>
    c.discountType === 'RATE'
      ? `${c.discountValue}%${c.maxDiscount ? ` (최대 ${formatKRW(c.maxDiscount)})` : ''}`
      : formatKRW(c.discountValue)

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">쿠폰 정책 관리</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            쿠폰을 생성하고 발급 상태를 관리하세요.
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-1 h-4 w-4" /> 쿠폰 생성
        </Button>
      </div>

      <div className="flex flex-col gap-3 rounded-xl border bg-card p-4 sm:flex-row sm:items-center">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="쿠폰명 검색"
            className="pl-9"
          />
        </div>
        <Select
          value={typeFilter}
          onValueChange={(v) => setTypeFilter(v as 'ALL' | CouponPolicyType)}
        >
          <SelectTrigger className="w-full sm:w-40">
            <SelectValue>
              {(v: string) =>
                v === 'ALL' ? '전체 타입' : couponTypeLabel[v as CouponPolicyType]
              }
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">전체 타입</SelectItem>
            <SelectItem value="NORMAL">일반</SelectItem>
            <SelectItem value="FCFS">선착순</SelectItem>
            <SelectItem value="AUTO">자동</SelectItem>
          </SelectContent>
        </Select>
        <Select
          value={statusFilter}
          onValueChange={(v) => setStatusFilter(v as 'ALL' | CouponStatus)}
        >
          <SelectTrigger className="w-full sm:w-40">
            <SelectValue>
              {(v: string) =>
                v === 'ALL' ? '전체 상태' : couponStatusMeta[v as CouponStatus].label
              }
            </SelectValue>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">전체 상태</SelectItem>
            <SelectItem value="SCHEDULED">발급 예정</SelectItem>
            <SelectItem value="ACTIVE">진행 중</SelectItem>
            <SelectItem value="ENDED">종료됨</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="rounded-xl border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-16">ID</TableHead>
              <TableHead>쿠폰명</TableHead>
              <TableHead>적용 대상</TableHead>
              <TableHead>쿠폰 타입</TableHead>
              <TableHead>할인 상세</TableHead>
              <TableHead>상태</TableHead>
              <TableHead className="w-12 text-right">관리</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((c) => {
              const status = couponStatusMeta[c.status] || couponStatusMeta['ACTIVE']
              return (
                <TableRow key={c.id}>
                  <TableCell className="text-muted-foreground">{c.id}</TableCell>
                  <TableCell className="font-medium">{c.name}</TableCell>
                  <TableCell>{couponTargetLabel[c.target]}</TableCell>
                  <TableCell>
                    <Badge variant="outline">{couponTypeLabel[c.type]}</Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {discountText(c)}
                  </TableCell>
                  <TableCell>
                    <Badge variant={status.variant}>{status.label}</Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger
                        render={
                          <Button variant="ghost" size="icon-sm">
                            <MoreHorizontal className="h-4 w-4" />
                            <span className="sr-only">관리 메뉴</span>
                          </Button>
                        }
                      />
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => openDetail(c)}>
                          <Eye className="h-4 w-4" /> 상세보기
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => openEdit(c)}>
                          <Pencil className="h-4 w-4" /> 수정
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          disabled={c.status === 'ENDED'}
                          onClick={() => setConfirm({ type: 'end', coupon: c })}
                        >
                          <StopCircle className="h-4 w-4" /> 조기 종료
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem
                          variant="destructive"
                          onClick={() => setConfirm({ type: 'delete', coupon: c })}
                        >
                          <Trash2 className="h-4 w-4" /> 삭제
                        </DropdownMenuItem>
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
                  조건에 맞는 쿠폰이 없습니다.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      <CouponFormSheet
        open={formOpen}
        onOpenChange={setFormOpen}
        coupon={editing}
        onSave={handleSave}
      />

      <CouponDetailDialog
        open={detailOpen}
        onOpenChange={setDetailOpen}
        coupon={detail}
      />

      <AlertDialog
        open={confirm !== null}
        onOpenChange={(open) => !open && setConfirm(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {confirm?.type === 'delete' ? '쿠폰 삭제' : '쿠폰 조기 종료'}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {confirm?.type === 'delete'
                ? `"${confirm?.coupon.name}" 쿠폰을 삭제합니다. 이 작업은 되돌릴 수 없습니다.`
                : `"${confirm?.coupon.name}" 쿠폰을 지금 종료합니다. 더 이상 발급되지 않습니다.`}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={processing}>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleConfirm}
              disabled={processing}
              className={
                confirm?.type === 'delete'
                  ? 'bg-destructive text-white hover:bg-destructive/90'
                  : undefined
              }
            >
              {processing && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {confirm?.type === 'delete' ? '삭제' : '조기 종료'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
