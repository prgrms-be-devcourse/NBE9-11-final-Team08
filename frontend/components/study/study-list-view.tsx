import Link from 'next/link'
import { ArrowRight, BookOpen, Clock, ExternalLink, Users } from 'lucide-react'
import { Progress } from '@/components/ui/progress'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { EnrolledCourse } from '@/lib/types'

interface StudyListViewProps {
  courses: EnrolledCourse[]
}

export function StudyListView({ courses }: StudyListViewProps) {
  const active = courses.filter((c) => c.status === '진행 중')
  const done = courses.filter((c) => c.status === '완료')

  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">내 스터디</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            참여 중인 스터디 {active.length}개 · 전체 {courses.length}개
          </p>
        </div>
        <Badge variant="secondary" className="gap-1">
          <Users className="h-3.5 w-3.5" />
          참여 중 {active.length}
        </Badge>
      </header>

      {active.length > 0 && (
        <section className="mt-8">
          <h2 className="mb-4 text-lg font-bold">진행 중인 스터디</h2>
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {active.map((c) => (
              <StudyCard key={c.id} course={c} />
            ))}
          </ul>
        </section>
      )}

      {done.length > 0 && (
        <section className="mt-10">
          <h2 className="mb-4 text-lg font-bold">완료한 스터디</h2>
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {done.map((c) => (
              <StudyCard key={c.id} course={c} />
            ))}
          </ul>
        </section>
      )}

      {courses.length === 0 && (
        <div className="mt-16 flex flex-col items-center gap-3 rounded-xl border border-dashed py-16 text-center">
          <BookOpen className="h-10 w-10 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">
            아직 참여 중인 스터디가 없습니다.
          </p>
          <Button asChild size="sm">
            <Link href="/">강좌 둘러보기</Link>
          </Button>
        </div>
      )}
    </div>
  )
}

function StudyCard({ course }: { course: EnrolledCourse }) {
  return (
    <li className="group relative flex flex-col rounded-xl border bg-card p-5 transition-colors hover:border-primary/50 focus-within:border-primary/50">
      {/* 카드 전체 클릭 시 스터디 메인페이지로 이동 (오버레이 링크) */}
      <Link
        href={`/study/${course.id}`}
        className="absolute inset-0 rounded-xl focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        aria-label={`${course.title} 스터디 메인페이지로 이동`}
      />

      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate font-semibold">{course.title}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">
            {course.instructor}
          </p>
        </div>
        {course.status === '완료' ? (
          <Badge className="shrink-0">완료</Badge>
        ) : course.dday !== undefined ? (
          <Badge variant="secondary" className="shrink-0">
            D-{course.dday}
          </Badge>
        ) : null}
      </div>

      <div className="mt-4 flex items-center gap-3">
        <Progress value={course.progress} className="h-2 flex-1" />
        <span className="w-10 text-right text-xs font-semibold">
          {course.progress}%
        </span>
      </div>

      <p className="mt-2 flex items-center gap-1 text-xs text-muted-foreground">
        <Clock className="h-3.5 w-3.5" />
        {course.completedLectures}/{course.totalLectures}강 수강
      </p>

      {/* 버튼들은 오버레이 링크 위(z-10)에 두어 개별 동작하도록 한다 */}
      <div className="relative z-10 mt-4 flex items-center gap-2">
        <Button asChild variant="secondary" size="sm" className="flex-1 justify-between">
          <Link href={`/study/${course.id}`}>
            스터디 입장
            <ArrowRight className="h-4 w-4" />
          </Link>
        </Button>
        <Button
          asChild
          variant="outline"
          size="sm"
          aria-label="강좌 페이지로 이동"
        >
          <Link href={`/courses/${course.id}`}>
            <ExternalLink className="h-4 w-4" />
            <span className="sr-only sm:not-sr-only">강좌</span>
          </Link>
        </Button>
      </div>
    </li>
  )
}
