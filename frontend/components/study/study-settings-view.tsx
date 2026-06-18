'use client'

import { useState } from 'react'
import { Check, UserMinus, UserPlus, X } from 'lucide-react'
import { toast } from 'sonner'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { Study, StudyApplicant, StudyMember } from '@/lib/types'

export function StudySettingsView({ study }: { study: Study }) {
  const [name, setName] = useState(study.name)
  const [intro, setIntro] = useState(study.intro)
  const [members, setMembers] = useState<StudyMember[]>(study.members)
  const [applicants, setApplicants] = useState<StudyApplicant[]>(
    study.applicants,
  )

  const saveInfo = () => {
    if (!name.trim()) {
      toast.error('스터디 이름을 입력해주세요.')
      return
    }
    toast.success('스터디룸 정보가 저장되었습니다.')
  }

  const kick = (id: string, memberName: string) => {
    setMembers((prev) => prev.filter((m) => m.id !== id))
    toast.success(`${memberName} 님을 강퇴했습니다.`)
  }

  const accept = (applicant: StudyApplicant) => {
    setApplicants((prev) => prev.filter((a) => a.id !== applicant.id))
    setMembers((prev) => [
      ...prev,
      {
        id: applicant.id,
        name: applicant.name,
        progress: 0,
        role: 'member',
        joinedAt: new Date().toISOString().slice(0, 10).replace(/-/g, '.'),
      },
    ])
    toast.success(`${applicant.name} 님의 참여를 수락했습니다.`)
  }

  const reject = (applicant: StudyApplicant) => {
    setApplicants((prev) => prev.filter((a) => a.id !== applicant.id))
    toast.success(`${applicant.name} 님의 신청을 거절했습니다.`)
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold">스터디 설정</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          스터디룸 정보와 구성원을 관리하세요.
        </p>
      </div>

      {/* 스터디룸 정보 */}
      <section className="rounded-xl border bg-card">
        <div className="border-b px-5 py-3">
          <h3 className="text-sm font-semibold">스터디룸 정보</h3>
        </div>
        <div className="space-y-4 p-5">
          <div className="space-y-2">
            <Label htmlFor="study-name">이름</Label>
            <Input
              id="study-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="study-intro">소개</Label>
            <Textarea
              id="study-intro"
              value={intro}
              onChange={(e) => setIntro(e.target.value)}
              className="min-h-24 resize-none"
            />
          </div>
          <div className="flex justify-end">
            <Button onClick={saveInfo}>저장</Button>
          </div>
        </div>
      </section>

      {/* 구성원 목록 */}
      <section className="rounded-xl border bg-card">
        <div className="flex items-center gap-2 border-b px-5 py-3">
          <h3 className="text-sm font-semibold">구성원 목록</h3>
          <Badge variant="secondary" className="ml-auto">
            {members.length}명
          </Badge>
        </div>
        <ul className="divide-y">
          {members.map((m) => (
            <li key={m.id} className="flex items-center gap-3 px-5 py-3">
              <Avatar className="h-8 w-8">
                <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                  {m.name[0]}
                </AvatarFallback>
              </Avatar>
              <div className="min-w-0 flex-1">
                <p className="flex items-center gap-1.5 truncate text-sm font-medium">
                  {m.name}
                  {m.role === 'owner' && (
                    <Badge variant="outline" className="h-5 px-1.5 text-[10px]">
                      운영자
                    </Badge>
                  )}
                </p>
                <p className="text-xs text-muted-foreground">참여 {m.joinedAt}</p>
              </div>
              {m.role !== 'owner' && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => kick(m.id, m.name)}
                >
                  <UserMinus className="mr-1 h-4 w-4" />
                  강퇴
                </Button>
              )}
            </li>
          ))}
        </ul>
      </section>

      {/* 신청자 목록 */}
      <section className="rounded-xl border bg-card">
        <div className="flex items-center gap-2 border-b px-5 py-3">
          <h3 className="text-sm font-semibold">신청자 목록</h3>
          <Badge variant="secondary" className="ml-auto">
            {applicants.length}명
          </Badge>
        </div>
        {applicants.length === 0 ? (
          <div className="flex flex-col items-center gap-2 py-10 text-center">
            <UserPlus className="h-8 w-8 text-muted-foreground" />
            <p className="text-sm text-muted-foreground">
              대기 중인 신청자가 없습니다.
            </p>
          </div>
        ) : (
          <ul className="divide-y">
            {applicants.map((a) => (
              <li key={a.id} className="flex items-center gap-3 px-5 py-3">
                <Avatar className="h-8 w-8">
                  <AvatarFallback className="bg-secondary text-xs text-secondary-foreground">
                    {a.name[0]}
                  </AvatarFallback>
                </Avatar>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{a.name}</p>
                  <p className="text-xs text-muted-foreground">
                    신청 {a.appliedAt}
                  </p>
                </div>
                <Button size="sm" onClick={() => accept(a)}>
                  <Check className="mr-1 h-4 w-4" />
                  수락
                </Button>
                <Button variant="outline" size="sm" onClick={() => reject(a)}>
                  <X className="mr-1 h-4 w-4" />
                  거절
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
