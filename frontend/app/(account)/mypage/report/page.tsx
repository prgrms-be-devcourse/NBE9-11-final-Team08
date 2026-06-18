import { ReportView } from '@/components/account/report-view'
import { api } from '@/lib/api'

export const metadata = {
  title: '내 스터디 리포트 — PlayLearn',
}

export default async function ReportPage() {
  const report = await api.getStudyReport('study-1')
  if (!report) return null
  return <ReportView report={report} />
}
