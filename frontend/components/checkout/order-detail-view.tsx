// frontend/components/checkout/order-detail-view.tsx
'use client'

import { useState } from 'react'
import Link from 'next/link'
import { ChevronLeft } from 'lucide-react'
import { formatKRW } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { toast } from 'sonner'
import type { OrderDetailResponse, OrderItemResponse } from '@/lib/types'

const statusVariant: Record<string, 'secondary' | 'outline' | 'destructive'> = {
  'COMPLETED': 'secondary',
  '결제 완료': 'secondary',
  'REFUND_AVAILABLE': 'outline',
  '환불 가능': 'outline',
  'REFUNDED': 'destructive',
  '환불 완료': 'destructive',
  'PARTIAL_REFUNDED': 'destructive',
  '부분 환불': 'destructive',
}

export function OrderDetailView({ order: initial }: { order: OrderDetailResponse }) {
  const [order, setOrder] = useState<OrderDetailResponse>(initial)
  const [target, setTarget] = useState<OrderItemResponse | null>(null)

  function confirmRefund() {
    if (!target) return
    // TODO: 실제 환불 API 연동 필요 (POST /api/orders/{id}/refund)
    setOrder((prev) => ({
      ...prev,
      status: 'PARTIAL_REFUNDED',
      finalPrice: prev.finalPrice - target.finalPrice,
      items: prev.items.map((it) =>
        it.orderItemId === target.orderItemId ? { ...it, status: '환불 완료' } as any : it,
      ),
    }))
    toast.success(`${formatKRW(target.finalPrice)} 환불이 접수되었습니다.`)
    setTarget(null)
  }

  const displayStatus = order.status === 'COMPLETED' ? '결제 완료' : order.status === 'REFUNDED' ? '환불 완료' : order.status
  const orderedDate = order.orderedAt ? new Date(order.orderedAt).toLocaleString() : ''
  const refundedAmount = initial.finalPrice - order.finalPrice

  return (
    <div className="max-w-3xl">
      <Link
        href="/orders"
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ChevronLeft className="h-4 w-4" /> 주문 내역으로
      </Link>

      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-bold">주문 상세</h1>
        <Badge variant={displayStatus === '환불 완료' || displayStatus === 'PARTIAL_REFUNDED' ? 'destructive' : 'secondary'}>
          {displayStatus === 'PARTIAL_REFUNDED' ? '부분 환불' : displayStatus}
        </Badge>
      </div>
      <p className="mt-1 text-sm text-muted-foreground">
        {order.orderNumber} · {orderedDate}
      </p>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-base">주문 강의</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {order.items?.map((item) => {
            const itemStatus = (item as any).status || '결제 완료'
            return (
              <div
                key={item.orderItemId || item.courseId}
                className="flex items-center justify-between gap-4 border-b pb-4 last:border-0 last:pb-0"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-medium">{item.courseTitle}</p>
                    <Badge variant={statusVariant[itemStatus] || 'secondary'} className="text-[11px]">
                      {itemStatus}
                    </Badge>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">담당 강사</p>
                  <p className="mt-1 text-sm font-semibold">{formatKRW(item.finalPrice)}</p>
                </div>
                {itemStatus === '결제 완료' && (
                  <Button variant="outline" size="sm" onClick={() => setTarget(item)}>
                    환불 요청
                  </Button>
                )}
              </div>
            )
          })}
        </CardContent>
      </Card>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-base">결제 정보</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          <Row label="결제 상태" value={displayStatus === 'PARTIAL_REFUNDED' ? '부분 환불' : displayStatus} />
          <Row label="결제 수단" value="신용카드 (기본)" />
          <Separator />
          <Row label="상품 금액" value={formatKRW(order.totalPrice)} />
          <Row label="할인 금액" value={`- ${formatKRW(order.discountPrice)}`} />
          {refundedAmount > 0 && (
            <Row label="환불 금액" value={`- ${formatKRW(refundedAmount)}`} muted />
          )}
          <Separator />
          <div className="flex items-center justify-between">
            <span className="font-medium">최종 결제 금액</span>
            <span className="text-lg font-bold text-primary">
              {formatKRW(order.finalPrice)}
            </span>
          </div>
        </CardContent>
      </Card>

      <Dialog open={!!target} onOpenChange={(o) => !o && setTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>환불 요청</DialogTitle>
            <DialogDescription>
              {target?.courseTitle} 강의를 환불하시겠습니까? 환불 금액{' '}
              {target && formatKRW(target.finalPrice)}이 결제 수단으로 환불됩니다.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">취소</Button>
            </DialogClose>
            <Button variant="destructive" onClick={confirmRefund}>
              환불 요청하기
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function Row({ label, value, muted }: { label: string; value: string; muted?: boolean }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span className={muted ? 'text-destructive' : ''}>{value}</span>
    </div>
  )
}
