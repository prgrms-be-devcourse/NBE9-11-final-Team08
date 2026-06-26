'use client'

import { useState } from 'react'
import { BarChart3, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { ReportView } from '@/components/account/report-view'
import { api } from '@/lib/api'
import type { StudyReport } from '@/lib/types'

// 학습 화면에서 페이지 이동 없이 학습 리포트를 모달로 띄운다.
// 리포트는 모달을 처음 열 때 한 번만 가져온다(집계 비용 절감).
export function StudyReportDialog({ studyId }: { studyId: string }) {
  const [open, setOpen] = useState(false)
  const [report, setReport] = useState<StudyReport | null>(null)
  const [loading, setLoading] = useState(false)

  const handleOpenChange = async (next: boolean) => {
    setOpen(next)
    if (next && !report && !loading) {
      setLoading(true)
      try {
        setReport(await api.getStudyReport(studyId))
      } finally {
        setLoading(false)
      }
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <BarChart3 className="mr-1 h-4 w-4" /> 학습 리포트
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>학습 리포트</DialogTitle>
        </DialogHeader>
        {loading || !report ? (
          <div className="flex h-64 items-center justify-center text-muted-foreground">
            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            리포트를 불러오는 중…
          </div>
        ) : (
          <ReportView report={report} />
        )}
      </DialogContent>
    </Dialog>
  )
}
