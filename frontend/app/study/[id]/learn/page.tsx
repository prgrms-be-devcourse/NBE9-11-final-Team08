import { notFound } from 'next/navigation'
import { StudyView } from '@/components/study/study-view'
import { api } from '@/lib/api'

export default async function StudyLearnPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const study = await api.getStudy(id)
  if (!study) notFound()

  const course = await api.getCourse(study.courseId)
  
  if (!course) notFound()
  const readOnly = study.status === 'READONLY' || study.status === 'INACTIVE'

  return (
    <StudyView course={course} studyId={study.id} readOnly={readOnly} qna={[]} />
  )
}
