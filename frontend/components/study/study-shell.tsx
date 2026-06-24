'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import {
  ArrowLeft,
  BarChart3,
  BookOpen,
  ExternalLink,
  LayoutDashboard,
  MessageSquare,
  Settings,
  type LucideIcon,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import type { Study, StudyStatus } from '@/lib/types'

const STATUS_LABEL: Record<StudyStatus, string> = {
  DRAFT: '준비 중',
  ACTIVE: '진행 중',
  READONLY: '읽기 전용',
  INACTIVE: '비활성',
}

interface NavItem {
  segment: string // '' = 대시보드
  label: string
  icon: LucideIcon
  ownerOnly?: boolean
}

const NAV: NavItem[] = [
  { segment: '', label: '대시보드', icon: LayoutDashboard },
  { segment: 'course', label: '강좌', icon: BookOpen },
  { segment: 'board', label: '게시판', icon: MessageSquare },
  { segment: 'report', label: '리포트', icon: BarChart3 },
  { segment: 'settings', label: '설정', icon: Settings, ownerOnly: true },
]

export function StudyShell({
  study,
  children,
}: {
  study: Study
  children: React.ReactNode
}) {
  const pathname = usePathname()
  const base = `/study/${study.id}`

  const isActive = (segment: string) => {
    if (segment === '') return pathname === base
    return pathname.startsWith(`${base}/${segment}`)
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-6">
      {/* Top bar */}
      <div className="flex flex-wrap items-center gap-3">
        <Button asChild variant="ghost" size="icon" aria-label="내 스터디 목록으로">
          <Link href="/studies">
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <h1 className="truncate text-lg font-bold">{study.name}</h1>
            <Badge
              variant={study.status === 'ACTIVE' ? 'secondary' : 'outline'}
              className="shrink-0"
            >
              {STATUS_LABEL[study.status]}
            </Badge>
          </div>
          <p className="truncate text-xs text-muted-foreground">
            운영자 {study.ownerName} · 멤버 {study.members.length}명
          </p>
        </div>
        <Button asChild variant="outline" size="sm" className="ml-auto">
          <Link href={`/courses/${study.courseId}`}>
            <ExternalLink className="mr-1 h-4 w-4" />
            강좌 페이지
          </Link>
        </Button>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-[200px_1fr]">
        {/* Sidebar nav */}
        <aside className="lg:sticky lg:top-20 lg:self-start">
          <nav className="flex gap-1 overflow-x-auto rounded-xl border bg-card p-2 lg:flex-col lg:overflow-visible">
            {NAV.filter((n) => !n.ownerOnly || study.myRole === 'owner').map(
              (item) => {
                const href = item.segment ? `${base}/${item.segment}` : base
                const active = isActive(item.segment)
                return (
                  <Link
                    key={item.label}
                    href={href}
                    className={cn(
                      'flex shrink-0 items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground',
                      active && 'bg-secondary text-foreground',
                    )}
                  >
                    <item.icon className="h-4 w-4" />
                    {item.label}
                  </Link>
                )
              },
            )}
          </nav>
        </aside>

        {/* Content */}
        <div className="min-w-0">{children}</div>
      </div>
    </div>
  )
}
