'use client'

import { useState } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Separator } from '@/components/ui/separator'

export function SettingsView() {
  const [name, setName] = useState('최지바')
  const [title, setTitle] = useState('JPA 전문 강사 · 10년차 백엔드 개발자')
  const [bio, setBio] = useState('실무 중심의 백엔드 강의를 만듭니다.')
  const [bank, setBank] = useState('국민은행')
  const [account, setAccount] = useState('123456-01-789012')

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold">판매자 설정</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          프로필과 정산 정보를 관리하세요.
        </p>
      </div>

      <section className="space-y-4 rounded-xl border bg-card p-5">
        <h2 className="font-semibold">강사 프로필</h2>
        <div className="grid gap-2">
          <Label htmlFor="name">이름</Label>
          <Input id="name" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="grid gap-2">
          <Label htmlFor="title">직함</Label>
          <Input id="title" value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div className="grid gap-2">
          <Label htmlFor="bio">소개</Label>
          <Textarea
            id="bio"
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            className="min-h-24"
          />
        </div>
      </section>

      <section className="space-y-4 rounded-xl border bg-card p-5">
        <h2 className="font-semibold">정산 계좌</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="grid gap-2">
            <Label htmlFor="bank">은행</Label>
            <Input id="bank" value={bank} onChange={(e) => setBank(e.target.value)} />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="account">계좌번호</Label>
            <Input
              id="account"
              value={account}
              onChange={(e) => setAccount(e.target.value)}
            />
          </div>
        </div>
      </section>

      <Separator />

      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => toast.info('변경 사항을 취소했습니다.')}>
          취소
        </Button>
        <Button onClick={() => toast.success('설정이 저장되었습니다.')}>저장</Button>
      </div>
    </div>
  )
}
