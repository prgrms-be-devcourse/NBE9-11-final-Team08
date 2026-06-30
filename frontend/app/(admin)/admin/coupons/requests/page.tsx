import { api } from '@/lib/api'
import { CouponRequestsList } from '@/components/admin/coupon-requests-list'

export default async function AdminCouponRequestsPage() {
  const initialData = await api.getCouponIssueRequests(0, 20)
  
  return <CouponRequestsList initialData={initialData} />
}
