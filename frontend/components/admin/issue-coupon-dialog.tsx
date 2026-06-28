'use client'

import { useState } from 'react'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import type { AdminCoupon } from '@/lib/types'
import { api } from '@/lib/api'

interface IssueCouponDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  coupon: AdminCoupon | null
}

export function IssueCouponDialog({
  open,
  onOpenChange,
  coupon,
}: IssueCouponDialogProps) {
  const [issueType, setIssueType] = useState<'ALL' | 'TARGET'>('TARGET')
  const [userIdsText, setUserIdsText] = useState('')
  const [processing, setProcessing] = useState(false)

  if (!coupon) return null

  const handleIssue = async () => {
    setProcessing(true)
    const requestKey = `req-${Date.now()}` // simple unique key for idempotency

    try {
      if (issueType === 'ALL') {
        await api.issueCouponToAll(coupon.id, requestKey)
        toast.success('전체 회원 발급 요청이 전송되었습니다.')
      } else {
        const userIds = userIdsText
          .split(',')
          .map((id) => id.trim())
          .filter((id) => id.length > 0)
          .map((id) => Number(id))

        if (userIds.length === 0 || userIds.some(isNaN)) {
          toast.error('유효한 회원 ID를 입력해주세요. (쉼표로 구분)')
          setProcessing(false)
          return
        }

        await api.issueCouponToUsers(coupon.id, userIds, requestKey)
        toast.success(`특정 회원(${userIds.length}명) 발급 요청이 전송되었습니다.`)
      }
      onOpenChange(false)
      setUserIdsText('')
    } catch (error: any) {
      toast.error(`발급 실패: ${error.message || '알 수 없는 오류'}`)
    } finally {
      setProcessing(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>쿠폰 발급하기</DialogTitle>
          <DialogDescription>
            "{coupon.name}" 쿠폰을 누구에게 발급할지 선택하세요.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          <RadioGroup
            value={issueType}
            onValueChange={(val) => setIssueType(val as 'ALL' | 'TARGET')}
            className="flex flex-col space-y-2"
          >
            <div className="flex items-center space-x-2">
              <RadioGroupItem value="TARGET" id="r-target" />
              <Label htmlFor="r-target">특정 회원에게 발급</Label>
            </div>
            <div className="flex items-center space-x-2">
              <RadioGroupItem value="ALL" id="r-all" />
              <Label htmlFor="r-all">전체 회원에게 발급</Label>
            </div>
          </RadioGroup>

          {issueType === 'TARGET' && (
            <div className="space-y-2">
              <Label htmlFor="userIds">회원 ID (쉼표로 구분)</Label>
              <Textarea
                id="userIds"
                placeholder="예: 1, 2, 3"
                value={userIdsText}
                onChange={(e) => setUserIdsText(e.target.value)}
                rows={3}
              />
              <p className="text-xs text-muted-foreground">
                발급받을 회원의 숫자 ID를 쉼표(,)로 구분하여 입력하세요.
              </p>
            </div>
          )}
          {issueType === 'ALL' && (
            <div className="rounded-md bg-secondary p-3 text-sm">
              <p className="font-medium text-destructive">주의:</p>
              전체 회원 발급은 시스템 리소스를 많이 소모할 수 있으며, 발급 취소가 어려울 수 있습니다.
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={processing}>
            취소
          </Button>
          <Button onClick={handleIssue} disabled={processing}>
            {processing && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            발급 요청
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
