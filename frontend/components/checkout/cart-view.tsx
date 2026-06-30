// frontend/components/checkout/cart-view.tsx
'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { Loader2, ShoppingCart, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { useCart } from '@/components/providers/cart-provider'
import { api } from '@/lib/api'
import { formatKRW } from '@/lib/utils'

export function CartView() {
  const router = useRouter()
  const { items, total: providerTotal, removeItem, clear, loading, refreshCart } = useCart()
  const [ordering, setOrdering] = useState(false)

  const handleCreateCartOrder = async () => {
    setOrdering(true)
    try {
      const order = await api.createOrderFromCart()
      await refreshCart()
      router.push(`/checkout?orderId=${order.orderId}`)
    } catch (err) {
      const message = err instanceof Error ? err.message : '장바구니 주문 생성에 실패했습니다.'
      toast.error(message)
    } finally {
      setOrdering(false)
    }
  }

  if (loading) {
    return (
      <div className="mt-10 flex flex-col items-center rounded-xl border border-dashed py-20 text-center">
        <p className="mt-4 text-sm text-muted-foreground">장바구니 정보를 불러오는 중입니다...</p>
      </div>
    )
  }

  if (items.length === 0) {
    return (
      <div className="mt-10 flex flex-col items-center rounded-xl border border-dashed py-20 text-center">
        <ShoppingCart className="h-10 w-10 text-muted-foreground" />
        <p className="mt-4 text-sm text-muted-foreground">장바구니가 비어 있습니다.</p>
        <Button asChild className="mt-4">
          <Link href="/">강좌 둘러보기</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="mt-6 grid gap-8 lg:grid-cols-[1fr_320px]">
      <div>
        <ul className="space-y-3">
          {items.map((item) => (
            <li key={item.cartItemId} className="flex items-center gap-3 rounded-xl border bg-card p-3">
              <div className="relative h-16 w-28 shrink-0 overflow-hidden rounded-md bg-muted">
                <Image
                  src={item.thumbnailUrl || '/placeholder.svg'}
                  alt={item.title}
                  fill
                  sizes="112px"
                  className="object-cover"
                />
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium">{item.title}</p>
                <p className="mt-1 text-sm font-semibold">{formatKRW(item.price)}</p>
              </div>
              <Button
                variant="ghost"
                size="icon"
                aria-label="삭제"
                onClick={() => removeItem(item.cartItemId)}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </li>
          ))}
        </ul>
      </div>

      <div className="lg:sticky lg:top-20 lg:self-start">
        <div className="rounded-xl border bg-card p-5">
          <h2 className="font-semibold">주문 요약</h2>
          <Separator className="my-4" />
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">장바구니 강의 ({items.length})</span>
            <span>{formatKRW(providerTotal)}</span>
          </div>
          <Separator className="my-4" />
          <div className="flex items-baseline justify-between">
            <span className="text-sm font-medium">총 상품 금액</span>
            <span className="text-xl font-bold">{formatKRW(providerTotal)}</span>
          </div>
          <Button
            className="mt-5 w-full"
            size="lg"
            disabled={items.length === 0 || ordering}
            onClick={handleCreateCartOrder}
          >
            {ordering ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
            장바구니 전체 주문하기
          </Button>
          <Button
            className="mt-2 w-full"
            variant="outline"
            disabled={items.length === 0}
            onClick={() => clear()}
          >
            장바구니 비우기
          </Button>
        </div>
      </div>
    </div>
  )
}
