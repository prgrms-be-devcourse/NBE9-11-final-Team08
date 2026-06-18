// frontend/app/(account)/mypage/page.tsx
import Link from 'next/link'
import { ArrowRight } from 'lucide-react'
import { EnrolledCard } from '@/components/account/enrolled-card'
import { Button } from '@/components/ui/button'
import { api } from '@/lib/api'

export const metadata = {
  title: '내 스터디 — PlayLearn',
}

export default async function MyPage() {
  const [enrolled, purchased] = await Promise.all([
    api.getMyStudies(),
    api.getPurchasedCourses(),
  ])

  const inProgress = enrolled.filter((c) => c.status === '진행 중')
  const completed = enrolled.filter((c) => c.status === '완료')

  const stats = [
    { label: '진행 중인 스터디', value: `${inProgress.length}곳` },
    { label: '완료한 스터디', value: `${completed.length}곳` },
    { label: '구매한 강좌', value: `${purchased.length}강좌` },
  ]

  return (
    <div className="space-y-10">
      <div>
        <h1 className="text-2xl font-bold">내 스터디</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          참여 중인 스터디와 구매한 강좌를 한눈에 확인하세요.
        </p>
      </div>

      <div className="grid grid-cols-3 gap-3">
        {stats.map((s) => (
          <div key={s.label} className="rounded-xl border bg-card p-4 text-center">
            <p className="text-2xl font-bold">{s.value}</p>
            <p className="mt-1 text-xs text-muted-foreground">{s.label}</p>
          </div>
        ))}
      </div>

      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-bold">진행 중인 스터디</h2>
          <Button asChild variant="ghost" size="sm" className="text-muted-foreground">
            <Link href="/dashboard">
              대시보드 <ArrowRight className="ml-1 h-4 w-4" />
            </Link>
          </Button>
        </div>
        {inProgress.length > 0 ? (
          <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {inProgress.map((c) => (
              <EnrolledCard key={c.id} course={c} />
            ))}
          </ul>
        ) : (
          <EmptyState />
        )}
      </section>

      {completed.length > 0 && (
        <section>
          <h2 className="mb-4 text-lg font-bold">완료한 스터디</h2>
          <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {completed.map((c) => (
              <EnrolledCard key={c.id} course={c} />
            ))}
          </ul>
        </section>
      )}

      <section>
        <h2 className="mb-4 text-lg font-bold">구매한 강좌</h2>
        <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {purchased.map((c) => (
            <EnrolledCard key={c.id} course={c} />
          ))}
        </ul>
      </section>
    </div>
  )
}

function EmptyState() {
  return (
    <div className="rounded-xl border border-dashed py-16 text-center">
      <p className="text-sm text-muted-foreground">참여 중인 스터디가 없습니다.</p>
      <Button asChild className="mt-4">
        <Link href="/">강좌 둘러보기</Link>
      </Button>
    </div>
  )
}
