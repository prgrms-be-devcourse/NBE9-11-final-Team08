// frontend/app/(account)/dashboard/page.tsx
import { DashboardView } from '@/components/dashboard/dashboard-view'
import { api } from '@/lib/api'

export default async function DashboardPage() {
  const courses = await api.getMyStudies()

  return <DashboardView courses={courses} />
}
