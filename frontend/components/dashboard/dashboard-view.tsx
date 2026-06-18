'use client'

import Link from 'next/link'
import { useState } from 'react'
import { Clock, Flame, Sparkles, TrendingUp } from 'lucide-react'
import { toast } from 'sonner'
import { Progress } from '@/components/ui/progress'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import type { ActivityFeedItem, EnrolledCourse } from '@/lib/types'

interface DashboardViewProps {
  courses: EnrolledCourse[]
  feed: ActivityFeedItem[]
}

export function DashboardView({ courses, feed }: DashboardViewProps) {
  const active = courses.filter((c) => c.status === '진행 중')
  const avgProgress = Math.round(
    courses.reduce((s, c) => s + c.progress, 0) / Math.max(courses.length, 1),
  )
  const totalLectures = courses.reduce((s, c) => s + c.totalLectures, 0)
  const doneLectures = courses.reduce((s, c) => s + c.completedLectures, 0)

  const summary = [
    { icon: TrendingUp, label: '평균 진행률', value: `${avgProgress}%` },
    { icon: Clock, label: '수강 강의', value: `${doneLectures}/${totalLectures}강` },
    { icon: Flame, label: '연속 학습', value: '12일' },
  ]

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">대시보드</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          오늘도 한 걸음 — 학습 현황과 활동을 확인하세요.
        </p>
      </div>

      {/* 수강현황 summary */}
      <section className="grid grid-cols-3 gap-3">
        {summary.map((s) => (
          <div key={s.label} className="rounded-xl border bg-card p-4">
            <s.icon className="h-5 w-5 text-muted-foreground" />
            <p className="mt-3 text-xl font-bold">{s.value}</p>
            <p className="text-xs text-muted-foreground">{s.label}</p>
          </div>
        ))}
      </section>

      <div className="grid gap-8 lg:grid-cols-[1fr_330px]">
        {/* 강좌 progress */}
        <section>
          <h2 className="mb-4 text-lg font-bold">강좌별 진행 현황</h2>
          <ul className="space-y-3">
            {active.map((c) => (
              <li key={c.id} className="rounded-xl border bg-card p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold">{c.title}</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                      {c.instructor} · {c.completedLectures}/{c.totalLectures}강
                    </p>
                  </div>
                  {c.dday !== undefined && (
                    <Badge variant="secondary" className="shrink-0">
                      D-{c.dday}
                    </Badge>
                  )}
                </div>
                <div className="mt-3 flex items-center gap-3">
                  <Progress value={c.progress} className="h-2 flex-1" />
                  <span className="w-10 text-right text-xs font-semibold">
                    {c.progress}%
                  </span>
                </div>
                <Button asChild size="sm" variant="ghost" className="mt-2 h-8 px-2 text-xs">
                  <Link href={`/study/${c.id}`}>이어서 학습하기</Link>
                </Button>
              </li>
            ))}
          </ul>
        </section>

        {/* 학습 활동 피드 */}
        <section>
          <h2 className="mb-4 text-lg font-bold">학습 활동 피드</h2>
          <ul className="space-y-3">
            {feed.map((item) => (
              <ActivityCard key={item.id} item={item} />
            ))}
          </ul>
        </section>
      </div>
    </div>
  )
}

function ActivityCard({ item }: { item: ActivityFeedItem }) {
  const [feedback, setFeedback] = useState(item.aiFeedback)
  const [loading, setLoading] = useState(false)

  const requestFeedback = () => {
    setLoading(true)
    setTimeout(() => {
      setFeedback(
        '회고 내용을 잘 정리하셨습니다. 핵심 개념을 본인의 언어로 다시 설명해보면 이해가 더 단단해져요. 다음 강의와 연결해 학습해보세요.',
      )
      setLoading(false)
      toast.success('AI 피드백이 도착했습니다.')
    }, 1200)
  }

  return (
    <li className="rounded-xl border bg-card p-4">
      <div className="flex items-center gap-2">
        <Avatar className="h-7 w-7">
          <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
            {item.author[0]}
          </AvatarFallback>
        </Avatar>
        <div>
          <p className="text-xs font-semibold">{item.author}</p>
          <p className="text-[11px] text-muted-foreground">{item.date}</p>
        </div>
      </div>
      <p className="mt-2 text-sm leading-relaxed">{item.content}</p>

      {feedback ? (
        <div className="mt-3 rounded-lg bg-secondary/60 p-3">
          <p className="flex items-center gap-1 text-xs font-semibold text-primary">
            <Sparkles className="h-3.5 w-3.5" /> AI 피드백
          </p>
          <p className="mt-1 text-sm leading-relaxed">{feedback}</p>
        </div>
      ) : (
        <>
          <Separator className="my-3" />
          <Button
            size="sm"
            variant="secondary"
            className="w-full"
            onClick={requestFeedback}
            disabled={loading}
          >
            <Sparkles className="mr-1 h-4 w-4" />
            {loading ? '피드백 생성 중…' : 'AI 피드백 요청'}
          </Button>
        </>
      )}
    </li>
  )
}
