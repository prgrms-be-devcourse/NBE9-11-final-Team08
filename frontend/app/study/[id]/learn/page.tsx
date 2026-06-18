import { notFound } from 'next/navigation'
import { StudyView } from '@/components/study/study-view'
import { api } from '@/lib/api'

export default async function StudyLearnPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const [course, qna] = await Promise.all([api.getCourse(id), api.getQna(id)])
  if (!course) notFound()
  return <StudyView course={course} qna={qna} />
}
