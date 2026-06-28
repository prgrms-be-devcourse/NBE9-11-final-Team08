'use client'

import { useEffect, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  XAxis,
  YAxis,
} from 'recharts'
import {
  ArrowLeft,
  CheckCircle2,
  DollarSign,
  Flame,
  Loader2,
  ShoppingCart,
  Users,
} from 'lucide-react'
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatKRW } from '@/lib/utils'
import { api } from '@/lib/api'
import type { LecturePauses, SellerCourseDetail } from '@/lib/types'

type Range = '3m' | '6m' | '1y'

const revenueConfig = {
  revenue: { label: '매출', color: 'var(--chart-1)' },
} satisfies ChartConfig

const pauseConfig = {
  count: { label: '멈춤 횟수', color: 'var(--chart-1)' },
} satisfies ChartConfig

function mmss(totalSeconds: number) {
  const s = Math.max(0, Math.round(totalSeconds))
  const m = Math.floor(s / 60)
  const r = s % 60
  return `${m}:${String(r).padStart(2, '0')}`
}

export function CourseDrilldown({
  courseId,
  range,
  onBack,
}: {
  courseId: number
  range: Range
  onBack: () => void
}) {
  const [detail, setDetail] = useState<SellerCourseDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [selectedLecture, setSelectedLecture] = useState<number | null>(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    api
      .getSellerCourseDetail(courseId, range)
      .then((res) => {
        if (active) setDetail(res)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [courseId, range])

  const stats = [
    { label: '매출', value: formatKRW(detail?.totalRevenue ?? 0), icon: DollarSign },
    { label: '판매 건수', value: `${(detail?.totalOrders ?? 0).toLocaleString()}건`, icon: ShoppingCart },
    { label: '수강생', value: `${(detail?.activeStudents ?? 0).toLocaleString()}명`, icon: Users },
    { label: '완강', value: `${(detail?.completions ?? 0).toLocaleString()}회`, icon: CheckCircle2 },
  ]

  const monthly = detail?.monthly ?? []
  const lectures = detail?.lectures ?? []

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" onClick={onBack} className="gap-1">
          <ArrowLeft className="size-4" /> 전체 분석
        </Button>
        {loading && <Loader2 className="size-4 animate-spin text-muted-foreground" />}
      </div>

      <div>
        <h2 className="text-xl font-bold">{detail?.title ?? '강좌 분석'}</h2>
        <p className="mt-1 text-sm text-muted-foreground">강좌 단위 매출·참여도 드릴다운</p>
      </div>

      {/* KPI */}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => (
          <div key={s.label} className="rounded-xl border bg-card p-5">
            <span className="flex size-9 items-center justify-center rounded-lg bg-secondary text-foreground">
              <s.icon className="size-4" />
            </span>
            <p className="mt-4 text-2xl font-bold">{s.value}</p>
            <p className="text-xs text-muted-foreground">{s.label}</p>
          </div>
        ))}
      </section>

      {/* Monthly revenue */}
      <section className="rounded-xl border bg-card p-5">
        <h3 className="font-semibold">월별 매출 추이</h3>
        <p className="text-xs text-muted-foreground">결제 완료 기준</p>
        <ChartContainer config={revenueConfig} className="mt-4 h-56 w-full">
          <LineChart data={monthly} margin={{ left: 12, right: 12 }}>
            <CartesianGrid vertical={false} />
            <XAxis dataKey="month" tickLine={false} axisLine={false} tickMargin={8} />
            <YAxis tickLine={false} axisLine={false} width={48} tickFormatter={(v) => `${v / 10000}만`} />
            <ChartTooltip content={<ChartTooltipContent formatter={(v) => formatKRW(Number(v))} />} />
            <Line type="monotone" dataKey="revenue" stroke="var(--color-revenue)" strokeWidth={2} dot={{ r: 3 }} />
          </LineChart>
        </ChartContainer>
      </section>

      {/* Lecture engagement */}
      <section className="rounded-xl border bg-card p-5">
        <h3 className="font-semibold">강의별 참여도</h3>
        <p className="text-xs text-muted-foreground">강의를 클릭하면 어려워서 멈춘 구간을 볼 수 있어요</p>
        {lectures.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>강의</TableHead>
                  <TableHead className="text-right">길이</TableHead>
                  <TableHead className="text-right">입장</TableHead>
                  <TableHead className="text-right">완료</TableHead>
                  <TableHead className="text-right">시청자</TableHead>
                  <TableHead className="text-right">평균 멈춤</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {lectures.map((l) => (
                  <TableRow
                    key={l.lectureId}
                    onClick={() => setSelectedLecture(l.lectureId)}
                    data-active={selectedLecture === l.lectureId}
                    className="cursor-pointer data-[active=true]:bg-secondary/60"
                  >
                    <TableCell className="max-w-[280px]">
                      <p className="line-clamp-1 font-medium">{l.title}</p>
                      <p className="text-xs text-muted-foreground">{l.chapterTitle}</p>
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{mmss(l.durationSeconds)}</TableCell>
                    <TableCell className="text-right tabular-nums">{l.enterCount.toLocaleString()}</TableCell>
                    <TableCell className="text-right tabular-nums">{l.completeCount.toLocaleString()}</TableCell>
                    <TableCell className="text-right tabular-nums">{l.viewerCount.toLocaleString()}</TableCell>
                    <TableCell className="text-right tabular-nums">{mmss(l.avgWatchSeconds)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        ) : (
          <p className="mt-12 text-center text-sm text-muted-foreground">등록된 강의가 없습니다.</p>
        )}
      </section>

      {selectedLecture !== null && <PauseHotspots lectureId={selectedLecture} />}
    </div>
  )
}

function PauseHotspots({ lectureId }: { lectureId: number }) {
  const [pauses, setPauses] = useState<LecturePauses | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setLoading(true)
    api
      .getSellerLecturePauses(lectureId, 40)
      .then((res) => {
        if (active) setPauses(res)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [lectureId])

  const bins = pauses?.bins ?? []
  const hasData = bins.some((b) => b.count > 0)

  return (
    <section className="rounded-xl border bg-card p-5">
      <div className="flex items-center gap-2">
        <Flame className="size-4 text-primary" />
        <h3 className="font-semibold">어려워서 멈춘 구간</h3>
        {loading && <Loader2 className="size-4 animate-spin text-muted-foreground" />}
      </div>
      <p className="text-xs text-muted-foreground">
        {pauses
          ? `${pauses.title} · 학습자 ${pauses.viewerCount.toLocaleString()}명 · 멈춤 ${pauses.totalPauses.toLocaleString()}회`
          : '멈춤 데이터를 불러오는 중'}
      </p>

      {hasData ? (
        <>
          <ChartContainer config={pauseConfig} className="mt-4 h-48 w-full">
            <BarChart data={bins} margin={{ left: 12, right: 12 }}>
              <CartesianGrid vertical={false} />
              <XAxis
                dataKey="startSeconds"
                tickLine={false}
                axisLine={false}
                tickMargin={8}
                tickFormatter={(v) => mmss(Number(v))}
                interval="preserveStartEnd"
                minTickGap={32}
              />
              <YAxis tickLine={false} axisLine={false} width={28} allowDecimals={false} />
              <ChartTooltip
                content={
                  <ChartTooltipContent
                    labelFormatter={(_, payload) => {
                      const p = payload?.[0]?.payload as LecturePauses['bins'][number] | undefined
                      return p ? `${mmss(p.startSeconds)} – ${mmss(p.endSeconds)}` : ''
                    }}
                    formatter={(v) => `${Number(v).toLocaleString()}회 멈춤`}
                  />
                }
              />
              <Bar dataKey="count" radius={[3, 3, 0, 0]}>
                {bins.map((b) => (
                  <Cell
                    key={b.index}
                    fill="var(--color-count)"
                    fillOpacity={0.25 + 0.75 * b.heat}
                  />
                ))}
              </Bar>
            </BarChart>
          </ChartContainer>

          {pauses && pauses.hotspots.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-2">
              <span className="text-xs text-muted-foreground">어려운 구간:</span>
              {pauses.hotspots.map((h, i) => (
                <span
                  key={i}
                  className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary"
                >
                  {mmss(h.startSeconds)}–{mmss(h.endSeconds)} ({h.count.toLocaleString()}회)
                </span>
              ))}
            </div>
          )}
        </>
      ) : (
        !loading && (
          <p className="mt-10 text-center text-sm text-muted-foreground">
            아직 멈춤 데이터가 없습니다.
          </p>
        )
      )}
    </section>
  )
}
