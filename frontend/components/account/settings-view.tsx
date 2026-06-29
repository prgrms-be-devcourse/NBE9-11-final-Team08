'use client'

import { useState } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { Switch } from '@/components/ui/switch'
import type { UserProfile } from '@/lib/types'

const notifications = [
  { key: 'study', label: '스터디 진행 알림', desc: '진행 중인 스터디의 새 강의·공지 알림' },
  { key: 'qna', label: 'QnA 답변 알림', desc: '내 질문에 답변이 달리면 알려드려요' },
  { key: 'ai', label: 'AI 피드백 알림', desc: '회고에 대한 AI 피드백이 준비되면 알림' },
  { key: 'event', label: '이벤트·쿠폰 알림', desc: '할인, 출석 이벤트 등 마케팅 알림' },
]

export function SettingsView({ profile }: { profile: UserProfile }) {
  const [name, setName] = useState(profile.name)
  const [email, setEmail] = useState(profile.email)
  const [toggles, setToggles] = useState<Record<string, boolean>>({
    study: true,
    qna: true,
    ai: true,
    event: false,
  })

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">계정 및 알림설정</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          프로필 정보와 알림 수신 설정을 관리하세요.
        </p>
      </div>

      <section className="rounded-xl border bg-card p-5">
        <h2 className="font-semibold">기본 정보</h2>
        <Separator className="my-4" />
        <div className="grid gap-4 sm:max-w-md">
          <div className="grid gap-2">
            <Label htmlFor="name">이름</Label>
            <Input id="name" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="email">이메일</Label>
            <Input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <Button
            className="w-fit"
            onClick={() => toast.success('프로필이 저장되었습니다.')}
          >
            저장하기
          </Button>
        </div>
      </section>
    </div>
  )
}
