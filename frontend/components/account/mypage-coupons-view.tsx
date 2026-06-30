'use client'

import { useState } from 'react'
import { Ticket, Info } from 'lucide-react'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { Coupon, Course } from '@/lib/types'

interface MyPageCouponsViewProps {
  coupons: Coupon[]
  courses?: Course[]
}

const CATEGORY_NAMES: Record<string, string> = {
  '1': '개발',
  '2': '디자인',
  '3': '비즈니스',
  '4': '백엔드',
  '5': '프론트엔드',
  '6': 'DevOps',
  '7': 'UI/UX',
  '8': '마케팅',
}

export function MyPageCouponsView({ coupons, courses = [] }: MyPageCouponsViewProps) {
  const [tab, setTab] = useState('active')
  const [page, setPage] = useState(1)
  const pageSize = 8

  const activeCoupons = coupons.filter((c) => c.status === 'ACTIVE')
  const inactiveCoupons = coupons.filter((c) => c.status !== 'ACTIVE')

  const currentCoupons = tab === 'active' ? activeCoupons : inactiveCoupons
  const totalPages = Math.max(1, Math.ceil(currentCoupons.length / pageSize))
  const paginated = currentCoupons.slice((page - 1) * pageSize, page * pageSize)

  const handleTabChange = (value: string) => {
    setTab(value)
    setPage(1)
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">내 쿠폰함</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          보유하신 쿠폰과 혜택 목록을 확인하고 강좌 결제 시 사용해 보세요.
        </p>
      </div>

      <div className="rounded-xl border bg-card p-6">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Info className="h-4 w-4 text-primary font-bold shrink-0" />
          <span>쿠폰은 결제 페이지에서 적용하여 즉시 할인받으실 수 있습니다.</span>
        </div>
      </div>

      <Tabs value={tab} onValueChange={handleTabChange} className="w-full">
        <TabsList className="grid w-full max-w-[400px] grid-cols-2">
          <TabsTrigger value="active">
            사용 가능 ({activeCoupons.length})
          </TabsTrigger>
          <TabsTrigger value="inactive">
            사용 완료 / 만료 ({inactiveCoupons.length})
          </TabsTrigger>
        </TabsList>

        <TabsContent value="active" className="mt-6">
          {activeCoupons.length > 0 ? (
            <div className="grid gap-4 sm:grid-cols-2">
              {paginated.map((coupon) => (
                <CouponCard key={coupon.id} coupon={coupon} isActive={true} courses={courses} />
              ))}
            </div>
          ) : (
            <EmptyState message="사용 가능한 쿠폰이 없습니다." />
          )}
        </TabsContent>

        <TabsContent value="inactive" className="mt-6">
          {inactiveCoupons.length > 0 ? (
            <div className="grid gap-4 sm:grid-cols-2">
              {paginated.map((coupon) => (
                <CouponCard key={coupon.id} coupon={coupon} isActive={false} courses={courses} />
              ))}
            </div>
          ) : (
            <EmptyState message="사용 완료 또는 만료된 쿠폰이 없습니다." />
          )}
        </TabsContent>

        {totalPages > 1 && (
          <div className="mt-8 flex items-center justify-center gap-4">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
            >
              이전
            </Button>
            <span className="text-sm font-medium">
              {page} / {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page === totalPages}
            >
              다음
            </Button>
          </div>
        )}
      </Tabs>
    </div>
  )
}

function CouponCard({
  coupon,
  isActive,
  courses = [],
}: {
  coupon: Coupon
  isActive: boolean
  courses?: Course[]
}) {
  const getTargetDisplay = () => {
    if (coupon.categoryIds && coupon.categoryIds.length > 0) {
      const names = coupon.categoryIds.map(
        (id) => CATEGORY_NAMES[id.toString()] || `카테고리 ${id}`
      )
      return `${names.join(', ')}`
    }
    if (coupon.courseIds && coupon.courseIds.length > 0) {
      const names = coupon.courseIds.map((id) => {
        const course = courses.find((c) => c.id === id.toString())
        return course ? course.title : `강좌 #${id}`
      })
      return names.join(', ')
    }
    return '전체 적용'
  }

  return (
    <div
      className={`relative flex flex-col justify-between overflow-hidden rounded-xl border bg-card p-5 shadow-sm transition-all ${
        isActive
          ? 'border-primary/20 hover:shadow-md'
          : 'border-muted opacity-60'
      }`}
    >
      {/* Decorative Coupon Punch Holes */}
      <div className="absolute top-[40%] -left-3 h-6 w-6 -translate-y-1/2 rounded-full border bg-background" />
      <div className="absolute top-[40%] -right-3 h-6 w-6 -translate-y-1/2 rounded-full border bg-background" />

      <div className="pl-3 pr-3">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-2">
            <span
              className={`flex h-8 w-8 items-center justify-center rounded-lg ${
                isActive ? 'bg-primary/10 text-primary' : 'bg-muted text-muted-foreground'
              }`}
            >
              <Ticket className="h-4 w-4" />
            </span>
          </div>
          <div className="flex gap-1.5">
            {coupon.isStackable && (
              <Badge variant="outline" className={isActive ? 'border-primary/30 bg-primary/5 text-primary text-[10px]' : 'text-[10px]'}>
                중복 가능
              </Badge>
            )}
            <Badge variant="secondary" className="text-[10px]">
              {coupon.usageType || '1회용'}
            </Badge>
          </div>
        </div>

        <h3 className="mt-3 text-base font-bold text-foreground line-clamp-1">{coupon.name}</h3>
        <p className={`mt-1 text-xl font-extrabold ${isActive ? 'text-primary' : 'text-muted-foreground'}`}>
          {coupon.amount}
        </p>
      </div>

      {/* Dashed separator */}
      <div className="my-4 border-t border-dashed border-muted pl-3 pr-3" />

      {/* Metadata info */}
      <div className="space-y-2 pl-3 pr-3 text-xs">
        <div className="flex justify-between gap-4">
          <span className="text-muted-foreground shrink-0">적용 대상</span>
          <span className="font-medium text-foreground text-right break-words max-w-[70%] text-pretty">
            {getTargetDisplay()}
          </span>
        </div>
        {coupon.maxDiscountAmount !== null && coupon.maxDiscountAmount !== undefined && coupon.maxDiscountAmount > 0 && (
          <div className="flex justify-between gap-4">
            <span className="text-muted-foreground shrink-0">최대 할인 금액</span>
            <span className="font-medium text-foreground text-right">
              {coupon.maxDiscountAmount.toLocaleString()}원
            </span>
          </div>
        )}
        {coupon.minOrderAmount !== null && coupon.minOrderAmount !== undefined && coupon.minOrderAmount > 0 && (
          <div className="flex justify-between gap-4">
            <span className="text-muted-foreground shrink-0">최소 주문 금액</span>
            <span className="font-medium text-foreground text-right">
              {coupon.minOrderAmount.toLocaleString()}원
            </span>
          </div>
        )}
        <div className="flex justify-between gap-4 pt-1">
          <span className="text-muted-foreground shrink-0">유효기간</span>
          <span className="font-medium text-foreground text-right">
            {coupon.condition}
          </span>
        </div>
      </div>
    </div>
  )
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="rounded-xl border border-dashed py-16 text-center">
      <p className="text-sm text-muted-foreground">{message}</p>
    </div>
  )
}
