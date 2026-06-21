import Link from 'next/link'
import { CheckCircle2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { formatKRW } from '@/lib/utils'

export default async function OrderCompletePage({
  searchParams,
}: {
  searchParams: Promise<{ orderId?: string; amount?: string }>
}) {
  const { orderId, amount } = await searchParams

  return (
    <div className="mx-auto max-w-xl px-4 py-20">
      <Card>
        <CardContent className="flex flex-col items-center gap-5 py-12 text-center">
          <CheckCircle2 className="size-16 text-primary" />
          <div>
            <h1 className="text-2xl font-bold">결제가 완료되었습니다</h1>
            <p className="mt-2 text-sm text-muted-foreground">
              이제 바로 학습을 시작할 수 있어요.
            </p>
          </div>
          <div className="w-full space-y-2 rounded-lg bg-muted p-5 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">주문번호</span>
              <span className="font-medium">{orderId ?? '-'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">결제금액</span>
              <span className="font-semibold text-primary">
                {formatKRW(Number(amount ?? 0))}
              </span>
            </div>
          </div>
          <div className="flex w-full gap-3">
            <Button asChild variant="outline" className="flex-1">
              <Link href="/orders">주문 내역</Link>
            </Button>
            <Button asChild className="flex-1">
              <Link href="/dashboard">학습 시작하기</Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
