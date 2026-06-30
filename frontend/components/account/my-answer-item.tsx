import { MessageSquare } from 'lucide-react'
import { formatDateTime } from '@/lib/utils'
import type { MyAnswer } from '@/lib/types'

// 판매자가 작성한 답변 한 건(질문 + 내 답변). 마이페이지·판매자 센터에서 공통으로 쓴다.
export function MyAnswerItem({ answer }: { answer: MyAnswer }) {
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
