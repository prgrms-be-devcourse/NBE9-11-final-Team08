import { StudyListView } from '@/components/study/study-list-view'
import { api } from '@/lib/api'

export const metadata = {
  title: '내 스터디 — PlayLearn',
}

export default async function StudiesPage() {
  const courses = await api.getEnrolledCourses()
  return <StudyListView courses={courses} />
}
