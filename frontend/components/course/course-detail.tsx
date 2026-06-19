// frontend/components/course/course-detail.tsx
'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState, useEffect } from 'react'
import { CheckCircle2, Clock, PlayCircle, Star, Users, Loader2 } from 'lucide-react'
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
import { api } from '@/lib/api'

export function CourseDetail({ course }: { course: Course }) {
  const router = useRouter()
  const { addItem, has } = useCart()
  const [buying, setBuying] = useState(false)
  const [adding, setAdding] = useState(false)
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [isPurchased, setIsPurchased] = useState(false)
  const [hasStudyAccess, setHasStudyAccess] = useState(false)
  
  const final = discountedPrice(course.price, course.discountRate)
  const totalLectures = course.chapters.reduce((s, c) => s + c.lectures.length, 0)
  const inCart = has(course.id)

  useEffect(() => {
    const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null
    setIsLoggedIn(!!token)
    
    const fetchAccess = async () => {
      if (!token) return
      try {
        let purchased = false
        if ('getPurchasedCourses' in api) {
          const list = await api.getPurchasedCourses()
          purchased = list.some(c => c.id.toString() === course.id.toString())
          if (purchased) {
            setIsPurchased(true)
            setHasStudyAccess(true)
          }
        }
        
        if (!purchased && 'getStudy' in api) {
          const study = await api.getStudy(course.id)
          if (study) {
            setHasStudyAccess(true)
          }
        }
      } catch (e) {
        console.error('Failed to fetch course/study access:', e)
      }
    }
    fetchAccess()
  }, [])

  const handleAdd = async () => {
    if (!isLoggedIn) {
      toast.error('로그인이 필요한 서비스입니다.')
      router.push('/login')
      return
    }
    if (inCart || isPurchased) return
    setAdding(true)
    try {
      await addItem(course)
    } catch (err) {
      // handled in addItem
    } finally {
      setAdding(false)
    }
  }

  const handleBuy = async () => {
    if (!isLoggedIn) {
      toast.error('로그인이 필요한 서비스입니다.')
      router.push('/login')
      return
    }
    if (isPurchased) return
    setBuying(true)
    try {
      if ('createDirectOrder' in api) {
        const order = await (api as any).createDirectOrder(Number(course.id))
        router.push(`/orders/${order.orderId || order.id}`)
      } else {
        await addItem(course)
        router.push('/cart')
      }
    } catch (err) {
      toast.error('주문 생성에 실패했습니다.')
    } finally {
      setBuying(false)
    }
  }

  return (
    <div className="bg-card">
      <div className="border-b bg-foreground text-background">
        <div className="mx-auto max-w-7xl px-4 py-10 lg:grid lg:grid-cols-[1fr_360px] lg:gap-10">
          <div>
            <Badge variant="secondary" className="mb-3">
              {course.category} · {course.subCategory || '분류 없음'}
            </Badge>
            <h1 className="text-2xl font-bold leading-tight text-balance sm:text-3xl">
              {course.title}
            </h1>
            <p className="mt-3 text-base leading-relaxed text-background/80">{course.subtitle || course.description?.substring(0, 100)}</p>
            <div className="mt-5 flex flex-wrap items-center gap-4 text-sm">
              <span className="flex items-center gap-1 text-amber-400">
                <Star className="h-4 w-4 fill-current" />
                <span className="font-semibold">{(course.rating || 0).toFixed(1)}</span>
                <span className="text-background/70">({course.reviewCount || 0}개 수강평)</span>
              </span>
              <span className="flex items-center gap-1 text-background/80">
                <Users className="h-4 w-4" />
                {(course.studentCount || 0).toLocaleString('ko-KR')}명 참여
              </span>
              <Badge className="bg-primary text-primary-foreground">{course.level}</Badge>
            </div>
            <div className="mt-4 flex items-center gap-3">
              <Avatar className="h-9 w-9">
                <AvatarFallback className="bg-primary text-primary-foreground">
                  {course.instructor.name?.[0] || '?'}
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
        <div className="space-y-10">
          <section>
            <h2 className="mb-3 text-xl font-bold">강좌 소개</h2>
            <p className="leading-relaxed text-muted-foreground">{course.description}</p>
            <div className="mt-4 flex flex-wrap gap-2">
              {course.tags?.map((t) => (
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
            {course.chapters.length > 0 ? (
              <Accordion multiple defaultValue={[course.chapters[0]?.id]} className="rounded-xl border">
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
            ) : (
              <div className="rounded-xl border p-8 text-center text-sm text-muted-foreground">
                등록된 커리큘럼이 없습니다.
              </div>
            )}
          </section>

          <section>
            <h2 className="mb-3 text-xl font-bold">강사 소개</h2>
            <div className="flex items-start gap-4 rounded-xl border p-5">
              <Avatar className="h-14 w-14">
                <AvatarFallback className="bg-primary text-lg text-primary-foreground">
                  {course.instructor.name?.[0] || '?'}
                </AvatarFallback>
              </Avatar>
              <div>
                <p className="font-semibold">{course.instructor.name}</p>
                <p className="text-sm text-muted-foreground">{course.instructor.title}</p>
              </div>
            </div>
          </section>
        </div>

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

                <Button onClick={handleBuy} className="w-full" size="lg" disabled={buying || (isLoggedIn && isPurchased)}>
                  {buying ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                  {isLoggedIn && isPurchased ? '이미 구매한 강좌' : '코스 구매하기'}
                </Button>
                <Button onClick={handleAdd} variant="secondary" className="w-full" disabled={adding || (isLoggedIn && (inCart || isPurchased))}>
                  {adding ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : (isLoggedIn && isPurchased) ? '구매 완료' : (isLoggedIn && inCart) ? '장바구니에 담김' : '장바구니 담기'}
                </Button>
                {!isLoggedIn ? (
                  <Button onClick={() => {
                    toast.error('로그인이 필요한 서비스입니다.')
                    router.push('/login')
                  }} variant="outline" className="w-full">
                    스터디 입장
                  </Button>
                ) : hasStudyAccess ? (
                  <Button asChild variant="outline" className="w-full">
                    <Link href={`/study/${course.id}`}>스터디 입장</Link>
                  </Button>
                ) : (
                  <Button variant="outline" className="w-full" disabled>
                    스터디 입장
                  </Button>
                )}

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
                    <span className="font-medium text-foreground">{course.subCategory || '분류 없음'}</span>
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
