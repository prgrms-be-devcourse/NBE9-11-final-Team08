// frontend/app/(shop)/studies/page.tsx
import { StudyListView } from '@/components/study/study-list-view'
import { api } from '@/lib/api'
import type { EnrolledCourse } from '@/lib/types'

export default async function StudiesPage() {
  const [myStudies, purchasedCourses] = await Promise.all([
    api.getMyStudies(),
    api.getPurchasedCourses(),
  ])

  const combinedCoursesMap = new Map<string, EnrolledCourse>()

  // Prioritize active studies (myStudies) as they have more accurate status and progress
  myStudies.forEach(study => {
    combinedCoursesMap.set(study.id, study)
  })

  // Add purchased courses that are not already active studies
  purchasedCourses.forEach(course => {
    if (!combinedCoursesMap.has(course.id)) {
      // For purchased courses not yet active studies, set default progress and status
      combinedCoursesMap.set(course.id, {
        ...course,
        progress: course.progress ?? 0,
        totalLectures: course.totalLectures ?? 0,
        completedLectures: course.completedLectures ?? 0,
        status: course.status ?? '진행 중',
      })
    }
  })

  const courses = Array.from(combinedCoursesMap.values())
  
  return <StudyListView courses={courses} />
}
