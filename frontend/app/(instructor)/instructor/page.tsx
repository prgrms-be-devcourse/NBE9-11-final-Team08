import { ProductManager } from '@/components/instructor/product-manager'
import { api } from '@/lib/api'

export const metadata = {
  title: '판매자 센터 — PlayLearn',
}

export default async function InstructorPage() {
  const [response, profile] = await Promise.all([
    api.getCourses(0, 100),
    api.getProfile(),
  ])
  const myCourses = response.content.filter((course) => course.instructor.id === profile?.id)

  return <ProductManager courses={myCourses} />
}
