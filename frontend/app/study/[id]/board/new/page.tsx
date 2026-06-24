import { notFound } from 'next/navigation'
import { StudyShell } from '@/components/study/study-shell'
import { StudyPostComposer } from '@/components/study/study-post-composer'
import { api } from '@/lib/api'

export default async function StudyBoardNewPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const study = await api.getStudyForEntry(id)
  if (!study) notFound()
  if (study.myRole === 'viewer') notFound()
  return (
    <StudyShell study={study}>
      <StudyPostComposer study={study} />
    </StudyShell>
  )
}
