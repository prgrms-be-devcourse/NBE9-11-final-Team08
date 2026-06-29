import { api } from '@/lib/api'
import { CourseManager } from '@/components/admin/course-manager'

export const metadata = {
  title: '강좌 관리 — PlayLearn 관리자 콘솔',
}

export default async function AdminCoursesPage() {
  const response = await api.getAdminCourses(undefined, 0, 100)
  return <CourseManager initialCourses={response.content} />
}
