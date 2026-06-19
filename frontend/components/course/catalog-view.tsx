// frontend/components/course/catalog-view.tsx
'use client'

import { useEffect, useMemo, useState } from 'react'
import { Search, SlidersHorizontal, Loader2 } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { CourseCard } from '@/components/course/course-card'
import type { Course } from '@/lib/types'
import { api } from '@/lib/api'
import { cn } from '@/lib/utils'

const sorts = ['VIEW_DESC', 'LATEST', 'PRICE_ASC'] as const
const sortLabels: Record<string, string> = {
  VIEW_DESC: '인기순',
  LATEST: '최신순',
  PRICE_ASC: '낮은가격순',
}
const filters = ['무료', '할인']

export function CatalogView() {
  const [category, setCategory] = useState('전체')
  const [sort, setSort] = useState<(typeof sorts)[number]>('VIEW_DESC')
  const [query, setQuery] = useState('')
  
  const [courses, setCourses] = useState<Course[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchCourses = async () => {
      setLoading(true)
      try {
        // Fetch up to 100 items to allow efficient client-side filtering by category
        const response = await api.getCourses(0, 100, sort)
        setCourses(response.content)
        setTotalElements(response.totalElements)
      } catch (err) {
        console.error('Failed to fetch courses', err)
      } finally {
        setLoading(false)
      }
    }
    fetchCourses()
  }, [sort])

  const categories = useMemo(
    () => ['전체', ...Array.from(new Set(courses.map((c) => c.category).filter(Boolean)))],
    [courses],
  )

  const visible = useMemo(() => {
    let list = courses.filter(
      (c) =>
        (category === '전체' || c.category === category) &&
        (query === '' || c.title.toLowerCase().includes(query.toLowerCase())),
    )
    return list
  }, [courses, category, query])

  return (
    <div className="mx-auto max-w-7xl gap-8 px-4 py-8 lg:grid lg:grid-cols-[220px_1fr]">
      <aside className="hidden lg:block">
        <div className="sticky top-20">
          <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold">
            <SlidersHorizontal className="h-4 w-4" /> 카테고리
          </h2>
          <ul className="space-y-1">
            {categories.map((c) => (
              <li key={c}>
                <button
                  onClick={() => setCategory(c)}
                  className={cn(
                    'w-full rounded-md px-3 py-1.5 text-left text-sm text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground',
                    category === c && 'bg-secondary font-medium text-foreground',
                  )}
                >
                  {c}
                </button>
              </li>
            ))}
          </ul>
        </div>
      </aside>

      <div>
        <div className="mb-6 overflow-hidden rounded-2xl bg-primary px-6 py-10 text-primary-foreground sm:px-10">
          <p className="text-sm font-medium opacity-90">요즘 대세는</p>
          <h1 className="mt-1 text-2xl font-bold text-balance sm:text-3xl">
            개발부터 AI까지, 실무로 통하는 강좌 모음전
          </h1>
          <p className="mt-2 max-w-lg text-sm leading-relaxed opacity-90">
            PlayLearn에서 강의를 듣고 스터디에 참여하며 회고와 AI 피드백으로 성장하세요.
          </p>
        </div>

        <div className="mb-4 lg:hidden">
          <div className="relative flex items-center">
            <Search className="pointer-events-none absolute left-3 h-4 w-4 text-muted-foreground" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="강좌 검색"
              className="pl-9"
            />
          </div>
          <div className="mt-3 flex gap-2 overflow-x-auto pb-1">
            {categories.slice(0, 8).map((c) => (
              <button
                key={c}
                onClick={() => setCategory(c)}
                className={cn(
                  'whitespace-nowrap rounded-full border px-3 py-1 text-xs',
                  category === c
                    ? 'border-primary bg-primary text-primary-foreground'
                    : 'text-muted-foreground',
                )}
              >
                {c}
              </button>
            ))}
          </div>
        </div>

        <div className="mb-4 flex flex-wrap gap-2">
          {filters.map((f) => (
            <Badge key={f} variant="outline" className="cursor-pointer font-normal">
              {f}
            </Badge>
          ))}
        </div>

        <div className="mb-4 flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            전체 강의 <span className="font-semibold text-foreground">{visible.length}</span>개
          </p>
          <div className="flex gap-1">
            {sorts.map((s) => (
              <Button
                key={s}
                variant={sort === s ? 'secondary' : 'ghost'}
                size="sm"
                onClick={() => setSort(s)}
              >
                {sortLabels[s]}
              </Button>
            ))}
          </div>
        </div>

        {loading ? (
          <div className="flex flex-col items-center justify-center py-16">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground mb-4" />
            <p className="text-sm text-muted-foreground">강좌를 불러오는 중입니다...</p>
          </div>
        ) : visible.length === 0 ? (
          <p className="py-16 text-center text-sm text-muted-foreground">
            조건에 맞는 강좌가 없습니다.
          </p>
        ) : (
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {visible.map((course) => (
              <CourseCard key={course.id} course={course} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
