import { api } from '@/lib/api'
import { CouponManager } from '@/components/admin/coupon-manager'

export default async function AdminCouponsPage() {
  const coupons = await api.getAdminCoupons()
  return <CouponManager initialCoupons={coupons} />
}
