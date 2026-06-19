'use client'

import { useMemo, useState } from 'react'
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
import { ArrowDownRight, ArrowUpRight, DollarSign, ShoppingCart, Star, Users } from 'lucide-react'
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { formatKRW } from '@/lib/utils'
import type { Course } from '@/lib/types'

const monthlyRevenue = [
  { month: '1월', revenue: 3200000, orders: 42 },
  { month: '2월', revenue: 2800000, orders: 36 },
  { month: '3월', revenue: 4100000, orders: 55 },
  { month: '4월', revenue: 3900000, orders: 51 },
  { month: '5월', revenue: 5200000, orders: 68 },
  { month: '6월', revenue: 6100000, orders: 79 },
]

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

export function AnalyticsView({ courses }: { courses: Course[] }) {
  const [range, setRange] = useState('6m')

  const totalRevenue = monthlyRevenue.reduce((acc, m) => acc + m.revenue, 0)
  const totalOrders = monthlyRevenue.reduce((acc, m) => acc + m.orders, 0)
  const totalStudents = courses.reduce((acc, c) => acc + (c.studentCount ?? 0), 0)
  const avgRating =
    courses.length > 0
      ? (courses.reduce((acc, c) => acc + (c.rating ?? 0), 0) / courses.length).toFixed(1)
      : '0.0'

  const stats = [
    {
      label: '총 매출',
      value: formatKRW(totalRevenue),
      icon: DollarSign,
      delta: 12.4,
    },
    {
      label: '총 판매 건수',
      value: `${totalOrders.toLocaleString()}건`,
      icon: ShoppingCart,
      delta: 8.1,
    },
    {
      label: '총 수강생',
      value: `${totalStudents.toLocaleString()}명`,
      icon: Users,
      delta: 5.6,
    },
    {
      label: '평균 평점',
      value: avgRating,
      icon: Star,
      delta: -1.2,
    },
  ]

  const categoryData = useMemo(() => {
    const map = new Map<string, number>()
    courses.forEach((c) => {
      map.set(c.category, (map.get(c.category) ?? 0) + (c.studentCount ?? 1))
    })
    return Array.from(map, ([name, value]) => ({ name, value }))
  }, [courses])

  const topCourses = useMemo(
    () =>
      [...courses]
        .sort((a, b) => (b.studentCount ?? 0) - (a.studentCount ?? 0))
        .slice(0, 5),
    [courses],
  )

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">판매 분석</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            매출과 수강생 추이를 한눈에 확인하세요.
          </p>
        </div>
        <Tabs value={range} onValueChange={setRange}>
          <TabsList>
            <TabsTrigger value="3m">3개월</TabsTrigger>
            <TabsTrigger value="6m">6개월</TabsTrigger>
            <TabsTrigger value="1y">1년</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {/* KPI cards */}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => {
          const positive = s.delta >= 0
          return (
            <div key={s.label} className="rounded-xl border bg-card p-5">
              <div className="flex items-center justify-between">
                <span className="flex size-9 items-center justify-center rounded-lg bg-secondary text-foreground">
                  <s.icon className="size-4" />
                </span>
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
          <p className="text-xs text-muted-foreground">최근 6개월 매출</p>
          <ChartContainer config={revenueConfig} className="mt-4 h-64 w-full">
            <LineChart data={monthlyRevenue} margin={{ left: 12, right: 12 }}>
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
          <ChartContainer config={categoryConfig} className="mt-4 h-64 w-full">
            <PieChart>
              <ChartTooltip content={<ChartTooltipContent nameKey="name" />} />
              <Pie data={categoryData} dataKey="value" nameKey="name" innerRadius={48}>
                {categoryData.map((_, i) => (
                  <Cell key={i} fill={pieColors[i % pieColors.length]} />
                ))}
              </Pie>
            </PieChart>
          </ChartContainer>
          <ul className="mt-2 space-y-1">
            {categoryData.map((c, i) => (
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
        </section>
      </div>

      {/* Orders bar + top courses */}
      <div className="grid gap-6 lg:grid-cols-[1.6fr_1fr]">
        <section className="rounded-xl border bg-card p-5">
          <h2 className="font-semibold">월별 판매 건수</h2>
          <p className="text-xs text-muted-foreground">결제 완료 기준</p>
          <ChartContainer config={ordersConfig} className="mt-4 h-56 w-full">
            <BarChart data={monthlyRevenue} margin={{ left: 12, right: 12 }}>
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
          <ol className="mt-4 space-y-3">
            {topCourses.map((c, i) => (
              <li key={c.id} className="flex items-center gap-3">
                <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-secondary text-xs font-bold">
                  {i + 1}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="line-clamp-1 text-sm font-medium">{c.title}</p>
                  <p className="text-xs text-muted-foreground">
                    {(c.studentCount ?? 0).toLocaleString()}명 수강
                  </p>
                </div>
                <span className="shrink-0 text-sm font-semibold">{formatKRW(c.price)}</span>
              </li>
            ))}
          </ol>
        </section>
      </div>
    </div>
  )
}
