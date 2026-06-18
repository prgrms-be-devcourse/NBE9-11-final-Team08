import Link from 'next/link'
import { MessageCircle, PenLine, Sparkles } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { Study, StudyActivityResponse } from '@/lib/types'

export function StudyBoardView({
  study,
  posts,
}: {
  study: Study
  posts: StudyActivityResponse[]
}) {
  const base = `/study/${study.courseId}`

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold">학습 활동 피드</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            학습한 내용을 회고로 남기고 멤버들과 공유하세요. 글 {posts.length}건
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
        <ul className="overflow-hidden rounded-xl border bg-card">
          {posts.map((post, index) => (
            <li key={post.id?.toString() || `post-${index}`} className="border-b last:border-b-0">
              <Link
                href={`${base}/board/${post.id?.toString() || '0'}`}
                className="flex items-center gap-4 px-5 py-4 transition-colors hover:bg-secondary/60"
              >
                <div className="min-w-0 flex-1">
                  <p className="flex items-center gap-2 truncate font-medium">
                    {post.content.split('\n')[0] || '내용 없음'}
                    {post.aiFeedbackId && (
                      <Badge
                        variant="secondary"
                        className="h-5 shrink-0 gap-1 px-1.5 text-[10px]"
                      >
                        <Sparkles className="h-3 w-3" />
                        AI 피드백
                      </Badge>
                    )}
                  </p>
                  <p className="mt-1 line-clamp-1 text-xs text-muted-foreground">
                    {post.content}
                  </p>
                </div>
                <span className="flex shrink-0 items-center gap-1 text-xs text-muted-foreground">
                  <MessageCircle className="h-3.5 w-3.5" />
                  0
                </span>
                <div className="hidden w-28 shrink-0 items-center gap-2 sm:flex">
                  <Avatar className="h-6 w-6">
                    <AvatarFallback className="bg-secondary text-[10px] text-secondary-foreground">
                      {post.authorName ? post.authorName.charAt(0) : '?'}
                    </AvatarFallback>
                  </Avatar>
                  <span className="truncate text-xs">{post.authorName}</span>
                </div>
                <span className="hidden shrink-0 text-xs text-muted-foreground md:block">
                  {new Date(post.createdAt).toLocaleDateString()}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
