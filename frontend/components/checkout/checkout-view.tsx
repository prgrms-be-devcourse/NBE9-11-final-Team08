// frontend/components/checkout/checkout-view.tsx
'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import Image from 'next/image'
import { useRouter } from 'next/navigation'
import { AlertCircle, CreditCard, Loader2, ChevronDown } from 'lucide-react'
import { toast } from 'sonner'
import { useCart } from '@/components/providers/cart-provider'
import { api } from '@/lib/api'
import {
  createPaymentIdempotencyKey,
  getOrderName,
  savePendingPayment,
  type PaymentProvider,
} from '@/lib/checkout-payment'
import { requestNicepayPayment, type NicepayAuthResult } from '@/lib/nicepay-payments'
import { loadTossPayments } from '@/lib/toss-payments'
import { formatKRW } from '@/lib/utils'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogClose,
} from '@/components/ui/dialog'
import type { OrderDetailResponse } from '@/lib/types'

type CouponOption = {
  id: string
  name: string
  kind: 'amount' | 'percent'
  value: number
  originalAmountString?: string
  minOrderAmount?: number
  maxDiscountAmount?: number | null
  categoryIds?: number[]
  courseIds?: number[]
  isStackable?: boolean
  endDate?: string
}

type CheckoutStep = 'idle' | 'loading-order' | 'creating-order' | 'mock-confirming' | 'opening-toss' | 'opening-nicepay'

const resolveNicepayMethod = (result: NicepayAuthResult) => {
  if (result.EasyPayCl === '16' || result.ClickpayCl === '16') {
    return 'KAKAOPAY'
  }
  return result.EasyPayMethod || result.SelectPayMethod || result.PayMethod || 'CARD'
}

export function CheckoutView({ initialOrderId }: { initialOrderId?: string }) {
  const router = useRouter()
  const { items, clear } = useCart()
  const submittingRef = useRef(false)
  const tossClientKey = process.env.NEXT_PUBLIC_TOSS_PAYMENTS_CLIENT_KEY ?? ''
  const nicepayMid = process.env.NEXT_PUBLIC_NICEPAY_MID ?? ''
  const canUseToss = tossClientKey.trim().length > 0
  const canUseNicepay = nicepayMid.trim().length > 0
  const [directOrder, setDirectOrder] = useState<OrderDetailResponse | null>(null)
  const [orderLoadError, setOrderLoadError] = useState('')
  const [itemCouponIds, setItemCouponIds] = useState<Record<string, string>>({})
  const [stackableCouponId, setStackableCouponId] = useState<string>('none')
  const [provider, setProvider] = useState<PaymentProvider>('mock')
  const [agree, setAgree] = useState(false)
  const [step, setStep] = useState<CheckoutStep>(initialOrderId ? 'loading-order' : 'idle')
  const [userCoupons, setUserCoupons] = useState<CouponOption[]>([])
  const [courseCategoryMap, setCourseCategoryMap] = useState<Record<number, number[]>>({})

  useEffect(() => {
    if (!canUseToss && provider === 'toss') {
      setProvider('mock')
    }
    if (!canUseNicepay && provider === 'nicepay') {
      setProvider('mock')
    }
  }, [canUseNicepay, canUseToss, provider])

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
            id: String(c.id),
            name: c.name,
            kind: (typeof c.amount === 'string' && c.amount.includes('%') ? 'percent' : 'amount') as CouponOption['kind'],
            value: Number(c.amount?.replace(/[^0-9]/g, '') || 0),
            originalAmountString: c.amount,
            minOrderAmount: c.minOrderAmount || 0,
            maxDiscountAmount: c.maxDiscountAmount || null,
            categoryIds: c.categoryIds || [],
            courseIds: c.courseIds || [],
            isStackable: c.isStackable || false,
            endDate: c.endDate,
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
        thumbnailUrl: item.thumbnailUrl,
        price: item.finalPrice,
      }))
    }

    return items.map((item) => ({
      key: `cart-${item.cartItemId}`,
      courseId: item.courseId,
      title: item.title,
      thumbnailUrl: item.thumbnailUrl,
      price: item.price,
    }))
  }, [directOrder, items])

  useEffect(() => {
    let active = true
    const courseIds = Array.from(new Set(checkoutItems.map((item) => Number(item.courseId)).filter(Boolean)))
    if (courseIds.length > 0) {
      Promise.all([
        api.getCategories().catch(() => []),
        Promise.all(courseIds.map((id) => api.getCourse(id).catch(() => undefined)))
      ]).then(([categories, courses]) => {
        if (!active) return
        const catMap = new Map(categories.map((c) => [c.id, c.parentCategoryId]))
        const map: Record<number, number[]> = {}
        courses.forEach((c) => {
          if (c?.category) {
            const catId = Number(c.category)
            const parentId = catMap.get(catId)
            map[Number(c.id)] = parentId ? [catId, parentId] : [catId]
          }
        })
        setCourseCategoryMap(map)
      })
    }
    return () => {
      active = false
    }
  }, [checkoutItems])

  const subtotal = useMemo(() => {
    if (directOrder) return directOrder.totalPrice
    return checkoutItems.reduce((sum, item) => sum + item.price, 0)
  }, [checkoutItems, directOrder])

  const baseFinalPrice = directOrder?.finalPrice ?? subtotal

  const calculateDiscount = (coupon: CouponOption, targetAmount: number) => {
    if (coupon.kind === 'percent') {
      const calculated = Math.round((targetAmount * coupon.value) / 100)
      return coupon.maxDiscountAmount ? Math.min(calculated, coupon.maxDiscountAmount) : calculated
    }
    return Math.min(coupon.value, targetAmount)
  }

  const { totalItemDiscount, itemDiscounts } = useMemo(() => {
    let sum = 0
    const discounts: Record<string, number> = {}

    for (const item of checkoutItems) {
      const selectedId = itemCouponIds[item.key]
      if (!selectedId || selectedId === 'none') {
        discounts[item.key] = 0
        continue
      }
      const coupon = userCoupons.find((c) => c.id === selectedId)
      if (!coupon) continue

      const discount = calculateDiscount(coupon, item.price)
      discounts[item.key] = discount
      sum += discount
    }
    return { totalItemDiscount: sum, itemDiscounts: discounts }
  }, [checkoutItems, itemCouponIds, userCoupons])

  const stackableCoupon = userCoupons.find((c) => c.id === stackableCouponId)
  
  const stackableDiscount = useMemo(() => {
    if (!stackableCoupon) return 0
    // 중복 쿠폰은 원래 총 상품 금액 기준으로 계산합니다.
    return calculateDiscount(stackableCoupon, subtotal)
  }, [stackableCoupon, subtotal])

  const [itemCouponIdsMap, setItemCouponIdsMap] = useState<Record<number, number>>({})

  useEffect(() => {
    const newMap: Record<number, number> = {}
    Object.entries(itemCouponIds).forEach(([key, id]) => {
      if (id !== 'none') {
        const item = checkoutItems.find((i) => i.key === key)
        if (item && item.courseId) {
          newMap[Number(item.courseId)] = Number(id)
        }
      }
    })
    setItemCouponIdsMap(newMap)
  }, [itemCouponIds, checkoutItems])

  const parsedStackableCouponId = useMemo(() => {
    return stackableCouponId === 'none' ? null : Number(stackableCouponId)
  }, [stackableCouponId])

  const couponDiscount = Math.min(subtotal, totalItemDiscount + stackableDiscount)

  const displayTotal = Math.max(0, baseFinalPrice - couponDiscount)
  const isSubmitting = step === 'creating-order' || step === 'mock-confirming' || step === 'opening-toss' || step === 'opening-nicepay'

  const stepTextByStep: Record<CheckoutStep, string> = {
    idle: '',
    'loading-order': '주문 정보를 불러오는 중...',
    'creating-order': '주문을 생성하는 중...',
    'mock-confirming': 'Mock 결제를 승인하는 중...',
    'opening-toss': 'Toss 결제창을 여는 중...',
    'opening-nicepay': 'NICEPAY 결제창을 여는 중...',
  }
  const stepText = stepTextByStep[step]

  const createOrGetOrder = async () => {
    if (directOrder) return directOrder
    setStep('creating-order')
    return api.createOrderFromCart()
  }

  const buildPendingPayment = (order: OrderDetailResponse, paymentProvider: PaymentProvider): PendingPayment => {
    const pendingPayment: PendingPayment = {
      serviceOrderId: order.orderId,
      orderNumber: order.orderNumber,
      amount: Math.max(0, order.finalPrice - couponDiscount),
      orderName: getOrderName(order),
      provider: paymentProvider,
      fromCart: !directOrder,
      itemCouponIds: Object.keys(itemCouponIdsMap).length > 0 ? itemCouponIdsMap : null,
      stackableCouponId: parsedStackableCouponId,
      idempotencyKey: createPaymentIdempotencyKey(order.orderId, paymentProvider),
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
    if (submittingRef.current) return
    if (!assertReadyToPay()) return

    submittingRef.current = true
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
        pendingPayment.itemCouponIds,
        pendingPayment.stackableCouponId,
        pendingPayment.idempotencyKey,
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
      submittingRef.current = false
    }
  }

  async function handleTossPay() {
    if (submittingRef.current) return
    if (!assertReadyToPay()) return

    if (!canUseToss) {
      toast.error('Toss client key가 설정되지 않았습니다. .env.local을 확인해주세요.')
      return
    }

    submittingRef.current = true
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
      setStep('idle')
      submittingRef.current = false
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Toss 결제창 호출에 실패했습니다.'
      toast.error(message)
      setStep('idle')
      submittingRef.current = false
    }
  }

  async function handleNicepayPay() {
    if (submittingRef.current) return
    if (!assertReadyToPay()) return

    if (!canUseNicepay) {
      toast.error('NICEPAY MID가 설정되지 않았습니다. .env.local을 확인해주세요.')
      return
    }

    submittingRef.current = true
    setStep('creating-order')
    try {
      const order = await createOrGetOrder()
      const pendingPayment = buildPendingPayment(order, 'nicepay')
      const prepare = await api.prepareNicepayPayment(order.orderId, {
        payMethod: 'CARD',
        itemCouponIds: pendingPayment.itemCouponIds,
        stackableCouponId: pendingPayment.stackableCouponId,
      })

      setStep('opening-nicepay')
      const result = await requestNicepayPayment(prepare)

      const paymentKey = result.TxTid || result.TID || result.tid
      if (!paymentKey) {
        throw new Error('NICEPAY 결제 키를 확인할 수 없습니다.')
      }
      const payMethod = result.PayMethod || 'CARD'

      const paymentResponse = await api.confirmProviderPayment(order.orderId, 'NICEPAY', {
        paymentKey,
        method: resolveNicepayMethod(result),
        amount: pendingPayment.amount,
        itemCouponIds: pendingPayment.itemCouponIds,
        stackableCouponId: pendingPayment.stackableCouponId,
        idempotencyKey: pendingPayment.idempotencyKey,
        authResultCode: result.AuthResultCode,
        authResultMsg: result.AuthResultMsg,
        authToken: result.AuthToken,
        txTid: paymentKey,
        mid: result.MID,
        moid: result.Moid || result.moid,
        signature: result.Signature,
        nextAppUrl: result.NextAppURL,
        netCancelUrl: result.NetCancelURL,
        payMethod,
      })

      if (pendingPayment.fromCart && paymentResponse.orderStatus === 'PAID') {
        await clear()
      }

      const orderNumber = paymentResponse.orderNumber || order.orderNumber
      const amount = paymentResponse.amount || pendingPayment.amount
      const status = paymentResponse.paymentStatus === 'SUCCESS'
        ? 'success'
        : paymentResponse.paymentStatus === 'DECLINED'
          ? 'fail'
          : 'pending'
      router.push(
        `/orders/complete?payment=nicepay&status=${status}&serviceOrderId=${order.orderId}&orderId=${encodeURIComponent(orderNumber)}&amount=${amount}`,
      )
    } catch (err) {
      const message = err instanceof Error ? err.message : 'NICEPAY 결제창 호출에 실패했습니다.'
      toast.error(message)
      setStep('idle')
      submittingRef.current = false
    }
  }

  const handlePay = () => {
    if (provider === 'toss') {
      void handleTossPay()
      return
    }

    if (provider === 'nicepay') {
      void handleNicepayPay()
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
                <div key={item.key} className="flex gap-4 border-b pb-4 last:border-0 last:pb-0">
                  <Image
                    src={item.thumbnailUrl || '/placeholder.svg'}
                    alt={item.title}
                    width={96}
                    height={56}
                    className="h-16 w-24 rounded-md object-cover"
                  />
                  <div className="flex flex-1 flex-col gap-2">
                    <div className="flex items-start justify-between gap-4">
                      <p className="line-clamp-2 flex-1 text-sm font-medium">{item.title}</p>
                      <div className="text-right">
                        <span className="shrink-0 text-sm font-semibold">
                          {formatKRW(item.price)}
                        </span>
                        {itemDiscounts[item.key] ? (
                          <p className="mt-0.5 text-xs text-destructive">
                            - {formatKRW(itemDiscounts[item.key])} 할인
                          </p>
                        ) : null}
                      </div>
                    </div>
                    <div className="mt-auto w-full max-w-sm">
                      <Dialog>
                        <DialogTrigger asChild>
                          <Button variant="outline" size="sm" className="w-full justify-between font-normal text-xs h-8 px-3">
                            <span className="truncate">
                              {(() => {
                                const selectedVal = itemCouponIds[item.key]
                                if (!selectedVal || selectedVal === 'none') return '이 상품에 적용할 쿠폰 선택'
                                const c = userCoupons.find((x) => x.id === selectedVal)
                                if (!c) return '이 상품에 적용할 쿠폰 선택'
                                const discount = calculateDiscount(c, item.price)
                                return `${c.name} (-${formatKRW(discount)})`
                              })()}
                            </span>
                            <ChevronDown className="h-4 w-4 opacity-50 shrink-0 ml-2" />
                          </Button>
                        </DialogTrigger>
                        <DialogContent className="max-w-md w-full p-6">
                          <DialogHeader className="mb-4">
                            <DialogTitle>적용 가능한 쿠폰</DialogTitle>
                          </DialogHeader>
                          <div className="flex flex-col gap-3 max-h-[60vh] overflow-y-auto pr-2 pb-2">
                            <DialogClose asChild>
                              <Button 
                                variant={(!itemCouponIds[item.key] || itemCouponIds[item.key] === 'none') ? 'default' : 'outline'}
                                className="w-full justify-start h-auto py-3 px-4"
                                onClick={() => setItemCouponIds((prev) => ({ ...prev, [item.key]: 'none' }))}
                              >
                                선택 안 함
                              </Button>
                            </DialogClose>
                            {userCoupons
                              .filter((c) => {
                                if (c.isStackable) return false
                                if (c.courseIds?.length) return c.courseIds.includes(Number(item.courseId))
                                if (c.categoryIds?.length) {
                                  const categoryIds = courseCategoryMap[Number(item.courseId)]
                                  return categoryIds ? categoryIds.some(id => c.categoryIds!.includes(id)) : false
                                }
                                return true
                              })
                              .map((c) => ({ couponOption: c, discount: calculateDiscount(c, item.price) }))
                              .sort((a, b) => {
                                if (a.discount !== b.discount) return b.discount - a.discount
                                if (a.couponOption.endDate && b.couponOption.endDate) return new Date(a.couponOption.endDate).getTime() - new Date(b.couponOption.endDate).getTime()
                                if (a.couponOption.endDate) return -1
                                if (b.couponOption.endDate) return 1
                                return 0
                              })
                              .map(({ couponOption, discount }) => {
                                const isUsedByOtherItem = Object.entries(itemCouponIds).some(([key, id]) => key !== item.key && id === couponOption.id)
                                const isSelected = itemCouponIds[item.key] === couponOption.id
                                return (
                                  <DialogClose asChild key={couponOption.id}>
                                    <Button
                                      variant={isSelected ? 'default' : 'outline'}
                                      disabled={isUsedByOtherItem && !isSelected}
                                      className={`w-full justify-start flex-col items-start h-auto py-3 px-4 gap-1 border-2 ${isSelected ? 'border-primary' : 'border-border'}`}
                                      onClick={() => setItemCouponIds((prev) => ({ ...prev, [item.key]: couponOption.id }))}
                                    >
                                      <div className="flex w-full justify-between items-center text-sm">
                                        <span className="font-semibold text-left line-clamp-1">{couponOption.name}</span>
                                        <span className={`font-bold shrink-0 ml-2 ${isSelected ? 'text-primary-foreground' : 'text-primary'}`}>
                                          -{formatKRW(discount)}
                                        </span>
                                      </div>
                                      <div className="flex w-full justify-between items-center text-xs opacity-80 mt-1">
                                        <span>{couponOption.originalAmountString ? `${couponOption.originalAmountString} 할인` : ''}</span>
                                        <div className="flex gap-2">
                                          {isUsedByOtherItem && !isSelected ? <span className="text-destructive font-semibold">다른 상품에 적용됨</span> : null}
                                          {couponOption.endDate ? <span>{new Date(couponOption.endDate).toLocaleDateString().replace(/\.$/, '')} 만료</span> : null}
                                        </div>
                                      </div>
                                    </Button>
                                  </DialogClose>
                                )
                              })}
                          </div>
                        </DialogContent>
                      </Dialog>
                    </div>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">장바구니 쿠폰 목록</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="space-y-2">
                <Dialog>
                  <DialogTrigger asChild>
                    <Button variant="outline" className="w-full justify-between font-normal px-3">
                      <span className="truncate">
                        {(() => {
                          const selectedVal = stackableCouponId
                          if (!selectedVal || selectedVal === 'none') return '사용할 중복 쿠폰을 선택하세요'
                          const c = userCoupons.find((x) => x.id === selectedVal)
                          if (!c) return '사용할 중복 쿠폰을 선택하세요'
                          const discount = calculateDiscount(c, subtotal)
                          return `${c.name} (-${formatKRW(discount)})`
                        })()}
                      </span>
                      <ChevronDown className="h-4 w-4 opacity-50 shrink-0 ml-2" />
                    </Button>
                  </DialogTrigger>
                  <DialogContent className="max-w-md w-full p-6">
                    <DialogHeader className="mb-4">
                      <DialogTitle>추가 할인 쿠폰 선택</DialogTitle>
                    </DialogHeader>
                    <div className="flex flex-col gap-3 max-h-[60vh] overflow-y-auto pr-2 pb-2">
                      <DialogClose asChild>
                        <Button 
                          variant={(!stackableCouponId || stackableCouponId === 'none') ? 'default' : 'outline'}
                          className="w-full justify-start h-auto py-3 px-4"
                          onClick={() => setStackableCouponId('none')}
                        >
                          선택 안 함
                        </Button>
                      </DialogClose>
                      {userCoupons
                        .filter((c) => c.isStackable)
                        .map((c) => ({ couponOption: c, discount: calculateDiscount(c, subtotal) }))
                        .sort((a, b) => {
                          if (a.discount !== b.discount) return b.discount - a.discount
                          if (a.couponOption.endDate && b.couponOption.endDate) return new Date(a.couponOption.endDate).getTime() - new Date(b.couponOption.endDate).getTime()
                          if (a.couponOption.endDate) return -1
                          if (b.couponOption.endDate) return 1
                          return 0
                        })
                        .map(({ couponOption, discount }) => {
                          const applicable = !couponOption.minOrderAmount || subtotal >= couponOption.minOrderAmount
                          const isSelected = stackableCouponId === couponOption.id
                          return (
                            <DialogClose asChild key={couponOption.id}>
                              <Button
                                variant={isSelected ? 'default' : 'outline'}
                                disabled={!applicable}
                                className={`w-full justify-start flex-col items-start h-auto py-3 px-4 gap-1 border-2 ${isSelected ? 'border-primary' : 'border-border'}`}
                                onClick={() => setStackableCouponId(couponOption.id)}
                              >
                                <div className="flex w-full justify-between items-center text-sm">
                                  <span className="font-semibold text-left line-clamp-1">{couponOption.name}</span>
                                  <span className={`font-bold shrink-0 ml-2 ${isSelected ? 'text-primary-foreground' : 'text-primary'}`}>
                                    -{formatKRW(discount)}
                                  </span>
                                </div>
                                <div className="flex w-full justify-between items-center text-xs opacity-80 mt-1">
                                  <span>{couponOption.originalAmountString ? `${couponOption.originalAmountString} 할인` : ''}</span>
                                  <div className="flex gap-2">
                                    {!applicable ? <span className="text-destructive font-semibold">적용 불가(최소주문금액 미달)</span> : null}
                                    {couponOption.endDate ? <span>{new Date(couponOption.endDate).toLocaleDateString().replace(/\.$/, '')} 만료</span> : null}
                                  </div>
                                </div>
                              </Button>
                            </DialogClose>
                          )
                        })}
                    </div>
                  </DialogContent>
                </Dialog>
              </div>
              {userCoupons.filter(c => c.isStackable).length === 0 ? (
                <p className="text-xs text-muted-foreground">사용 가능한 중복 쿠폰이 없습니다.</p>
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
                  {
                    value: 'nicepay',
                    label: 'NICEPAY 결제',
                    description: canUseNicepay
                      ? 'NICEPAY 결제창 인증 후 백엔드 Provider confirm API를 호출합니다.'
                      : 'NICEPAY MID 설정 후 사용할 수 있습니다.',
                    disabled: !canUseNicepay,
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
