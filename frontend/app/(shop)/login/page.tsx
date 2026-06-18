import type { Metadata } from 'next'
import { LoginForm } from '@/components/auth/login-form'

export const metadata: Metadata = {
  title: '로그인 — PlayLearn',
  description: 'PlayLearn 계정으로 로그인하고 강좌와 스터디를 이어가세요.',
}

export default function LoginPage() {
  return <LoginForm />
}
