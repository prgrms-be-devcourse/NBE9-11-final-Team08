'use client'

import Link from 'next/link'
import { useState } from 'react'
import { ArrowLeft, MessageCircle, Send, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Textarea } from '@/components/ui/textarea'
import type { Study, StudyActivityResponse } from '@/lib/types'

export function StudyPostView({
  study,
  post,
  currentUserId,
}: {
  study: Study
  post: StudyActivityResponse
  currentUserId: number | null
}) {
  const base = `/study/${study.id}`
  const isAuthor = currentUserId !== null && post.authorId === currentUserId

  const [comment, setComment] = useState('')

  // 제목: 첫 줄, 본문: 나머지
  const lines = post.content.split('\n')
  const title = lines[0] || '제목 없음'
  const body = lines.slice(2).join('\n') || lines.slice(1).join('\n') || post.content

  const submitComment = () => {
    toast.info('댓글 백엔드 기능 없음')
  }

  const deletePost = async () => {
    toast.success('학습 활동이 삭제되었습니다.')
    window.location.href = `${base}/board`
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
        {/* 헤더 */}
        <div className="border-b px-6 py-5">
          <h1 className="text-xl font-bold text-balance">{title}</h1>
          <div className="mt-3 flex items-center gap-3">
            <Avatar className="h-8 w-8">
              <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                {String(post.authorId).charAt(0)}
              </AvatarFallback>
            </Avatar>
            <div className="text-sm">
              <p className="font-medium">작성자 {post.authorId}</p>
              <p className="text-xs text-muted-foreground">
                {new Date(post.createdAt).toLocaleString()}
              </p>
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

        {/* 본문 */}
        <div className="px-6 py-5">
          <p className="whitespace-pre-line text-sm leading-relaxed text-foreground/90">
            {body}
          </p>
        </div>
      </article>

      {/* 댓글 */}
      <section className="rounded-xl border bg-card">
        <div className="flex items-center gap-2 border-b px-6 py-3">
          <MessageCircle className="h-4 w-4" />
          <h2 className="text-sm font-semibold">댓글</h2>
          <Badge variant="secondary" className="ml-auto">
            0
          </Badge>
        </div>

        <ul className="divide-y">
          <li className="px-6 py-6 text-center text-sm text-muted-foreground">
            댓글 백엔드 기능 없음
          </li>
        </ul>

        <Separator />
        <div className="space-y-2 p-4">
          <Textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="댓글 백엔드 기능 없음"
            className="min-h-20 resize-none"
          />
          <div className="flex justify-end">
            <Button onClick={submitComment} size="sm" disabled>
              <Send className="mr-1 h-4 w-4" /> 댓글 등록
            </Button>
          </div>
        </div>
      </section>
    </div>
  )
}
