'use client'

import { useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import { StudyShell } from '@/components/study/study-shell'
import { StudyPostView } from '@/components/study/study-post-view'
import { api } from '@/lib/api'
import type { Study, StudyActivityResponse } from '@/lib/types'

export default function StudyPostPage() {
  const { id, postId } = useParams<{ id: string; postId: string }>()

  const [study, setStudy] = useState<Study | undefined>(undefined)
  const [post, setPost] = useState<StudyActivityResponse | undefined>(undefined)
  const [currentUserId, setCurrentUserId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)

  useEffect(() => {
    const load = async () => {
      try {
        const [studyData, profile] = await Promise.all([
          api.getStudy(id),
          api.getProfile(),
        ])
        if (!studyData) { setNotFound(true); return }

        const postData = await api.getBoardPost(studyData.id, postId)
        if (!postData) { setNotFound(true); return }

        setStudy(studyData)
        setPost(postData)
        setCurrentUserId(profile?.id ? Number(profile.id) : null)
      } catch {
        setNotFound(true)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id, postId])

  if (loading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center text-muted-foreground text-sm">
        불러오는 중...
      </div>
    )
  }

  if (notFound || !study || !post) {
    return (
      <div className="flex min-h-[40vh] flex-col items-center justify-center gap-2">
        <p className="text-lg font-semibold">글을 찾을 수 없습니다.</p>
        <a href={`/study/${id}/board`} className="text-sm text-primary underline">
          게시판으로 돌아가기
        </a>
      </div>
    )
  }

  return (
    <StudyShell study={study}>
      <StudyPostView study={study} post={post} currentUserId={currentUserId} />
    </StudyShell>
  )
}
