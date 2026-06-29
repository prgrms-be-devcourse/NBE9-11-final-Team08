import Link from 'next/link'
import { GraduationCap } from 'lucide-react'

export function SiteFooter() {
  return (
    <footer className="mt-16 border-t bg-card">
      {/* <div className="mx-auto grid max-w-7xl gap-8 px-4 py-12 md:grid-cols-4">
        <div className="md:col-span-1">
          <div className="flex items-center gap-2">
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <GraduationCap className="h-4 w-4" />
            </span>
            <span className="font-bold">PlayLearn</span>
          </div>
          <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
            배우고 성장하는 온라인 러닝 스페이스. 강좌, 스터디, 회고, AI 피드백을 한 곳에서.
          </p>
        </div>
        {[
          { title: '학습', links: ['강좌 목록', '진행 중인 스터디', '학습 리포트', '대시보드'] },
          { title: '판매자', links: ['판매자 신청', '강좌 생성', '상품 관리', '판매 분석'] },
          { title: '고객지원', links: ['공지사항', '자주 묻는 질문', '쿠폰/이벤트', '문의하기'] },
        ].map((col) => (
          <div key={col.title}>
            <h4 className="text-sm font-semibold">{col.title}</h4>
            <ul className="mt-3 space-y-2">
              {col.links.map((l) => (
                <li key={l}>
                  <Link href="/" className="text-sm text-muted-foreground hover:text-foreground">
                    {l}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div> */}
      <div className="border-t py-6">
        <p className="mx-auto max-w-7xl px-4 text-xs text-muted-foreground">
          © 2026 PlayLearn.
        </p>
      </div>
    </footer>
  )
}
