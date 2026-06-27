'use client'

import { useState } from 'react'
import { Layers } from 'lucide-react'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { ReportView } from '@/components/account/report-view'
import type { MyStudyReport } from '@/lib/types'

interface MyReportsViewProps {
  reports: MyStudyReport[]
  aggregate: MyStudyReport | null
}

export function MyReportsView({ reports, aggregate }: MyReportsViewProps) {
  // 합산 탭이 있으면 그것을 기본값으로, 없으면 첫 스터디를 기본값으로 한다.
  const defaultTab = aggregate ? 'all' : reports[0]?.studyId
  const [active, setActive] = useState<string | undefined>(defaultTab)

  if (reports.length === 0) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold">스터디 리포트</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            참여 중인 스터디의 학습 리포트를 확인하세요.
          </p>
        </div>
        <div className="rounded-xl border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">
            아직 참여 중인 스터디가 없습니다.
          </p>
        </div>
      </div>
    )
  }

  // 스터디가 하나뿐이고 합산이 없으면 탭 없이 단일 리포트만 보여준다.
  if (!aggregate && reports.length === 1) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold">스터디 리포트</h1>
        <ReportView report={reports[0]} />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">스터디 리포트</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          참여 중인 {reports.length}개 스터디의 학습 리포트를 한곳에서 확인하세요.
        </p>
      </div>

      <Tabs value={active} onValueChange={setActive} className="w-full">
        <TabsList className="flex h-auto w-full flex-wrap justify-start gap-1">
          {aggregate && (
            <TabsTrigger value="all" className="gap-1">
              <Layers className="h-4 w-4" /> 전체 합산
            </TabsTrigger>
          )}
          {reports.map((r) => (
            <TabsTrigger key={r.studyId} value={r.studyId}>
              {r.studyName}
            </TabsTrigger>
          ))}
        </TabsList>

        {aggregate && (
          <TabsContent value="all" className="mt-6">
            <ReportView report={aggregate} />
          </TabsContent>
        )}
        {reports.map((r) => (
          <TabsContent key={r.studyId} value={r.studyId} className="mt-6">
            <ReportView report={r} />
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}
