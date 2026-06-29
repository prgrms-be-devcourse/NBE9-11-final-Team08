// frontend/components/course/course-detail.tsx
'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState, useEffect, useRef, useCallback } from 'react'
import { CheckCircle2, Clock, PlayCircle, Star, Users, Loader2, Play } from 'lucide-react'
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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useCart } from '@/components/providers/cart-provider'
import type { Course, Lecture } from '@/lib/types'
import { discountedPrice, formatKRW } from '@/lib/utils'
import { api } from '@/lib/api'
import Hls from 'hls.js'

export function CourseDetail({ course }: { course: Course }) {
  const router = useRouter()
  const { addItem, has } = useCart()
  const [buying, setBuying] = useState(false)
  const [adding, setAdding] = useState(false)
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [isPurchased, setIsPurchased] = useState(false)
  const [hasStudyAccess, setHasStudyAccess] = useState(false)
  const [studyId, setStudyId] = useState<string | null>(null)
  const [isAdmin, setIsAdmin] = useState(false)

  const [previewOpen, setPreviewOpen] = useState(false)
  const [previewVideoUrl, setPreviewVideoUrl] = useState<string | null>(null)
  const [previewLectureTitle, setPreviewLectureTitle] = useState<string>('')
  const [previewLoading, setPreviewLoading] = useState(false)
  const hlsRef = useRef<Hls | null>(null)

  const videoRef = useCallback((video: HTMLVideoElement | null) => {
    if (hlsRef.current) {
      hlsRef.current.destroy()
      hlsRef.current = null
    }

    if (!video || !previewVideoUrl) return

    if (Hls.isSupported()) {
      const hls = new Hls({
        xhrSetup: (xhr) => {
          xhr.withCredentials = true
        }
      })
      hlsRef.current = hls
      hls.loadSource(previewVideoUrl)
      hls.attachMedia(video)
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        video.play().catch((e) => console.log('Auto-play failed:', e))
      })
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = previewVideoUrl
      video.addEventListener('loadedmetadata', () => {
        video.play().catch((e) => console.log('Auto-play failed:', e))
      })
    }
  }, [previewVideoUrl])

  const handlePreview = async (lec: Lecture) => {
    setPreviewLoading(true)
    try {
      const chId = lec.chapterId || '';
      const streamUrlText = await api.getVideoStreamUrl(course.id, chId, lec.id)

      if (!streamUrlText) {
        toast.error('비디오 스트리밍 주소를 가져오지 못했습니다.')
        return
      }

      const formattedUrl = streamUrlText.startsWith('http')
        ? streamUrlText
        : `http://localhost:8080/videos-local/${streamUrlText}`

      setPreviewLectureTitle(lec.title)
      setPreviewVideoUrl(formattedUrl)
      setPreviewOpen(true)
    } catch (error) {
      toast.error('미리보기 재생을 시작할 수 없습니다.')
    } finally {
      setPreviewLoading(false)
    }
  }

  const final = discountedPrice(course.price, course.discountRate)
  const totalLectures = course.chapters.reduce((s, c) => s + c.lectures.length, 0)
  const inCart = has(course.id)
  const canPurchase = !(isLoggedIn && isPurchased) && !isAdmin

  useEffect(() => {
    const fetchAccess = async () => {
      try {
        const profile = await api.getProfile()
        setIsLoggedIn(!!profile)
        const nextIsAdmin = profile ? profile.isAdmin : false
        setIsAdmin(nextIsAdmin)

        const nextStudyId = await api.getStudyIdByCourseId(course.id)

        let active = false
        if (profile && !nextIsAdmin) {
          try {
            active = await api.isCourseEnrollmentActive(course.id)
          } catch (err) {
            console.error('Failed to check active enrollment:', err)
          }
        }

        setIsPurchased(active)
        setHasStudyAccess((active || nextIsAdmin) && !!nextStudyId)
        setStudyId(nextStudyId)
      } catch (e) {
        console.error('Failed to fetch course/study access:', e)
        setIsPurchased(false)
        setHasStudyAccess(false)
        setStudyId(null)
      }
    }
    fetchAccess()
  }, [course.id])

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
      const order = await api.createDirectOrder(Number(course.id))
      router.push(`/checkout?orderId=${order.orderId}`)
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
              <span className="flex items-center gap-1 text-background/80">
                조회수: {(course.viewCount || 0).toLocaleString('ko-KR')}
              </span>
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
                        {ch.lectures.map((lec) => {
                          const isFreePreview = lec.isFreePreview === true;
                          const showPreviewButton = isFreePreview && !isPurchased;

                          return (
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
                                <span className="font-medium">{lec.title}</span>
                                {isFreePreview && (
                                  <Badge variant="secondary" className="px-1.5 py-0 text-[10px] bg-primary/10 text-primary hover:bg-primary/20 border-none font-semibold">
                                    무료 미리보기
                                  </Badge>
                                )}
                              </span>
                              <div className="flex items-center gap-3">
                                {showPreviewButton && (
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="h-7 gap-1 px-2.5 text-xs font-semibold hover:bg-primary hover:text-primary-foreground border-primary text-primary transition-all duration-200"
                                    onClick={() => handlePreview(lec)}
                                    disabled={previewLoading}
                                  >
                                    <Play className="h-3 w-3 fill-current" />
                                    미리보기
                                  </Button>
                                )}
                                <span className="flex items-center gap-1 text-xs text-muted-foreground shrink-0">
                                  <Clock className="h-3 w-3" />
                                  {lec.duration}
                                </span>
                              </div>
                            </li>
                          );
                        })}
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
                {canPurchase ? (
                  <>
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
                    <Button onClick={handleBuy} className="w-full" size="lg" disabled={buying}>
                      {buying ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                      코스 구매하기
                    </Button>
                    <Button onClick={handleAdd} variant="secondary" className="w-full" disabled={adding || (isLoggedIn && inCart)}>
                      {adding ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : (isLoggedIn && inCart) ? '장바구니에 담김' : '장바구니 담기'}
                    </Button>
                  </>
                ) : null}
                {hasStudyAccess && studyId ? (
                  <Button asChild variant="outline" className="w-full">
                    <Link href={`/study/${studyId}`}>스터디 입장</Link>
                  </Button>
                ) : (isAdmin || isPurchased) ? (
                  <Button variant="outline" className="w-full" disabled>
                    스터디 미개설
                  </Button>
                ) : null}

                <Separator />
                <ul className="space-y-2 text-sm text-muted-foreground">
                  <li className="flex justify-between">
                    <span>강의 수</span>
                    <span className="font-medium text-foreground">{totalLectures}개</span>
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

      {/* 무료 미리보기 HLS 비디오 모달 */}
      <Dialog open={previewOpen} onOpenChange={setPreviewOpen}>
        <DialogContent className="sm:max-w-[800px] p-0 overflow-hidden bg-black border-none shadow-2xl animate-in fade-in-50 duration-200">
          <DialogHeader className="p-4 bg-background border-b flex flex-row items-center justify-between">
            <DialogTitle className="text-base font-bold flex items-center gap-2">
              <Badge variant="secondary" className="bg-primary/10 text-primary border-none">미리보기</Badge>
              <span className="truncate max-w-[500px]">{previewLectureTitle}</span>
            </DialogTitle>
          </DialogHeader>
          <div className="relative aspect-video w-full bg-neutral-950 flex items-center justify-center">
            <video
              ref={videoRef}
              controls
              className="h-full w-full object-contain animate-in fade-in-0 duration-300"
              playsInline
            />
            {!previewVideoUrl && (
              <div className="absolute inset-0 flex flex-col items-center justify-center bg-neutral-950/80 text-muted-foreground gap-2">
                <Loader2 className="h-8 w-8 animate-spin" />
                <span className="text-sm">스트리밍을 로드하는 중...</span>
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
