import { CartView } from '@/components/checkout/cart-view'

export default function CartPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="text-2xl font-bold">장바구니</h1>
      <CartView />
    </div>
  )
}
