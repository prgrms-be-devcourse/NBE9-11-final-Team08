import { AnomalyPanel } from '@/components/admin/anomaly-panel'

export default function AdminAnomaliesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">이상 데이터 탐지</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          이탈률 임계값을 초과한 강좌와 짧은 시간에 몰린 중복 이벤트를 탐지합니다.
        </p>
      </div>

      <AnomalyPanel />
    </div>
  )
}
