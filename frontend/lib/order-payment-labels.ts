import type { OrderStatus, PaymentStatus } from './types'

export const orderStatusLabels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '결제 대기',
  PAID: '결제 완료',
  CANCELED: '주문 취소',
  REFUNDED: '환불 완료',
  EXPIRED: '주문 만료',
}

export const paymentStatusLabels: Record<PaymentStatus, string> = {
  READY: '결제 대기',
  PROCESSING: '결제 처리 중',
  SUCCESS: '결제 완료',
  DECLINED: '결제 실패',
  UNKNOWN: '결제 확인 중',
  CANCELED: '결제 취소',
  REFUNDED: '환불 완료',
}

export function formatOrderStatus(status?: string | null) {
  if (!status) return '상태 없음'
  return orderStatusLabels[status as OrderStatus] ?? status
}

export function formatPaymentStatus(status?: string | null) {
  if (!status) return '상태 없음'
  return paymentStatusLabels[status as PaymentStatus] ?? status
}

export function getOrderStatusVariant(status?: string | null): 'secondary' | 'outline' | 'destructive' {
  if (status === 'PAID') return 'secondary'
  if (status === 'PENDING_PAYMENT') return 'outline'
  if (status === 'CANCELED' || status === 'REFUNDED' || status === 'EXPIRED') return 'destructive'
  return 'secondary'
}
