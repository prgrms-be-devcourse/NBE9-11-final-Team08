import { Database, Clock, History, FileSearch, ShieldAlert, ShieldCheck } from 'lucide-react'
import { api } from '@/lib/api'
import { Badge } from '@/components/ui/badge'

function fmt(iso: string | null) {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('ko-KR', {
    year: '2-digit',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default async function AdminAuditPage() {
  const audit = await api.getAdminAudit()

  if (!audit) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold">데이터 보존 현황 감사</h1>
        <div className="rounded-xl border bg-card p-8 text-center text-sm text-muted-foreground">
          감사 데이터를 불러오지 못했습니다. 관리자 권한으로 로그인했는지 확인해주세요.
        </div>
      </div>
    )
  }

  const r = audit.retention
  const retentionCards = [
    { icon: Database, label: '보존 학습 이벤트', value: r.learningEventCount.toLocaleString() },
    { icon: History, label: '진도 기록', value: r.lectureProgressCount.toLocaleString() },
    { icon: FileSearch, label: '강좌 상태 이력', value: r.courseStatusHistoryCount.toLocaleString() },
    { icon: Clock, label: '최초 이벤트', value: fmt(r.oldestEventTime) },
    { icon: Clock, label: '최근 이벤트', value: fmt(r.newestEventTime) },
  ]

  const hasErrors = audit.integrityErrors.some((e) => e.count > 0)

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">데이터 보존 현황 감사</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          보존 로그·접근 이력·정합성 오류를 기존 데이터에서 파생해 보여줍니다.
        </p>
      </div>

      {/* 보존 로그 요약 */}
      <section>
        <h2 className="mb-3 font-semibold">보존 로그</h2>
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-5">
          {retentionCards.map((c) => (
            <div key={c.label} className="rounded-xl border bg-card p-4">
              <c.icon className="h-5 w-5 text-muted-foreground" />
              <p className="mt-3 text-lg font-bold">{c.value}</p>
              <p className="text-xs text-muted-foreground">{c.label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* 정합성 오류 */}
      <section>
        <h2 className="mb-3 flex items-center gap-2 font-semibold">
          {hasErrors ? (
            <ShieldAlert className="h-4 w-4 text-destructive" />
          ) : (
            <ShieldCheck className="h-4 w-4 text-emerald-500" />
          )}
          정합성 오류
        </h2>
        <div className="space-y-2">
          {audit.integrityErrors.map((e) => (
            <div
              key={e.type}
              className="flex items-center justify-between rounded-lg border bg-card px-4 py-3"
            >
              <div>
                <p className="text-sm font-medium">{e.description}</p>
                <p className="text-xs text-muted-foreground">
                  {e.type}
                  {e.count > 0 && e.sampleIds.length > 0
                    ? ` · 예시 ID: ${e.sampleIds.join(', ')}`
                    : ''}
                </p>
              </div>
              {e.count > 0 ? (
                <Badge variant="destructive">{e.count.toLocaleString()}건</Badge>
              ) : (
                <Badge variant="outline">정상</Badge>
              )}
            </div>
          ))}
        </div>
      </section>

      {/* 접근/변경 이력 */}
      <section>
        <h2 className="mb-3 font-semibold">접근 이력 (최근 30건)</h2>
        {audit.accessHistory.length === 0 ? (
          <p className="rounded-xl border bg-card p-6 text-center text-sm text-muted-foreground">
            기록된 이력이 없습니다.
          </p>
        ) : (
          <ol className="space-y-1.5">
            {audit.accessHistory.map((h, i) => (
              <li
                key={`${h.source}-${i}`}
                className="flex items-center gap-3 rounded-lg border bg-card px-4 py-2.5 text-sm"
              >
                <span className="w-28 shrink-0 text-muted-foreground">{fmt(h.occurredAt)}</span>
                <Badge variant="outline" className="shrink-0">
                  {h.source}
                </Badge>
                <span className="flex-1">{h.description}</span>
                {h.actorId != null && (
                  <span className="shrink-0 text-xs text-muted-foreground">actor #{h.actorId}</span>
                )}
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  )
}
