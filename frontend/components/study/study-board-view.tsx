import Link from 'next/link'
import { MessageCircle, PenLine } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { formatDateTime } from '@/lib/utils'
import type { Study, StudyActivityResponse } from '@/lib/types'

export function StudyBoardView({
  study,
  posts,
}: {
  study: Study
  posts: StudyActivityResponse[]
}) {
  const base = `/study/${study.id}`

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold">학습 활동</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            학습한 내용을 남기고 멤버들과 공유하세요. 글 {posts.length}건
          </p>
        </div>
        <Button asChild size="sm">
          <Link href={`${base}/board/new`}>
            <PenLine className="mr-1 h-4 w-4" />
            글쓰기
          </Link>
        </Button>
      </div>

      {posts.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed py-16 text-center">
          <MessageCircle className="h-10 w-10 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">
            아직 작성된 학습 활동이 없어요.
          </p>
        </div>
      ) : (
        <ul className="space-y-3">
          {posts.map((post, index) => (
            <li key={post.activityId?.toString() || `post-${index}`}>
              <Link
                href={`${base}/board/${post.activityId?.toString() || '0'}`}
                className="flex gap-3 rounded-xl border bg-card px-5 py-4 transition-colors hover:border-foreground/20 hover:bg-secondary/40"
              >
                <Avatar className="mt-0.5 h-9 w-9 shrink-0">
                  <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                    {post.authorNickname?.charAt(0) || '?'}
                  </AvatarFallback>
                </Avatar>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <span className="font-medium text-foreground">
                      {post.authorNickname}
                    </span>
                    <span>·</span>
                    <span>{formatDateTime(post.createdAt)}</span>
                  </div>
                  <p className="mt-1.5 line-clamp-[8] whitespace-pre-line text-sm leading-relaxed text-foreground/90">
                    {post.content || '내용 없음'}
                  </p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
