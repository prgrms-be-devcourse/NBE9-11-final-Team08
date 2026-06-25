import { notFound } from 'next/navigation'
import { StudyShell } from '@/components/study/study-shell'
import { StudyReportView } from '@/components/study/study-report-view'
import { api } from '@/lib/api'

export const metadata = {
  title: '스터디 학습 리포트 — PlayLearn',
}

export default async function StudyReportPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const study = await api.getStudyForEntry(id)
  if (!study) notFound()
  if (study.myRole === 'viewer') notFound()

  const report = await api.getStudyReport(study.id)

  return (
    <StudyShell study={study}>
      <StudyReportView report={report} studyId={study.id} />
    </StudyShell>
  )
}
