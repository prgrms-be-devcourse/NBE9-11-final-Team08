'use client'

import { CalendarDays, Clock, Download, MessageSquare, PlayCircle, RefreshCw } from 'lucide-react'
import {
  Area,
  AreaChart,
  CartesianGrid,
  XAxis,
  YAxis,
} from 'recharts'
import { toast } from 'sonner'
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { cn, formatDateTime } from '@/lib/utils'
import type { StudyReport } from '@/lib/types'

const chartConfig = {
  progress: { label: '진도(%)', color: 'var(--chart-3)' },
} satisfies ChartConfig

// 학습량 레벨(0~3)에 따른 칸 색상. 진할수록 학습량이 많음.
const CELL_LEVELS = [
  'bg-secondary',
  'bg-primary/30',
  'bg-primary/60',
  'bg-primary',
]

type CalendarDay = { date: string; active: boolean; level?: number }

function StudyCalendar({ calendar }: { calendar: CalendarDay[] }) {
  // 7일씩 묶어 주 단위 열(column)을 구성한다.
  const weeks: CalendarDay[][] = []
  for (let i = 0; i < calendar.length; i += 7) {
    weeks.push(calendar.slice(i, i + 7))
  }

  // 6개월 간격으로 월 라벨을 만든다. 각 라벨은 해당 월이 시작하는 주 열에 배치.
  const monthFmt = new Intl.DateTimeFormat('ko-KR', {
    year: '2-digit',
    month: 'short',
  })
  const labels: { col: number; text: string }[] = []
  let startMonth: number | null = null
  let lastKey = ''
  weeks.forEach((week, wi) => {
    const first = week[0]
    if (!first) return
    const d = new Date(first.date)
    const monthIndex = d.getFullYear() * 12 + d.getMonth()
    if (startMonth === null) startMonth = monthIndex
    const diff = monthIndex - startMonth
    const key = `${d.getFullYear()}-${d.getMonth()}`
    if (diff >= 0 && diff % 6 === 0 && key !== lastKey) {
      labels.push({ col: wi + 1, text: monthFmt.format(d) })
      lastKey = key
    }
  })

  const today = new Date()

  return (
    <div className="mt-4 overflow-x-auto pb-1">
      <div
        className="inline-flex flex-col gap-2"
        style={{ ['--cell' as string]: '14px' }}
      >
        <div
          className="grid gap-[3px] text-xs text-muted-foreground"
          style={{ gridTemplateColumns: `repeat(${weeks.length}, var(--cell))` }}
        >
          {labels.map((l) => (
            <span
              key={l.col}
              className="whitespace-nowrap"
              style={{ gridColumnStart: l.col }}
            >
              {l.text}
            </span>
          ))}
        </div>

        <div
          className="grid grid-flow-col grid-rows-7 gap-[3px]"
          style={{ gridAutoColumns: 'var(--cell)' }}
        >
          {calendar.map((day, i) => {
            const isFuture = new Date(day.date) > today
            const level = day.active ? day.level ?? 1 : 0
            return (
              <div
                key={i}
                title={`${day.date} · ${day.active ? '학습함' : '학습 없음'}`}
                className={cn(
                  'h-[var(--cell)] w-[var(--cell)] rounded-[3px] border border-border/40',
                  isFuture ? 'opacity-0' : CELL_LEVELS[level],
                )}
              />
            )
          })}
        </div>
      </div>
    </div>
  )
}

export function ReportView({
  report,
  onRefresh,
  refreshing = false,
}: {
  report: StudyReport
  // 제공되면 헤더에 "갱신하기" 버튼을 노출한다. 미제공(예: 정적 미리보기)이면 버튼을 숨긴다.
  onRefresh?: () => void
  refreshing?: boolean
}) {
  const stats = [
    { icon: Clock, label: '총 학습시간', value: report.totalStudyTime },
    { icon: MessageSquare, label: '작성 질문 수', value: report.commentCount === -1 ? '기능 없음' : `${report.commentCount}개` },
    { icon: CalendarDays, label: '수강일 수', value: report.studyDays === -1 ? '기능 없음' : `${report.studyDays}일` },
  ]

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm text-muted-foreground">{report.studyName}</p>
          <h1 className="mt-1 text-2xl font-bold">{report.userName}님의 리포트</h1>
          <p className="mt-1 text-sm text-muted-foreground">{report.period}</p>
        </div>
        {onRefresh && (
          <div className="flex flex-col items-end gap-1.5">
            <p className="text-xs text-muted-foreground">
              마지막 갱신: {report.updatedAt ? formatDateTime(report.updatedAt) : '기록 없음'}
            </p>
            <Button size="sm" variant="outline" onClick={onRefresh} disabled={refreshing}>
              <RefreshCw className={cn('mr-1.5 h-4 w-4', refreshing && 'animate-spin')} />
              {refreshing ? '갱신 중…' : '갱신하기'}
            </Button>
          </div>
        )}
      </div>

      <div className="rounded-xl border bg-foreground p-6 text-background">
        <p className="text-sm text-background/70">오늘도 화이팅!</p>
        <p className="mt-1 text-lg font-semibold text-balance">
          {report.studyDays !== -1 ? (
            <>
              이번 스터디 기간 동안 총 {report.studyDays}일 학습에 참여하고{' '}
              {report.totalStudyTime} 동안 러닝 스페이스를 이용했어요.
            </>
          ) : (
            <>이번 스터디에 참여해 주셔서 감사합니다! 앞으로도 열공해 보아요.</>
          )}
        </p>
      </div>

      <section className="grid grid-cols-3 gap-3">
        {stats.map((s) => (
          <div key={s.label} className="rounded-xl border bg-card p-4">
            <s.icon className="h-5 w-5 text-muted-foreground" />
            <p className="mt-3 text-xl font-bold">{s.value}</p>
            <p className="text-xs text-muted-foreground">{s.label}</p>
          </div>
        ))}
      </section>

      <section className="rounded-xl border bg-card p-5">
        <h2 className="font-semibold">학습 활동 그래프</h2>
        <p className="text-xs text-muted-foreground">주차별 누적 진도</p>
        {report.progressData && report.progressData.length > 0 ? (
          <ChartContainer config={chartConfig} className="mt-4 h-56 w-full">
            <AreaChart data={report.progressData} margin={{ left: -10, right: 8 }}>
              <CartesianGrid vertical={false} strokeDasharray="3 3" />
              <XAxis dataKey="day" tickLine={false} axisLine={false} tickMargin={8} />
              <YAxis tickLine={false} axisLine={false} width={32} />
              <ChartTooltip content={<ChartTooltipContent />} />
              <Area
                dataKey="progress"
                type="monotone"
                stroke="var(--color-progress)"
                fill="var(--color-progress)"
                fillOpacity={0.15}
                strokeWidth={2}
              />
            </AreaChart>
          </ChartContainer>
        ) : (
          <div className="mt-6 flex h-40 items-center justify-center rounded-xl border border-dashed text-sm text-muted-foreground bg-muted/20">
            기능 없음 (학습 활동 그래프 데이터가 존재하지 않습니다)
          </div>
        )}
      </section>

      <section className="rounded-xl border bg-card p-5">
        <h2 className="font-semibold">학습 캘린더</h2>
        <p className="text-xs text-muted-foreground">
          한 열이 한 주, 가로로 최근 1년의 학습 기록이에요. 진한 칸일수록 학습량이 많은 날입니다.
        </p>
        {report.calendar && report.calendar.length > 0 ? (
          <>
            <StudyCalendar calendar={report.calendar} />
            <div className="mt-3 flex items-center justify-end gap-1 text-xs text-muted-foreground">
              <span>적음</span>
              {CELL_LEVELS.map((cls, i) => (
                <span key={i} className={cn('h-3 w-3 rounded-[3px]', cls)} />
              ))}
              <span>많음</span>
            </div>
          </>
        ) : (
          <div className="mt-6 flex h-40 items-center justify-center rounded-xl border border-dashed text-sm text-muted-foreground bg-muted/20">
            기능 없음 (학습 캘린더 데이터가 존재하지 않습니다)
          </div>
        )}
      </section>

      <section className="rounded-xl border bg-card p-5">
        <h2 className="font-semibold">가장 많이 학습한 강의</h2>
        <Separator className="my-4" />
        {report.topLectures && report.topLectures.length > 0 ? (
          <ul className="space-y-3">
            {report.topLectures.map((title, i) => (
              <li key={title} className="flex items-center gap-3">
                <span className="flex h-7 w-7 items-center justify-center rounded-full bg-secondary text-sm font-semibold">
                  {i + 1}
                </span>
                <PlayCircle className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm font-medium">{title}</span>
              </li>
            ))}
          </ul>
        ) : (
          <div className="flex h-20 items-center justify-center text-sm text-muted-foreground">
            기능 없음 (강의 학습 데이터가 존재하지 않습니다)
          </div>
        )}
      </section>
    </div>
  )
}
