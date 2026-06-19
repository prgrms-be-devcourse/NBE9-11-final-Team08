'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { BarChart2, GraduationCap, LayoutGrid, Settings } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

const links = [
  { href: '/instructor', label: '상품 관리', icon: LayoutGrid },
  { href: '/instructor/analytics', label: '판매 분석', icon: BarChart2 },
  { href: '/instructor/settings', label: '설정', icon: Settings },
]

export function InstructorHeader() {
  const pathname = usePathname()

  return (
    <header className="sticky top-0 z-40 border-b bg-card">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-6 px-4">
        <Link href="/instructor" className="flex items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <GraduationCap className="h-5 w-5" />
          </span>
          <span className="font-bold tracking-tight">
            PlayLearn <span className="text-muted-foreground">판매자 센터</span>
          </span>
        </Link>

        <nav className="hidden items-center gap-1 md:flex">
          {links.map((l) => {
            const active = pathname === l.href
            return (
              <Link
                key={l.href}
                href={l.href}
                className={cn(
                  'flex items-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground',
                  active && 'text-foreground',
                )}
              >
                <l.icon className="h-4 w-4" />
                {l.label}
              </Link>
            )
          })}
        </nav>

        <Button asChild variant="ghost" size="sm" className="ml-auto">
          <Link href="/">스토어로 가기</Link>
        </Button>
      </div>
    </header>
  )
}
