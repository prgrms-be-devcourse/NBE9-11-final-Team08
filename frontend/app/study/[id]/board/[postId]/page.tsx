import { notFound } from 'next/navigation'
import { StudyShell } from '@/components/study/study-shell'
import { StudyPostView } from '@/components/study/study-post-view'
import { api } from '@/lib/api'

export default async function StudyPostPage({
  params,
}: {
  params: Promise<{ id: string; postId: string }>
}) {
  const { id, postId } = await params
  const study = await api.getStudy(id)
  if (!study) notFound()

  const [post, profile] = await Promise.all([
    api.getBoardPost(study.id, postId),
    api.getProfile(),
  ])
  if (!post) notFound()

  return (
    <StudyShell study={study}>
      <StudyPostView study={study} post={post} currentUser={profile?.name ?? ''} />
    </StudyShell>
  )
}
