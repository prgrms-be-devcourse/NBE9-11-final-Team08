// frontend/app/(account)/mypage/page.tsx
import Link from 'next/link'
import { ArrowRight } from 'lucide-react'
import { MyStudyActivityList } from '@/components/account/my-study-activity-list'
import { PurchasedCourseList } from '@/components/account/purchased-course-list'
import { Button } from '@/components/ui/button'
import { api } from '@/lib/api'

export const metadata = {
  title: '내 강좌 & 스터디 — PlayLearn',
}

export default async function MyPage() {
  const [purchased, activities] = await Promise.all([
    api.getPurchasedCourses(),
    api.getMyStudyActivities(0, 10),
  ])

  const inProgress = purchased.filter((c) => c.status === '진행 중')
  const recentActivities = activities.content ?? []

  const stats = [
    { label: '구매한 강좌', value: `${purchased.length}강좌` },
    { label: '진행 중인 강좌', value: `${inProgress.length}강좌` },
    { label: '작성한 활동', value: `${activities.totalElements ?? recentActivities.length}개` },
  ]

  return (
    <div className="space-y-10">
      <div>
        <h1 className="text-2xl font-bold">내 강좌 & 스터디</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          구매한 강좌와 내가 작성한 스터디 활동을 한눈에 확인하세요.
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
          <h2 className="text-lg font-bold">구매한 강좌</h2>
        </div>
        {purchased.length > 0 ? (
          <PurchasedCourseList courses={purchased} />
        ) : (
          <EmptyState />
        )}
      </section>

      <section>
        <h2 className="mb-4 text-lg font-bold">내 스터디 활동</h2>
        <MyStudyActivityList initialPage={activities} />
      </section>
    </div>
  )
}

function EmptyState() {
  return (
    <div className="rounded-xl border border-dashed py-16 text-center">
      <p className="text-sm text-muted-foreground">구매한 강좌가 없습니다.</p>
      <Button asChild className="mt-4">
        <Link href="/">강좌 둘러보기</Link>
      </Button>
    </div>
  )
}
