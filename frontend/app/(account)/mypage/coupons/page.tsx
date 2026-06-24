import { MyPageCouponsView } from '@/components/account/mypage-coupons-view'
import { api } from '@/lib/api'
import { redirect } from 'next/navigation'

export const metadata = {
  title: '내 쿠폰함 — PlayLearn',
}

export default async function MyCouponsPage() {
  const profile = await api.getProfile()
  if (!profile) {
    redirect('/login')
  }

  const [coupons, coursesRes] = await Promise.all([
    api.getMyCoupons(),
    api.getCourses(0, 100).catch(() => ({ content: [] })),
  ])

  const courses = coursesRes?.content || []

  return <MyPageCouponsView coupons={coupons} courses={courses} />
}
