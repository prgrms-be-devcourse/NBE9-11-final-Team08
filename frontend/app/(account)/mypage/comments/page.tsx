import Link from 'next/link'
import { MessageSquare, CheckCircle2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { api } from '@/lib/api'

export const metadata = {
  title: '작성한 댓글 — PlayLearn',
}

function formatDate(value: string) {
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(d)
}

export default async function CommentsPage() {
  const comments = await api.getMyComments()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">작성한 댓글</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          강의에서 작성한 질문과 댓글 {comments.length}개
        </p>
      </div>

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
                {formatDate(c.createdAt)}
              </p>
            </li>
          ))}
        </ul>
      ) : (
        <EmptyState />
      )}
    </div>
  )
}

function EmptyState() {
  return (
    <div className="rounded-xl border border-dashed py-16 text-center">
      <MessageSquare className="mx-auto h-8 w-8 text-muted-foreground/50" />
      <p className="mt-3 text-sm text-muted-foreground">
        아직 작성한 댓글이 없습니다.
      </p>
      <p className="mt-1 text-xs text-muted-foreground">
        강의를 수강하며 궁금한 점을 질문해 보세요.
      </p>
      <Button asChild className="mt-4">
        <Link href="/mypage">내 스터디로 가기</Link>
      </Button>
    </div>
  )
}
