import { Suspense } from 'react'
import { CatalogView } from '@/components/course/catalog-view'
import { api } from '@/lib/api'

export default async function CatalogPage() {
  return (
    <Suspense fallback={null}>
      <CatalogView />
    </Suspense>
  )
}
