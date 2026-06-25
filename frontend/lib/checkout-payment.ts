import type { OrderDetailResponse } from './types'

export type PaymentProvider = 'mock' | 'toss'

export interface PendingPayment {
  serviceOrderId: number
  orderNumber: string
  amount: number
  orderName: string
  provider: PaymentProvider
  fromCart: boolean
  issuedCouponId: number | null
  idempotencyKey?: string
  createdAt: string
}

const STORAGE_PREFIX = 'playlearn.pendingPayment'

const isBrowser = () => typeof window !== 'undefined'

const byIdKey = (serviceOrderId: number | string) => `${STORAGE_PREFIX}:id:${serviceOrderId}`
const byOrderNumberKey = (orderNumber: string) => `${STORAGE_PREFIX}:orderNumber:${orderNumber}`

export function getOrderName(order: OrderDetailResponse) {
  const items = order.items ?? []
  const firstTitle = items[0]?.courseTitle || '강의'
  return items.length > 1 ? `${firstTitle} 외 ${items.length - 1}건` : firstTitle
}

export function createPaymentIdempotencyKey(serviceOrderId: number | string, provider: PaymentProvider) {
  const randomPart = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`

  return `${provider}-${serviceOrderId}-${randomPart}`
}

export function savePendingPayment(payment: PendingPayment) {
  if (!isBrowser()) return

  const serialized = JSON.stringify(payment)
  window.localStorage.setItem(byIdKey(payment.serviceOrderId), serialized)
  window.localStorage.setItem(byOrderNumberKey(payment.orderNumber), String(payment.serviceOrderId))
}

export function getPendingPayment(serviceOrderId?: string | null, orderNumber?: string | null) {
  if (!isBrowser()) return null

  const id = serviceOrderId || (orderNumber ? window.localStorage.getItem(byOrderNumberKey(orderNumber)) : null)
  if (!id) return null

  const raw = window.localStorage.getItem(byIdKey(id))
  if (!raw) return null

  try {
    return JSON.parse(raw) as PendingPayment
  } catch {
    return null
  }
}

export function removePendingPayment(payment: PendingPayment) {
  if (!isBrowser()) return

  window.localStorage.removeItem(byIdKey(payment.serviceOrderId))
  window.localStorage.removeItem(byOrderNumberKey(payment.orderNumber))
}
