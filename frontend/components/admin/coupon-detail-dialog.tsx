'use client'

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
} from '@/lib/coupon-labels'
import type { AdminCoupon } from '@/lib/types'

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
  coupon,
}: CouponDetailDialogProps) {
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
          <Row label="적용 대상" value={couponTargetLabel[coupon.target]} />
          {targets && targets.length > 0 && (
            <Row
              label="대상 상세"
              value={
                <span className="flex flex-wrap justify-end gap-1">
                  {targets.map((t) => (
                    <Badge key={t} variant="secondary">
                      {t}
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
          <Row label="최소 주문 금액" value={formatKRW(coupon.minOrderAmount)} />
          <Row
            label="총 발급 수량"
            value={
              coupon.totalQuantity
                ? `${coupon.totalQuantity.toLocaleString('ko-KR')}개`
                : '무제한'
            }
          />
          <Row
            label="발급 현황"
            value={`${coupon.issuedCount.toLocaleString('ko-KR')}건 발급`}
          />
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
