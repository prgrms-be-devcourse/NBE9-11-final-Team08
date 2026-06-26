'use client'

import { CartesianGrid, Line, LineChart, XAxis, YAxis } from 'recharts'
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart'
import type { DailySessionPoint } from '@/lib/types'

const config = {
  sessions: { label: '세션', color: 'var(--chart-1)' },
  distinctLearners: { label: '학습자', color: 'var(--chart-2)' },
} satisfies ChartConfig

export function OverviewCharts({ data }: { data: DailySessionPoint[] }) {
  if (!data || data.length === 0) {
    return (
      <p className="py-12 text-center text-sm text-muted-foreground">
        표시할 세션 데이터가 없습니다.
      </p>
    )
  }

  const fmtDate = (d: string) => d.slice(5) // MM-DD

  return (
    <ChartContainer config={config} className="mt-4 h-72 w-full">
      <LineChart data={data} margin={{ left: 4, right: 12, top: 8 }}>
        <CartesianGrid vertical={false} strokeDasharray="3 3" />
        <XAxis
          dataKey="date"
          tickFormatter={fmtDate}
          tickLine={false}
          axisLine={false}
          fontSize={11}
        />
        <YAxis tickLine={false} axisLine={false} width={32} fontSize={11} />
        <ChartTooltip content={<ChartTooltipContent />} />
        <Line
          dataKey="sessions"
          type="monotone"
          stroke="var(--color-sessions)"
          strokeWidth={2}
          dot={false}
        />
        <Line
          dataKey="distinctLearners"
          type="monotone"
          stroke="var(--color-distinctLearners)"
          strokeWidth={2}
          dot={false}
        />
      </LineChart>
    </ChartContainer>
  )
}
