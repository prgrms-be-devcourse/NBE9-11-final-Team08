'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { GraduationCap, Search, ShoppingCart } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { useCart } from '@/components/providers/cart-provider'
import { cn } from '@/lib/utils'

const navLinks = [
  { href: '/studies', label: '스터디' },
  { href: '/', label: '강좌' },
  { href: '/events', label: '쿠폰/이벤트' },
  { href: '/attendance', label: '출석' },
]

export function SiteHeader() {
  const pathname = usePathname()
  const { items } = useCart()

  return (
    <header className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4">
        <Link href="/" className="flex shrink-0 items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <GraduationCap className="h-5 w-5" />
          </span>
          <span className="text-lg font-bold tracking-tight">PlayLearn</span>
        </Link>

        <nav className="hidden items-center gap-1 md:flex">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={cn(
                'rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground',
                pathname === link.href && 'text-foreground',
              )}
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="relative ml-auto hidden max-w-xs flex-1 items-center lg:flex">
          <Search className="pointer-events-none absolute left-3 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="배우고 싶은 지식을 검색해보세요"
            className="pl-9"
            aria-label="강좌 검색"
          />
        </div>

        <div className="ml-auto flex items-center gap-1 lg:ml-2">
          <Button asChild variant="ghost" size="icon" aria-label="장바구니">
            <Link href="/cart" className="relative">
              <ShoppingCart className="h-5 w-5" />
              {items.length > 0 && (
                <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-semibold text-primary-foreground">
                  {items.length}
                </span>
              )}
            </Link>
          </Button>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" className="gap-2 px-2">
                <Avatar className="h-7 w-7">
                  <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                    김
                  </AvatarFallback>
                </Avatar>
                <span className="hidden text-sm font-medium sm:inline">마이</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48">
              <DropdownMenuLabel>김아무개님</DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem asChild>
                <Link href="/mypage">마이페이지</Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link href="/dashboard">대시보드</Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link href="/mypage/report">내 스터디 리포트</Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link href="/instructor">판매자 센터</Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link href="/admin">관리자 콘솔</Link>
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem asChild>
                <Link href="/login">로그인</Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link href="/signup">회원가입</Link>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </header>
  )
}
