'use client'

import { useState } from 'react'
import { CalendarDays, Check, Flame } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

export function AttendanceView() {
  const [checkedToday, setCheckedToday] = useState(false)
  const [monthTotal, setMonthTotal] = useState(18)
  const [streak, setStreak] = useState(6)
  const today = 19
  const checkedDays = Array.from({ length: 18 }, (_, i) => i + 1)

  const handleCheck = () => {
    setCheckedToday(true)
    setMonthTotal((v) => v + 1)
    setStreak((v) => v + 1)
    toast.success('출석 완료! 포인트가 적립되었습니다.')
  }

  return (
    <div className="mt-6 space-y-6">
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-xl border bg-card p-6">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <CalendarDays className="h-4 w-4" /> 이번 달 누적 출석
          </div>
          <p className="mt-2 text-3xl font-bold">
            {monthTotal}
            <span className="ml-1 text-base font-normal text-muted-foreground">일</span>
          </p>
        </div>
        <div className="rounded-xl border bg-card p-6">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Flame className="h-4 w-4 text-destructive" /> 며칠 연속 출석
          </div>
          <p className="mt-2 text-3xl font-bold">
            {streak}
            <span className="ml-1 text-base font-normal text-muted-foreground">일 연속</span>
          </p>
        </div>
      </div>

      <div className="rounded-xl border bg-card p-6">
        <h2 className="mb-4 text-sm font-semibold">6월 출석 현황</h2>
        <div className="grid grid-cols-7 gap-2">
          {Array.from({ length: 30 }, (_, i) => i + 1).map((day) => {
            const checked = checkedDays.includes(day) || (day === today && checkedToday)
            const isToday = day === today
            return (
              <div
                key={day}
                className={cn(
                  'flex aspect-square flex-col items-center justify-center rounded-lg border text-xs',
                  checked && 'border-primary bg-primary/10 text-primary',
                  isToday && !checkedToday && 'border-primary border-dashed',
                )}
              >
                {checked ? <Check className="h-4 w-4" /> : <span>{day}</span>}
              </div>
            )
          })}
        </div>
      </div>

      <Button size="lg" className="w-full" onClick={handleCheck} disabled={checkedToday}>
        {checkedToday ? '오늘 출석 완료' : '출석 체크하기'}
      </Button>
    </div>
  )
}
