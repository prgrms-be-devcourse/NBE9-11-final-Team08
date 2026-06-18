import { CatalogView } from '@/components/course/catalog-view'
import { api } from '@/lib/api'

export default async function CatalogPage() {
  const courses = await api.getCourses()
  return <CatalogView courses={courses} />
}
