import { notFound } from 'next/navigation'
import { StudyShell } from '@/components/study/study-shell'
import { StudyDashboard } from '@/components/study/study-dashboard'
import { api } from '@/lib/api'

export default async function StudyPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const study = await api.getStudyForEntry(id)
  if (!study) notFound()
  const [members, lastWatched] = await Promise.all([
    api.getStudyMembers(id),
    api.getLastWatched(study.courseId),
  ])
  return (
    <StudyShell study={study}>
      <StudyDashboard study={{ ...study, members }} lastWatched={lastWatched} />
    </StudyShell>
  )
}
