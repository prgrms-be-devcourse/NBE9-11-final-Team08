'use client'

import { useState, useEffect } from 'react'
import { CalendarCheck, Gift, Ticket, Timer } from 'lucide-react'
import { toast } from 'sonner'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import type { Coupon, Course } from '@/lib/types'
import { api } from '@/lib/api'

const iconFor = (type: Coupon['type']) => {
  if (type === 'attendance') return CalendarCheck
  if (type === 'firstcome') return Timer
  return Gift
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

function CouponCard({
  coupon,
  isOwned,
  courses = [],
}: {
  coupon: Coupon
  isOwned: boolean
  courses?: Course[]
}) {
  const [claimed, setClaimed] = useState(isOwned)
  const [loading, setLoading] = useState(false)
  const [quantity, setQuantity] = useState(coupon.totalQuantity)
  const Icon = iconFor(coupon.type)
  const ended = coupon.category === '종료된 이벤트'
  const isUpcoming = coupon.status === 'SCHEDULED'

  const handleDownload = async () => {
    setLoading(true)
    try {
      await api.downloadCoupon(Number(coupon.id))
      setClaimed(true)
      if (coupon.type === 'firstcome' && quantity !== null && quantity !== undefined) {
        setQuantity((prev) => (prev && prev > 0 ? prev - 1 : 0))
      }
      toast.success('쿠폰이 발급되었습니다.')
    } catch (e: any) {
      if (e.message?.includes('401')) {
        toast.error('로그인이 필요한 기능입니다. 로그인 페이지로 이동합니다.')
        setTimeout(() => {
          window.location.href = '/login'
        }, 1000)
      } else {
        toast.error(e.message || '쿠폰 발급에 실패했습니다.')
      }
    } finally {
      setLoading(false)
    }
  }

  const formatDate = (dateVal?: any | null) => {
    if (!dateVal) return '-'
    let dateStr = ''
    if (Array.isArray(dateVal)) {
      const [year, month, day, hour = 0, minute = 0] = dateVal
      const pad = (n: number) => String(n).padStart(2, '0')
      dateStr = `${year}-${pad(month)}-${pad(day)}T${pad(hour)}:${pad(minute)}`
    } else if (typeof dateVal === 'string') {
      dateStr = dateVal
    } else if (dateVal instanceof Date) {
      const pad = (n: number) => String(n).padStart(2, '0')
      dateStr = `${dateVal.getFullYear()}-${pad(dateVal.getMonth() + 1)}-${pad(dateVal.getDate())}T${pad(dateVal.getHours())}:${pad(dateVal.getMinutes())}`
    } else {
      dateStr = String(dateVal)
    }

    if (!dateStr || dateStr === 'null' || dateStr === 'undefined') return '-'

    const parts = dateStr.split('T')
    const datePart = parts[0].replace(/-/g, '.')
    if (parts[1]) {
      const timeParts = parts[1].split(':')
      return `${datePart} ${timeParts[0]}:${timeParts[1]}`
    }
    return datePart
  }

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

  const couponTypeLabel = coupon.type === 'firstcome' ? '선착순' : coupon.type === 'attendance' ? '출석체크' : '일반'

  return (
    <div className="flex flex-col justify-between rounded-xl border bg-card p-5 shadow-sm transition-all hover:shadow-md">
      <div>
        <div className="flex items-start justify-between">
          <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent text-accent-foreground">
            <Icon className="h-5 w-5" />
          </span>
          <div className="flex flex-col items-end gap-1">
            <div className="flex gap-1.5">
              {coupon.isStackable && (
                <Badge variant="outline" className="border-primary/30 bg-primary/5 text-primary text-xs">
                  중복 사용 가능
                </Badge>
              )}
              <Badge variant={ended ? 'outline' : 'secondary'} className="text-xs">
                {couponTypeLabel}
              </Badge>
            </div>
            {coupon.type === 'firstcome' && quantity !== undefined && quantity !== null && (
              <span className="text-xs font-semibold text-destructive mt-0.5">
                남은 수량: {quantity.toLocaleString()}개
              </span>
            )}
          </div>
        </div>

        <h3 className="mt-4 font-semibold text-pretty text-foreground text-lg">{coupon.name}</h3>
        <p className="mt-1.5 flex items-center gap-1.5 text-base font-bold text-primary">
          <Ticket className="h-4 w-4" /> {coupon.amount}
        </p>

        {/* Metadata info */}
        <div className="mt-4 space-y-2 border-t pt-4 text-xs">
          <div className="flex justify-between gap-4">
            <span className="text-muted-foreground shrink-0">발급 기간</span>
            <span className="font-medium text-foreground text-right">
              {formatDate(coupon.startDate)} ~ {formatDate(coupon.endDate)}
            </span>
          </div>
          <div className="flex justify-between gap-4">
            <span className="text-muted-foreground shrink-0">유효 기간</span>
            <span className="font-medium text-foreground text-right">
              {coupon.validDays ? `발급 후 ${coupon.validDays}일` : '무기한'}
            </span>
          </div>
          <div className="flex justify-between gap-4">
            <span className="text-muted-foreground shrink-0">적용 대상</span>
            <span className="font-medium text-foreground text-right break-words max-w-[70%] text-pretty">
              {getTargetDisplay()}
            </span>
          </div>
          <div className="flex justify-between gap-4">
            <span className="text-muted-foreground shrink-0">사용 타입</span>
            <span className="font-medium text-foreground text-right">{coupon.usageType || '1회용'}</span>
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
        </div>
      </div>

      <Button
        className="mt-5 w-full"
        variant={ended ? 'outline' : isUpcoming ? 'secondary' : 'default'}
        disabled={ended || claimed || loading || isUpcoming}
        onClick={handleDownload}
      >
        {ended ? '종료됨' : isUpcoming ? '시작 전' : claimed ? '발급 완료' : loading ? '발급 중...' : '쿠폰 받기'}
      </Button>
    </div>
  )
}

export function EventsView({
  coupons,
  ownedPolicyIds = [],
  courses = [],
}: {
  coupons: Coupon[]
  ownedPolicyIds?: (string | undefined)[]
  courses?: Course[]
}) {
  const [tab, setTab] = useState('ongoing')
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(9)

  useEffect(() => {
    const handleResize = () => {
      // lg 뷰포트(1024px) 이상이면 3칸이므로 9개, 그 미만(1칸 또는 2칸)이면 8개
      if (window.innerWidth >= 1024) {
        setPageSize(9)
      } else {
        setPageSize(8)
      }
    }
    handleResize()
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  const ongoing = coupons.filter((c) => c.category === '진행 중인 이벤트')
  const ended = coupons.filter((c) => c.category === '종료된 이벤트')

  const currentCoupons = tab === 'ongoing' ? ongoing : ended
  const totalPages = Math.max(1, Math.ceil(currentCoupons.length / pageSize))
  const paginated = currentCoupons.slice((page - 1) * pageSize, page * pageSize)

  const handleTabChange = (value: string) => {
    setTab(value)
    setPage(1)
  }

  const isCouponOwned = (coupon: Coupon) => {
    return ownedPolicyIds.includes(coupon.policyId?.toString())
  }

  return (
    <Tabs value={tab} onValueChange={handleTabChange} className="mt-6">
      <TabsList>
        <TabsTrigger value="ongoing">진행 중인 이벤트</TabsTrigger>
        <TabsTrigger value="ended">종료된 이벤트</TabsTrigger>
      </TabsList>
      <TabsContent value="ongoing" className="mt-6">
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {paginated.map((c) => (
            <CouponCard key={c.id} coupon={c} isOwned={isCouponOwned(c)} courses={courses} />
          ))}
        </div>
      </TabsContent>
      <TabsContent value="ended" className="mt-6">
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {paginated.map((c) => (
            <CouponCard key={c.id} coupon={c} isOwned={isCouponOwned(c)} courses={courses} />
          ))}
        </div>
      </TabsContent>

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
    </Tabs>
  )
}
