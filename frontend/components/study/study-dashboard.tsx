import Link from 'next/link'
import { BookOpen, PlayCircle, Users } from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { StudyRealtimeFeed } from '@/components/study/study-realtime-feed'
import { formatSeconds } from '@/lib/utils'
import type { LectureEnterResponse, Study } from '@/lib/types'

export function StudyDashboard({
  study,
  lastWatched,
}: {
  study: Study
  lastWatched?: LectureEnterResponse | null
}) {
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
        {lastWatched ? (
          <div className="flex flex-wrap items-center gap-3 rounded-lg border bg-secondary/40 px-4 py-3">
            <PlayCircle className="h-5 w-5 shrink-0 text-primary" />
            <div className="min-w-0 flex-1">
              <p className="text-[11px] text-muted-foreground">
                이어서 학습할 강의
              </p>
              <p className="truncate text-sm font-medium">{lastWatched.title}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">
                {formatSeconds(lastWatched.progress?.lastPositionSeconds)} /{' '}
                {formatSeconds(lastWatched.durationSeconds)}
                {lastWatched.progress?.completed ? ' · 완료' : ''}
              </p>
            </div>
            <Button asChild size="sm" className="ml-auto">
              <Link href={`${base}/learn`}>
                <BookOpen className="mr-1 h-4 w-4" />
                이어서 학습하기
              </Link>
            </Button>
          </div>
        ) : (
          <div className="flex flex-wrap items-center gap-3 rounded-lg border border-dashed px-4 py-3">
            <BookOpen className="h-5 w-5 shrink-0 text-muted-foreground" />
            <p className="min-w-0 flex-1 text-sm text-muted-foreground">
              아직 시청한 강의가 없어요. 첫 강의부터 시작해보세요.
            </p>
            <Button asChild size="sm" className="ml-auto">
              <Link href={`${base}/learn`}>
                <BookOpen className="mr-1 h-4 w-4" />
                학습 시작하기
              </Link>
            </Button>
          </div>
        )}
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
