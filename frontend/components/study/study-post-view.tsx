'use client'

import Link from 'next/link'
import { ArrowLeft, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { formatDateTime } from '@/lib/utils'
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

  const deletePost = async () => {
    toast.success('학습 활동이 삭제되었습니다.')
    window.location.href = `${base}/board`
  }

  return (
    <div className="space-y-5">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link href={`${base}/board`}>
          <ArrowLeft className="mr-1 h-4 w-4" />
          학습 활동
        </Link>
      </Button>

      <article className="rounded-xl border bg-card">
        {/* 헤더 */}
        <div className="border-b px-6 py-5">
          <div className="flex items-center gap-3">
            <Avatar className="h-8 w-8">
              <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                {post.authorNickname?.charAt(0) || '?'}
              </AvatarFallback>
            </Avatar>
            <div className="text-sm">
              <p className="font-medium">{post.authorNickname}</p>
              <p className="text-xs text-muted-foreground">
                {formatDateTime(post.createdAt)}
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
            {post.content}
          </p>
        </div>
      </article>
    </div>
  )
}
