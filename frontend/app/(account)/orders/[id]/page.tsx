import { notFound } from 'next/navigation'
import { api } from '@/lib/api'
import { OrderDetailView } from '@/components/checkout/order-detail-view'

export default async function OrderDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const order = await api.getOrder(id)
  if (!order) notFound()
  return <OrderDetailView order={order} />
}
