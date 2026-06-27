'use client'

import { useEffect, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  XAxis,
  YAxis,
} from 'recharts'
import {
  ArrowDownRight,
  ArrowUpRight,
  DollarSign,
  Loader2,
  ShoppingCart,
  Users,
  BookOpen,
} from 'lucide-react'
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { formatKRW } from '@/lib/utils'
import { api } from '@/lib/api'
import type { SellerAnalytics } from '@/lib/types'

const revenueConfig = {
  revenue: { label: '매출', color: 'var(--chart-1)' },
} satisfies ChartConfig

const ordersConfig = {
  orders: { label: '판매 건수', color: 'var(--chart-2)' },
} satisfies ChartConfig

const categoryConfig = {
  value: { label: '비중' },
} satisfies ChartConfig

const pieColors = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)', 'var(--chart-5)']

type Range = '3m' | '6m' | '1y'

export function AnalyticsView({ initialData }: { initialData: SellerAnalytics | null }) {
  const [range, setRange] = useState<Range>('6m')
  const [data, setData] = useState<SellerAnalytics | null>(initialData)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    // 최초 렌더는 서버에서 받은 6m 데이터를 그대로 쓰고, 기간이 바뀔 때만 다시 불러온다.
    if (range === '6m' && data === initialData) return
    let active = true
    setLoading(true)
    api
      .getSellerAnalytics(range)
      .then((res) => {
        if (active) setData(res)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [range])

  const stats = [
    {
      label: '총 매출',
      value: formatKRW(data?.totalRevenue ?? 0),
      icon: DollarSign,
      delta: data?.revenueDelta ?? 0,
    },
    {
      label: '총 판매 건수',
      value: `${(data?.totalOrders ?? 0).toLocaleString()}건`,
      icon: ShoppingCart,
      delta: data?.ordersDelta ?? 0,
    },
    {
      label: '총 수강생',
      value: `${(data?.totalStudents ?? 0).toLocaleString()}명`,
      icon: Users,
      delta: null,
    },
    {
      label: '판매 중 강좌',
      value: `${(data?.onSaleCourses ?? 0).toLocaleString()} / ${(data?.totalCourses ?? 0).toLocaleString()}개`,
      icon: BookOpen,
      delta: null,
    },
  ]

  const monthly = data?.monthly ?? []
  const categories = data?.categories ?? []
  const topCourses = data?.topCourses ?? []

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">판매 분석</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            매출과 수강생 추이를 한눈에 확인하세요.
          </p>
        </div>
        <div className="flex items-center gap-3">
          {loading && <Loader2 className="size-4 animate-spin text-muted-foreground" />}
          <Tabs value={range} onValueChange={(v) => setRange(v as Range)}>
            <TabsList>
              <TabsTrigger value="3m">3개월</TabsTrigger>
              <TabsTrigger value="6m">6개월</TabsTrigger>
              <TabsTrigger value="1y">1년</TabsTrigger>
            </TabsList>
          </Tabs>
        </div>
      </div>

      {/* KPI cards */}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => {
          const positive = (s.delta ?? 0) >= 0
          return (
            <div key={s.label} className="rounded-xl border bg-card p-5">
              <div className="flex items-center justify-between">
                <span className="flex size-9 items-center justify-center rounded-lg bg-secondary text-foreground">
                  <s.icon className="size-4" />
                </span>
                {s.delta !== null && (
                  <span
                    className={
                      positive
                        ? 'flex items-center gap-0.5 text-xs font-medium text-emerald-600'
                        : 'flex items-center gap-0.5 text-xs font-medium text-destructive'
                    }
                  >
                    {positive ? (
                      <ArrowUpRight className="size-3.5" />
                    ) : (
                      <ArrowDownRight className="size-3.5" />
                    )}
                    {Math.abs(s.delta)}%
                  </span>
                )}
              </div>
              <p className="mt-4 text-2xl font-bold">{s.value}</p>
              <p className="text-xs text-muted-foreground">{s.label}</p>
            </div>
          )
        })}
      </section>

      <div className="grid gap-6 lg:grid-cols-[1.6fr_1fr]">
        {/* Revenue trend */}
        <section className="rounded-xl border bg-card p-5">
          <h2 className="font-semibold">월별 매출 추이</h2>
          <p className="text-xs text-muted-foreground">결제 완료 기준</p>
          <ChartContainer config={revenueConfig} className="mt-4 h-64 w-full">
            <LineChart data={monthly} margin={{ left: 12, right: 12 }}>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="month" tickLine={false} axisLine={false} tickMargin={8} />
              <YAxis
                tickLine={false}
                axisLine={false}
                width={48}
                tickFormatter={(v) => `${v / 10000}만`}
              />
              <ChartTooltip
                content={<ChartTooltipContent formatter={(v) => formatKRW(Number(v))} />}
              />
              <Line
                type="monotone"
                dataKey="revenue"
                stroke="var(--color-revenue)"
                strokeWidth={2}
                dot={{ r: 3 }}
              />
            </LineChart>
          </ChartContainer>
        </section>

        {/* Category distribution */}
        <section className="rounded-xl border bg-card p-5">
          <h2 className="font-semibold">카테고리별 비중</h2>
          <p className="text-xs text-muted-foreground">수강생 기준</p>
          {categories.length > 0 ? (
            <>
              <ChartContainer config={categoryConfig} className="mt-4 h-64 w-full">
                <PieChart>
                  <ChartTooltip content={<ChartTooltipContent nameKey="name" />} />
                  <Pie data={categories} dataKey="value" nameKey="name" innerRadius={48}>
                    {categories.map((_, i) => (
                      <Cell key={i} fill={pieColors[i % pieColors.length]} />
                    ))}
                  </Pie>
                </PieChart>
              </ChartContainer>
              <ul className="mt-2 space-y-1">
                {categories.map((c, i) => (
                  <li key={c.name} className="flex items-center gap-2 text-xs">
                    <span
                      className="size-2.5 rounded-full"
                      style={{ backgroundColor: pieColors[i % pieColors.length] }}
                    />
                    <span className="flex-1 text-muted-foreground">{c.name}</span>
                    <span className="font-medium">{c.value.toLocaleString()}</span>
                  </li>
                ))}
              </ul>
            </>
          ) : (
            <p className="mt-12 text-center text-sm text-muted-foreground">
              아직 수강생 데이터가 없습니다.
            </p>
          )}
        </section>
      </div>

      {/* Orders bar + top courses */}
      <div className="grid gap-6 lg:grid-cols-[1.6fr_1fr]">
        <section className="rounded-xl border bg-card p-5">
          <h2 className="font-semibold">월별 판매 건수</h2>
          <p className="text-xs text-muted-foreground">결제 완료 기준</p>
          <ChartContainer config={ordersConfig} className="mt-4 h-56 w-full">
            <BarChart data={monthly} margin={{ left: 12, right: 12 }}>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="month" tickLine={false} axisLine={false} tickMargin={8} />
              <YAxis tickLine={false} axisLine={false} width={32} />
              <ChartTooltip content={<ChartTooltipContent />} />
              <Bar dataKey="orders" fill="var(--color-orders)" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ChartContainer>
        </section>

        <section className="rounded-xl border bg-card p-5">
          <h2 className="font-semibold">인기 상품 TOP 5</h2>
          <p className="text-xs text-muted-foreground">수강생 수 기준</p>
          {topCourses.length > 0 ? (
            <ol className="mt-4 space-y-3">
              {topCourses.map((c, i) => (
                <li key={c.courseId} className="flex items-center gap-3">
                  <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-secondary text-xs font-bold">
                    {i + 1}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="line-clamp-1 text-sm font-medium">{c.title}</p>
                    <p className="text-xs text-muted-foreground">
                      {c.studentCount.toLocaleString()}명 수강
                    </p>
                  </div>
                  <span className="shrink-0 text-sm font-semibold">{formatKRW(c.price)}</span>
                </li>
              ))}
            </ol>
          ) : (
            <p className="mt-12 text-center text-sm text-muted-foreground">
              등록된 강좌가 없습니다.
            </p>
          )}
        </section>
      </div>
    </div>
  )
}
