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
