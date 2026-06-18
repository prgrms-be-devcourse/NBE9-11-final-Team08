'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { CheckCircle2, Clock, PlayCircle, Star, Users } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion'
import { useCart } from '@/components/providers/cart-provider'
import type { Course } from '@/lib/types'
import { discountedPrice, formatKRW } from '@/lib/utils'

export function CourseDetail({ course }: { course: Course }) {
  const router = useRouter()
  const { addItem, has } = useCart()
  const final = discountedPrice(course.price, course.discountRate)
  const totalLectures = course.chapters.reduce((s, c) => s + c.lectures.length, 0)
  const inCart = has(course.id)

  const handleAdd = () => {
    addItem(course)
    toast.success('장바구니에 담았습니다.')
  }

  const handleBuy = () => {
    addItem(course)
    router.push('/cart')
  }

  return (
    <div className="bg-card">
      {/* Hero */}
      <div className="border-b bg-foreground text-background">
        <div className="mx-auto max-w-7xl px-4 py-10 lg:grid lg:grid-cols-[1fr_360px] lg:gap-10">
          <div>
            <Badge variant="secondary" className="mb-3">
              {course.category} · {course.subCategory}
            </Badge>
            <h1 className="text-2xl font-bold leading-tight text-balance sm:text-3xl">
              {course.title}
            </h1>
            <p className="mt-3 text-base leading-relaxed text-background/80">{course.subtitle}</p>
            <div className="mt-5 flex flex-wrap items-center gap-4 text-sm">
              <span className="flex items-center gap-1 text-amber-400">
                <Star className="h-4 w-4 fill-current" />
                <span className="font-semibold">{course.rating.toFixed(1)}</span>
                <span className="text-background/70">({course.reviewCount}개 수강평)</span>
              </span>
              <span className="flex items-center gap-1 text-background/80">
                <Users className="h-4 w-4" />
                {course.studentCount.toLocaleString('ko-KR')}명 참여
              </span>
              <Badge className="bg-primary text-primary-foreground">{course.level}</Badge>
            </div>
            <div className="mt-4 flex items-center gap-3">
              <Avatar className="h-9 w-9">
                <AvatarFallback className="bg-primary text-primary-foreground">
                  {course.instructor.name[0]}
                </AvatarFallback>
              </Avatar>
              <div>
                <p className="text-sm font-medium">{course.instructor.name}</p>
                <p className="text-xs text-background/70">{course.instructor.title}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-7xl px-4 py-10 lg:grid lg:grid-cols-[1fr_360px] lg:gap-10">
        {/* Main content */}
        <div className="space-y-10">
          <section>
            <h2 className="mb-3 text-xl font-bold">강좌 소개</h2>
            <p className="leading-relaxed text-muted-foreground">{course.description}</p>
            <div className="mt-4 flex flex-wrap gap-2">
              {course.tags.map((t) => (
                <Badge key={t} variant="outline">
                  {t}
                </Badge>
              ))}
            </div>
          </section>

          <section>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-xl font-bold">커리큘럼</h2>
              <p className="text-sm text-muted-foreground">
                총 {course.chapters.length}개 챕터 · {totalLectures}개 강의
              </p>
            </div>
            <Accordion type="multiple" defaultValue={[course.chapters[0]?.id]} className="rounded-xl border">
              {course.chapters.map((ch) => (
                <AccordionItem key={ch.id} value={ch.id} className="px-4 last:border-b-0">
                  <AccordionTrigger className="text-sm font-semibold">{ch.title}</AccordionTrigger>
                  <AccordionContent>
                    <ul className="space-y-1">
                      {ch.lectures.map((lec) => (
                        <li
                          key={lec.id}
                          className="flex items-center justify-between rounded-md px-2 py-2 text-sm hover:bg-secondary"
                        >
                          <span className="flex items-center gap-2">
                            {lec.completed ? (
                              <CheckCircle2 className="h-4 w-4 text-primary" />
                            ) : (
                              <PlayCircle className="h-4 w-4 text-muted-foreground" />
                            )}
                            {lec.title}
                          </span>
                          <span className="flex items-center gap-1 text-xs text-muted-foreground">
                            <Clock className="h-3 w-3" />
                            {lec.duration}
                          </span>
                        </li>
                      ))}
                    </ul>
                  </AccordionContent>
                </AccordionItem>
              ))}
            </Accordion>
          </section>

          <section>
            <h2 className="mb-3 text-xl font-bold">강사 소개</h2>
            <div className="flex items-start gap-4 rounded-xl border p-5">
              <Avatar className="h-14 w-14">
                <AvatarFallback className="bg-primary text-lg text-primary-foreground">
                  {course.instructor.name[0]}
                </AvatarFallback>
              </Avatar>
              <div>
                <p className="font-semibold">{course.instructor.name}</p>
                <p className="text-sm text-muted-foreground">{course.instructor.title}</p>
              </div>
            </div>
          </section>
        </div>

        {/* Sticky purchase card */}
        <div className="mt-8 lg:mt-0">
          <div className="lg:sticky lg:top-20">
            <div className="overflow-hidden rounded-xl border bg-background shadow-sm">
              <div className="relative aspect-video bg-muted">
                <Image
                  src={course.thumbnailUrl || '/placeholder.svg'}
                  alt={`${course.title} 썸네일`}
                  fill
                  sizes="360px"
                  className="object-cover"
                />
              </div>
              <div className="space-y-4 p-5">
                <div className="flex items-baseline gap-2">
                  {course.discountRate ? (
                    <>
                      <span className="text-lg font-bold text-destructive">
                        {course.discountRate}%
                      </span>
                      <span className="text-2xl font-bold">{formatKRW(final)}</span>
                    </>
                  ) : (
                    <span className="text-2xl font-bold">{formatKRW(final)}</span>
                  )}
                </div>
                {course.discountRate ? (
                  <p className="-mt-2 text-sm text-muted-foreground line-through">
                    {formatKRW(course.price)}
                  </p>
                ) : null}

                <Button onClick={handleBuy} className="w-full" size="lg">
                  코스 구매하기
                </Button>
                <Button onClick={handleAdd} variant="secondary" className="w-full" disabled={inCart}>
                  {inCart ? '장바구니에 담김' : '장바구니 담기'}
                </Button>
                <Button asChild variant="outline" className="w-full">
                  <Link href={`/study/${course.id}`}>스터디 입장</Link>
                </Button>

                <Separator />
                <ul className="space-y-2 text-sm text-muted-foreground">
                  <li className="flex justify-between">
                    <span>강의 수</span>
                    <span className="font-medium text-foreground">{totalLectures}개</span>
                  </li>
                  <li className="flex justify-between">
                    <span>난이도</span>
                    <span className="font-medium text-foreground">{course.level}</span>
                  </li>
                  <li className="flex justify-between">
                    <span>수강 대상</span>
                    <span className="font-medium text-foreground">{course.subCategory}</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
