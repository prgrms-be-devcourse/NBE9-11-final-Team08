// frontend/app/(shop)/courses/[id]/page.tsx
import { notFound } from 'next/navigation'
import { CourseDetail } from '@/components/course/course-detail'
import { api } from '@/lib/api'

export default async function CoursePage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  
  // api가 제대로 임포트되어 있어야 이 함수가 동작합니다
  const course = await api.getCourse(id)

  // 데이터가 없을 때 notFound() 대신 확인을 위해 강제로 출력
  if (!course) {
    return (
      <div className="mx-auto flex h-[50vh] max-w-2xl flex-col items-center justify-center text-center">
        <h1 className="text-xl font-bold">강좌를 찾을 수 없습니다.</h1>
        <p className="mt-2 text-muted-foreground">요청하신 ID: {id}에 해당하는 강좌가 존재하지 않거나 서버에서 불러올 수 없습니다.</p>
      </div>
    )
  }
  
  return <CourseDetail course={course} />
}
