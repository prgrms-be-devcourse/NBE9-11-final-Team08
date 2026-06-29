// frontend/components/checkout/payment-result-view.tsx
'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import Link from 'next/link'
import { AlertCircle, CheckCircle2, Loader2, XCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { api } from '@/lib/api'
import { getPendingPayment, removePendingPayment } from '@/lib/checkout-payment'
import { formatOrderStatus, formatPaymentStatus } from '@/lib/order-payment-labels'
import { formatKRW } from '@/lib/utils'
import { useCart } from '@/components/providers/cart-provider'
import type { ConfirmPaymentResponse } from '@/lib/types'

export type PaymentResultParams = {
  payment?: string
  status?: string
  serviceOrderId?: string
  orderId?: string
  amount?: string
  paymentKey?: string
  code?: string
  message?: string
}

type ResultState = 'checking' | 'success' | 'fail' | 'api-error'

export function PaymentResultView({ params }: { params: PaymentResultParams }) {
  const { clear } = useCart()
  const calledRef = useRef(false)
  const [state, setState] = useState<ResultState>(() => {
    if (params.payment === 'toss' && params.status !== 'fail') return 'checking'
    if (params.status === 'fail') return 'fail'
    return 'success'
  })
  const [detailMessage, setDetailMessage] = useState(params.message || '')
  const [confirmedPayment, setConfirmedPayment] = useState<ConfirmPaymentResponse | null>(null)

  const pendingPayment = useMemo(
    () => getPendingPayment(params.serviceOrderId, params.orderId),
    [params.serviceOrderId, params.orderId],
  )

  useEffect(() => {
    if (calledRef.current) return
    calledRef.current = true

    if (params.payment !== 'toss') {
      if (params.status === 'success' && pendingPayment) {
        removePendingPayment(pendingPayment)
      }
      return
    }

    if (params.status === 'fail') {
      setState('fail')
      setDetailMessage(params.message || params.code || 'Toss 결제창에서 결제가 완료되지 않았습니다.')
      return
    }

    const paymentKey = params.paymentKey
    const tossOrderId = params.orderId
    const amount = Number(params.amount)

    if (!paymentKey || !tossOrderId || !Number.isFinite(amount)) {
      setState('api-error')
      setDetailMessage('Toss 결제 완료 파라미터(paymentKey, orderId, amount)가 부족합니다.')
      return
    }

    if (!pendingPayment) {
      setState('api-error')
      setDetailMessage('브라우저에 저장된 주문 대기 정보를 찾지 못했습니다. 다시 주문을 시도해주세요.')
      return
    }

    if (tossOrderId !== pendingPayment.orderNumber) {
      setState('api-error')
      setDetailMessage('Toss에서 돌아온 주문번호가 생성된 주문번호와 다릅니다.')
      return
    }

    if (amount !== pendingPayment.amount) {
      setState('api-error')
      setDetailMessage('Toss에서 돌아온 금액이 주문 금액과 다릅니다.')
      return
    }

    const idempotencyKey =
      pendingPayment.idempotencyKey ?? `${pendingPayment.provider}-${pendingPayment.serviceOrderId}-${paymentKey}`

    api.confirmTossPayment(pendingPayment.serviceOrderId, {
      paymentKey,
      method: 'CARD',
      amount,
      issuedCouponId: pendingPayment.issuedCouponId,
      idempotencyKey,
    })
      .then(async (response) => {
        setConfirmedPayment(response)

        if (response.paymentStatus === 'SUCCESS' && response.orderStatus === 'PAID') {
          if (pendingPayment.fromCart) {
            await clear()
          }
          removePendingPayment(pendingPayment)
          setState('success')
          return
        }

        const statusMessage = `결제 상태: ${formatPaymentStatus(response.paymentStatus)}, 주문 상태: ${formatOrderStatus(response.orderStatus)}`
        if (response.paymentStatus === 'PROCESSING' || response.paymentStatus === 'UNKNOWN') {
          setState('checking')
          setDetailMessage(statusMessage)
          return
        }

        setState('fail')
        setDetailMessage(statusMessage)
      })
      .catch((err) => {
        setState('api-error')
        setDetailMessage(err instanceof Error ? err.message : '백엔드 Toss confirm API 호출에 실패했습니다.')
      })
  }, [clear, params, pendingPayment])

  const orderNumber = confirmedPayment?.orderNumber || params.orderId || pendingPayment?.orderNumber || '-'
  const serviceOrderId = confirmedPayment?.orderId || pendingPayment?.serviceOrderId || params.serviceOrderId
  const amount = confirmedPayment?.amount || Number(params.amount || pendingPayment?.amount || 0)

  const copy = {
    checking: {
      title: '결제 결과 확인 중입니다',
      description: detailMessage || 'Toss 결제 결과를 백엔드 confirm API로 승인하고 있습니다.',
      icon: <Loader2 className="size-16 animate-spin text-primary" />,
    },
    success: {
      title: '결제가 완료되었습니다',
      description: '이제 바로 학습을 시작할 수 있습니다.',
      icon: <CheckCircle2 className="size-16 text-primary" />,
    },
    fail: {
      title: '결제에 실패했습니다',
      description: detailMessage || '결제가 완료되지 않았습니다. 주문은 결제 대기 상태로 남아있을 수 있습니다.',
      icon: <XCircle className="size-16 text-destructive" />,
    },
    'api-error': {
      title: '결제 확인 중 오류가 발생했습니다',
      description: detailMessage || '결제 승인 API 호출 또는 금액 검증 중 문제가 발생했습니다.',
      icon: <AlertCircle className="size-16 text-destructive" />,
    },
  }[state]

  return (
    <div className="mx-auto max-w-xl px-4 py-20">
      <Card>
        <CardContent className="flex flex-col items-center gap-5 py-12 text-center">
          {copy.icon}
          <div>
            <h1 className="text-2xl font-bold">{copy.title}</h1>
            <p className="mt-2 text-sm text-muted-foreground">{copy.description}</p>
          </div>

          <div className="w-full space-y-2 rounded-lg bg-muted p-5 text-sm">
            <div className="flex justify-between gap-4">
              <span className="text-muted-foreground">서비스 주문 ID</span>
              <span className="font-medium">{serviceOrderId ?? '-'}</span>
            </div>
            <div className="flex justify-between gap-4">
              <span className="text-muted-foreground">주문번호</span>
              <span className="break-all font-medium">{orderNumber}</span>
            </div>
            <div className="flex justify-between gap-4">
              <span className="text-muted-foreground">결제금액</span>
              <span className="font-semibold text-primary">{formatKRW(amount)}</span>
            </div>
          </div>

          <div className="flex w-full gap-3">
            {state === 'success' ? (
              <>
                <Button asChild variant="outline" className="flex-1">
                  <Link href="/orders">주문 내역</Link>
                </Button>
                <Button asChild className="flex-1">
                  <Link href="/mypage">학습 시작하기</Link>
                </Button>
              </>
            ) : (
              <>
                <Button asChild variant="outline" className="flex-1">
                  <Link href={serviceOrderId ? `/checkout?orderId=${serviceOrderId}` : '/cart'}>다시 시도</Link>
                </Button>
                <Button asChild className="flex-1">
                  <Link href="/orders">주문 내역</Link>
                </Button>
              </>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
