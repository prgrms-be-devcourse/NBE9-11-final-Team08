import { DashboardView } from '@/components/dashboard/dashboard-view'
import { api } from '@/lib/api'

export const metadata = {
  title: '대시보드 — PlayLearn',
}

export default async function DashboardPage() {
  const [courses, feed] = await Promise.all([
    api.getEnrolledCourses(),
    api.getActivityFeed('study-1'),
  ])
  return <DashboardView courses={courses} feed={feed} />
}
