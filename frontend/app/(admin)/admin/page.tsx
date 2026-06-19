import Link from 'next/link'
import { ArrowRight, CheckCircle2, Clock, TicketPercent } from 'lucide-react'
import { api } from '@/lib/api'
import { Button } from '@/components/ui/button'

export default async function AdminDashboardPage() {
  const userProfile = await api.getProfile()

  if (!userProfile || !userProfile.isSeller) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] text-center">
        <h1 className="text-3xl font-bold">접근 권한 없음</h1>
        <p className="mt-2 text-muted-foreground">
          이 페이지에 접근하려면 관리자 권한이 필요합니다.
        </p>
        <Button asChild className="mt-6">
          <Link href="/">메인 페이지로 돌아가기</Link>
        </Button>
      </div>
    )
  }

  const coupons = await api.getAdminCoupons()
  const active = coupons.filter((c) => c.status === 'ACTIVE').length
  const scheduled = coupons.filter((c) => c.status === 'SCHEDULED').length
  const totalIssued = coupons.reduce((s, c) => s + c.issuedCount, 0)

  const stats = [
    { icon: TicketPercent, label: '전체 쿠폰 정책', value: `${coupons.length}개` },
    { icon: CheckCircle2, label: '진행 중', value: `${active}개` },
    { icon: Clock, label: '발급 예정', value: `${scheduled}개` },
    {
      icon: TicketPercent,
      label: '누적 발급 수',
      value: totalIssued.toLocaleString('ko-KR'),
    },
  ]

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">관리자 대시보드</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          PlayLearn 운영 현황을 한눈에 확인하세요.
        </p>
      </div>

      <section className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {stats.map((s) => (
          <div key={s.label} className="rounded-xl border bg-card p-4">
            <s.icon className="h-5 w-5 text-muted-foreground" />
            <p className="mt-3 text-xl font-bold">{s.value}</p>
            <p className="text-xs text-muted-foreground">{s.label}</p>
          </div>
        ))}
      </section>

      <section className="rounded-xl border bg-card p-6">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-secondary text-secondary-foreground">
            <TicketPercent className="h-5 w-5" />
          </span>
          <div>
            <h2 className="font-semibold">쿠폰 정책 관리</h2>
            <p className="text-sm text-muted-foreground">
              쿠폰을 생성하고 발급 상태를 관리하세요.
            </p>
          </div>
        </div>
        <Button asChild className="mt-4">
          <Link href="/admin/coupons">
            쿠폰 관리로 이동 <ArrowRight className="ml-1 h-4 w-4" />
          </Link>
        </Button>
      </section>
    </div>
  )
}
