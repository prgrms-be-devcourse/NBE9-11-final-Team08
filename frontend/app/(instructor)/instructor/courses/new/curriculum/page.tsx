import { CurriculumPage } from "@/components/instructor/curriculum-page"

export const metadata = {
  title: "커리큘럼 구성 | PlayLearn 셀러센터",
}

export default function NewCurriculumPage() {
  return <CurriculumPage backHref="/instructor/courses/new" />
}
