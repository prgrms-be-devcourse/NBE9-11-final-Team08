import Link from 'next/link'
import { CheckCircle2, Ticket } from 'lucide-react'
import { Button } from '@/components/ui/button'

export default function SignupCompletePage() {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center px-4 py-20 text-center">
      <span className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
        <CheckCircle2 className="h-9 w-9" />
      </span>
      <h1 className="mt-6 text-2xl font-bold">회원가입이 완료되었습니다</h1>
      <div className="mt-4 flex items-center gap-2 rounded-lg bg-accent px-4 py-3 text-sm text-accent-foreground">
        <Ticket className="h-4 w-4" /> 신규 회원 10% 쿠폰 발급 완료
      </div>
      <p className="mt-4 text-sm leading-relaxed text-muted-foreground">
        PlayLearn에 오신 것을 환영합니다. 로그인 후 강좌와 스터디를 둘러보세요.
      </p>
      <div className="mt-8 flex w-full flex-col gap-2">
        <Button asChild size="lg">
          <Link href="/login">로그인 페이지 가기</Link>
        </Button>
        <Button asChild variant="outline">
          <Link href="/">강좌 둘러보기</Link>
        </Button>
      </div>
    </div>
  )
}
