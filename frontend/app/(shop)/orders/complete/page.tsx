import { PaymentResultView, type PaymentResultParams } from '@/components/checkout/payment-result-view'

export default async function OrderCompletePage({
  searchParams,
}: {
  searchParams: Promise<PaymentResultParams>
}) {
  const params = await searchParams

  return <PaymentResultView params={params} />
}
