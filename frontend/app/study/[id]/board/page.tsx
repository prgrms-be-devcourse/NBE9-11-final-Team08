import { notFound } from 'next/navigation'
import { StudyShell } from '@/components/study/study-shell'
import { StudyBoardView } from '@/components/study/study-board-view'
import { api } from '@/lib/api'

export default async function StudyBoardPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  const [study, posts] = await Promise.all([
    api.getStudy(id),
    api.getBoardPosts(id),
  ])
  if (!study) notFound()
  return (
    <StudyShell study={study}>
      <StudyBoardView study={study} posts={posts} />
    </StudyShell>
  )
}
