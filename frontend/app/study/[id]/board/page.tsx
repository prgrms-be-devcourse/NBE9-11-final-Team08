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
  const study = await api.getStudyForEntry(id)
  if (!study) notFound()
  if (study.myRole === 'viewer') notFound()

  const posts = await api.getBoardPosts(study.id)

  return (
    <StudyShell study={study}>
      <StudyBoardView study={study} posts={posts} />
    </StudyShell>
  )
}
