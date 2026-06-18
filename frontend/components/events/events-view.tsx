'use client'

import { useState } from 'react'
import { CalendarCheck, Gift, Ticket, Timer } from 'lucide-react'
import { toast } from 'sonner'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import type { Coupon } from '@/lib/types'

const iconFor = (type: Coupon['type']) => {
  if (type === 'attendance') return CalendarCheck
  if (type === 'firstcome') return Timer
  return Gift
}

function CouponCard({ coupon }: { coupon: Coupon }) {
  const [claimed, setClaimed] = useState(false)
  const Icon = iconFor(coupon.type)
  const ended = coupon.category === '종료된 이벤트'

  return (
    <div className="flex flex-col rounded-xl border bg-card p-5">
      <div className="flex items-start justify-between">
        <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent text-accent-foreground">
          <Icon className="h-5 w-5" />
        </span>
        <Badge variant={ended ? 'outline' : 'secondary'}>{coupon.condition}</Badge>
      </div>
      <h3 className="mt-4 font-semibold text-pretty">{coupon.name}</h3>
      <p className="mt-1 flex items-center gap-1 text-sm text-primary">
        <Ticket className="h-4 w-4" /> {coupon.amount}
      </p>
      <Button
        className="mt-4"
        variant={ended ? 'outline' : 'default'}
        disabled={ended || claimed}
        onClick={() => {
          setClaimed(true)
          toast.success('쿠폰이 발급되었습니다.')
        }}
      >
        {ended ? '종료됨' : claimed ? '발급 완료' : '쿠폰 받기'}
      </Button>
    </div>
  )
}

export function EventsView({ coupons }: { coupons: Coupon[] }) {
  const ongoing = coupons.filter((c) => c.category === '진행 중인 이벤트')
  const ended = coupons.filter((c) => c.category === '종료된 이벤트')

  return (
    <Tabs defaultValue="ongoing" className="mt-6">
      <TabsList>
        <TabsTrigger value="ongoing">진행 중인 이벤트</TabsTrigger>
        <TabsTrigger value="ended">종료된 이벤트</TabsTrigger>
      </TabsList>
      <TabsContent value="ongoing" className="mt-6">
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {ongoing.map((c) => (
            <CouponCard key={c.id} coupon={c} />
          ))}
        </div>
      </TabsContent>
      <TabsContent value="ended" className="mt-6">
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {ended.map((c) => (
            <CouponCard key={c.id} coupon={c} />
          ))}
        </div>
      </TabsContent>
    </Tabs>
  )
}
