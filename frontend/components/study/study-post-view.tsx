'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import {
  ArrowLeft,
  MessageCircle,
  Send,
  Sparkles,
  Trash2,
} from 'lucide-react'
import { toast } from 'sonner'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Textarea } from '@/components/ui/textarea'
import type { BoardPost, Study, StudyComment } from '@/lib/types'

const AI_DAILY_LIMIT = 3

export function StudyPostView({
  study,
  post,
  currentUser,
}: {
  study: Study
  post: BoardPost
  currentUser: string
}) {
  const router = useRouter()
  const base = `/study/${study.id}`
  const isAuthor = post.author === currentUser

  const [comments, setComments] = useState<StudyComment[]>(post.comments)
  const [comment, setComment] = useState('')
  const [feedback, setFeedback] = useState(post.aiFeedback)
  const [loading, setLoading] = useState(false)
  // AI 코치 일일 이용 횟수 (1일 3회 제한)
  const [aiUsed, setAiUsed] = useState(0)

  const remaining = AI_DAILY_LIMIT - aiUsed

  const submitComment = () => {
    if (!comment.trim()) return
    setComments((prev) => [
      ...prev,
      {
        id: `c-${Date.now()}`,
        author: currentUser,
        content: comment.trim(),
        createdAt: new Date()
          .toLocaleString('ko-KR', { hour12: false })
          .replace(/\. /g, '.'),
      },
    ])
    setComment('')
    toast.success('댓글이 등록되었습니다.')
  }

  const requestFeedback = () => {
    if (remaining <= 0) {
      toast.error('오늘 AI 코치 이용 횟수를 모두 사용했어요. (1일 3회)')
      return
    }
    setLoading(true)
    setTimeout(() => {
      setFeedback(
        '학습 기록을 구체적으로 정리하셨습니다. 개념을 본인의 언어로 다시 설명하고, 헷갈렸던 부분과 보완할 점을 한 줄로 덧붙이면 회고의 완성도가 높아집니다. 정답 여부보다 이해의 깊이에 집중해보세요.',
      )
      setAiUsed((n) => n + 1)
      setLoading(false)
      toast.success('AI 코치 피드백이 도착했습니다.')
    }, 1200)
  }

  const deletePost = () => {
    toast.success('학습 활동이 삭제되었습니다.')
    router.push(`${base}/board`)
  }

  return (
    <div className="space-y-5">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link href={`${base}/board`}>
          <ArrowLeft className="mr-1 h-4 w-4" />
          게시판
        </Link>
      </Button>

      <article className="rounded-xl border bg-card">
        <div className="border-b px-6 py-5">
          <h1 className="text-xl font-bold text-balance">{post.title}</h1>
          <div className="mt-3 flex items-center gap-3">
            <Avatar className="h-8 w-8">
              <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                {post.author[0]}
              </AvatarFallback>
            </Avatar>
            <div className="text-sm">
              <p className="font-medium">{post.author}</p>
              <p className="text-xs text-muted-foreground">{post.createdAt}</p>
            </div>
            {isAuthor && (
              <div className="ml-auto flex items-center gap-2">
                <Button variant="ghost" size="sm" onClick={deletePost}>
                  <Trash2 className="mr-1 h-4 w-4" />
                  삭제
                </Button>
              </div>
            )}
          </div>
        </div>

        <div className="px-6 py-5">
          <p className="whitespace-pre-line text-sm leading-relaxed text-foreground/90">
            {post.content}
          </p>
        </div>

        {/* AI 코치 */}
        <div className="border-t px-6 py-5">
          {feedback ? (
            <div className="rounded-lg bg-secondary/60 p-4">
              <p className="flex items-center gap-1.5 text-xs font-semibold text-primary">
                <Sparkles className="h-3.5 w-3.5" /> AI 코치 피드백
              </p>
              <p className="mt-2 text-sm leading-relaxed">{feedback}</p>
            </div>
          ) : isAuthor ? (
            <div className="flex flex-col gap-2 rounded-lg border border-dashed p-4">
              <p className="text-sm font-medium">AI 코치에게 피드백 받기</p>
              <p className="text-xs text-muted-foreground">
                학습 기록의 구체성과 보완점을 안내받을 수 있어요. 오늘 남은 횟수{' '}
                {remaining}/{AI_DAILY_LIMIT}회
              </p>
              <Button
                size="sm"
                className="mt-1 w-fit"
                onClick={requestFeedback}
                disabled={loading}
              >
                <Sparkles className="mr-1 h-4 w-4" />
                {loading ? 'AI 피드백 생성 중…' : 'AI 피드백 요청'}
              </Button>
            </div>
          ) : null}
        </div>
      </article>

      {/* 댓글 */}
      <section className="rounded-xl border bg-card">
        <div className="flex items-center gap-2 border-b px-6 py-3">
          <MessageCircle className="h-4 w-4" />
          <h2 className="text-sm font-semibold">댓글</h2>
          <Badge variant="secondary" className="ml-auto">
            {comments.length}
          </Badge>
        </div>

        <ul className="divide-y">
          {comments.map((c) => (
            <li key={c.id} className="flex gap-3 px-6 py-4">
              <Avatar className="h-7 w-7 shrink-0">
                <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                  {c.author[0]}
                </AvatarFallback>
              </Avatar>
              <div className="min-w-0">
                <p className="text-xs font-semibold">
                  {c.author}
                  <span className="ml-2 font-normal text-muted-foreground">
                    {c.createdAt}
                  </span>
                </p>
                <p className="mt-1 text-sm leading-relaxed">{c.content}</p>
              </div>
            </li>
          ))}
          {comments.length === 0 && (
            <li className="px-6 py-6 text-center text-sm text-muted-foreground">
              첫 댓글을 남겨보세요.
            </li>
          )}
        </ul>

        <Separator />
        <div className="space-y-2 p-4">
          <Textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="댓글을 입력하세요."
            className="min-h-20 resize-none"
          />
          <div className="flex justify-end">
            <Button onClick={submitComment} size="sm">
              <Send className="mr-1 h-4 w-4" /> 댓글 등록
            </Button>
          </div>
        </div>
      </section>
    </div>
  )
}
