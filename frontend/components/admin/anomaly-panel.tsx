'use client'

import { useEffect, useState } from 'react'
import { AlertTriangle, Loader2, RefreshCw, TrendingDown } from 'lucide-react'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { AnomalyResponse } from '@/lib/types'

export function AnomalyPanel() {
  const [incompletionThreshold, setIncompletionThreshold] = useState(50)
  const [burstThreshold, setBurstThreshold] = useState(10)
  const [windowMinutes, setWindowMinutes] = useState(1)
  const [data, setData] = useState<AnomalyResponse | null>(null)
  const [loading, setLoading] = useState(true)

  async function run() {
    setLoading(true)
    const res = await api.getAdminAnomalies(incompletionThreshold, burstThreshold, windowMinutes)
    setData(res)
    setLoading(false)
  }

  useEffect(() => {
    run()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="space-y-6">
      {/* 임계값 컨트롤 */}
      <div className="rounded-xl border bg-card p-5">
        <h2 className="font-semibold">탐지 임계값</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-3">
          <label className="space-y-1.5 text-sm">
            <span className="text-muted-foreground">미완강률 임계값 (%)</span>
            <Input
              type="number"
              min={0}
              max={100}
              value={incompletionThreshold}
              onChange={(e) => setIncompletionThreshold(Number(e.target.value))}
            />
          </label>
          <label className="space-y-1.5 text-sm">
            <span className="text-muted-foreground">중복 이벤트 임계 (건)</span>
            <Input
              type="number"
              min={1}
              value={burstThreshold}
              onChange={(e) => setBurstThreshold(Number(e.target.value))}
            />
          </label>
          <label className="space-y-1.5 text-sm">
            <span className="text-muted-foreground">집계 구간 (분)</span>
            <Input
              type="number"
              min={1}
              value={windowMinutes}
              onChange={(e) => setWindowMinutes(Number(e.target.value))}
            />
          </label>
        </div>
        <Button onClick={run} disabled={loading} className="mt-4">
          {loading ? (
            <Loader2 className="mr-1 h-4 w-4 animate-spin" />
          ) : (
            <RefreshCw className="mr-1 h-4 w-4" />
          )}
          재탐지
        </Button>
      </div>

      {/* 고미완강 강좌 */}
      <div className="rounded-xl border bg-card">
        <div className="flex items-center gap-2 border-b px-5 py-3">
          <TrendingDown className="h-4 w-4 text-destructive" />
          <h2 className="font-semibold">미완강률 초과 강좌</h2>
          <Badge variant="secondary" className="ml-auto">
            {data?.highIncompletionCourses.length ?? 0}건
          </Badge>
        </div>
        {!data || data.highIncompletionCourses.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            임계값을 초과한 강좌가 없습니다.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>강좌</TableHead>
                <TableHead className="text-right">수강자</TableHead>
                <TableHead className="text-right">완강</TableHead>
                <TableHead className="text-right">미완강률</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.highIncompletionCourses.map((c) => (
                <TableRow key={c.courseId}>
                  <TableCell className="font-medium">
                    <span className="text-muted-foreground">#{c.courseId}</span> {c.title}
                  </TableCell>
                  <TableCell className="text-right">{c.enrollees.toLocaleString()}</TableCell>
                  <TableCell className="text-right">{c.completionCount.toLocaleString()}</TableCell>
                  <TableCell className="text-right">
                    <Badge variant="destructive">{c.incompletionRate.toFixed(1)}%</Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      {/* 중복 이벤트 다발 */}
      <div className="rounded-xl border bg-card">
        <div className="flex items-center gap-2 border-b px-5 py-3">
          <AlertTriangle className="h-4 w-4 text-amber-500" />
          <h2 className="font-semibold">중복 이벤트 다발</h2>
          <Badge variant="secondary" className="ml-auto">
            {data?.duplicateBursts.length ?? 0}건
          </Badge>
        </div>
        {!data || data.duplicateBursts.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            임계값을 초과한 다발 이벤트가 없습니다.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>사용자</TableHead>
                <TableHead>강의</TableHead>
                <TableHead>이벤트</TableHead>
                <TableHead>구간</TableHead>
                <TableHead className="text-right">횟수</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.duplicateBursts.map((b, i) => (
                <TableRow key={`${b.userId}-${b.lectureId}-${b.eventType}-${b.bucketMinute}-${i}`}>
                  <TableCell>#{b.userId}</TableCell>
                  <TableCell>#{b.lectureId}</TableCell>
                  <TableCell>
                    <Badge variant="outline">{b.eventType}</Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">{b.bucketMinute}</TableCell>
                  <TableCell className="text-right font-semibold">{b.count}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>
    </div>
  )
}
