import { CheckoutView } from '@/components/checkout/checkout-view'

export default async function CheckoutPage({
  searchParams,
}: {
  searchParams: Promise<{ orderId?: string }>
}) {
  const { orderId } = await searchParams

  return <CheckoutView initialOrderId={orderId} />
}
