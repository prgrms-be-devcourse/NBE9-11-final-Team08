"use client"

import Link from "next/link"
import { ArrowLeft } from "lucide-react"
import { Button } from "@/components/ui/button"
import { CurriculumBuilder } from "@/components/instructor/curriculum-builder"

export function CurriculumPage({
  backHref,
  courseId,
  courseTitle,
}: {
  backHref: string
  courseId?: string
  courseTitle?: string
}) {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <Button asChild variant="ghost" size="icon" aria-label="강의 정보로 돌아가기">
          <Link href={backHref}>
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-bold">커리큘럼 구성</h1>
          {courseTitle ? (
            <p className="text-sm text-muted-foreground">{courseTitle}</p>
          ) : null}
        </div>
      </div>

      {courseId ? (
        <>
          <CurriculumBuilder courseId={courseId} backHref={backHref} />

          <div className="flex justify-end">
            <Button asChild variant="outline">
              <Link href={backHref}>취소</Link>
            </Button>
          </div>
        </>
      ) : (
        <div className="rounded-xl border border-dashed bg-card px-6 py-12 text-center">
          <p className="text-sm font-medium">강좌 기본 정보를 먼저 저장해주세요.</p>
          <p className="mt-2 text-sm text-muted-foreground">
            커리큘럼은 강좌가 생성된 뒤 등록할 수 있습니다.
          </p>
          <Button asChild className="mt-5">
            <Link href={backHref}>강좌 기본정보로 돌아가기</Link>
          </Button>
        </div>
      )}
    </div>
  )
}
