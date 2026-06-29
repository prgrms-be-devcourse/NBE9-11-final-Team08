import Link from 'next/link'
import { MessageSquare, CheckCircle2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { api } from '@/lib/api'
import { formatDateTime } from '@/lib/utils'
import type { MyAnswer } from '@/lib/types'

export const metadata = {
  title: 'QnA — PlayLearn',
}

export default async function CommentsPage() {
  const profile = await api.getProfile()
  const isSeller = profile?.isSeller ?? false

  // 강사/판매자는 자신이 "작성한 답변", 일반 수강생은 자신이 "작성한 질문"을 본다.
  if (isSeller) {
    const answers = await api.getMyAnswers()
    return (
      <div className="space-y-6">
        <Header noun="답변" count={answers.length} />
        {answers.length > 0 ? (
          <ul className="space-y-3">
            {answers.map((a) => (
              <AnswerItem key={a.answerId} answer={a} />
            ))}
          </ul>
        ) : (
          <EmptyState
            noun="답변"
            hint="수강생의 질문에 답변을 남겨보세요."
            cta={{ href: '/instructor', label: '판매자 센터로 가기' }}
          />
        )}
      </div>
    )
  }

  const comments = await api.getMyComments()
  return (
    <div className="space-y-6">
      <Header noun="질문" count={comments.length} />
      {comments.length > 0 ? (
        <ul className="space-y-3">
          {comments.map((c) => (
            <li key={c.id} className="rounded-xl border bg-card p-4">
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <span className="font-medium text-foreground">{c.courseTitle}</span>
                <span>·</span>
                <span>{c.lectureTitle}</span>
                {c.answered && (
                  <Badge
                    variant="secondary"
                    className="ml-auto gap-1 text-[10px] text-primary"
                  >
                    <CheckCircle2 className="h-3 w-3" />
                    답변 완료
                  </Badge>
                )}
              </div>

              {c.title && (
                <p className="mt-2 flex items-start gap-2 text-sm font-semibold leading-relaxed">
                  <MessageSquare className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
                  {c.title}
                </p>
              )}
              <p className="mt-1 whitespace-pre-wrap pl-6 text-sm leading-relaxed text-muted-foreground">
                {c.content}
              </p>
              <p className="mt-2 pl-6 text-xs text-muted-foreground">
                {formatDateTime(c.createdAt)}
              </p>
            </li>
          ))}
        </ul>
      ) : (
        <EmptyState
          noun="질문"
          hint="강의를 수강하며 궁금한 점을 질문해 보세요."
          cta={{ href: '/mypage', label: '내 스터디로 가기' }}
        />
      )}
    </div>
  )
}

function Header({ noun, count }: { noun: string; count: number }) {
  return (
    <div>
      <h1 className="text-2xl font-bold">작성한 {noun}</h1>
      <p className="mt-1 text-sm text-muted-foreground">
        강의에서 작성한 {noun} {count}개
      </p>
    </div>
  )
}

function AnswerItem({ answer }: { answer: MyAnswer }) {
  return (
    <li className="rounded-xl border bg-card p-4">
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <span className="font-medium text-foreground">{answer.courseTitle}</span>
        <span>·</span>
        <span>{answer.lectureTitle}</span>
      </div>

      {answer.questionTitle && (
        <p className="mt-2 flex items-start gap-2 text-sm font-semibold leading-relaxed">
          <MessageSquare className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
          {answer.questionTitle}
        </p>
      )}
      <p className="mt-1 whitespace-pre-wrap pl-6 text-sm leading-relaxed text-muted-foreground">
        {answer.questionContent}
      </p>

      <div className="mt-3 ml-6 rounded-md bg-secondary/60 p-3">
        <p className="text-xs font-semibold text-primary">내 답변</p>
        <p className="mt-1 whitespace-pre-wrap text-sm leading-relaxed">
          {answer.answerContent}
        </p>
        <p className="mt-2 text-xs text-muted-foreground">
          {formatDateTime(answer.createdAt)}
        </p>
      </div>
    </li>
  )
}

function EmptyState({
  noun,
  hint,
  cta,
}: {
  noun: string
  hint: string
  cta: { href: string; label: string }
}) {
  return (
    <div className="rounded-xl border border-dashed py-16 text-center">
      <MessageSquare className="mx-auto h-8 w-8 text-muted-foreground/50" />
      <p className="mt-3 text-sm text-muted-foreground">
        아직 작성한 {noun}이 없습니다.
      </p>
      <p className="mt-1 text-xs text-muted-foreground">{hint}</p>
      <Button asChild className="mt-4">
        <Link href={cta.href}>{cta.label}</Link>
      </Button>
    </div>
  )
}
