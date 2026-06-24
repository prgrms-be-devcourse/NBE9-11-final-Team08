import Image from 'next/image'
import Link from 'next/link'
import { Star, Users } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import type { Course } from '@/lib/types'
import { cn, discountedPrice, formatKRW } from '@/lib/utils'

export function CourseCard({ course }: { course: Course }) {
  const final = discountedPrice(course.price, course.discountRate)

  return (
    <Link
      href={`/courses/${course.id}`}
      className="group flex flex-col overflow-hidden rounded-xl border bg-card transition-shadow hover:shadow-md"
    >
      <div className="relative aspect-video overflow-hidden bg-muted">
        <Image
          src={course.thumbnailUrl || '/placeholder.svg'}
          alt={`${course.title} 썸네일`}
          fill
          sizes="(max-width: 768px) 100vw, 25vw"
          className="object-cover transition-transform duration-300 group-hover:scale-105"
        />
      </div>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <div className="flex flex-wrap gap-1">
          {course.badges?.map((b) => (
            <Badge
              key={b}
              variant="secondary"
              className={cn(
                'text-[11px]',
                b === 'New' && 'bg-accent text-accent-foreground',
              )}
            >
              {b}
            </Badge>
          ))}
        </div>
        <h3 className="line-clamp-2 text-sm font-semibold leading-snug text-pretty">
          {course.title}
        </h3>
        <p className="text-xs text-muted-foreground">{course.instructor.name}</p>

        <div className="mt-auto flex items-center gap-3 pt-1 text-xs text-muted-foreground">
          <span className="flex items-center gap-1 text-amber-500">
            <Star className="h-3.5 w-3.5 fill-current" />
            <span className="font-medium text-foreground">{course.rating.toFixed(1)}</span>
            <span>({course.reviewCount})</span>
          </span>
          <span className="flex items-center gap-1">
            <Users className="h-3.5 w-3.5" />
            {course.studentCount.toLocaleString('ko-KR')}+
          </span>
        </div>

        <div className="flex items-baseline gap-2 pt-1">
          {course.discountRate ? (
            <>
              <span className="text-sm font-bold text-destructive">{course.discountRate}%</span>
              <span className="text-base font-bold">{formatKRW(final)}</span>
            </>
          ) : (
            <span className="text-base font-bold">{formatKRW(final)}</span>
          )}
        </div>
      </div>
    </Link>
  )
}
