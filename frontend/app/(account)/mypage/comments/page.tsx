import { MessageSquare } from 'lucide-react'
import { api } from '@/lib/api'

export const metadata = {
  title: '작성한 댓글 — PlayLearn',
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

      <ul className="space-y-3">
        {comments.map((c) => (
          <li key={c.id} className="rounded-xl border bg-card p-4">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <span className="font-medium text-foreground">{c.courseTitle}</span>
              <span>·</span>
              <span>{c.lectureTitle}</span>
            </div>
            <p className="mt-2 flex items-start gap-2 text-sm leading-relaxed">
              <MessageSquare className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />
              {c.content}
            </p>
            <p className="mt-2 text-xs text-muted-foreground">{c.createdAt}</p>
          </li>
        ))}
      </ul>
    </div>
  )
}
