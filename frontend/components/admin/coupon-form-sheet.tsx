'use client'

import { useEffect, useState } from 'react'
import { toast } from 'sonner'
import {
  Sheet,
  SheetBody,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command'
import { Badge } from '@/components/ui/badge'
import { Check, ChevronsUpDown, X } from 'lucide-react'
import { cn } from '@/lib/utils'
import { api } from '@/lib/api'
import {
  autoIssueTypeLabel,
  couponDiscountTypeLabel,
  couponTargetLabel,
  couponTypeLabel,
  couponUseTypeLabel,
} from '@/lib/coupon-labels'
import type {
  AdminCoupon,
  AutoIssueType,
  CouponApplyTarget,
  CouponDiscountType,
  CouponPolicyType,
  CouponUseType,
  Course,
} from '@/lib/types'

interface CouponFormSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  coupon?: AdminCoupon | null
  onSave: (coupon: AdminCoupon) => Promise<void>
}

type FormState = {
  name: string
  totalQuantity: string
  type: CouponPolicyType
  autoIssueType: AutoIssueType | 'NONE'
  target: CouponApplyTarget
  useType: CouponUseType
  stackable: boolean
  discountType: CouponDiscountType
  discountValue: string
  maxDiscount: string
  minOrderAmount: string
  validDays: string
  startAt: string
  endAt: string
  targets: string[]
}

const emptyForm: FormState = {
  name: '',
  totalQuantity: '',
  type: 'NORMAL',
  autoIssueType: 'NONE',
  target: 'ALL',
  useType: 'SINGLE',
  stackable: false,
  discountType: 'AMOUNT',
  discountValue: '',
  maxDiscount: '',
  minOrderAmount: '',
  validDays: '',
  startAt: '',
  endAt: '',
  targets: [],
}

function toForm(coupon: AdminCoupon): FormState {
  return {
    name: coupon.name,
    totalQuantity: coupon.totalQuantity?.toString() ?? '',
    type: coupon.type,
    autoIssueType: coupon.autoIssueType ?? 'NONE',
    target: coupon.target,
    useType: coupon.useType,
    stackable: coupon.stackable,
    discountType: coupon.discountType,
    discountValue: coupon.discountValue.toString(),
    maxDiscount: coupon.maxDiscount?.toString() ?? '',
    minOrderAmount: coupon.minOrderAmount?.toString() ?? '',
    validDays: coupon.validDays?.toString() ?? '',
    startAt: coupon.startAt,
    endAt: coupon.endAt,
    targets:
      coupon.target === 'CATEGORY'
        ? (coupon.targetCategories ?? [])
        : coupon.target === 'COURSE'
          ? (coupon.targetCourses ?? [])
          : [],
  }
}

function Section({
  title,
  children,
}: {
  title: string
  children: React.ReactNode
}) {
  return (
    <section className="space-y-4">
      <h3 className="text-sm font-semibold text-foreground">{title}</h3>
      {children}
    </section>
  )
}

function Field({
  label,
  htmlFor,
  children,
}: {
  label: string
  htmlFor?: string
  children: React.ReactNode
}) {
  return (
    <div className="space-y-1.5">
      <Label htmlFor={htmlFor} className="text-xs text-muted-foreground">
        {label}
      </Label>
      {children}
    </div>
  )
}

export function CouponFormSheet({
  open,
  onOpenChange,
  coupon,
  onSave,
}: CouponFormSheetProps) {
  const [form, setForm] = useState<FormState>(emptyForm)
  const [targetOpen, setTargetOpen] = useState(false)
  const [courses, setCourses] = useState<Course[]>([])
  const [saving, setSaving] = useState(false)
  const isEdit = Boolean(coupon)

  useEffect(() => {
    if (open) setForm(coupon ? toForm(coupon) : emptyForm)
  }, [open, coupon])

  useEffect(() => {
    if (!open) return
    let active = true
    api.getCourses(0, 100)
      .then((response) => {
        if (active) setCourses(response.content)
      })
      .catch(() => {
        if (active) setCourses([])
      })
    return () => {
      active = false
    }
  }, [open])

  const update = <K extends keyof FormState>(key: K, value: FormState[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const toggleTarget = (value: string) =>
    setForm((prev) => ({
      ...prev,
      targets: prev.targets.includes(value)
        ? prev.targets.filter((t) => t !== value)
        : [...prev.targets, value],
    }))

  const handleSubmit = async () => {
    if (!form.name.trim()) {
      toast.error('쿠폰명을 입력해주세요.')
      return
    }
    if (form.type === 'FCFS' && !form.totalQuantity) {
      toast.error('선착순 쿠폰은 총 발급 수량을 입력해야 합니다.')
      return
    }
    if (!form.discountValue) {
      toast.error('할인 값을 입력해주세요.')
      return
    }

    const saved: AdminCoupon = {
      id: coupon?.id ?? Date.now(),
      name: form.name.trim(),
      totalQuantity: form.type === 'FCFS' && form.totalQuantity ? Number(form.totalQuantity) : null,
      type: form.type,
      autoIssueType: form.type === 'AUTO' && form.autoIssueType !== 'NONE' ? form.autoIssueType : null,
      target: form.target,
      useType: form.useType,
      stackable: form.stackable,
      discountType: form.discountType,
      discountValue: Number(form.discountValue),
      maxDiscount: form.maxDiscount ? Number(form.maxDiscount) : null,
      minOrderAmount: form.minOrderAmount ? Number(form.minOrderAmount) : null,
      validDays: form.validDays ? Number(form.validDays) : null,
      startAt: form.startAt,
      endAt: form.endAt,
      status: coupon?.status ?? 'SCHEDULED',
      issuedCount: coupon?.issuedCount ?? 0,
      targetCategories: form.target === 'CATEGORY' ? form.targets : undefined,
      targetCourses: form.target === 'COURSE' ? form.targets : undefined,
    }

    setSaving(true)
    try {
      await onSave(saved)
      onOpenChange(false)
      toast.success(isEdit ? '쿠폰이 수정되었습니다.' : '쿠폰이 생성되었습니다.')
    } catch (error) {
      toast.error(isEdit ? '쿠폰 수정에 실패했습니다.' : '쿠폰 생성에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  const courseOptions = courses.map((c) => ({ label: c.title, value: c.id }))
  const categoryOptions = Array.from(new Set(courses.map((c) => c.category).filter(Boolean)))
    .map((category) => ({ label: `카테고리 ${category}`, value: category }))

  const targetList =
    form.target === 'CATEGORY'
      ? categoryOptions
      : form.target === 'COURSE'
        ? courseOptions
        : []

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>{isEdit ? '쿠폰 수정' : '쿠폰 생성'}</SheetTitle>
          <SheetDescription>
            쿠폰 정책 정보를 입력하고 저장하세요.
          </SheetDescription>
        </SheetHeader>

        <SheetBody className="space-y-7">
          <Section title="기본 정보">
            <Field label="쿠폰명 *" htmlFor="coupon-name">
              <Input
                id="coupon-name"
                value={form.name}
                onChange={(e) => update('name', e.target.value)}
                placeholder="예) 신규가입 1만원 할인"
              />
            </Field>
          </Section>

          <Section title="적용 규칙">
            <div className="space-y-4">
              <div className="space-y-4">
                <Field label="쿠폰 타입">
                  <Select
                    value={form.type}
                    onValueChange={(v) => {
                      update('type', v as CouponPolicyType)
                      if (v !== 'FCFS') {
                        update('totalQuantity', '')
                      }
                      if (v !== 'AUTO') {
                        update('autoIssueType', 'NONE')
                      }
                    }}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue>
                        {(v: string) => couponTypeLabel[v as CouponPolicyType]}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="AUTO">자동</SelectItem>
                      <SelectItem value="NORMAL">일반</SelectItem>
                      <SelectItem value="FCFS">선착순</SelectItem>
                    </SelectContent>
                  </Select>
                </Field>
                {form.type === 'FCFS' && (
                  <Field label="총 발급 수량 *" htmlFor="coupon-qty">
                    <Input
                      id="coupon-qty"
                      type="number"
                      value={form.totalQuantity}
                      onChange={(e) => update('totalQuantity', e.target.value)}
                      placeholder="예) 100"
                    />
                  </Field>
                )}
                {form.type === 'AUTO' && (
                  <Field label="자동 발급 용도">
                    <Select
                      value={form.autoIssueType}
                      onValueChange={(v) => update('autoIssueType', v as AutoIssueType | 'NONE')}
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue>
                          {(v: string) =>
                            v === 'NONE' ? '연결 안 함' : autoIssueTypeLabel[v as AutoIssueType]
                          }
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="NONE">연결 안 함</SelectItem>
                        <SelectItem value="SIGNUP">회원가입</SelectItem>
                        <SelectItem value="ATTENDANCE_STREAK">연속 출석</SelectItem>
                        <SelectItem value="MONTHLY_ATTENDANCE">월간 출석</SelectItem>
                      </SelectContent>
                    </Select>
                  </Field>
                )}
              </div>

              <Field label="쿠폰 적용 대상">
                <Select
                  value={form.target}
                  onValueChange={(v) =>
                    setForm((prev) => ({
                      ...prev,
                      target: v as CouponApplyTarget,
                      targets: [],
                    }))
                  }
                >
                  <SelectTrigger className="w-full">
                    <SelectValue>
                      {(v: string) => couponTargetLabel[v as CouponApplyTarget]}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">전체</SelectItem>
                    <SelectItem value="CATEGORY">카테고리</SelectItem>
                    <SelectItem value="COURSE">코스</SelectItem>
                  </SelectContent>
                </Select>
              </Field>

              {form.target !== 'ALL' && (
                <Field
                  label={
                    form.target === 'CATEGORY'
                      ? '대상 카테고리 선택'
                      : '대상 코스 선택'
                  }
                >
                  <Popover open={targetOpen} onOpenChange={setTargetOpen}>
                    <PopoverTrigger asChild>
                      <Button
                        variant="outline"
                        role="combobox"
                        aria-expanded={targetOpen}
                        className="w-full justify-between font-normal"
                      >
                        <span className="text-muted-foreground">
                          {form.target === 'CATEGORY'
                            ? '카테고리 검색·선택'
                            : '코스 검색·선택'}
                        </span>
                        <ChevronsUpDown className="size-4 shrink-0 opacity-50" />
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent
                      className="w-[var(--radix-popover-trigger-width)] p-0"
                      align="start"
                    >
                      <Command>
                        <CommandInput
                          placeholder={
                            form.target === 'CATEGORY'
                              ? '카테고리 검색...'
                              : '코스 검색...'
                          }
                        />
                        <CommandList>
                          <CommandEmpty>검색 결과가 없습니다.</CommandEmpty>
                          <CommandGroup>
                            {targetList.map((item) => {
                              const selected = form.targets.includes(item.value)
                              return (
                                <CommandItem
                                  key={item.value}
                                  value={item.label}
                                  onSelect={() => toggleTarget(item.value)}
                                >
                                  <Check
                                    className={cn(
                                      'size-4',
                                      selected ? 'opacity-100' : 'opacity-0',
                                    )}
                                  />
                                  {item.label}
                                </CommandItem>
                              )
                            })}
                          </CommandGroup>
                        </CommandList>
                      </Command>
                    </PopoverContent>
                  </Popover>

                  {form.targets.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 pt-2">
                      {form.targets.map((item) => (
                        <Badge
                          key={item}
                          variant="secondary"
                          className="gap-1 pr-1"
                        >
                          {targetList.find((option) => option.value === item)?.label ?? item}
                          <button
                            type="button"
                            onClick={() => toggleTarget(item)}
                            className="rounded-full p-0.5 hover:bg-muted-foreground/20"
                            aria-label={`${item} 제거`}
                          >
                            <X className="size-3" />
                          </button>
                        </Badge>
                      ))}
                    </div>
                  )}
                </Field>
              )}

              <Field label="사용 타입">
                <Select
                  value={form.useType}
                  onValueChange={(v) => update('useType', v as CouponUseType)}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue>
                      {(v: string) => couponUseTypeLabel[v as CouponUseType]}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="SINGLE">1회용</SelectItem>
                    <SelectItem value="MULTI">다회용</SelectItem>
                  </SelectContent>
                </Select>
              </Field>

              <div className="flex items-center justify-between rounded-lg border bg-muted/30 px-3 py-2.5">
                <div>
                  <p className="text-sm font-medium">중복 적용 가능</p>
                  <p className="text-xs text-muted-foreground">
                    다른 쿠폰과 함께 사용할 수 있습니다.
                  </p>
                </div>
                <Switch
                  checked={form.stackable}
                  onCheckedChange={(v) => update('stackable', v)}
                />
              </div>
            </div>
          </Section>

          <Section title="할인 상세">
            <Field label="할인 타입">
              <Select
                value={form.discountType}
                onValueChange={(v) =>
                  update('discountType', v as CouponDiscountType)
                }
              >
                <SelectTrigger className="w-full">
                  <SelectValue>
                    {(v: string) => couponDiscountTypeLabel[v as CouponDiscountType]}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="AMOUNT">정액 할인 (원)</SelectItem>
                  <SelectItem value="RATE">정률(%) 할인</SelectItem>
                </SelectContent>
              </Select>
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label={form.discountType === 'RATE' ? '할인 값 (%)' : '할인 값 (원)'}>
                <Input
                  type="number"
                  value={form.discountValue}
                  onChange={(e) => update('discountValue', e.target.value)}
                  placeholder={form.discountType === 'RATE' ? '예) 20' : '예) 10000'}
                />
              </Field>
              <Field label="최대 할인 금액 (원)">
                <Input
                  type="number"
                  value={form.maxDiscount}
                  onChange={(e) => update('maxDiscount', e.target.value)}
                  placeholder="비워두면 제한 없음"
                />
              </Field>
              <Field label="최소 주문 금액 (원)">
                <Input
                  type="number"
                  value={form.minOrderAmount}
                  onChange={(e) => update('minOrderAmount', e.target.value)}
                  placeholder="예) 30000"
                />
              </Field>
            </div>
          </Section>

          <Section title="발급 기간">
            <Field label="유효 기간 (일)">
              <Input
                type="number"
                value={form.validDays}
                onChange={(e) => update('validDays', e.target.value)}
                placeholder="발급 후 O일"
              />
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="발급 시작 일시">
                <Input
                  type="datetime-local"
                  value={form.startAt}
                  onChange={(e) => update('startAt', e.target.value)}
                />
              </Field>
              <Field label="발급 종료 일시">
                <Input
                  type="datetime-local"
                  value={form.endAt}
                  onChange={(e) => update('endAt', e.target.value)}
                />
              </Field>
            </div>
          </Section>
        </SheetBody>

        <SheetFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={saving}>
            취소
          </Button>
          <Button onClick={handleSubmit} disabled={saving}>
            {isEdit ? '수정 저장' : '쿠폰 생성'}
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  )
}
