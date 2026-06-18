import type { Metadata } from 'next'
import { SignupForm } from '@/components/auth/signup-form'

export const metadata: Metadata = {
  title: '회원가입 — PlayLearn',
  description: '지금 PlayLearn에 가입하고 신규 회원 10% 쿠폰을 받아보세요.',
}

export default function SignupPage() {
  return <SignupForm />
}
