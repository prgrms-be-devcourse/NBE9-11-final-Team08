import { api } from '@/lib/api'
import { CouponRequestsList } from '@/components/admin/coupon-requests-list'

export default async function AdminCouponRequestsPage() {
  // Let's fetch the first page from the server to pass initial data
  const initialData = await api.getCouponIssueRequests(0, 20)
  
  return <CouponRequestsList initialData={initialData} />
}
