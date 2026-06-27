'use client'

import { useState } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { api } from '@/lib/api'
import type { Study } from '@/lib/types'

export function StudyPostComposer({ study }: { study: Study }) {
  const base = `/study/${study.id}`

  const [content, setContent] = useState('')

  const submit = async () => {
    const trimmed = content.trim()
    if (!trimmed) {
      toast.error('내용을 입력해주세요.')
      return
    }

    if (trimmed.length < 20 || trimmed.length > 2000) {
      toast.error('스터디 활동 내용은 20자 이상 2000자 이하로 입력해야 합니다.')
      return
    }

    try {
      const res = await api.createStudyActivity(Number(study.id), trimmed)
      if (res && res.activityId) {
        toast.success('학습 활동이 등록되었습니다.')
        window.location.href = `${base}/board`
      } else {
        throw new Error('Failed to save study activity')
      }
    } catch (err: any) {
      console.error('학습 활동 등록 에러:', err)
      toast.error(`학습 활동 등록에 실패했습니다: ${err.message || err}`)
    }
  }

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-xl font-bold">학습 활동 작성</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          오늘 학습한 내용을 회고로 기록해보세요.
        </p>
      </div>

      <div className="space-y-4 rounded-xl border bg-card p-6">
        <div className="space-y-2">
          <Label htmlFor="post-content">내용</Label>
          <Textarea
            id="post-content"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="학습한 내용, 이해한 점, 헷갈렸던 부분을 자유롭게 정리하세요."
            className="min-h-60 resize-none"
          />
        </div>
      </div>

      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => window.location.href = `${base}/board`}>
          취소
        </Button>
        <Button onClick={submit}>등록</Button>
      </div>
    </div>
  )
}
