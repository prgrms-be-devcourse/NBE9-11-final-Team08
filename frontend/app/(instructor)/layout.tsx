import { redirect } from 'next/navigation'
import { InstructorHeader } from '@/components/instructor/instructor-header'
import { api } from '@/lib/api'

export default async function InstructorLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const userProfile = await api.getProfile()
  if (!userProfile || !userProfile.isSeller) {
    redirect('/')
  }

  return (
    <div className="flex min-h-screen flex-col bg-secondary/30">
      <InstructorHeader />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-8">{children}</main>
    </div>
  )
}
