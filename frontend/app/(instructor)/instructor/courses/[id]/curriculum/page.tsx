import { notFound } from "next/navigation"
import { CurriculumPage } from "@/components/instructor/curriculum-page"
import { api } from "@/lib/api"

export const metadata = {
  title: "커리큘럼 구성 | PlayLearn 셀러센터",
}

export default async function EditCurriculumPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const course = await api.getCourse(id)
  if (!course) notFound()
  return (
    <CurriculumPage backHref={`/instructor/courses/${id}`} courseTitle={course.title} />
  )
}
