// frontend/components/providers/cart-provider.tsx
'use client'

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { CartItemResponse, Course } from '@/lib/types'
import { api } from '@/lib/api'
import { usePathname } from 'next/navigation'
import { toast } from 'sonner'

interface CartContextValue {
  items: CartItemResponse[]
  addItem: (course: Course) => Promise<void>
  removeItem: (cartItemId: number) => Promise<void>
  removeItems: (cartItemIds: number[]) => Promise<void>
  clear: () => Promise<void>
  refreshCart: () => Promise<void>
  has: (courseId: string | number) => boolean
  total: number
  loading: boolean
}

const CartContext = createContext<CartContextValue | null>(null)

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItemResponse[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const pathname = usePathname()

  const refreshCart = async () => {
    try {
      const res = await api.getCart()
      setItems(res.items || [])
      setTotal(res.totalPrice || 0)
    } catch (e) {
      console.error("장바구니 갱신 실패:", e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      refreshCart()
    } else {
      setItems([])
      setTotal(0)
      setLoading(false)
    }
  }, [pathname])

  const addItem = async (course: Course) => {
    // 1. 프론트에서 먼저 중복 체크
    if (items.some((i) => i.courseId.toString() === course.id.toString())) {
      toast.warning("이미 장바구니에 담긴 강의입니다.")
      return
    }

    try {
      // 2. API 호출
      await api.addToCart(Number(course.id))
      
      // 3. 성공 시 장바구니 갱신
      await refreshCart()
      toast.success("장바구니에 담겼습니다.")
    } catch (e: any) {
      // 4. 여기서 에러를 잡아서 처리 (앱이 죽지 않음)
      const errorMessage = e.message || ""
      if (errorMessage.includes("이미 장바구니에 담긴")) {
        toast.warning("이미 장바구니에 담긴 강의입니다.")
      } else {
        toast.error("장바구니 담기에 실패했습니다.")
      }
      console.error("장바구니 추가 에러:", e)
    }
  }

  const removeItem = async (cartItemId: number) => {
    try {
      await api.removeFromCart(cartItemId)
      await refreshCart()
      toast.success("제거되었습니다.")
    } catch (e) {
      toast.error("제거에 실패했습니다.")
      console.error(e)
    }
  }

  const removeItems = async (cartItemIds: number[]) => {
    for (const id of cartItemIds) {
      await removeItem(id)
    }
  }

  const clear = async () => {
    try {
      await api.clearCart()
      await refreshCart()
      toast.success("장바구니를 비웠습니다.")
    } catch (e) {
      toast.error("장바구니 비우기에 실패했습니다.")
      console.error(e)
    }
  }

  const value = useMemo<CartContextValue>(
    () => ({
      items,
      addItem,
      removeItem,
      removeItems,
      clear,
      refreshCart,
      has: (courseId: string | number) => items.some((i) => i.courseId.toString() === courseId.toString()),
      total,
      loading
    }),
    [items, total, loading],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within CartProvider')
  return ctx
}
