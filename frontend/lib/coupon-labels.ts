import type {
  AutoIssueType,
  CouponApplyTarget,
  CouponDiscountType,
  CouponPolicyType,
  CouponStatus,
  CouponUseType,
} from './types'

export const autoIssueTypeLabel: Record<AutoIssueType, string> = {
  SIGNUP: '회원가입',
  ATTENDANCE_STREAK: '연속 출석',
  MONTHLY_ATTENDANCE: '월간 출석',
}

export const couponTypeLabel: Record<CouponPolicyType, string> = {
  AUTO: '자동',
  NORMAL: '일반',
  FCFS: '선착순',
  ADMIN: '관리자 발급',
}

export const couponTargetLabel: Record<CouponApplyTarget, string> = {
  ALL: '전체',
  CATEGORY: '카테고리',
  COURSE: '코스',
}

export const couponUseTypeLabel: Record<CouponUseType, string> = {
  SINGLE: '1회용',
  MULTI: '다회용',
}

export const couponDiscountTypeLabel: Record<CouponDiscountType, string> = {
  AMOUNT: '정액 할인',
  RATE: '정률(%) 할인',
}

export const couponStatusMeta: Record<
  CouponStatus,
  { label: string; variant: 'default' | 'secondary' | 'outline' | 'destructive' }
> = {
  SCHEDULED: { label: '발급 예정', variant: 'secondary' },
  ACTIVE: { label: '진행 중', variant: 'default' },
  ENDED: { label: '종료됨', variant: 'outline' },
}
