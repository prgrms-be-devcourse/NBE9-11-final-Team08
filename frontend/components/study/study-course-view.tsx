'use client'

import Link from 'next/link'
import { useState } from 'react'
import {
  CheckCircle2,
  ChevronDown,
  Clock,
  PlayCircle,
  Plus,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import { cn } from '@/lib/utils'
import type { Course, Study } from '@/lib/types'

export function StudyCourseView({
  study,
  course,
}: {
  study: Study
  course: Course
}) {
  const [open, setOpen] = useState<string[]>(course.chapters.map((c) => c.id))
  const base = `/study/${study.id}`

  const toggle = (id: string) =>
    setOpen((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold">강좌 커리큘럼</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            {course.title}
          </p>
        </div>
        {study.myRole === 'owner' && (
          <Button variant="outline" size="sm">
            <Plus className="mr-1 h-4 w-4" />
            챕터 생성하기
          </Button>
        )}
      </div>

      <div className="overflow-hidden rounded-xl border bg-card">
        {course.chapters.map((ch) => {
          const isOpen = open.includes(ch.id)
          const total = ch.lectures.length
          const done = ch.lectures.filter((l) => l.completed).length
          const chapterProgress = Math.round((done / Math.max(total, 1)) * 100)
          return (
            <div key={ch.id} className="border-b last:border-b-0">
              <button
                type="button"
                onClick={() => toggle(ch.id)}
                className="flex w-full items-center gap-3 px-5 py-4 text-left hover:bg-secondary/60"
              >
                <ChevronDown
                  className={cn(
                    'h-4 w-4 shrink-0 text-muted-foreground transition-transform',
                    isOpen && 'rotate-180',
                  )}
                />
                <span className="flex-1 text-sm font-semibold">{ch.title}</span>
                <span className="hidden text-xs text-muted-foreground sm:inline">
                  강의 {total}개
                </span>
                <div className="flex w-28 items-center gap-2">
                  <Progress value={chapterProgress} className="h-2 flex-1" />
                  <span className="w-9 text-right text-xs font-semibold">
                    {chapterProgress}%
                  </span>
                </div>
              </button>

              {isOpen && (
                <ul className="border-t bg-background/50">
                  {ch.lectures.map((lec) => (
                    <li key={lec.id}>
                      <Link
                        href={`${base}/learn`}
                        className="flex items-center gap-3 px-5 py-3 pl-12 text-sm hover:bg-secondary"
                      >
                        {lec.completed ? (
                          <CheckCircle2 className="h-4 w-4 shrink-0 text-primary" />
                        ) : (
                          <PlayCircle className="h-4 w-4 shrink-0 text-muted-foreground" />
                        )}
                        <span className="min-w-0 flex-1 truncate">
                          {lec.title}
                        </span>
                        {lec.completed ? (
                          <Badge variant="outline" className="h-5 px-1.5 text-[10px]">
                            완료
                          </Badge>
                        ) : (
                          <span className="flex items-center gap-1 text-xs text-muted-foreground">
                            <Clock className="h-3 w-3" />
                            {lec.progress}%
                          </span>
                        )}
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
