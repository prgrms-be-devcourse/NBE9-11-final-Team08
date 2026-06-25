// frontend/components/events/attendance-view.tsx
'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { CalendarDays, Check, Flame, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { api } from '@/lib/api'

export function AttendanceView() {
  const router = useRouter()
  const [checkedToday, setCheckedToday] = useState(false)
  const [monthTotal, setMonthTotal] = useState(0)
  const [streak, setStreak] = useState(0)
  const [loading, setLoading] = useState(true)
  const [checking, setChecking] = useState(false)
  
  const today = new Date().getDate()
  const currentMonth = new Date().getMonth() + 1
  const daysInMonth = new Date(new Date().getFullYear(), currentMonth, 0).getDate()
  const [checkedDays, setCheckedDays] = useState<number[]>([])

  useEffect(() => {
    api.getAttendance()
      .then((res) => {
        if (!res) {
          router.replace('/login?redirect=/attendance')
          return
        }
        setMonthTotal(res.monthlyTotalDays)
        setStreak(res.consecutiveDays)
        setCheckedToday(res.checkedToday)
        setCheckedDays(res.checkedDays)
      })
      .finally(() => setLoading(false))
  }, [router])

  const handleCheck = async () => {
    setChecking(true)
    try {
      const res = await api.checkAttendance()
      if (res) {
        setMonthTotal(res.monthlyTotalDays || monthTotal + 1)
        setStreak(res.consecutiveDays || streak + 1)
      }
      
      setCheckedToday(true)
      setCheckedDays(prev => prev.includes(today) ? prev : [...prev, today])
      toast.success('출석 완료! 포인트가 적립되었습니다.')
    } catch (err: any) {
      if (String(err.message || '').includes('이미 출석')) {
        setCheckedToday(true)
        setCheckedDays(prev => prev.includes(today) ? prev : [...prev, today])
        toast.info('오늘은 이미 출석하셨습니다.')
      } else {
        toast.error('출석 체크에 실패했습니다.')
      }
    } finally {
      setChecking(false)
    }
  }

  if (loading) {
    return (
      <div className="mt-6 flex h-40 items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    )
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
        <h2 className="mb-4 text-sm font-semibold">{currentMonth}월 출석 현황</h2>
        <div className="grid grid-cols-7 gap-2">
          {Array.from({ length: daysInMonth }, (_, i) => i + 1).map((day) => {
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

      <Button size="lg" className="w-full" onClick={handleCheck} disabled={checkedToday || checking}>
        {checking ? <Loader2 className="h-4 w-4 animate-spin" /> : checkedToday ? '오늘 출석 완료' : '출석 체크하기'}
      </Button>
    </div>
  )
}
