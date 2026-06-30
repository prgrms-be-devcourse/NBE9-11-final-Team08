import { notFound } from "next/navigation"
import { CourseForm } from "@/components/instructor/course-form"
import { api } from "@/lib/api"

export const metadata = {
  title: "강의 수정 | PlayLearn 셀러센터",
}

export default async function EditCoursePage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const course = await api.getInstructorCourse(id)
  if (!course) notFound()
  return <CourseForm course={course} />
}
