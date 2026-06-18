import { AttendanceView } from '@/components/events/attendance-view'

export default function AttendancePage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="text-2xl font-bold">출석 체크</h1>
      <p className="mt-1 text-sm text-muted-foreground">
        매일 출석하고 포인트를 모아 쿠폰으로 교환하세요.
      </p>
      <AttendanceView />
    </div>
  )
}
