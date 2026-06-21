import Link from 'next/link'
import {
  ArrowRight,
  Users,
  Store,
  BookOpen,
  GraduationCap,
  Activity,
  CheckCircle2,
  PlayCircle,
  UserCheck,
  TicketPercent,
} from 'lucide-react'
import { api } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { OverviewCharts } from '@/components/admin/overview-charts'

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

  const [overview, daily] = await Promise.all([
    api.getAdminOverview(),
    api.getAdminDailySessions(),
  ])

  const fmt = (n: number | undefined) => (n ?? 0).toLocaleString('ko-KR')

  const kpis = overview
    ? [
        { icon: Users, label: '전체 회원', value: fmt(overview.totalUsers) },
        { icon: Store, label: '판매자', value: fmt(overview.sellerCount) },
        { icon: BookOpen, label: '판매중 강좌', value: fmt(overview.onSaleCourseCount) },
        { icon: GraduationCap, label: '활성 수강', value: fmt(overview.activeEnrollmentCount) },
        { icon: Activity, label: '누적 학습 이벤트', value: fmt(overview.totalLearningEvents) },
        { icon: CheckCircle2, label: '누적 완강', value: fmt(overview.totalCompletions) },
        { icon: PlayCircle, label: '오늘 세션', value: fmt(overview.todaySessions) },
        { icon: UserCheck, label: '오늘 학습자', value: fmt(overview.todayActiveLearners) },
      ]
    : []

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">플랫폼 전체 현황</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          PlayLearn 운영 현황을 한눈에 확인하세요.
        </p>
      </div>

      {!overview ? (
        <div className="rounded-xl border bg-card p-8 text-center text-sm text-muted-foreground">
          현황 데이터를 불러오지 못했습니다. 관리자 권한으로 로그인했는지, 백엔드 서버가 실행 중인지
          확인해주세요.
        </div>
      ) : (
        <>
          <section className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            {kpis.map((s) => (
              <div key={s.label} className="rounded-xl border bg-card p-4">
                <s.icon className="h-5 w-5 text-muted-foreground" />
                <p className="mt-3 text-xl font-bold">{s.value}</p>
                <p className="text-xs text-muted-foreground">{s.label}</p>
              </div>
            ))}
          </section>

          <section className="rounded-xl border bg-card p-6">
            <h2 className="font-semibold">일별 세션 추이</h2>
            <p className="text-sm text-muted-foreground">
              최근 30일 · 세션(강의 입장) 수와 순 학습자 수
            </p>
            <OverviewCharts data={daily} />
          </section>
        </>
      )}

      <section className="grid gap-3 sm:grid-cols-2">
        <Link
          href="/admin/learning"
          className="flex items-center justify-between rounded-xl border bg-card p-5 transition-colors hover:bg-secondary"
        >
          <div>
            <h3 className="font-semibold">학습 드릴다운</h3>
            <p className="text-sm text-muted-foreground">강좌 → 강의 → 수강자 → 타임라인 탐색</p>
          </div>
          <ArrowRight className="h-5 w-5 text-muted-foreground" />
        </Link>
        <Link
          href="/admin/anomalies"
          className="flex items-center justify-between rounded-xl border bg-card p-5 transition-colors hover:bg-secondary"
        >
          <div>
            <h3 className="font-semibold">이상 데이터 탐지</h3>
            <p className="text-sm text-muted-foreground">이탈률 임계값·중복 이벤트 다발 탐지</p>
          </div>
          <ArrowRight className="h-5 w-5 text-muted-foreground" />
        </Link>
        <Link
          href="/admin/audit"
          className="flex items-center justify-between rounded-xl border bg-card p-5 transition-colors hover:bg-secondary"
        >
          <div>
            <h3 className="font-semibold">데이터 보존 감사</h3>
            <p className="text-sm text-muted-foreground">보존 로그·접근 이력·정합성 오류</p>
          </div>
          <ArrowRight className="h-5 w-5 text-muted-foreground" />
        </Link>
        <Link
          href="/admin/coupons"
          className="flex items-center justify-between rounded-xl border bg-card p-5 transition-colors hover:bg-secondary"
        >
          <div>
            <h3 className="flex items-center gap-2 font-semibold">
              <TicketPercent className="h-4 w-4" /> 쿠폰 정책 관리
            </h3>
            <p className="text-sm text-muted-foreground">쿠폰 생성 및 발급 상태 관리</p>
          </div>
          <ArrowRight className="h-5 w-5 text-muted-foreground" />
        </Link>
      </section>
    </div>
  )
}
