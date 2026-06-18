"use client"

import Link from "next/link"
import { ArrowLeft } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { CurriculumBuilder } from "@/components/instructor/curriculum-builder"

export function CurriculumPage({
  backHref,
  courseTitle,
}: {
  backHref: string
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

      <CurriculumBuilder />

      <div className="flex justify-end gap-2">
        <Button asChild variant="outline">
          <Link href={backHref}>취소</Link>
        </Button>
        <Button onClick={() => toast.success("커리큘럼이 저장되었습니다.")}>저장</Button>
      </div>
    </div>
  )
}
