import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatKRW(value?: number | null) {
  if (value === null || value === undefined) return '0원'
  return `${value.toLocaleString('ko-KR')}원`
}

export function discountedPrice(price: number, rate?: number) {
  if (!rate) return price
  return Math.round((price * (100 - rate)) / 100)
}

// 백엔드 LocalDateTime 은 배열([year, month, ...]) 또는 ISO 문자열로 올 수 있다.
export function toDateValue(value: unknown): Date | null {
  if (Array.isArray(value)) {
    const [year, month = 1, day = 1, hour = 0, minute = 0, second = 0, nano = 0] =
      value as number[]
    return new Date(year, month - 1, day, hour, minute, second, Math.floor(nano / 1_000_000))
  }
  if (value === null || value === undefined || value === '') return null
  const date = new Date(value as string | number)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatDateTime(value: unknown): string {
  const date = toDateValue(value)
  return date ? date.toLocaleString('ko-KR') : ''
}
