'use client'

import { useCallback, useEffect, useRef, useState } from 'react'
import { Loader2, MessageSquare, Radio } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { api } from '@/lib/api'
import type { BackendDateTime, FeedCursor, FeedItemResponse } from '@/lib/types'

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'
const PAGE_SIZE = 10

type ConnectionState = 'connecting' | 'open' | 'error'

export function StudyRealtimeFeed({ studyId }: { studyId: string }) {
  const [items, setItems] = useState<FeedItemResponse[]>([])
  const [nextCursor, setNextCursor] = useState<FeedCursor | null>(null)
  const [hasNext, setHasNext] = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [connectionState, setConnectionState] = useState<ConnectionState>('connecting')
  const scrollRef = useRef<HTMLDivElement | null>(null)
  const sentinelRef = useRef<HTMLDivElement | null>(null)

  const mergeItems = useCallback((incoming: FeedItemResponse[], placement: 'start' | 'end') => {
    setItems((current) => {
      const known = new Set(current.map((item) => item.id))
      const fresh = incoming.filter((item) => !known.has(item.id))
      return placement === 'start' ? [...fresh, ...current] : [...current, ...fresh]
    })
  }, [])

  const loadMore = useCallback(async () => {
    if (!hasNext || !nextCursor || loadingMore) return

    setLoadingMore(true)
    try {
      const response = await api.getStudyFeed(studyId, nextCursor, PAGE_SIZE)
      mergeItems(response.items, 'end')
      setNextCursor(response.nextCursor)
      setHasNext(response.hasNext)
    } finally {
      setLoadingMore(false)
    }
  }, [hasNext, loadingMore, mergeItems, nextCursor, studyId])

  useEffect(() => {
    let active = true

    const loadInitialFeed = async () => {
      setItems([])
      setInitialLoading(true)
      try {
        const response = await api.getStudyFeed(studyId, null, PAGE_SIZE)
        if (!active) return
        setItems((current) => {
          const initialIds = new Set(response.items.map((item) => item.id))
          const liveItems = current.filter((item) => !initialIds.has(item.id))
          return [...liveItems, ...response.items]
        })
        setNextCursor(response.nextCursor)
        setHasNext(response.hasNext)
      } finally {
        if (active) setInitialLoading(false)
      }
    }

    loadInitialFeed()

    return () => {
      active = false
    }
  }, [studyId])

  useEffect(() => {
    const source = new EventSource(`${BASE_URL}/api/studies/${studyId}/feed/stream`, {
      withCredentials: true,
    })

    source.onopen = () => setConnectionState('open')
    source.onerror = () => setConnectionState('error')

    source.addEventListener('feed-item.created', (event) => {
      try {
        const item = JSON.parse(event.data) as FeedItemResponse
        mergeItems([item], 'start')
        setConnectionState('open')
      } catch (error) {
        console.error('피드 이벤트 파싱 실패:', error)
      }
    })

    source.addEventListener('server-draining', () => {
      setConnectionState('connecting')
    })

    source.addEventListener('connected', () => {
      setConnectionState('open')
    })

    return () => {
      source.close()
    }
  }, [mergeItems, studyId])

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

  return (
    <section className="rounded-xl border bg-card">
      <div className="flex items-center gap-2 border-b px-5 py-3">
        <MessageSquare className="h-4 w-4" />
        <h3 className="text-sm font-semibold">스터디 피드</h3>
        <ConnectionBadge state={connectionState} />
      </div>

      <div ref={scrollRef} className="h-[360px] overflow-y-auto">
        {initialLoading ? (
          <div className="flex items-center justify-center gap-2 px-5 py-12 text-xs text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            피드를 불러오는 중
          </div>
        ) : items.length === 0 ? (
          <div className="px-5 py-12 text-center">
            <MessageSquare className="mx-auto h-8 w-8 text-muted-foreground" />
            <p className="mt-3 text-sm font-medium">아직 피드가 없어요.</p>
            <p className="mt-1 text-xs text-muted-foreground">
              멤버가 학습 활동을 작성하면 여기에 표시됩니다.
            </p>
          </div>
        ) : (
          <ul className="divide-y">
            {items.map((item) => (
              <li key={item.id} className="px-5 py-3">
                <p className="text-sm font-medium">{getFeedMessage(item)}</p>
                <p className="mt-1 text-[11px] text-muted-foreground">
                  {formatFeedTime(item.occurredAt)}
                </p>
              </li>
            ))}
          </ul>
        )}

        <div ref={sentinelRef} className="min-h-8 px-5 py-2">
          {loadingMore ? (
            <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
              이전 피드를 불러오는 중
            </div>
          ) : !initialLoading && items.length > 0 && !hasNext ? (
            <p className="text-center text-[11px] text-muted-foreground">
              모든 피드를 확인했어요.
            </p>
          ) : null}
        </div>
      </div>
    </section>
  )
}

function ConnectionBadge({ state }: { state: ConnectionState }) {
  const label = state === 'open' ? '실시간' : state === 'connecting' ? '연결 중' : '재연결 중'

  return (
    <Badge variant={state === 'open' ? 'secondary' : 'outline'} className="ml-auto gap-1">
      <Radio className="h-3 w-3" />
      {label}
    </Badge>
  )
}

function getFeedMessage(item: FeedItemResponse) {
  switch (item.type) {
    case 'STUDY_ACTIVITY':
      return `${item.actorNickname}가 학습활동을 작성하였습니다.`
    default:
      return `${item.actorNickname}의 새 활동이 있습니다.`
  }
}

function formatFeedTime(value: BackendDateTime) {
  if (!value) return ''

  const date = toDate(value)
  if (!date) return Array.isArray(value) ? '' : value

  const diffSeconds = Math.floor((Date.now() - date.getTime()) / 1000)
  if (diffSeconds < 60) return '방금 전'

  const diffMinutes = Math.floor(diffSeconds / 60)
  if (diffMinutes < 60) return `${diffMinutes}분 전`

  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) return `${diffHours}시간 전`

  const now = new Date()
  const isSameYear = date.getFullYear() === now.getFullYear()
  return date.toLocaleString('ko-KR', {
    year: isSameYear ? undefined : 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function toDate(value: BackendDateTime) {
  if (Array.isArray(value)) {
    const [year, month = 1, day = 1, hour = 0, minute = 0, second = 0, nano = 0] = value
    return new Date(year, month - 1, day, hour, minute, second, Math.floor(nano / 1_000_000))
  }

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}
