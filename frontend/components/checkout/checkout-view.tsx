// frontend/components/checkout/checkout-view.tsx
'use client'

import { useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import Image from 'next/image'
import { useCart } from '@/components/providers/cart-provider'
import { api } from '@/lib/api'
import { formatKRW } from '@/lib/utils'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { toast } from 'sonner'
import { Loader2 } from 'lucide-react'

const POINTS_AVAILABLE = 12000

// 기본 제공되는 테스트용 쿠폰 데이터 (실제 데이터 로드 전 Fallback)
const fallbackCoupons = [
  { id: 'welcome', name: '신규 가입 5,000원 할인', kind: 'amount' as const, value: 5000 },
  { id: 'firstcome', name: '오전 10시 선착순 10% 할인', kind: 'percent' as const, value: 10 },
  { id: 'book', name: '작가의 날 10% 할인', kind: 'percent' as const, value: 10 },
]

export function CheckoutView() {
  const router = useRouter()
  const { items, clear } = useCart()
  const [couponId, setCouponId] = useState<string>('none')
  const [points, setPoints] = useState(0)
  const [method, setMethod] = useState('card')
  const [agree, setAgree] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  
  const [userCoupons, setUserCoupons] = useState<any[]>([])

  useEffect(() => {
    // 사용자가 보유한 쿠폰 목록 불러오기 연동
    const fetchCoupons = async () => {
      try {
        if ('getCoupons' in api) {
          const res = await (api as any).getCoupons()
          if (Array.isArray(res) && res.length > 0) {
            // 백엔드 구조에 맞춰 변환 (CouponListResponse)
            const mapped = res.map((c: any) => ({
              id: c.issuedCouponId?.toString() || c.id,
              name: c.policyName || c.name,
              kind: c.discountType === 'RATE' || c.type === 'percent' ? 'percent' : 'amount',
              value: Number(c.discountValue) || Number(c.amount?.replace(/[^0-9]/g, '') || 0),
            }))
            setUserCoupons(mapped)
          } else {
            setUserCoupons(fallbackCoupons)
          }
        }
      } catch (err) {
        setUserCoupons(fallbackCoupons)
      }
    }
    fetchCoupons()
  }, [])

  const subtotal = useMemo(
    () => items.reduce((sum, c) => sum + c.price, 0),
    [items],
  )

  const activeCoupons = userCoupons.length > 0 ? userCoupons : fallbackCoupons
  const coupon = activeCoupons.find((c) => c.id === couponId)
  
  const couponDiscount = coupon
    ? coupon.kind === 'percent'
      ? Math.round((subtotal * coupon.value) / 100)
      : coupon.value
    : 0
    
  const usablePoints = Math.max(
    0,
    Math.min(points || 0, POINTS_AVAILABLE, subtotal - couponDiscount),
  )
  const total = Math.max(0, subtotal - couponDiscount - usablePoints)

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-20 text-center">
        <h1 className="text-xl font-semibold">결제할 강의가 없습니다</h1>
        <Button className="mt-6" onClick={() => router.push('/')}>
          강의 둘러보기
        </Button>
      </div>
    )
  }

  async function handlePay() {
    if (!agree) {
      toast.error('결제 정보 확인 및 동의가 필요합니다.')
      return
    }
    
    setIsSubmitting(true)
    try {
      // 결제 생성 API (api.createOrderFromCart)
      const order = await api.createOrderFromCart()
      await clear() // 결제 성공 시 장바구니 비우기
      
      // 결제 완료 페이지로 리다이렉트 (주문 번호 및 금액 전달)
      const orderNum = order.orderNumber || order.orderId || 'NEW_ORDER'
      const finalPrice = order.finalPrice || total
      router.push(`/orders/complete?orderId=${orderNum}&amount=${finalPrice}`)
      toast.success('결제가 완료되었습니다.')
    } catch (e: any) {
      toast.error(e.message || '주문 생성에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-2xl font-bold">결제하기</h1>
      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">주문 강의 {items.length}개</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {items.map((c) => (
                <div key={c.courseId || c.cartItemId} className="flex gap-4">
                  <Image
                    src={'/placeholder.svg'}
                    alt={c.title}
                    width={96}
                    height={56}
                    className="h-14 w-24 rounded-md object-cover"
                  />
                  <div className="flex flex-1 items-center justify-between">
                    <p className="line-clamp-2 text-sm font-medium">{c.title}</p>
                    <span className="ml-3 shrink-0 text-sm font-semibold">
                      {formatKRW(c.price)}
                    </span>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">할인 적용</CardTitle>
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="space-y-2">
                <Label>쿠폰</Label>
                <Select value={couponId} onValueChange={(value) => setCouponId(value ?? 'none')}>
                  <SelectTrigger>
                    <SelectValue placeholder="사용할 쿠폰을 선택하세요" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">선택 안함</SelectItem>
                    {activeCoupons.map((c) => (
                      <SelectItem key={c.id} value={c.id}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="points">
                  포인트 사용{' '}
                  <span className="text-xs text-muted-foreground">
                    (보유 {formatKRW(POINTS_AVAILABLE)})
                  </span>
                </Label>
                <div className="flex gap-2">
                  <Input
                    id="points"
                    type="number"
                    value={points || ''}
                    onChange={(e) => setPoints(Number(e.target.value))}
                    placeholder="0"
                  />
                  <Button
                    variant="outline"
                    onClick={() => setPoints(POINTS_AVAILABLE)}
                  >
                    전액 사용
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">결제 수단</CardTitle>
            </CardHeader>
            <CardContent>
              <RadioGroup
                value={method}
                onValueChange={setMethod}
                className="grid gap-3 sm:grid-cols-2"
              >
                {[
                  { v: 'card', l: '신용/체크카드' },
                  { v: 'kakao', l: '카카오페이' },
                  { v: 'toss', l: '토스페이' },
                  { v: 'bank', l: '무통장 입금' },
                ].map((m) => (
                  <Label
                    key={m.v}
                    htmlFor={m.v}
                    className="flex cursor-pointer items-center gap-3 rounded-lg border p-4 has-[:checked]:border-primary has-[:checked]:bg-accent"
                  >
                    <RadioGroupItem id={m.v} value={m.v} />
                    <span className="text-sm font-medium">{m.l}</span>
                  </Label>
                ))}
              </RadioGroup>
            </CardContent>
          </Card>
        </div>

        <div>
          <Card className="lg:sticky lg:top-24">
            <CardHeader>
              <CardTitle className="text-base">최종 결제 금액</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">상품 금액</span>
                <span>{formatKRW(subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">쿠폰 할인</span>
                <span className="text-destructive">- {formatKRW(couponDiscount)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">포인트 사용</span>
                <span className="text-destructive">- {formatKRW(usablePoints)}</span>
              </div>
              <Separator />
              <div className="flex items-center justify-between">
                <span className="font-medium">총 결제 금액</span>
                <span className="text-xl font-bold text-primary">
                  {formatKRW(total)}
                </span>
              </div>
              <Label className="flex items-start gap-2 pt-2 text-xs text-muted-foreground">
                <input
                  type="checkbox"
                  checked={agree}
                  onChange={(e) => setAgree(e.target.checked)}
                  className="mt-0.5 accent-[var(--primary)]"
                />
                주문 내용을 확인했으며 결제에 동의합니다.
              </Label>
              <Button className="w-full" size="lg" onClick={handlePay} disabled={isSubmitting}>
                {isSubmitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                {isSubmitting ? '결제 처리 중...' : `${formatKRW(total)} 결제하기`}
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
