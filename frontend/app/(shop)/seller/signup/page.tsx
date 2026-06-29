import type { Metadata } from 'next'
import { SignupForm } from '@/components/auth/signup-form'

export const metadata: Metadata = {
  title: '판매자 회원가입 — PlayLearn',
  description: 'PlayLearn 판매자 계정을 만들고 강좌를 등록해보세요.',
}

export default function SellerSignupPage() {
  return <SignupForm mode="seller" />
}
