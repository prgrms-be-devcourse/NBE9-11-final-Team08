'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Layers } from 'lucide-react'
import { toast } from 'sonner'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { ReportView } from '@/components/account/report-view'
import { api } from '@/lib/api'
import { formatDateTime } from '@/lib/utils'
import type { MyStudyReport } from '@/lib/types'

interface MyReportsViewProps {
  reports: MyStudyReport[]
  aggregate: MyStudyReport | null
}

export function MyReportsView({ reports, aggregate }: MyReportsViewProps) {
  // 합산 탭이 있으면 그것을 기본값으로, 없으면 첫 스터디를 기본값으로 한다.
  const defaultTab = aggregate ? 'all' : reports[0]?.studyId
  const [active, setActive] = useState<string | undefined>(defaultTab)
  const router = useRouter()
  // 갱신 중인 탭 id('all' 또는 studyId). 버튼 스피너 표시에만 쓴다.
  const [refreshingId, setRefreshingId] = useState<string | null>(null)

  // studyId 별 리포트 재집계(?refresh=true). 'all' 이면 모든 스터디를 갱신한다.
  // 갱신 후 router.refresh() 로 서버 컴포넌트를 다시 불러 최신 updatedAt 을 반영한다.
  const handleRefresh = async (studyId: string) => {
    setRefreshingId(studyId)
    try {
      if (studyId === 'all') {
        const results = await Promise.all(
          reports.map((r) => api.generateStudyReport(r.studyId).catch(() => null)),
        )
        const allCooldown =
          results.length > 0 && results.every((r) => r?.status === 'COOLDOWN')
        toast[allCooldown ? 'info' : 'success'](
          allCooldown ? '아직 갱신할 수 없어요. 잠시 후 다시 시도해주세요.' : '리포트를 갱신했어요.',
        )
      } else {
        const res = await api.generateStudyReport(studyId)
        if (res?.status === 'COOLDOWN') {
          const at = res.nextRegenerableAt ? formatDateTime(res.nextRegenerableAt) : ''
          toast.info(at ? `${at} 이후에 다시 갱신할 수 있어요.` : '아직 갱신할 수 없어요. 잠시 후 다시 시도해주세요.')
        } else {
          toast.success('리포트를 갱신했어요.')
        }
      }
      router.refresh()
    } catch {
      toast.error('리포트 갱신에 실패했습니다.')
    } finally {
      setRefreshingId(null)
    }
  }

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
        <ReportView
          report={reports[0]}
          onRefresh={() => handleRefresh(reports[0].studyId)}
          refreshing={refreshingId === reports[0].studyId}
        />
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
            <ReportView
              report={aggregate}
              onRefresh={() => handleRefresh('all')}
              refreshing={refreshingId === 'all'}
            />
          </TabsContent>
        )}
        {reports.map((r) => (
          <TabsContent key={r.studyId} value={r.studyId} className="mt-6">
            <ReportView
              report={r}
              onRefresh={() => handleRefresh(r.studyId)}
              refreshing={refreshingId === r.studyId}
            />
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}
