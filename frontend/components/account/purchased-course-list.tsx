'use client'

import { useState } from 'react'
import { EnrolledCard } from '@/components/account/enrolled-card'
import { Button } from '@/components/ui/button'
import type { EnrolledCourse } from '@/lib/types'

const PAGE_SIZE = 6

export function PurchasedCourseList({ courses }: { courses: EnrolledCourse[] }) {
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)
  const visibleCourses = courses.slice(0, visibleCount)
  const remainingCount = Math.max(0, courses.length - visibleCount)

  return (
    <div className="space-y-4">
      <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {visibleCourses.map((course) => (
          <EnrolledCard key={course.id} course={course} />
        ))}
      </ul>

      {remainingCount > 0 && (
        <div className="flex justify-center">
          <Button
            type="button"
            variant="outline"
            onClick={() => setVisibleCount((count) => count + PAGE_SIZE)}
          >
            더보기 ({remainingCount}개 남음)
          </Button>
        </div>
      )}
    </div>
  )
}
