// frontend/components/checkout/checkout-view.tsx
'use client'

import { useEffect, useMemo, useState } from 'react'
import Image from 'next/image'
import { useRouter } from 'next/navigation'
import { AlertCircle, CreditCard, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { useCart } from '@/components/providers/cart-provider'
import { api } from '@/lib/api'
import { getOrderName, savePendingPayment, type PaymentProvider } from '@/lib/checkout-payment'
import { loadTossPayments } from '@/lib/toss-payments'
import { formatKRW } from '@/lib/utils'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
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
import type { OrderDetailResponse } from '@/lib/types'

type CouponOption = {
  id: string
  name: string
  kind: 'amount' | 'percent'
  value: number
  originalAmountString?: string
  minOrderAmount?: number
  maxDiscountAmount?: number | null
  courseIds?: number[]
}

type CheckoutStep = 'idle' | 'loading-order' | 'creating-order' | 'mock-confirming' | 'opening-toss'

export function CheckoutView({ initialOrderId }: { initialOrderId?: string }) {
  const router = useRouter()
  const { items, clear } = useCart()
  const tossClientKey = process.env.NEXT_PUBLIC_TOSS_PAYMENTS_CLIENT_KEY ?? ''
  const canUseToss = tossClientKey.trim().length > 0
  const [directOrder, setDirectOrder] = useState<OrderDetailResponse | null>(null)
  const [orderLoadError, setOrderLoadError] = useState('')
  const [couponId, setCouponId] = useState<string>('none')
  const [provider, setProvider] = useState<PaymentProvider>('mock')
  const [agree, setAgree] = useState(false)
  const [step, setStep] = useState<CheckoutStep>(initialOrderId ? 'loading-order' : 'idle')
  const [userCoupons, setUserCoupons] = useState<CouponOption[]>([])

  useEffect(() => {
    if (!canUseToss && provider === 'toss') {
      setProvider('mock')
    }
  }, [canUseToss, provider])

  useEffect(() => {
    if (!initialOrderId) return

    let active = true
    setStep('loading-order')
    api.getOrder(initialOrderId)
      .then((order) => {
        if (!active) return
        if (order) {
          setDirectOrder(order)
          setOrderLoadError('')
        } else {
          setOrderLoadError('주문 정보를 찾을 수 없습니다.')
        }
      })
      .catch((err) => {
        if (!active) return
        setOrderLoadError(err instanceof Error ? err.message : '주문 정보를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (active) setStep('idle')
      })

    return () => {
      active = false
    }
  }, [initialOrderId])

  useEffect(() => {
    let active = true

    api.getMyCoupons()
      .then((res) => {
        if (!active || !Array.isArray(res)) return

        const mapped = res
          .filter((c: any) => c.status === 'ACTIVE')
          .map((c: any) => ({
            id: c.id,
            name: c.name,
            kind: (typeof c.amount === 'string' && c.amount.includes('%') ? 'percent' : 'amount') as CouponOption['kind'],
            value: Number(c.amount?.replace(/[^0-9]/g, '') || 0),
            originalAmountString: c.amount,
            minOrderAmount: c.minOrderAmount || 0,
            maxDiscountAmount: c.maxDiscountAmount || null,
            courseIds: c.courseIds || [],
          }))
          .filter((c) => Number.isFinite(Number(c.id)) && c.value > 0)

        setUserCoupons(mapped)
      })
      .catch(() => setUserCoupons([]))

    return () => {
      active = false
    }
  }, [])

  const checkoutItems = useMemo(() => {
    if (directOrder) {
      return directOrder.items.map((item) => ({
        key: `order-${item.orderItemId}`,
        courseId: item.courseId,
        title: item.courseTitle,
        price: item.finalPrice,
      }))
    }

    return items.map((item) => ({
      key: `cart-${item.cartItemId}`,
      courseId: item.courseId,
      title: item.title,
      price: item.price,
    }))
  }, [directOrder, items])

  const subtotal = useMemo(() => {
    if (directOrder) return directOrder.totalPrice
    return checkoutItems.reduce((sum, item) => sum + item.price, 0)
  }, [checkoutItems, directOrder])

  const baseFinalPrice = directOrder?.finalPrice ?? subtotal
  const coupon = userCoupons.find((c) => c.id === couponId)

  const isCouponApplicable = (c: CouponOption) => {
    if (c.minOrderAmount && subtotal < c.minOrderAmount) return false
    if (c.courseIds && c.courseIds.length > 0) {
      return checkoutItems.some((item) => c.courseIds?.includes(Number(item.courseId)))
    }
    return true
  }

  const couponDiscount = useMemo(() => {
    if (!coupon || !isCouponApplicable(coupon)) return 0

    if (coupon.kind === 'percent') {
      const calculated = Math.round((subtotal * coupon.value) / 100)
      return coupon.maxDiscountAmount ? Math.min(calculated, coupon.maxDiscountAmount) : calculated
    }

    return Math.min(coupon.value, subtotal)
  }, [coupon, subtotal, checkoutItems])

  const selectedCouponId = useMemo(() => {
    if (couponId === 'none') return null
    const parsed = Number(couponId)
    return Number.isFinite(parsed) ? parsed : null
  }, [couponId])

  const displayTotal = Math.max(0, baseFinalPrice - couponDiscount)
  const isSubmitting = step === 'creating-order' || step === 'mock-confirming' || step === 'opening-toss'

  const stepTextByStep: Record<CheckoutStep, string> = {
    idle: '',
    'loading-order': '주문 정보를 불러오는 중...',
    'creating-order': '주문을 생성하는 중...',
    'mock-confirming': 'Mock 결제를 승인하는 중...',
    'opening-toss': 'Toss 결제창을 여는 중...',
  }
  const stepText = stepTextByStep[step]

  const createOrGetOrder = async () => {
    if (directOrder) return directOrder
    setStep('creating-order')
    return api.createOrderFromCart()
  }

  const buildPendingPayment = (order: OrderDetailResponse, paymentProvider: PaymentProvider) => {
    const amount = Math.max(0, order.finalPrice - couponDiscount)
    const pendingPayment = {
      serviceOrderId: order.orderId,
      orderNumber: order.orderNumber,
      amount,
      orderName: getOrderName(order),
      provider: paymentProvider,
      fromCart: !directOrder,
      issuedCouponId: selectedCouponId,
      createdAt: new Date().toISOString(),
    }
    savePendingPayment(pendingPayment)
    return pendingPayment
  }

  const assertReadyToPay = () => {
    if (!agree) {
      toast.error('주문 내용 확인 및 결제 동의가 필요합니다.')
      return false
    }

    if (checkoutItems.length === 0) {
      toast.error('결제할 강의가 없습니다.')
      return false
    }

    return true
  }

  async function handleMockPay() {
    if (!assertReadyToPay()) return

    setStep('creating-order')
    try {
      const order = await createOrGetOrder()
      const pendingPayment = buildPendingPayment(order, 'mock')

      setStep('mock-confirming')
      const paymentResponse = await api.confirmMockPayment(
        order.orderId,
        `mock-payment-key-${Date.now()}`,
        'CARD',
        pendingPayment.amount,
        selectedCouponId,
      )

      if (pendingPayment.fromCart) {
        await clear()
      }

      const orderNumber = paymentResponse.orderNumber || order.orderNumber
      const amount = paymentResponse.amount || pendingPayment.amount
      router.push(
        `/orders/complete?payment=mock&status=success&serviceOrderId=${order.orderId}&orderId=${encodeURIComponent(orderNumber)}&amount=${amount}`,
      )
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Mock 결제 처리에 실패했습니다.'
      toast.error(message)
      setStep('idle')
    }
  }

  async function handleTossPay() {
    if (!assertReadyToPay()) return

    if (!canUseToss) {
      toast.error('Toss client key가 설정되지 않았습니다. .env.local을 확인해주세요.')
      return
    }

    setStep('creating-order')
    try {
      const order = await createOrGetOrder()
      const pendingPayment = buildPendingPayment(order, 'toss')
      const successUrl = new URL('/orders/complete', window.location.origin)
      successUrl.searchParams.set('payment', 'toss')
      successUrl.searchParams.set('serviceOrderId', String(order.orderId))

      const failUrl = new URL('/orders/complete', window.location.origin)
      failUrl.searchParams.set('payment', 'toss')
      failUrl.searchParams.set('status', 'fail')
      failUrl.searchParams.set('serviceOrderId', String(order.orderId))

      setStep('opening-toss')
      const TossPayments = await loadTossPayments()
      const tossPayments = TossPayments(tossClientKey)
      const payment = tossPayments.payment({ customerKey: TossPayments.ANONYMOUS })

      await payment.requestPayment({
        method: 'CARD',
        amount: {
          currency: 'KRW',
          value: pendingPayment.amount,
        },
        orderId: order.orderNumber,
        orderName: pendingPayment.orderName,
        successUrl: successUrl.toString(),
        failUrl: failUrl.toString(),
        card: {
          flowMode: 'DEFAULT',
        },
      })
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Toss 결제창 호출에 실패했습니다.'
      toast.error(message)
      setStep('idle')
    }
  }

  const handlePay = () => {
    if (provider === 'toss') {
      void handleTossPay()
      return
    }

    void handleMockPay()
  }

  if (step === 'loading-order') {
    return (
      <div className="mx-auto flex max-w-2xl flex-col items-center px-4 py-20 text-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
        <p className="mt-4 text-sm text-muted-foreground">{stepText}</p>
      </div>
    )
  }

  if (orderLoadError) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-20 text-center">
        <AlertCircle className="mx-auto h-10 w-10 text-destructive" />
        <h1 className="mt-4 text-xl font-semibold">주문 정보를 불러오지 못했습니다</h1>
        <p className="mt-2 text-sm text-muted-foreground">{orderLoadError}</p>
        <Button className="mt-6" onClick={() => router.push('/cart')}>
          장바구니로 돌아가기
        </Button>
      </div>
    )
  }

  if (checkoutItems.length === 0) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-20 text-center">
        <h1 className="text-xl font-semibold">결제할 강의가 없습니다</h1>
        <Button className="mt-6" onClick={() => router.push('/')}>
          강의 둘러보기
        </Button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <div>
        <h1 className="text-2xl font-bold">결제하기</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          {directOrder ? `바로 주문 ${directOrder.orderNumber}` : '장바구니 전체 항목으로 주문을 생성합니다.'}
        </p>
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">주문 강의 {checkoutItems.length}개</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {checkoutItems.map((item) => (
                <div key={item.key} className="flex gap-4">
                  <Image
                    src="/placeholder.svg"
                    alt={item.title}
                    width={96}
                    height={56}
                    className="h-14 w-24 rounded-md object-cover"
                  />
                  <div className="flex flex-1 items-center justify-between">
                    <p className="line-clamp-2 text-sm font-medium">{item.title}</p>
                    <span className="ml-3 shrink-0 text-sm font-semibold">
                      {formatKRW(item.price)}
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
            <CardContent className="space-y-3">
              <div className="space-y-2">
                <Label>쿠폰</Label>
                <Select value={couponId} onValueChange={(value) => setCouponId(value ?? 'none')}>
                  <SelectTrigger>
                    <SelectValue placeholder="사용할 쿠폰을 선택하세요" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">선택 안함</SelectItem>
                    {userCoupons.map((couponOption) => {
                      const applicable = isCouponApplicable(couponOption)
                      return (
                        <SelectItem key={couponOption.id} value={couponOption.id} disabled={!applicable}>
                          {couponOption.name}
                          {couponOption.originalAmountString ? ` (${couponOption.originalAmountString})` : ''}
                          {!applicable ? ' - 적용 불가' : ''}
                        </SelectItem>
                      )
                    })}
                  </SelectContent>
                </Select>
              </div>
              {userCoupons.length === 0 ? (
                <p className="text-xs text-muted-foreground">사용 가능한 쿠폰이 없거나 쿠폰 API 응답이 없습니다.</p>
              ) : null}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">결제 방식</CardTitle>
            </CardHeader>
            <CardContent>
              <RadioGroup
                value={provider}
                onValueChange={(value) => setProvider(value as PaymentProvider)}
                className="grid gap-3 sm:grid-cols-2"
              >
                {[
                  {
                    value: 'mock',
                    label: 'Mock 결제',
                    description: '백엔드 Mock 승인 API로 바로 결제를 완료합니다.',
                    disabled: false,
                  },
                  {
                    value: 'toss',
                    label: 'Toss 테스트 결제',
                    description: canUseToss
                      ? 'Toss 결제창 완료 후 백엔드 confirm API를 호출합니다.'
                      : 'Toss client key 설정 후 사용할 수 있습니다.',
                    disabled: !canUseToss,
                  },
                ].map((option) => (
                  <Label
                    key={option.value}
                    htmlFor={option.value}
                    className="flex min-h-28 cursor-pointer items-start gap-3 rounded-lg border p-4 has-[:checked]:border-primary has-[:checked]:bg-accent has-[:disabled]:cursor-not-allowed has-[:disabled]:opacity-60"
                  >
                    <RadioGroupItem id={option.value} value={option.value} className="mt-1" disabled={option.disabled} />
                    <span>
                      <span className="flex items-center gap-2 text-sm font-semibold">
                        <CreditCard className="h-4 w-4" />
                        {option.label}
                      </span>
                      <span className="mt-2 block text-xs leading-relaxed text-muted-foreground">
                        {option.description}
                      </span>
                    </span>
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
              <Separator />
              <div className="flex items-center justify-between">
                <span className="font-medium">총 결제 금액</span>
                <span className="text-xl font-bold text-primary">
                  {formatKRW(displayTotal)}
                </span>
              </div>
              <Label className="flex items-start gap-2 pt-2 text-xs text-muted-foreground">
                <input
                  type="checkbox"
                  checked={agree}
                  onChange={(event) => setAgree(event.target.checked)}
                  className="mt-0.5 accent-[var(--primary)]"
                />
                주문 내용을 확인했으며 결제에 동의합니다.
              </Label>
              {stepText ? (
                <p className="flex items-center gap-2 rounded-md bg-muted px-3 py-2 text-xs text-muted-foreground">
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  {stepText}
                </p>
              ) : null}
              <Button className="w-full" size="lg" onClick={handlePay} disabled={isSubmitting}>
                {isSubmitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                {isSubmitting ? '처리 중...' : `${formatKRW(displayTotal)} 결제하기`}
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
