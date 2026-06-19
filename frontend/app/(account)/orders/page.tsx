// frontend/app/(account)/orders/page.tsx
import Link from 'next/link'
import { api } from '@/lib/api'
import { formatKRW } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { ChevronRight } from 'lucide-react'

export default async function OrdersPage() {
  const response = await api.getOrders()
  const orders = Array.isArray(response) ? response : (response as any).content || []

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">주문 내역</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          결제한 강의의 주문 및 환불 상태를 확인할 수 있습니다.
        </p>
      </div>

      <div className="space-y-4">
        {orders.map((order: any) => (
          <Link key={order.orderId || order.id} href={`/orders/${order.orderId || order.id}`}>
            <Card className="transition-shadow hover:shadow-md">
              <CardContent className="flex items-center justify-between gap-4 py-5">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold">{order.orderNumber}</span>
                    <Badge
                      variant={order.status === 'REFUNDED' || order.status === '환불 완료' ? 'destructive' : 'secondary'}
                    >
                      {order.status === 'COMPLETED' ? '결제 완료' : order.status}
                    </Badge>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {order.orderedAt ? new Date(order.orderedAt).toLocaleString() : ''}
                  </p>
                  <p className="mt-2 text-sm text-muted-foreground">
                    {order.items && order.items.length > 0 
                      ? `${order.items[0]?.title || order.items[0]?.courseTitle}${order.items.length > 1 ? ` 외 ${order.items.length - 1}건` : ''}`
                      : '주문 상세 보기'}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <div className="text-right">
                    <p className="text-xs text-muted-foreground">결제금액</p>
                    <p className="text-base font-bold">{formatKRW(order.finalPrice || order.paidAmount || 0)}</p>
                  </div>
                  <ChevronRight className="h-5 w-5 text-muted-foreground" />
                </div>
              </CardContent>
            </Card>
          </Link>
        ))}
        
        {orders.length === 0 && (
          <div className="py-12 text-center text-sm text-muted-foreground">
            주문 내역이 없습니다.
          </div>
        )}
      </div>
    </div>
  )
}
