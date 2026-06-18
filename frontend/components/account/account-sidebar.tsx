'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import {
  Bell,
  LayoutDashboard,
  MessageSquare,
  Receipt,
  Store,
  type LucideIcon,
  BarChart3,
  BookOpen,
} from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import type { UserProfile } from '@/lib/types'

const navItems: { href: string; label: string; icon: LucideIcon }[] = [
  { href: '/dashboard', label: '대시보드', icon: LayoutDashboard },
  { href: '/mypage', label: '내 스터디', icon: BookOpen },
  { href: '/mypage/report', label: '레포트', icon: BarChart3 },
  { href: '/orders', label: '구매', icon: Receipt },
  { href: '/mypage/comments', label: '작성한 댓글', icon: MessageSquare },
  { href: '/mypage/settings', label: '계정 및 알림설정', icon: Bell },
]

export function AccountSidebar({ profile }: { profile: UserProfile }) {
  const pathname = usePathname()

  return (
    <aside className="space-y-4">
      <div className="rounded-xl border bg-card p-5 text-center">
        <Avatar className="mx-auto h-20 w-20">
          <AvatarFallback className="bg-secondary text-2xl text-secondary-foreground">
            {profile.name[0]}
          </AvatarFallback>
        </Avatar>
        <p className="mt-3 font-semibold">{profile.name}</p>
        <p className="truncate text-sm text-muted-foreground">{profile.email}</p>
        {profile.isSeller ? (
          <Button asChild variant="secondary" size="sm" className="mt-4 w-full">
            <Link href="/instructor">
              <Store className="mr-1 h-4 w-4" /> 판매자 센터
            </Link>
          </Button>
        ) : (
          <Button asChild variant="outline" size="sm" className="mt-4 w-full">
            <Link href="/instructor">판매자 신청</Link>
          </Button>
        )}
        <div className="mt-4 grid grid-cols-2 gap-2 text-center">
          <div className="rounded-lg bg-secondary/60 py-2">
            <p className="text-lg font-bold">{profile.studyCount}곳</p>
            <p className="text-xs text-muted-foreground">참여 스터디</p>
          </div>
          <div className="rounded-lg bg-secondary/60 py-2">
            <p className="text-lg font-bold">{profile.courseCount}강좌</p>
            <p className="text-xs text-muted-foreground">구매 강좌</p>
          </div>
        </div>
      </div>

      <nav className="rounded-xl border bg-card p-2">
        {navItems.map((item) => {
          const active =
            pathname === item.href ||
            (item.href !== '/mypage' &&
              item.href !== '/dashboard' &&
              pathname.startsWith(item.href))
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground',
                active && 'bg-secondary text-foreground',
              )}
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </Link>
          )
        })}
      </nav>

      <p className="px-2 text-center text-xs text-muted-foreground">
        <Badge variant="outline" className="font-normal">
          PlayLearn 러닝 스페이스
        </Badge>
      </p>
    </aside>
  )
}
