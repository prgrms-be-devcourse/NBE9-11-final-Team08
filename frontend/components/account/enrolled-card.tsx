import Image from 'next/image'
import Link from 'next/link'
import { Progress } from '@/components/ui/progress'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { EnrolledCourse } from '@/lib/types'

export function EnrolledCard({ course }: { course: EnrolledCourse }) {
  return (
    <li className="flex flex-col overflow-hidden rounded-xl border bg-card">
      <div className="relative aspect-video bg-muted">
        <Image
          src={course.thumbnailUrl || '/placeholder.svg'}
          alt={`${course.title} 썸네일`}
          fill
          sizes="(min-width: 1024px) 320px, 100vw"
          className="object-cover"
        />
        {course.status === '완료' ? (
          <Badge className="absolute left-2 top-2 bg-primary text-primary-foreground">
            완료
          </Badge>
        ) : course.dday !== undefined ? (
          <Badge variant="secondary" className="absolute left-2 top-2">
            D-{course.dday}
          </Badge>
        ) : null}
      </div>
      <div className="flex flex-1 flex-col p-4">
        <p className="line-clamp-2 text-sm font-semibold leading-snug">
          {course.title}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">{course.instructor}</p>

        <div className="mt-3">
          <div className="mb-1 flex items-center justify-between text-xs">
            <span className="text-muted-foreground">
              {course.completedLectures}/{course.totalLectures}강 수강
            </span>
            <span className="font-semibold">{course.progress}%</span>
          </div>
          <Progress value={course.progress} className="h-2" />
        </div>

        <Button asChild size="sm" className="mt-4 w-full" variant={course.progress === 100 ? 'outline' : 'default'}>
          <Link href={`/study/${course.id}`}>
            {course.progress === 0
              ? '학습 시작'
              : course.progress === 100
                ? '다시 보기'
                : '이어서 학습'}
          </Link>
        </Button>
      </div>
    </li>
  )
}
