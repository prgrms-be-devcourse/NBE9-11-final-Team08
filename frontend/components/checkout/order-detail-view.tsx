'use client'

import { useState } from 'react'
import Link from 'next/link'
import { ChevronLeft } from 'lucide-react'
import type { Order, OrderItem } from '@/lib/types'
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

const statusVariant: Record<OrderItem['status'], 'secondary' | 'outline' | 'destructive'> = {
  '정상 구매': 'secondary',
  '환불 가능': 'outline',
  '환불 완료': 'destructive',
  '부분 환불': 'destructive',
}

export function OrderDetailView({ order: initial }: { order: Order }) {
  const [order, setOrder] = useState<Order>(initial)
  const [target, setTarget] = useState<OrderItem | null>(null)

  function confirmRefund() {
    if (!target) return
    // POST /api/orders/{id}/refund  { courseId }
    setOrder((prev) => ({
      ...prev,
      status: '부분 환불',
      refundAmount: prev.refundAmount + target.price,
      paidAmount: prev.paidAmount - target.price,
      items: prev.items.map((it) =>
        it.courseId === target.courseId ? { ...it, status: '환불 완료' } : it,
      ),
    }))
    toast.success(`${formatKRW(target.price)} 환불이 접수되었습니다.`)
    setTarget(null)
  }

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
        <Badge variant={order.status === '환불 완료' ? 'destructive' : 'secondary'}>
          {order.status}
        </Badge>
      </div>
      <p className="mt-1 text-sm text-muted-foreground">
        {order.orderNumber} · {order.orderedAt}
      </p>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-base">주문 강의</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {order.items.map((item) => (
            <div
              key={item.courseId}
              className="flex items-center justify-between gap-4 border-b pb-4 last:border-0 last:pb-0"
            >
              <div>
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium">{item.title}</p>
                  <Badge variant={statusVariant[item.status]} className="text-[11px]">
                    {item.status}
                  </Badge>
                </div>
                <p className="mt-1 text-xs text-muted-foreground">{item.instructor}</p>
                <p className="mt-1 text-sm font-semibold">{formatKRW(item.price)}</p>
              </div>
              {item.status === '환불 가능' && (
                <Button variant="outline" size="sm" onClick={() => setTarget(item)}>
                  환불 요청
                </Button>
              )}
            </div>
          ))}
        </CardContent>
      </Card>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle className="text-base">결제 정보</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          <Row label="결제 상태" value={order.paymentStatus} />
          <Row label="결제 수단" value={order.paymentMethod} />
          <Separator />
          <Row label="상품 금액" value={formatKRW(order.productTotal)} />
          <Row label="할인 금액" value={`- ${formatKRW(order.discount)}`} />
          <Row label="환불 금액" value={`- ${formatKRW(order.refundAmount)}`} muted />
          <Separator />
          <div className="flex items-center justify-between">
            <span className="font-medium">최종 결제 금액</span>
            <span className="text-lg font-bold text-primary">
              {formatKRW(order.paidAmount)}
            </span>
          </div>
        </CardContent>
      </Card>

      <Dialog open={!!target} onOpenChange={(o) => !o && setTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>환불 요청</DialogTitle>
            <DialogDescription>
              {target?.title} 강의를 환불하시겠습니까? 환불 금액{' '}
              {target && formatKRW(target.price)}이 결제 수단으로 환불됩니다.
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
