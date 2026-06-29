'use client'

import { useState } from 'react'
import Link from 'next/link'
import { ChevronLeft, ChevronRight, RefreshCw, ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { CouponIssueRequestResponse, PageResponse } from '@/lib/types'
import { api } from '@/lib/api'
import { toast } from 'sonner'

export function CouponRequestsList({
  initialData,
}: {
  initialData: PageResponse<CouponIssueRequestResponse> | null
}) {
  const [data, setData] = useState<PageResponse<CouponIssueRequestResponse> | null>(initialData)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)

  const fetchPage = async (p: number) => {
    setLoading(true)
    try {
      const res = await api.getCouponIssueRequests(p, 20)
      if (res) {
        setData(res)
        setPage(p)
      }
    } catch (error) {
      toast.error('목록을 불러오는 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = () => fetchPage(page)

  const formatDateTime = (value: string | number[] | null) => {
    if (!value) return '-'
    if (Array.isArray(value)) {
      const [year, month, day, hour = 0, minute = 0, second = 0] = value
      const pad = (n: number) => String(n).padStart(2, '0')
      return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`
    }
    return typeof value === 'string' ? value.replace('T', ' ').slice(0, 19) : String(value)
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Badge variant="outline">대기 중</Badge>
      case 'IN_PROGRESS':
        return <Badge variant="secondary">진행 중</Badge>
      case 'COMPLETED':
        return <Badge className="bg-green-500">완료</Badge>
      case 'FAILED':
        return <Badge variant="destructive">실패</Badge>
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  const getIssueTypeLabel = (type: string) => {
    switch (type) {
      case 'TARGETED':
        return '특정 회원'
      case 'ALL_USERS':
        return '전체 회원'
      default:
        return type
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <Button variant="ghost" size="icon-sm" asChild>
              <Link href="/admin/coupons">
                <ArrowLeft className="h-4 w-4" />
                <span className="sr-only">뒤로</span>
              </Link>
            </Button>
            <h1 className="text-2xl font-bold">쿠폰 발급 내역</h1>
          </div>
          <p className="text-sm text-muted-foreground ml-8">
            진행 중이거나 완료된 쿠폰 발급 요청 상태를 확인합니다.
          </p>
        </div>
        <Button variant="outline" onClick={handleRefresh} disabled={loading}>
          <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          새로고침
        </Button>
      </div>

      <div className="rounded-xl border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-16">ID</TableHead>
              <TableHead>정책 ID</TableHead>
              <TableHead>요청 키</TableHead>
              <TableHead>발급 대상</TableHead>
              <TableHead>상태</TableHead>
              <TableHead className="text-right">요청 수량</TableHead>
              <TableHead className="text-right text-green-600">성공</TableHead>
              <TableHead className="text-right text-destructive">실패</TableHead>
              <TableHead className="text-right text-muted-foreground">스킵</TableHead>
              <TableHead>요청일시</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data?.content && data.content.length > 0 ? (
              data.content.map((req) => (
                <TableRow key={req.id}>
                  <TableCell className="font-medium text-muted-foreground">{req.id}</TableCell>
                  <TableCell>{req.policyId}</TableCell>
                  <TableCell>
                    <span className="max-w-[120px] truncate block" title={req.requestKey}>
                      {req.requestKey}
                    </span>
                  </TableCell>
                  <TableCell>{getIssueTypeLabel(req.issueType)}</TableCell>
                  <TableCell>{getStatusBadge(req.status)}</TableCell>
                  <TableCell className="text-right">{req.requestedCount.toLocaleString()}</TableCell>
                  <TableCell className="text-right font-medium text-green-600">
                    {req.successCount.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right text-destructive">
                    {req.failedCount.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right text-muted-foreground">
                    {req.skippedCount.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {formatDateTime(req.requestedAt)}
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={10} className="py-12 text-center text-sm text-muted-foreground">
                  발급 내역이 없습니다.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="icon"
            onClick={() => fetchPage(page - 1)}
            disabled={page === 0 || loading}
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="text-sm">
            {page + 1} / {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="icon"
            onClick={() => fetchPage(page + 1)}
            disabled={page >= data.totalPages - 1 || loading}
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      )}
    </div>
  )
}
