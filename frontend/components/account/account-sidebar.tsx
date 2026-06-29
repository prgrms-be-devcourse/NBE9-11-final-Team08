// frontend/components/account/account-sidebar.tsx
'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { cn } from '@/lib/utils'
import type { UserProfile } from '@/lib/types'

const links = [
  { label: '대시보드', href: '/dashboard' },
  { label: '내 강좌 & 스터디', href: '/mypage' },
  { label: '스터디 리포트', href: '/mypage/report' },
  { label: '주문 내역', href: '/orders' },
  { label: '내 쿠폰', href: '/mypage/coupons' },
  { label: '작성한 댓글', href: '/mypage/comments' },
  { label: '설정', href: '/mypage/settings' },
]

export function AccountSidebar({ profile }: { profile: UserProfile | null }) {
  const pathname = usePathname()

  return (
    <aside className="w-full lg:w-64">
      <div className="rounded-xl border bg-card p-6 text-center">
        <Avatar className="mx-auto h-20 w-20">
          <AvatarFallback className="bg-secondary text-2xl text-secondary-foreground">
            {profile?.name ? profile.name[0] : 'U'}
          </AvatarFallback>
        </Avatar>
        <p className="mt-3 font-semibold">{profile?.name ?? '사용자'}</p>
        <p className="text-xs text-muted-foreground">{profile?.email ?? '이메일 없음'}</p>
      </div>

      <nav className="mt-4 space-y-1">
        {links.map((l) => {
          const isActive = pathname === l.href || (
            l.href !== '/dashboard' && // Keep this condition for dashboard
            l.href !== '/mypage' && // Prevent /mypage from activating when on child routes
            pathname.startsWith(l.href)
          )
          return (
            <Link
              key={l.href}
              href={l.href}
              className={cn(
                'block rounded-lg px-4 py-2.5 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary/10 text-primary'
                  : 'text-muted-foreground hover:bg-secondary hover:text-foreground',
              )}
            >
              {l.label}
            </Link>
          )
        })}
      </nav>
    </aside>
  )
}
