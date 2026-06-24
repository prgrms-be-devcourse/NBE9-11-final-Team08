'use client'

import { useState } from 'react'
import { Check, UserMinus, UserPlus, X } from 'lucide-react'
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
          <p className="rounded-md border border-dashed px-3 py-2 text-sm text-muted-foreground">
            백엔드 기능 없음
          </p>
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
            <Button disabled>저장</Button>
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
                  disabled
                >
                  <UserMinus className="mr-1 h-4 w-4" />
                  백엔드 기능 없음
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
                <Button size="sm" disabled>
                  <Check className="mr-1 h-4 w-4" />
                  백엔드 기능 없음
                </Button>
                <Button variant="outline" size="sm" disabled>
                  <X className="mr-1 h-4 w-4" />
                  백엔드 기능 없음
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
