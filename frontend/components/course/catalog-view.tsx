'use client'

import { useMemo, useState } from 'react'
import { Search, SlidersHorizontal } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { CourseCard } from '@/components/course/course-card'
import { categories } from '@/lib/mock-data'
import type { Course } from '@/lib/types'
import { cn } from '@/lib/utils'

const sorts = ['추천순', '인기순', '최신순'] as const
const filters = ['AI 생성 콘텐츠 제외', '무료', '할인', '얼리버드 할인']

export function CatalogView({ courses }: { courses: Course[] }) {
  const [category, setCategory] = useState('전체')
  const [sort, setSort] = useState<(typeof sorts)[number]>('추천순')
  const [query, setQuery] = useState('')

  const visible = useMemo(() => {
    let list = courses.filter(
      (c) =>
        (category === '전체' || c.category === category) &&
        (query === '' || c.title.toLowerCase().includes(query.toLowerCase())),
    )
    if (sort === '인기순') list = [...list].sort((a, b) => b.studentCount - a.studentCount)
    if (sort === '최신순') list = [...list].reverse()
    if (sort === '추천순') list = [...list].sort((a, b) => b.rating - a.rating)
    return list
  }, [courses, category, sort, query])

  return (
    <div className="mx-auto max-w-7xl gap-8 px-4 py-8 lg:grid lg:grid-cols-[220px_1fr]">
      {/* Category sidebar */}
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
        {/* Hero banner */}
        <div className="mb-6 overflow-hidden rounded-2xl bg-primary px-6 py-10 text-primary-foreground sm:px-10">
          <p className="text-sm font-medium opacity-90">요즘 대세는</p>
          <h1 className="mt-1 text-2xl font-bold text-balance sm:text-3xl">
            개발부터 AI까지, 실무로 통하는 강좌 모음전
          </h1>
          <p className="mt-2 max-w-lg text-sm leading-relaxed opacity-90">
            PlayLearn에서 강의를 듣고 스터디에 참여하며 회고와 AI 피드백으로 성장하세요.
          </p>
        </div>

        {/* Mobile search + category */}
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

        {/* Filter chips */}
        <div className="mb-4 flex flex-wrap gap-2">
          {filters.map((f) => (
            <Badge key={f} variant="outline" className="cursor-pointer font-normal">
              {f}
            </Badge>
          ))}
        </div>

        {/* Sort + count */}
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
                {s}
              </Button>
            ))}
          </div>
        </div>

        {/* Grid */}
        {visible.length === 0 ? (
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
