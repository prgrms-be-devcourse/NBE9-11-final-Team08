import { api } from '@/lib/api'
import { CourseDrilldown } from '@/components/admin/course-drilldown'

export default async function AdminLearningPage() {
  const courses = await api.getAdminCourseStats(0, 50)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">학습 활동 드릴다운</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          강좌 → 강의 → 수강자 → 개별 이벤트 타임라인까지 탐색하세요.
        </p>
      </div>

      <CourseDrilldown initialCourses={courses?.content ?? []} />
    </div>
  )
}
