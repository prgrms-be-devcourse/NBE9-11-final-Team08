import { notFound } from 'next/navigation'
import { CourseDetail } from '@/components/course/course-detail'
import { api } from '@/lib/api'

export default async function CoursePage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const course = await api.getCourse(id)
  if (!course) notFound()
  return <CourseDetail course={course} />
}
