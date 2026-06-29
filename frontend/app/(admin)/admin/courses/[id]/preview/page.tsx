import { notFound } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft } from 'lucide-react'
import { CourseDetail } from '@/components/course/course-detail'
import { api } from '@/lib/api'

export const metadata = {
  title: '강좌 메인 미리보기 — PlayLearn 관리자 콘솔',
}

export default async function AdminCoursePreviewPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const course = await api.getCourse(id)

  if (!course) {
    notFound()
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3 rounded-xl bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-900/50 px-4 py-3">
        <Link
          href="/admin/courses"
          className="flex items-center gap-1.5 text-sm text-amber-700 dark:text-amber-400 hover:underline font-medium"
        >
          <ArrowLeft className="h-4 w-4" />
          강좌 심사 목록으로 돌아가기
        </Link>
        <span className="text-amber-400 dark:text-amber-600">|</span>
        <span className="text-xs font-semibold text-amber-700 dark:text-amber-400">
          🔒 관리자 전용 미리보기 — 모든 강의를 확인할 수 있습니다.
        </span>
      </div>
      <CourseDetail course={course} adminPreview={true} />
    </div>
  )
}
