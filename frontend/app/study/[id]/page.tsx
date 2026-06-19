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
  const study = await api.getStudy(id)
  if (!study) notFound()
  return (
    <StudyShell study={study}>
      <StudyDashboard study={study} />
    </StudyShell>
  )
}
