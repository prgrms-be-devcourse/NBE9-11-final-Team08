// frontend/app/(account)/dashboard/page.tsx
import { DashboardView } from '@/components/dashboard/dashboard-view'
import { api } from '@/lib/api'

export default async function DashboardPage() {
  const courses = await api.getMyStudies()
  
  const studyId = courses.length > 0 ? Number(courses[0].id) : 0
  const feed = studyId > 0 
    ? (await api.getStudyActivities(studyId)).content 
    : []

  return <DashboardView courses={courses} feed={feed} />
}
