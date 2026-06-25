import Link from 'next/link'
import { ArrowRight, Bell, BookOpen, MessageSquare, Users } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import { Separator } from '@/components/ui/separator'
import type { Study } from '@/lib/types'

export function StudyDashboard({ study }: { study: Study }) {
  const base = `/study/${study.id}`
  const avgProgress = Math.round(
    study.members.reduce((s, m) => s + m.progress, 0) /
      Math.max(study.members.length, 1),
  )
  const canUseStudyFeatures = study.myRole !== 'viewer'

  return (
    <div className="space-y-6">
      {/* Study intro */}
      <section className="rounded-xl border bg-card p-5">
        <p className="text-xs font-medium text-muted-foreground">스터디 소개</p>
        <h2 className="mt-1 text-xl font-bold text-balance">{study.name}</h2>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          {study.intro}
        </p>
        <Separator className="my-4" />
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-3">
            <span className="text-xs text-muted-foreground">내 진행률</span>
            <Progress value={study.progress} className="h-2 w-32" />
            <span className="text-xs font-semibold">{study.progress}%</span>
          </div>
          <Button asChild size="sm" className="ml-auto">
            <Link href={`${base}/learn`}>
              <BookOpen className="mr-1 h-4 w-4" />
              이어서 학습하기
            </Link>
          </Button>
        </div>
      </section>

      {canUseStudyFeatures ? (
        <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
          {/* 수강현황 */}
          <section className="rounded-xl border bg-card">
            <div className="flex items-center gap-2 border-b px-5 py-3">
              <Users className="h-4 w-4" />
              <h3 className="text-sm font-semibold">수강 현황</h3>
              <Badge variant="secondary" className="ml-auto">
                평균 {avgProgress}%
              </Badge>
            </div>
            <ul className="divide-y">
              {study.members.map((m) => (
                <li key={m.id} className="flex items-center gap-3 px-5 py-3">
                  <Avatar className="h-8 w-8">
                    <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                      {m.name[0]}
                    </AvatarFallback>
                  </Avatar>
                  <div className="min-w-0 flex-1">
                    <p className="flex items-center gap-1.5 truncate text-sm font-medium">
                      {m.name}
                      {m.role === 'owner' && (
                        <Badge variant="outline" className="h-5 px-1.5 text-[10px]">
                          운영자
                        </Badge>
                      )}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      참여 {m.joinedAt}
                    </p>
                  </div>
                  <div className="flex w-28 items-center gap-2">
                    <Progress value={m.progress} className="h-2 flex-1" />
                    <span className="w-9 text-right text-xs font-semibold">
                      {m.progress}%
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          </section>

          {/* 공지 + 게시판 바로가기 */}
          <div className="space-y-6">
            <section className="rounded-xl border bg-card">
              <div className="flex items-center gap-2 border-b px-5 py-3">
                <Bell className="h-4 w-4" />
                <h3 className="text-sm font-semibold">공지</h3>
              </div>
              <ul className="divide-y">
                {study.announcements.map((a) => (
                  <li key={a.id} className="px-5 py-3">
                    <p className="text-sm font-medium">{a.title}</p>
                    <p className="mt-1 line-clamp-2 text-xs leading-relaxed text-muted-foreground">
                      {a.content}
                    </p>
                    <p className="mt-1 text-[11px] text-muted-foreground">
                      {a.createdAt}
                    </p>
                  </li>
                ))}
              </ul>
            </section>

            <section className="rounded-xl border bg-card p-5">
              <div className="flex items-center gap-2">
                <MessageSquare className="h-4 w-4" />
                <h3 className="text-sm font-semibold">학습 활동 피드</h3>
              </div>
              <p className="mt-2 text-xs leading-relaxed text-muted-foreground">
                스터디 멤버들의 학습 회고 {study.posts.length}건이 공유되고 있어요.
              </p>
              <Button
                asChild
                variant="secondary"
                size="sm"
                className="mt-3 w-full justify-between"
              >
                <Link href={`${base}/board`}>
                  게시판 바로가기
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            </section>
          </div>
        </div>
      ) : null}
    </div>
  )
}
