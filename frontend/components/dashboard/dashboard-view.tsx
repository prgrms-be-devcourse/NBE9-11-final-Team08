
'use client'

import Link from 'next/link'
import { useState, useEffect } from 'react'
import { Clock, Flame, Sparkles, TrendingUp, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { Progress } from '@/components/ui/progress'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import type { EnrolledCourse, StudyActivityResponse, StructuredFeedback } from '@/lib/types'
import { api } from '@/lib/api'

interface DashboardViewProps {
  courses: EnrolledCourse[]
  feed: StudyActivityResponse[]
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

        <section>
          <h2 className="mb-4 text-lg font-bold">최근 스터디 활동</h2>
          <ul className="space-y-3">
            {feed.map((item, index) => (
              <ActivityCard key={item.activityId?.toString() || `activity-${index}`} item={item} />
            ))}
          </ul>
        </section>
      </div>
    </div>
  )
}

function ActivityCard({ item }: { item: StudyActivityResponse }) {
  const [feedback, setFeedback] = useState<StructuredFeedback | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (item.activityId) {
      api.getAiFeedback(item.studyId, item.activityId)
         .then(res => setFeedback(res?.feedback ?? null))
         .catch(err => console.error(err))
    }
  }, [item.studyId, item.activityId])

  const requestFeedback = async () => {
    setLoading(true)
    try {
      const res = await api.generateAiFeedback(item.studyId, item.activityId)
      if (res.feedback) {
        setFeedback(res.feedback)
        toast.success('AI 피드백이 도착했습니다.')
      }
    } catch (error) {
      toast.error('AI 피드백 생성에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const dateStr = item.createdAt ? new Date(item.createdAt).toLocaleDateString() : ''

  return (
    <li className="rounded-xl border bg-card p-4">
      <div className="flex items-center gap-2">
        <Avatar className="h-7 w-7">
          <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
            {String(item.authorId).charAt(0)}
          </AvatarFallback>
        </Avatar>
        <div>
          <p className="text-xs font-semibold">작성자 {item.authorId}</p>
          <p className="text-[11px] text-muted-foreground">{dateStr}</p>
        </div>
      </div>
      <p className="mt-2 text-sm leading-relaxed whitespace-pre-wrap">{item.content}</p>

      {feedback ? (
        <div className="mt-3 space-y-2 rounded-lg bg-secondary/60 p-3">
          <p className="flex items-center gap-1 text-xs font-semibold text-primary">
            <Sparkles className="h-3.5 w-3.5" /> AI 코치 피드백
          </p>
          <p className="text-sm font-medium">{feedback.summary}</p>
          {feedback.strengths && (
            <p className="text-xs text-muted-foreground"><strong className="text-foreground">👍 잘한 점:</strong> {feedback.strengths}</p>
          )}
          {feedback.improvements && (
            <p className="text-xs text-muted-foreground"><strong className="text-foreground">💡 보완점:</strong> {feedback.improvements}</p>
          )}
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
            {loading ? <Loader2 className="mr-1 h-4 w-4 animate-spin" /> : <Sparkles className="mr-1 h-4 w-4" />}
            {loading ? '피드백 생성 중…' : 'AI 피드백 요청'}
          </Button>
        </>
      )}
    </li>
  )
}
