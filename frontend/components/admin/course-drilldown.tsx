'use client'

import { useState } from 'react'
import { ChevronRight, Clock, Loader2 } from 'lucide-react'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { cn } from '@/lib/utils'
import type {
  CourseStatRow,
  EnrolleeRow,
  LearningEventRow,
  LectureStatRow,
} from '@/lib/types'

function fmtSeconds(s: number) {
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${m}:${String(sec).padStart(2, '0')}`
}

function fmtTime(iso: string | null) {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function dropoutBadge(rate: number) {
  const variant = rate >= 70 ? 'destructive' : rate >= 40 ? 'secondary' : 'outline'
  return <Badge variant={variant as any}>{rate.toFixed(1)}%</Badge>
}

export function CourseDrilldown({ initialCourses }: { initialCourses: CourseStatRow[] }) {
  const [selected, setSelected] = useState<CourseStatRow | null>(null)
  const [lectures, setLectures] = useState<LectureStatRow[]>([])
  const [enrollees, setEnrollees] = useState<EnrolleeRow[]>([])
  const [loadingDetail, setLoadingDetail] = useState(false)

  const [timelineUser, setTimelineUser] = useState<EnrolleeRow | null>(null)
  const [timeline, setTimeline] = useState<LearningEventRow[]>([])
  const [loadingTimeline, setLoadingTimeline] = useState(false)

  async function selectCourse(course: CourseStatRow) {
    setSelected(course)
    setTimelineUser(null)
    setTimeline([])
    setLoadingDetail(true)
    const [lec, enr] = await Promise.all([
      api.getAdminLectureStats(course.courseId),
      api.getAdminEnrollees(course.courseId),
    ])
    setLectures(lec)
    setEnrollees(enr?.content ?? [])
    setLoadingDetail(false)
  }

  async function selectUser(course: CourseStatRow, user: EnrolleeRow) {
    setTimelineUser(user)
    setLoadingTimeline(true)
    const res = await api.getAdminUserTimeline(user.userId, course.courseId)
    setTimeline(res?.content ?? [])
    setLoadingTimeline(false)
  }

  if (!initialCourses || initialCourses.length === 0) {
    return (
      <div className="rounded-xl border bg-card p-8 text-center text-sm text-muted-foreground">
        강좌 데이터를 불러오지 못했습니다. 관리자 권한으로 로그인했는지 확인해주세요.
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* 1단계: 강좌별 집계 */}
      <div className="rounded-xl border bg-card">
        <div className="border-b px-5 py-3">
          <h2 className="font-semibold">강좌별 학습 집계</h2>
          <p className="text-xs text-muted-foreground">행을 클릭하면 강의·수강자로 드릴다운합니다.</p>
        </div>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>강좌</TableHead>
              <TableHead>상태</TableHead>
              <TableHead className="text-right">수강자</TableHead>
              <TableHead className="text-right">입장</TableHead>
              <TableHead className="text-right">완강</TableHead>
              <TableHead className="text-right">이탈률</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {initialCourses.map((c) => (
              <TableRow
                key={c.courseId}
                onClick={() => selectCourse(c)}
                className={cn(
                  'cursor-pointer',
                  selected?.courseId === c.courseId && 'bg-secondary',
                )}
              >
                <TableCell className="font-medium">
                  <span className="text-muted-foreground">#{c.courseId}</span> {c.title}
                </TableCell>
                <TableCell>
                  <Badge variant="outline">{c.status}</Badge>
                </TableCell>
                <TableCell className="text-right">{c.enrollees.toLocaleString()}</TableCell>
                <TableCell className="text-right">{c.enterCount.toLocaleString()}</TableCell>
                <TableCell className="text-right">{c.completionCount.toLocaleString()}</TableCell>
                <TableCell className="text-right">{dropoutBadge(c.dropoutRate)}</TableCell>
                <TableCell>
                  <ChevronRight className="h-4 w-4 text-muted-foreground" />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* 2·3단계: 선택된 강좌 상세 */}
      {selected && (
        <div className="rounded-xl border bg-card p-5">
          <h2 className="font-semibold">
            <span className="text-muted-foreground">#{selected.courseId}</span> {selected.title}
          </h2>

          {loadingDetail ? (
            <div className="flex items-center gap-2 py-10 text-sm text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" /> 불러오는 중…
            </div>
          ) : (
            <Tabs defaultValue="lectures" className="mt-4">
              <TabsList>
                <TabsTrigger value="lectures">강의별 통계 ({lectures.length})</TabsTrigger>
                <TabsTrigger value="enrollees">수강자 ({enrollees.length})</TabsTrigger>
              </TabsList>

              <TabsContent value="lectures" className="mt-4">
                {lectures.length === 0 ? (
                  <p className="py-6 text-center text-sm text-muted-foreground">강의가 없습니다.</p>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>챕터</TableHead>
                        <TableHead>강의</TableHead>
                        <TableHead className="text-right">입장</TableHead>
                        <TableHead className="text-right">완료</TableHead>
                        <TableHead className="text-right">평균 시청</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {lectures.map((l) => (
                        <TableRow key={l.lectureId}>
                          <TableCell className="text-muted-foreground">{l.chapterTitle}</TableCell>
                          <TableCell className="font-medium">{l.title}</TableCell>
                          <TableCell className="text-right">{l.enterCount.toLocaleString()}</TableCell>
                          <TableCell className="text-right">{l.completeCount.toLocaleString()}</TableCell>
                          <TableCell className="text-right">{fmtSeconds(l.avgWatchSeconds)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </TabsContent>

              <TabsContent value="enrollees" className="mt-4">
                {enrollees.length === 0 ? (
                  <p className="py-6 text-center text-sm text-muted-foreground">수강자가 없습니다.</p>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>수강자</TableHead>
                        <TableHead className="text-right">완료 강의</TableHead>
                        <TableHead className="text-right">진도율</TableHead>
                        <TableHead>최근 활동</TableHead>
                        <TableHead />
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {enrollees.map((u) => (
                        <TableRow
                          key={u.userId}
                          className={cn(timelineUser?.userId === u.userId && 'bg-secondary')}
                        >
                          <TableCell className="font-medium">
                            {u.nickname}{' '}
                            <span className="text-muted-foreground">#{u.userId}</span>
                          </TableCell>
                          <TableCell className="text-right">
                            {u.completedLectures}/{u.totalLectures}
                          </TableCell>
                          <TableCell className="text-right">{u.progressRate.toFixed(1)}%</TableCell>
                          <TableCell className="text-muted-foreground">
                            {fmtTime(u.lastEventTime)}
                          </TableCell>
                          <TableCell className="text-right">
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => selectUser(selected, u)}
                            >
                              <Clock className="mr-1 h-3.5 w-3.5" /> 타임라인
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}

                {/* 개별 수강자 이벤트 타임라인 */}
                {timelineUser && (
                  <div className="mt-5 rounded-lg border bg-secondary/30 p-4">
                    <h3 className="text-sm font-semibold">
                      {timelineUser.nickname} 님의 이벤트 타임라인
                    </h3>
                    {loadingTimeline ? (
                      <div className="flex items-center gap-2 py-6 text-sm text-muted-foreground">
                        <Loader2 className="h-4 w-4 animate-spin" /> 불러오는 중…
                      </div>
                    ) : timeline.length === 0 ? (
                      <p className="py-4 text-sm text-muted-foreground">기록된 이벤트가 없습니다.</p>
                    ) : (
                      <ol className="mt-3 space-y-2">
                        {timeline.map((e) => (
                          <li key={e.id} className="flex items-center gap-3 text-sm">
                            <span className="w-32 shrink-0 text-muted-foreground">
                              {fmtTime(e.eventTime)}
                            </span>
                            <Badge variant="outline">{e.eventType}</Badge>
                            <span className="text-muted-foreground">
                              강의 #{e.lectureId ?? '-'}
                              {e.positionSeconds != null ? ` · ${fmtSeconds(e.positionSeconds)}` : ''}
                            </span>
                          </li>
                        ))}
                      </ol>
                    )}
                  </div>
                )}
              </TabsContent>
            </Tabs>
          )}
        </div>
      )}
    </div>
  )
}
