import { redirect } from 'next/navigation'
import { api } from '@/lib/api'
import { AdminHeader } from '@/components/admin/admin-header'

export default async function AdminLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const userProfile = await api.getProfile()
  if (!userProfile || !userProfile.isAdmin) {
    redirect('/login')
  }

  return (
    <div className="flex min-h-screen flex-col bg-secondary/30">
      <AdminHeader />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-8">
        {children}
      </main>
    </div>
  )
}

