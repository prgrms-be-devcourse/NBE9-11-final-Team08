import { notFound } from 'next/navigation'
import { StudyShell } from '@/components/study/study-shell'
import { StudyCourseView } from '@/components/study/study-course-view'
import { api } from '@/lib/api'

export default async function StudyCoursePage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const study = await api.getStudy(id)
  if (!study) notFound()

  const course = await api.getCourse(study.courseId)
  if (!course) notFound()

  return (
    <StudyShell study={study}>
      <StudyCourseView study={study} course={course} />
    </StudyShell>
  )
}
