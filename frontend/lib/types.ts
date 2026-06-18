// Domain types shared between the React frontend and the Spring backend (REST API).
// These mirror the JSON DTOs returned by the Java/Spring controllers.

export interface Instructor {
  id: string
  name: string
  title: string
  avatarUrl?: string
}

export interface Lecture {
  id: string
  title: string
  duration: string // e.g. "15:08"
  progress: number // 0 - 100
  completed: boolean
}

export interface Chapter {
  id: string
  title: string
  lectures: Lecture[]
}

export interface Course {
  id: string
  title: string
  subtitle: string
  description: string
  category: string
  subCategory: string
  thumbnailUrl: string
  price: number
  discountRate?: number
  rating: number
  reviewCount: number
  studentCount: number
  level: '왕초보' | '입문' | '초급' | '중급' | '심화'
  tags: string[]
  instructor: Instructor
  chapters: Chapter[]
  badges?: string[] // e.g. ["New", "답변 활발", "미션"]
  status?: 'PUBLISHED' | 'DRAFT' | 'REVIEW'
}

export interface CartItem {
  courseId: string
  title: string
  thumbnailUrl: string
  price: number
}

export interface Coupon {
  id: string
  name: string
  amount: string
  condition: string
  category: '진행 중인 이벤트' | '종료된 이벤트'
  type: 'discount' | 'attendance' | 'firstcome'
}

// --- Admin: coupon policy management ---
// Mirrors the Spring `CouponPolicy` entity / DTO.

export type CouponPolicyType = 'NORMAL' | 'FCFS' | 'AUTO'
export type CouponApplyTarget = 'ALL' | 'CATEGORY' | 'COURSE'
export type CouponUseType = 'SINGLE' | 'MULTI'
export type CouponDiscountType = 'AMOUNT' | 'RATE'
export type CouponStatus = 'SCHEDULED' | 'ACTIVE' | 'ENDED'

export interface AdminCoupon {
  id: number
  name: string
  totalQuantity: number | null // null = 무제한
  type: CouponPolicyType
  target: CouponApplyTarget
  useType: CouponUseType
  stackable: boolean
  discountType: CouponDiscountType
  discountValue: number
  maxDiscount: number | null
  minOrderAmount: number
  validDays: number | null // 발급 후 N일
  startAt: string // ISO datetime
  endAt: string // ISO datetime
  status: CouponStatus
  issuedCount: number
  targetCategories?: string[]
  targetCourses?: string[]
}

export interface QnaPost {
  id: string
  author: string
  role: 'student' | 'instructor'
  timestamp: string // lecture timecode e.g "3:58"
  content: string
  createdAt: string
  answers?: QnaPost[]
}

export interface ActivityFeedItem {
  id: string
  author: string
  date: string
  content: string
  hasAiFeedback?: boolean
  aiFeedback?: string
}

export interface OrderItem {
  courseId: string
  title: string
  instructor: string
  price: number
  status: '정상 구매' | '환불 가능' | '환불 완료' | '부분 환불'
}

export interface Order {
  id: string
  orderNumber: string
  orderedAt: string
  status: '결제 완료' | '부분 환불' | '환불 완료'
  paymentStatus: string
  paymentMethod: string
  items: OrderItem[]
  productTotal: number
  discount: number
  paidAmount: number
  refundAmount: number
}

export interface UserProfile {
  id: string
  name: string
  email: string
  avatarUrl?: string
  studyCount: number
  courseCount: number
  isSeller: boolean
}

export interface EnrolledCourse {
  id: string
  title: string
  instructor: string
  thumbnailUrl: string
  progress: number // 0 - 100
  totalLectures: number
  completedLectures: number
  dday?: number
  status: '진행 중' | '완료'
}

export interface MyComment {
  id: string
  courseTitle: string
  lectureTitle: string
  content: string
  createdAt: string
}

// --- Study room (스터디) ---
// 강좌 1개 = 스터디 1개. 강좌 구매자가 자동 참여하는 학습 공간.

export type StudyStatus = 'DRAFT' | 'ACTIVE' | 'READONLY' | 'INACTIVE'
export type StudyRole = 'owner' | 'member'

export interface StudyMember {
  id: string
  name: string
  progress: number // 0 - 100
  role: StudyRole
  joinedAt: string
}

export interface StudyApplicant {
  id: string
  name: string
  appliedAt: string
}

export interface StudyAnnouncement {
  id: string
  title: string
  content: string
  createdAt: string
}

export interface StudyComment {
  id: string
  author: string
  content: string
  createdAt: string
}

export interface BoardPost {
  id: string
  title: string
  author: string
  content: string
  createdAt: string
  comments: StudyComment[]
  // AI 코치: 학습 활동에 대한 AI 피드백 (작성자 요청 시 생성)
  aiFeedback?: string
}

export interface Study {
  id: string // 강좌 id와 동일
  courseId: string
  name: string
  intro: string
  status: StudyStatus
  ownerName: string
  myRole: StudyRole
  progress: number // 내 진행률
  members: StudyMember[]
  applicants: StudyApplicant[]
  announcements: StudyAnnouncement[]
  posts: BoardPost[]
}

export interface StudyReport {
  studyName: string
  userName: string
  period: string
  totalStudyTime: string
  commentCount: number
  studyDays: number
  progressData: { day: string; progress: number; minutes: number }[]
  calendar: { date: string; active: boolean; level?: number }[]
  topLectures: string[]
}
