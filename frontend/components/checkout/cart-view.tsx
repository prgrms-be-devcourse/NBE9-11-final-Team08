'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ShoppingCart, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Separator } from '@/components/ui/separator'
import { useCart } from '@/components/providers/cart-provider'
import { formatKRW } from '@/lib/utils'

export function CartView() {
  const router = useRouter()
  const { items, removeItem, removeItems } = useCart()
  const [selected, setSelected] = useState<string[]>(items.map((i) => i.courseId))

  // keep selection in sync if an item is removed elsewhere
  const validSelected = selected.filter((id) => items.some((i) => i.courseId === id))
  const allChecked = items.length > 0 && validSelected.length === items.length

  const toggle = (id: string) =>
    setSelected((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))

  const toggleAll = () =>
    setSelected(allChecked ? [] : items.map((i) => i.courseId))

  const selectedItems = items.filter((i) => validSelected.includes(i.courseId))
  const total = selectedItems.reduce((s, i) => s + i.price, 0)

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
        <div className="flex items-center justify-between rounded-lg border bg-card px-4 py-3">
          <label className="flex items-center gap-2 text-sm">
            <Checkbox checked={allChecked} onCheckedChange={toggleAll} />
            전체 선택 ({validSelected.length}/{items.length})
          </label>
          <Button
            variant="ghost"
            size="sm"
            className="text-muted-foreground"
            onClick={() => removeItems(validSelected)}
          >
            <Trash2 className="mr-1 h-4 w-4" /> 선택삭제
          </Button>
        </div>

        <ul className="mt-3 space-y-3">
          {items.map((item) => (
            <li key={item.courseId} className="flex items-center gap-3 rounded-xl border bg-card p-3">
              <Checkbox
                checked={validSelected.includes(item.courseId)}
                onCheckedChange={() => toggle(item.courseId)}
              />
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
                onClick={() => removeItem(item.courseId)}
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
            <span className="text-muted-foreground">선택 상품 ({selectedItems.length})</span>
            <span>{formatKRW(total)}</span>
          </div>
          <Separator className="my-4" />
          <div className="flex items-baseline justify-between">
            <span className="text-sm font-medium">총 상품 금액</span>
            <span className="text-xl font-bold">{formatKRW(total)}</span>
          </div>
          <Button
            className="mt-5 w-full"
            size="lg"
            disabled={selectedItems.length === 0}
            onClick={() => router.push('/checkout')}
          >
            주문하기
          </Button>
        </div>
      </div>
    </div>
  )
}
