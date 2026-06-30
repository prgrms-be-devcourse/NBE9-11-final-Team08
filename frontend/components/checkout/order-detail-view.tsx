// frontend/components/checkout/order-detail-view.tsx
'use client'

import { useState } from 'react'
import Link from 'next/link'
import { ChevronLeft, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '@/lib/api'
import { formatOrderStatus, formatPaymentStatus, getOrderStatusVariant } from '@/lib/order-payment-labels'
import { formatDateTime, formatKRW } from '@/lib/utils'
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
import type { OrderDetailResponse, PaymentStatus } from '@/lib/types'

type ActionType = 'cancel' | 'refund'

export function OrderDetailView({ order: initial }: { order: OrderDetailResponse }) {
  const [order, setOrder] = useState<OrderDetailResponse>(initial)
  const [dialogAction, setDialogAction] = useState<ActionType | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatus | null>(null)

  const displayStatus = formatOrderStatus(order.status)
  const payment = order.payment
  const effectivePaymentStatus = paymentStatus ?? payment?.status ?? null
  const canCancel = order.status === 'PENDING_PAYMENT'
  const canRefund = order.status === 'PAID'

  const refreshOrder = async () => {
    const latest = await api.getOrder(String(order.orderId))
    if (latest) {
      setOrder(latest)
    }
    return latest
  }

  const handleConfirmAction = async () => {
    if (!dialogAction) return

    setIsSubmitting(true)
    try {
      if (dialogAction === 'cancel') {
        const canceled = await api.cancelOrder(order.orderId)
        setOrder(canceled)
        toast.success('주문이 취소되었습니다.')
      } else {
        const refunded = await api.refundPayment(order.orderId)
        setPaymentStatus(refunded.paymentStatus)
        const latest = await refreshOrder()
        if (!latest) {
          setOrder((prev) => ({ ...prev, status: refunded.orderStatus }))
        }
        toast.success('전체 환불이 처리되었습니다.')
      }
      setDialogAction(null)
    } catch (err) {
      const message = err instanceof Error ? err.message : '요청 처리에 실패했습니다.'
      toast.error(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="max-w-3xl">
      <Link
        href="/orders"
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ChevronLeft className="h-4 w-4" /> 주문 내역으로
      </Link>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="text-2xl font-bold">주문 상세</h1>
          <Badge variant={getOrderStatusVariant(order.status)}>
            {displayStatus}
          </Badge>
        </div>
        <div className="flex gap-2">
          {canCancel ? (
            <Button variant="outline" size="sm" onClick={() => setDialogAction('cancel')}>
              주문취소
            </Button>
          ) : null}
          {canRefund ? (
            <Button variant="destructive" size="sm" onClick={() => setDialogAction('refund')}>
              전체 환불
            </Button>
          ) : null}
        </div>
      </div>
      <p className="mt-1 text-sm text-muted-foreground">
        {order.orderNumber} · {formatDateTime(order.orderedAt)}
      </p>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-base">주문 강의</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {order.items?.map((item) => (
            <div
              key={item.orderItemId || item.courseId}
              className="flex items-center justify-between gap-4 border-b pb-4 last:border-0 last:pb-0"
            >
              <div>
                <p className="text-sm font-medium">{item.courseTitle}</p>
              </div>
              <p className="shrink-0 text-sm font-semibold">{formatKRW(item.finalPrice)}</p>
            </div>
          ))}
        </CardContent>
      </Card>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-base">결제 정보</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          <Row label="주문 상태" value={displayStatus} />
          {effectivePaymentStatus ? (
            <Row label="결제 상태" value={formatPaymentStatus(effectivePaymentStatus)} />
          ) : null}
          {payment ? (
            <>
              <Row label="PG사" value={formatPaymentProvider(payment.provider)} />
              <Row label="결제 수단" value={formatPaymentMethod(payment.method)} />
              <Row label="결제 일시" value={payment.paidAt ? formatDateTime(payment.paidAt) : '-'} />
            </>
          ) : (
            <Row label="결제 정보" value="없음" />
          )}
          <Separator />
          <Row label="상품 금액" value={formatKRW(order.totalPrice)} />
          <Row label="할인 금액" value={`- ${formatKRW(order.discountPrice)}`} />
          <Separator />
          <div className="flex items-center justify-between">
            <span className="font-medium">최종 결제 금액</span>
            <span className="text-lg font-bold text-primary">
              {formatKRW(order.finalPrice)}
            </span>
          </div>
        </CardContent>
      </Card>

      <Dialog open={!!dialogAction} onOpenChange={(open) => !open && setDialogAction(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{dialogAction === 'refund' ? '전체 환불' : '주문취소'}</DialogTitle>
            <DialogDescription>
              {dialogAction === 'refund'
                ? `주문 ${order.orderNumber} 전체를 환불하시겠습니까? 환불 금액은 ${formatKRW(order.finalPrice)}입니다.`
                : `결제 대기 중인 주문 ${order.orderNumber}을 취소하시겠습니까?`}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline" disabled={isSubmitting}>닫기</Button>
            </DialogClose>
            <Button
              variant={dialogAction === 'refund' ? 'destructive' : 'default'}
              onClick={handleConfirmAction}
              disabled={isSubmitting}
            >
              {isSubmitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
              {dialogAction === 'refund' ? '전체 환불하기' : '주문취소하기'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right">{value}</span>
    </div>
  )
}

function formatPaymentProvider(provider?: string | null) {
  if (!provider) return '-'

  const labels: Record<string, string> = {
    MOCK: 'Mock',
    TOSS: 'Toss',
    NICEPAY: 'NICEPAY',
    KCP: 'KCP',
  }

  return labels[provider] ?? provider
}

function formatPaymentMethod(method?: string | null) {
  if (!method) return '-'

  const labels: Record<string, string> = {
    CARD: '신용카드',
    KAKAOPAY: '카카오페이',
  }

  return labels[method] ?? method
}
