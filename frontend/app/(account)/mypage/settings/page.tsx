import { SettingsView } from '@/components/account/settings-view'
import { api } from '@/lib/api'

export const metadata = {
  title: '계정 및 알림설정 — PlayLearn',
}

export default async function SettingsPage() {
  const profile = await api.getProfile()
  return <SettingsView profile={profile} />
}
