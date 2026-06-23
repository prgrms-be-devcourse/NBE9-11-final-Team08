'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { ReportView } from '@/components/account/report-view'
import { cn } from '@/lib/utils'
import { api } from '@/lib/api'
import type { StudyReport } from '@/lib/types'

// 스터디별 학습 리포트. 학습 이벤트를 집계한 결과를 보여주고, 최신 데이터로 재집계할 수 있다.
export function StudyReportView({
  report,
  studyId,
}: {
  report: StudyReport
  studyId: string
}) {
  const router = useRouter()
  const [loading, setLoading] = useState(false)

  const refresh = async () => {
    setLoading(true)
    try {
      await api.generateStudyReport(studyId)
      toast.success('학습 이벤트를 다시 집계했어요.')
      router.refresh()
    } catch {
      toast.error('리포트 갱신에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button variant="outline" size="sm" onClick={refresh} disabled={loading}>
          <RefreshCw className={cn('mr-1 h-4 w-4', loading && 'animate-spin')} />
          {loading ? '집계 중…' : '리포트 갱신'}
        </Button>
      </div>
      <ReportView report={report} />
    </div>
  )
}
