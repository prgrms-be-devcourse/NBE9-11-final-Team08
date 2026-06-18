'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft, ImagePlus, ListChecks, Sparkles, X } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { categories } from '@/lib/mock-data'
import type { Course } from '@/lib/types'

const subCategories = ['백엔드', '프론트엔드', '데브옵스', '프로그래밍 언어', '업무 생산성']

export function CourseForm({ course }: { course?: Course }) {
  const router = useRouter()
  const editing = Boolean(course)
  const [title, setTitle] = useState(course?.title ?? '')
  const [mainCat, setMainCat] = useState(course?.category ?? '')
  const [subCat, setSubCat] = useState(course?.subCategory ?? '')
  const [price, setPrice] = useState(course ? String(course.price) : '')
  const [description, setDescription] = useState(course?.description ?? '')
  const [tags, setTags] = useState<string[]>(course?.tags ?? [])
  const [tagInput, setTagInput] = useState('')

  const addTag = () => {
    const v = tagInput.trim()
    if (v && !tags.includes(v)) setTags((p) => [...p, v])
    setTagInput('')
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <Button asChild variant="ghost" size="icon" aria-label="목록으로">
          <Link href="/instructor">
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <h1 className="text-2xl font-bold">
          {editing ? '강의 수정' : '새 강의 등록'}
        </h1>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <section className="space-y-4 rounded-xl border bg-card p-5">
            <div className="grid gap-2">
              <Label htmlFor="title">강의 제목</Label>
              <Input
                id="title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="예: 자바 ORM 표준기술 JPA for beginner"
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label>대분류</Label>
                <Select value={mainCat} onValueChange={setMainCat}>
                  <SelectTrigger>
                    <SelectValue placeholder="대분류 선택" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories
                      .filter((c) => c !== '전체')
                      .map((c) => (
                        <SelectItem key={c} value={c}>
                          {c}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label>소분류</Label>
                <Select value={subCat} onValueChange={setSubCat}>
                  <SelectTrigger>
                    <SelectValue placeholder="소분류 선택" />
                  </SelectTrigger>
                  <SelectContent>
                    {subCategories.map((c) => (
                      <SelectItem key={c} value={c}>
                        {c}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="price">가격 (KRW)</Label>
              <Input
                id="price"
                type="number"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                placeholder="0"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="tags">태그 / 키워드</Label>
              <div className="flex flex-wrap gap-2">
                {tags.map((t) => (
                  <Badge key={t} variant="secondary" className="gap-1">
                    {t}
                    <button
                      type="button"
                      onClick={() => setTags((p) => p.filter((x) => x !== t))}
                      aria-label={`${t} 삭제`}
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </Badge>
                ))}
              </div>
              <div className="flex gap-2">
                <Input
                  id="tags"
                  value={tagInput}
                  onChange={(e) => setTagInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault()
                      addTag()
                    }
                  }}
                  placeholder="태그 입력 후 Enter"
                />
                <Button type="button" variant="outline" onClick={addTag}>
                  추가
                </Button>
              </div>
            </div>
          </section>

          <section className="space-y-3 rounded-xl border bg-card p-5">
            <div className="flex items-center justify-between">
              <Label htmlFor="desc">상세 설명</Label>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="text-xs text-muted-foreground"
                onClick={() => toast.info('AI 설명 생성 기능은 곧 제공될 예정입니다.')}
              >
                <Sparkles className="mr-1 h-3.5 w-3.5" /> AI로 생성
              </Button>
            </div>
            <Textarea
              id="desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="강의 소개, 학습 목표, 수강 대상 등을 작성하세요."
              className="min-h-44"
            />
            <Separator />
            <Button asChild variant="outline" className="w-full">
              <Link
                href={
                  course
                    ? `/instructor/courses/${course.id}/curriculum`
                    : '/instructor/courses/new/curriculum'
                }
              >
                <ListChecks className="mr-1 h-4 w-4" /> 커리큘럼 등록 / 수정
              </Link>
            </Button>
          </section>
        </div>

        {/* Side: cover + actions */}
        <div className="space-y-6">
          <section className="rounded-xl border bg-card p-5">
            <Label>커버 이미지</Label>
            <button
              type="button"
              onClick={() => toast.info('이미지 업로드는 데모에서 비활성화되어 있습니다.')}
              className="mt-2 flex aspect-video w-full flex-col items-center justify-center gap-2 rounded-lg border border-dashed text-muted-foreground transition-colors hover:bg-secondary"
            >
              <ImagePlus className="h-7 w-7" />
              <span className="text-xs">
                파일을 드래그하거나 클릭해 업로드
              </span>
              <span className="text-[11px]">최대 10MB · JPG, PNG</span>
            </button>
          </section>

          <section className="space-y-2 rounded-xl border bg-card p-5">
            <Button
              className="w-full"
              onClick={() => {
                toast.success('검수 요청이 접수되었습니다.')
                router.push('/instructor')
              }}
            >
              검수 요청하기
            </Button>
            <Button
              variant="outline"
              className="w-full"
              onClick={() => {
                toast.success('임시저장되었습니다.')
                router.push('/instructor')
              }}
            >
              임시저장
            </Button>
          </section>
        </div>
      </div>
    </div>
  )
}
