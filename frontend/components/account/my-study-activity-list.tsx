'use client'

import { useCallback, useEffect, useRef, useState } from 'react'
import Link from 'next/link'
import { Loader2 } from 'lucide-react'
import { api } from '@/lib/api'
import { formatDateTime } from '@/lib/utils'
import type { PageResponse, StudyActivityResponse } from '@/lib/types'

const PAGE_SIZE = 10

export function MyStudyActivityList({
  initialPage,
}: {
  initialPage: PageResponse<StudyActivityResponse>
}) {
  const [items, setItems] = useState(initialPage.content ?? [])
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(!initialPage.last)
  const [loadingMore, setLoadingMore] = useState(false)
  const scrollRef = useRef<HTMLDivElement | null>(null)
  const sentinelRef = useRef<HTMLDivElement | null>(null)

  const loadMore = useCallback(async () => {
    if (!hasMore || loadingMore) return

    setLoadingMore(true)
    try {
      const nextPage = await api.getMyStudyActivities(page, PAGE_SIZE)
      setItems((current) => {
        const known = new Set(current.map((item) => item.activityId))
        const fresh = (nextPage.content ?? []).filter((item) => !known.has(item.activityId))
        return [...current, ...fresh]
      })
      setPage((current) => current + 1)
      setHasMore(!nextPage.last)
    } finally {
      setLoadingMore(false)
    }
  }, [hasMore, loadingMore, page])

  useEffect(() => {
    const root = scrollRef.current
    const target = sentinelRef.current
    if (!root || !target) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry?.isIntersecting) {
          loadMore()
        }
      },
      { root, rootMargin: '80px 0px', threshold: 0.1 },
    )

    observer.observe(target)
    return () => observer.disconnect()
  }, [loadMore])

  if (items.length === 0) {
    return (
      <div className="rounded-xl border border-dashed py-12 text-center">
        <p className="text-sm text-muted-foreground">작성한 스터디 활동이 없습니다.</p>
      </div>
    )
  }

  return (
    <div ref={scrollRef} className="max-h-96 overflow-y-auto overscroll-contain rounded-xl border bg-card">
      <ul>
        {items.map((activity) => (
          <li key={activity.activityId} className="border-b p-4 last:border-b-0">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
              <span>{formatDateTime(activity.createdAt)}</span>
              <Link
                href={`/study/${activity.studyId}/board/${activity.activityId}`}
                className="font-medium text-primary hover:underline"
              >
                활동 보기
              </Link>
            </div>
            <p className="mt-2 line-clamp-3 whitespace-pre-wrap text-sm leading-relaxed">
              {activity.content}
            </p>
          </li>
        ))}
      </ul>

      <div ref={sentinelRef} className="min-h-8 px-4 py-2">
        {loadingMore ? (
          <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
            활동을 더 불러오는 중
          </div>
        ) : null}
      </div>
    </div>
  )
}
