import { ProductManager } from '@/components/instructor/product-manager'
import { api } from '@/lib/api'

export const metadata = {
  title: '판매자 센터 — PlayLearn',
}

export default async function InstructorPage() {
  const response = await api.getInstructorCourses('ALL', 0, 100)

  return <ProductManager courses={response.content} />
}
