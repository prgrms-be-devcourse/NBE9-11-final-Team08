import type { OrderDetailResponse } from './types'

export type PaymentProvider = 'mock' | 'toss' | 'nicepay'

export interface PendingPayment {
  serviceOrderId: number
  orderNumber: string
  amount: number
  orderName: string
  provider: PaymentProvider
  fromCart: boolean
  itemCouponIds?: Record<number, number> | null
  stackableCouponId?: number | null
  idempotencyKey?: string
  createdAt: string
}

const STORAGE_PREFIX = 'playlearn.pendingPayment'

const isBrowser = () => typeof window !== 'undefined'

const byIdKey = (serviceOrderId: number | string) => `${STORAGE_PREFIX}:id:${serviceOrderId}`
const byOrderNumberKey = (orderNumber: string) => `${STORAGE_PREFIX}:orderNumber:${orderNumber}`

type StoredPendingPayment = PendingPayment & {
  orderId?: number | string
}

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

  const normalized = normalizePendingPayment(payment)
  if (!normalized) return

  const serialized = JSON.stringify(normalized)
  window.localStorage.setItem(byIdKey(normalized.serviceOrderId), serialized)
  window.localStorage.setItem(byOrderNumberKey(normalized.orderNumber), String(normalized.serviceOrderId))
}

export function getPendingPayment(serviceOrderId?: string | null, orderNumber?: string | null) {
  if (!isBrowser()) return null

  const id = serviceOrderId || (orderNumber ? window.localStorage.getItem(byOrderNumberKey(orderNumber)) : null)
  if (id) {
    const pendingPayment = readPendingPayment(byIdKey(id))
    if (pendingPayment) return pendingPayment
  }

  if (!orderNumber) return null

  for (let index = 0; index < window.localStorage.length; index += 1) {
    const key = window.localStorage.key(index)
    if (!key?.startsWith(`${STORAGE_PREFIX}:id:`)) continue

    const pendingPayment = readPendingPayment(key)
    if (pendingPayment?.orderNumber === orderNumber) {
      return pendingPayment
    }
  }

  return null
}

export function removePendingPayment(payment: PendingPayment) {
  if (!isBrowser()) return

  const normalized = normalizePendingPayment(payment)
  if (!normalized) return

  window.localStorage.removeItem(byIdKey(normalized.serviceOrderId))
  window.localStorage.removeItem(byOrderNumberKey(normalized.orderNumber))
}

function readPendingPayment(key: string) {
  const raw = window.localStorage.getItem(key)
  if (!raw) return null

  try {
    return normalizePendingPayment(JSON.parse(raw))
  } catch {
    return null
  }
}

function normalizePendingPayment(value: unknown): PendingPayment | null {
  if (!value || typeof value !== 'object') return null

  const stored = value as StoredPendingPayment
  const serviceOrderId = stored.serviceOrderId ?? stored.orderId
  const normalizedServiceOrderId = Number(serviceOrderId)

  if (!Number.isFinite(normalizedServiceOrderId) || !stored.orderNumber) {
    return null
  }

  return {
    ...stored,
    serviceOrderId: normalizedServiceOrderId,
  }
}
