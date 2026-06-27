import Link from 'next/link'
import { BookOpen, Users } from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import { Separator } from '@/components/ui/separator'
import { StudyRealtimeFeed } from '@/components/study/study-realtime-feed'
import type { Study } from '@/lib/types'

export function StudyDashboard({ study }: { study: Study }) {
  const base = `/study/${study.id}`
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
        <div className="grid items-start gap-6 lg:grid-cols-[1fr_320px]">
          {/* 스터디 멤버 */}
          <section className="rounded-xl border bg-card">
            <div className="flex items-center gap-2 border-b px-5 py-3">
              <Users className="h-4 w-4" />
              <h3 className="text-sm font-semibold">스터디 멤버</h3>
              <Badge variant="secondary" className="ml-auto">
                {study.members.length}명
              </Badge>
            </div>
            <ul className="max-h-96 divide-y overflow-y-auto">
              {study.members.map((m) => (
                <li key={m.id} className="flex items-center gap-3 px-5 py-3">
                  <Avatar className="h-8 w-8">
                    {m.avatarUrl ? (
                      <AvatarImage src={m.avatarUrl} alt={m.name} />
                    ) : null}
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
                    {m.joinedAt && (
                      <p className="text-xs text-muted-foreground">
                        참여 {m.joinedAt.slice(0, 10)}
                      </p>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          </section>

          {/* 실시간 피드 */}
          <div className="space-y-6">
            <StudyRealtimeFeed studyId={study.id} />
          </div>
        </div>
      ) : null}
    </div>
  )
}
