import { EventsView } from '@/components/events/events-view'
import { api } from '@/lib/api'

export default async function EventsPage() {
  const coupons = await api.getCoupons()
  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <h1 className="text-2xl font-bold">쿠폰/이벤트</h1>
      <p className="mt-1 text-sm text-muted-foreground">
        진행 중인 이벤트에 참여하고 다양한 혜택을 받아보세요.
      </p>
      <EventsView coupons={coupons} />
    </div>
  )
}
