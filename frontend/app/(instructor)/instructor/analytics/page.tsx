import { AnalyticsView } from '@/components/instructor/analytics-view'
import { api } from '@/lib/api'

export const metadata = {
  title: '판매 분석 — PlayLearn 판매자 센터',
}

export default async function AnalyticsPage() {
  const response = await api.getCourses()
  return <AnalyticsView courses={response.content} />
}
