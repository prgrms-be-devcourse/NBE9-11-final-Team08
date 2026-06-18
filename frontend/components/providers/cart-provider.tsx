'use client'

import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import type { CartItem, Course } from '@/lib/types'

interface CartContextValue {
  items: CartItem[]
  addItem: (course: Course) => void
  removeItem: (courseId: string) => void
  removeItems: (courseIds: string[]) => void
  clear: () => void
  has: (courseId: string) => boolean
  total: number
}

const CartContext = createContext<CartContextValue | null>(null)

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([])

  const addItem = (course: Course) => {
    setItems((prev) => {
      if (prev.some((i) => i.courseId === course.id)) return prev
      const price = course.discountRate
        ? Math.round((course.price * (100 - course.discountRate)) / 100)
        : course.price
      return [
        ...prev,
        { courseId: course.id, title: course.title, thumbnailUrl: course.thumbnailUrl, price },
      ]
    })
  }

  const removeItem = (courseId: string) =>
    setItems((prev) => prev.filter((i) => i.courseId !== courseId))

  const removeItems = (courseIds: string[]) =>
    setItems((prev) => prev.filter((i) => !courseIds.includes(i.courseId)))

  const clear = () => setItems([])

  const value = useMemo<CartContextValue>(
    () => ({
      items,
      addItem,
      removeItem,
      removeItems,
      clear,
      has: (courseId: string) => items.some((i) => i.courseId === courseId),
      total: items.reduce((sum, i) => sum + i.price, 0),
    }),
    [items],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within CartProvider')
  return ctx
}
