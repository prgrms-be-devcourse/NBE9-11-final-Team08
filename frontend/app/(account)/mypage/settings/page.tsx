// frontend/app/(account)/mypage/settings/page.tsx
import { api } from '@/lib/api'
import { SettingsView } from '@/components/account/settings-view'
import { notFound } from 'next/navigation'

export default async function SettingsPage() {
  const profile = await api.getProfile()
  if (!profile) return null // 프로필을 불러올 수 없는 경우 처리

  return <SettingsView profile={profile} />
}
