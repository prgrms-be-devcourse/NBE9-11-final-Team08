'use client'

import { useEffect, useState } from 'react'

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { formatKRW } from '@/lib/utils'
import {
  couponDiscountTypeLabel,
  couponStatusMeta,
  couponTargetLabel,
  couponTypeLabel,
  couponUseTypeLabel,
  autoIssueTypeLabel,
} from '@/lib/coupon-labels'
import type { AdminCoupon, Course, CategoryResponse } from '@/lib/types'
import { api } from '@/lib/api'

interface CouponDetailDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  coupon: AdminCoupon | null
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 py-2.5">
      <span className="shrink-0 text-sm text-muted-foreground">{label}</span>
      <span className="text-right text-sm font-medium">{value}</span>
    </div>
  )
}

export function CouponDetailDialog({
  open,
  onOpenChange,
  coupon: initialCoupon,
}: CouponDetailDialogProps) {
  const [courses, setCourses] = useState<Course[]>([])
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [coupon, setCoupon] = useState<AdminCoupon | null>(initialCoupon)

  useEffect(() => {
    if (!open || !initialCoupon) return
    let active = true

    setCoupon(initialCoupon)

    api.getAdminCoupon(initialCoupon.id).then((couponRes) => {
      if (!active) return
      const c = couponRes || initialCoupon
      setCoupon(c)

      api.getCategories().catch(() => []).then(categoriesRes => {
        if (active) setCategories(categoriesRes || [])
      })

      const courseIds = c.target === 'COURSE' ? c.targetCourses : []
      if (courseIds && courseIds.length > 0) {
        Promise.all(courseIds.map(id => api.getAdminCourse(id).catch(() => undefined)))
          .then(res => {
            if (active) setCourses(res.filter(Boolean) as Course[])
          })
      }
    })

    return () => {
      active = false
    }
  }, [open, initialCoupon])

  if (!coupon) return null

  const status = couponStatusMeta[coupon.status]
  const discount =
    coupon.discountType === 'RATE'
      ? `${coupon.discountValue}%`
      : formatKRW(coupon.discountValue)
  const targets =
    coupon.target === 'CATEGORY'
      ? coupon.targetCategories
      : coupon.target === 'COURSE'
        ? coupon.targetCourses
        : undefined

  const targetNames = targets?.map((idStr) => {
    if (coupon.target === 'COURSE') {
      const course = courses.find((c) => String(c.id) === idStr)
      return course ? course.title : `코스 ${idStr}`
    } else if (coupon.target === 'CATEGORY') {
      const category = categories.find((c) => String(c.id) === idStr)
      return category ? category.name : `카테고리 ${idStr}`
    }
    return idStr
  })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-2">
            <DialogTitle>{coupon.name}</DialogTitle>
            <Badge variant={status.variant}>{status.label}</Badge>
          </div>
          <DialogDescription>쿠폰 ID #{coupon.id}</DialogDescription>
        </DialogHeader>

        <div className="divide-y">
          <Row label="쿠폰 타입" value={couponTypeLabel[coupon.type]} />
          {coupon.type === 'AUTO' && coupon.autoIssueType && (
            <Row label="자동 발급 조건" value={autoIssueTypeLabel[coupon.autoIssueType]} />
          )}
          <Row
            label="총 발급 수량"
            value={
              coupon.totalQuantity
                ? `${coupon.totalQuantity.toLocaleString('ko-KR')}개`
                : '무제한'
            }
          />
          <Row label="적용 대상" value={couponTargetLabel[coupon.target]} />
          {targetNames && targetNames.length > 0 && (
            <Row
              label="대상 상세"
              value={
                <span className="flex flex-wrap justify-end gap-1">
                  {targetNames.map((name, i) => (
                    <Badge key={i} variant="secondary">
                      {name}
                    </Badge>
                  ))}
                </span>
              }
            />
          )}
          <Row label="사용 타입" value={couponUseTypeLabel[coupon.useType]} />
          <Row label="중복 적용" value={coupon.stackable ? '가능' : '불가'} />
          <Row
            label="할인"
            value={`${couponDiscountTypeLabel[coupon.discountType]} · ${discount}`}
          />
          <Row
            label="최대 할인 금액"
            value={coupon.maxDiscount ? formatKRW(coupon.maxDiscount) : '제한 없음'}
          />
          <Row label="최소 주문 금액" value={coupon.minOrderAmount ? formatKRW(coupon.minOrderAmount) : '제한 없음'} />
          <Row
            label="유효 기간"
            value={coupon.validDays ? `발급 후 ${coupon.validDays}일` : '제한 없음'}
          />
          <Row label="발급 시작" value={coupon.startAt.replace('T', ' ')} />
          <Row label="발급 종료" value={coupon.endAt.replace('T', ' ')} />
        </div>
      </DialogContent>
    </Dialog>
  )
}
