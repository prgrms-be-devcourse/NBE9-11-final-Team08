import { MyReportsView } from '@/components/account/my-reports-view'
import { api } from '@/lib/api'

export const metadata = {
  title: '내 스터디 리포트 — PlayLearn',
}

export default async function ReportPage() {
  const { reports, aggregate } = await api.getMyStudyReports()
  return <MyReportsView reports={reports} aggregate={aggregate} />
}
