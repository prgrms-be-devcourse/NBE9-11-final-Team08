import { notFound, redirect } from 'next/navigation'
import { StudyShell } from '@/components/study/study-shell'
import { StudySettingsView } from '@/components/study/study-settings-view'
import { api } from '@/lib/api'

export default async function StudySettingsPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const study = await api.getStudy(id)
  if (!study) notFound()
  // 설정은 운영자만 접근할 수 있다.
  if (study.myRole !== 'owner') redirect(`/study/${id}`)
  return (
    <StudyShell study={study}>
      <StudySettingsView study={study} />
    </StudyShell>
  )
}
