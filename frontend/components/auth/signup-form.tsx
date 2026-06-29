// frontend/components/auth/signup-form.tsx
'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { Check, Eye, EyeOff, GraduationCap, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import { api } from '@/lib/api'

type SignupFormProps = {
  mode?: 'user' | 'seller'
}

export function SignupForm({ mode = 'user' }: SignupFormProps) {
  const router = useRouter()
  const [showPassword, setShowPassword] = useState(false)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [agree, setAgree] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const isSeller = mode === 'seller'
  const passwordMismatch = confirm.length > 0 && password !== confirm
  const title = isSeller ? '판매자 회원가입' : '회원가입'
  const description = isSeller
    ? '강좌를 등록하고 학습자를 만날 판매자 계정을 만들어보세요.'
    : '가입하면 신규 회원 10% 쿠폰을 드려요.'
  const nameLabel = isSeller ? '판매자명 (닉네임)' : '이름 (닉네임)'
  const namePlaceholder = isSeller
    ? 'PlayLearn의 새로운 판매자 (예: 홍길동)'
    : 'PlayLearn의 새로운 멤버 (예: 홍길동)'

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (passwordMismatch) {
      toast.error('비밀번호가 일치하지 않습니다')
      return
    }
    if (!agree) {
      toast.error('이용약관에 동의해주세요')
      return
    }
    setSubmitting(true)

    try {
      const signup = isSeller ? api.sellerSignup : api.signup
      await signup({
        email,
        password,
        nickname: name,
      })
      toast.success(isSeller ? '판매자 회원가입이 완료되었습니다!' : '회원가입이 완료되었습니다!')
      router.push('/login')
    } catch (err: any) {
      toast.error(err.message || '회원가입 중 오류가 발생했습니다')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto flex max-w-md flex-col px-4 py-16">
      <div className="mb-8 flex flex-col items-center text-center">
        <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary text-primary-foreground">
          <GraduationCap className="h-7 w-7" />
        </span>
        <h1 className="mt-4 text-2xl font-bold tracking-tight">{title}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{description}</p>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <div className="flex flex-col gap-2">
          <Label htmlFor="name">{nameLabel}</Label>
          <Input
            id="name"
            required
            placeholder={namePlaceholder}
            autoComplete="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="email">이메일</Label>
          <Input
            id="email"
            type="email"
            required
            placeholder="discovery@playlearn.dev"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="password">비밀번호</Label>
          <div className="relative">
            <Input
              id="password"
              type={showPassword ? 'text' : 'password'}
              required
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="안전한 비밀번호 (8자 이상)"
              autoComplete="new-password"
              className="pr-10"
            />
            <button
              type="button"
              onClick={() => setShowPassword((v) => !v)}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 표시'}
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="confirm">비밀번호 확인</Label>
          <Input
            id="confirm"
            type="password"
            required
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            placeholder="한 번 더 입력해 주세요"
            autoComplete="new-password"
            aria-invalid={passwordMismatch}
          />
          {passwordMismatch && (
            <p className="text-xs text-destructive">비밀번호가 일치하지 않습니다.</p>
          )}
          {confirm.length > 0 && !passwordMismatch && (
            <p className="flex items-center gap-1 text-xs text-primary">
              <Check className="h-3 w-3" /> 비밀번호가 일치합니다.
            </p>
          )}
        </div>

        <div className="flex items-start gap-2 rounded-lg border bg-card p-3">
          <Checkbox
            id="agree"
            checked={agree}
            onCheckedChange={(v) => setAgree(v === true)}
            className="mt-0.5"
          />
          <Label htmlFor="agree" className="text-sm font-normal leading-relaxed text-muted-foreground">
            <span className="text-foreground">이용약관</span> 및{' '}
            <span className="text-foreground">개인정보 처리방침</span>에 동의합니다. (필수)
          </Label>
        </div>

        <Button type="submit" size="lg" disabled={submitting}>
          {submitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
          {submitting ? '가입 중...' : isSeller ? '판매자 회원가입' : '회원가입'}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        이미 계정이 있으신가요?{' '}
        <Link href="/login" className="font-medium text-primary hover:underline">
          로그인
        </Link>
      </p>
      {!isSeller && (
        <p className="mt-3 text-center text-sm text-muted-foreground">
          강좌를 판매하고 싶으신가요?{' '}
          <Link href="/seller/signup" className="font-medium text-primary hover:underline">
            판매자 회원가입
          </Link>
        </p>
      )}
    </div>
  )
}
