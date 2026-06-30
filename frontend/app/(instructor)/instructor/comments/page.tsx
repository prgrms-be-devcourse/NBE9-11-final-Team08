import Link from 'next/link'
import { MessageSquare } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { MyAnswerItem } from '@/components/account/my-answer-item'
import { api } from '@/lib/api'

export const metadata = {
  title: '내 답변 — PlayLearn 판매자 센터',
}

export default async function InstructorCommentsPage() {
  const answers = await api.getMyAnswers()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">내 답변</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          수강생 질문에 작성한 답변 {answers.length}개
        </p>
      </div>

      {answers.length > 0 ? (
        <ul className="space-y-3">
          {answers.map((a) => (
            <MyAnswerItem key={a.answerId} answer={a} />
          ))}
        </ul>
      ) : (
        <div className="rounded-xl border border-dashed py-16 text-center">
          <MessageSquare className="mx-auto h-8 w-8 text-muted-foreground/50" />
          <p className="mt-3 text-sm text-muted-foreground">
            아직 작성한 답변이 없습니다.
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            수강생의 질문에 답변을 남겨보세요.
          </p>
          <Button asChild className="mt-4">
            <Link href="/instructor">상품 관리로 가기</Link>
          </Button>
        </div>
      )}
    </div>
  )
}
