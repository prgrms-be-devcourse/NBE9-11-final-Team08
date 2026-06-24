import { SiteHeader } from '@/components/layout/site-header'
import { SiteFooter } from '@/components/layout/site-footer'
import { AccountSidebar } from '@/components/account/account-sidebar'
import { api } from '@/lib/api'

export default async function AccountLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const profile = await api.getProfile()

  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader />
      <main className="flex-1">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-8 lg:grid-cols-[260px_1fr]">
          <div className="lg:sticky lg:top-20 lg:self-start">
            <AccountSidebar profile={profile} />
          </div>
          <div className="min-w-0">{children}</div>
        </div>
      </main>
      <SiteFooter />
    </div>
  )
}
