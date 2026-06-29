import { AnalyticsView } from '@/components/instructor/analytics-view'
import { api } from '@/lib/api'

export const metadata = {
  title: '판매 분석 — PlayLearn 판매자 센터',
}

export default async function AnalyticsPage() {
  const data = await api.getSellerAnalytics('6m')
  return <AnalyticsView initialData={data} />
}
