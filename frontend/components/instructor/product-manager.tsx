// frontend/components/instructor/product-manager.tsx
'use client'

import Image from 'next/image'
import Link from 'next/link'
import { Pencil, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { formatKRW } from '@/lib/utils'
import type { Course } from '@/lib/types'

const statusLabel: Record<string, { label: string; variant: 'default' | 'secondary' | 'outline' }> = {
  PUBLISHED: { label: '게시중', variant: 'default' },
  REVIEW: { label: '승인 대기', variant: 'secondary' },
  DRAFT: { label: '임시저장', variant: 'outline' },
  CLOSED: { label: '비공개', variant: 'outline' },
}

export function ProductManager({ courses }: { courses: Course[] }) {
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">내가 등록한 강의 관리</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            강의를 등록하고 승인 상태를 관리하세요.
          </p>
        </div>
        <Button asChild>
          <Link href="/instructor/courses/new">
            <Plus className="mr-1 h-4 w-4" /> 새 강의 등록
          </Link>
        </Button>
      </div>

      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {courses.map((c) => {
          const status = statusLabel[c.status ?? 'DRAFT'] || statusLabel['DRAFT']
          return (
            <li key={c.id} className="overflow-hidden rounded-xl border bg-card">
              <div className="relative aspect-video bg-muted">
                <Image
                  src={c.thumbnailUrl || '/placeholder.svg'}
                  alt={`${c.title} 표지`}
                  fill
                  sizes="(min-width:1024px) 320px, 100vw"
                  className="object-cover"
                />
                <Badge
                  variant={status.variant}
                  className="absolute left-2 top-2"
                >
                  {status.label}
                </Badge>
              </div>
              <div className="p-4">
                <p className="line-clamp-1 text-sm font-semibold">{c.title}</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  {c.category} &gt; {c.subCategory || '분류 없음'}
                </p>
                <p className="mt-2 text-sm font-bold">{formatKRW(c.price)}</p>
                <div className="mt-3 flex gap-2">
                  <Button asChild size="sm" variant="outline" className="flex-1">
                    <Link href={`/instructor/courses/${c.id}`}>
                      <Pencil className="mr-1 h-3.5 w-3.5" /> 수정하기
                    </Link>
                  </Button>
                  <Button asChild size="sm" variant="ghost">
                    <Link href={`/instructor/courses/${c.id}/curriculum`}>
                      커리큘럼
                    </Link>
                  </Button>
                </div>
              </div>
            </li>
          )
        })}
      </ul>
      
      {courses.length === 0 && (
        <div className="rounded-xl border border-dashed py-16 text-center text-sm text-muted-foreground">
          등록한 강의가 없습니다. 첫 강의를 등록해보세요!
        </div>
      )}
    </div>
  )
}
