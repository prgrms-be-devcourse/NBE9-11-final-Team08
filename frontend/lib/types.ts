export interface Instructor {
  id: string
  name: string
  title: string
  avatarUrl?: string
}

export interface Lecture {
  id: string
  title: string
  duration: string
  progress: number
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
  badges?: string[]
  status?: 'PUBLISHED' | 'DRAFT' | 'REVIEW'
}

export interface CourseCardResponse {
  id: number
  instructorId: number
  categoryId: number
  title: string
  thumbnail: string
  price: number
  viewCount: number
}

export interface LectureInfoResponse {
  id: number
  title: string
  m3u8Path: string | null
  durationSeconds: number
  orderNo: number
  isFreePreview: boolean
}

export interface ChapterInfoResponse {
  id: number
  title: string
  orderNo: number
  lectures: LectureInfoResponse[]
}

export interface CourseDetailResponse {
  id: number
  instructorId: number
  categoryId: number
  title: string
  description: string
  thumbnail: string
  price: number
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED'
  viewCount: number
  chapters: ChapterInfoResponse[]
}

export interface PageResponse<T> {
  content: T[]
  pageable: {
    pageNumber: number
    pageSize: number
  }
  totalElements: number
  totalPages: number
  last: boolean
}

export interface CartItemResponse {
  cartItemId: number
  courseId: number
  title: string
  price: number
}

export interface CartResponse {
  items: CartItemResponse[]
  totalPrice: number
}

export interface OrderItemResponse {
  orderItemId: number
  courseId: number
  courseTitle: string
  price: number
  discountPrice: number
  finalPrice: number
}

export interface OrderDetailResponse {
  orderId: number
  orderNumber: string
  totalPrice: number
  discountPrice: number
  finalPrice: number
  status: string
  orderedAt: string
  canceledAt?: string
  items: OrderItemResponse[]
}

export interface OrderSummaryResponse {
  orderId: number
  orderNumber: string
  orderedAt: string
  status: string
  totalPrice: number
  finalPrice: number
}

export interface Coupon {
  id: string
  name: string
  amount: string
  condition: string
  category: '진행 중인 이벤트' | '종료된 이벤트'
  type: 'discount' | 'attendance' | 'firstcome'
  status?: 'SCHEDULED' | 'ACTIVE' | 'ENDED'
  startDate?: string | null
  endDate?: string | null
  validDays?: number | null
  couponTarget?: string
  usageType?: string
  isStackable?: boolean
  maxDiscountAmount?: number | null
  minOrderAmount?: number | null
  categoryIds?: number[]
  courseIds?: number[]
  totalQuantity?: number | null
}

export type CouponPolicyType = 'NORMAL' | 'FCFS' | 'AUTO'
export type CouponApplyTarget = 'ALL' | 'CATEGORY' | 'COURSE'
export type CouponUseType = 'SINGLE' | 'MULTI'
export type CouponDiscountType = 'AMOUNT' | 'RATE'
export type CouponStatus = 'SCHEDULED' | 'ACTIVE' | 'ENDED'

export interface AdminCoupon {
  id: number
  name: string
  totalQuantity: number | null
  type: CouponPolicyType
  target: CouponApplyTarget
  useType: CouponUseType
  stackable: boolean
  discountType: CouponDiscountType
  discountValue: number
  maxDiscount: number | null
  minOrderAmount: number
  validDays: number | null
  startAt: string
  endAt: string
  status: CouponStatus
  issuedCount: number
  targetCategories?: string[]
  targetCourses?: string[]
}

export interface AdminCouponPolicyResponse {
  id: number
  name: string
  couponTarget: CouponApplyTarget
  couponType: CouponPolicyType
  totalQuantity: number | null
  usageType: CouponUseType | 'SINGLE_USE' | 'MULTI_USE'
  isStackable: boolean
  discountType: CouponDiscountType | 'PERCENT'
  discountValue: number
  maxDiscountAmount: number | null
  minOrderAmount: number | null
  validDays: number | null
  issueStartDate: string | null
  issueEndDate: string | null
  categoryIds: number[]
  courseIds: number[]
}

export interface AdminCouponPolicyRequest {
  name: string
  couponTarget?: CouponApplyTarget
  couponType?: CouponPolicyType
  totalQuantity: number | null
  usageType: CouponUseType
  isStackable: boolean
  discountType: CouponDiscountType
  discountValue: number
  maxDiscountAmount: number | null
  minOrderAmount: number
  validDays: number | null
  issueStartDate: string | null
  issueEndDate: string | null
  categoryIds: number[]
  courseIds: number[]
}

export interface CouponListResponse {
  issuedCouponId: number
  couponName: string
  discountType: string
  discountValue: number
  expiredAt: string
  status: string
  usageType: string
  isStackable: boolean
}

export interface QnaPost {
  id: string
  author: string
  role: 'student' | 'instructor'
  timestamp: string
  content: string
  createdAt: string
  answers?: QnaPost[]
}

export interface QnaAnswerSummary {
  id: number
  authorId: number
  authorName: string
  content: string
  createdAt: string
  updatedAt: string
}

export interface QnaQuestionResponse {
  id: number
  lectureId: number
  authorId: number
  authorName: string
  title: string
  content: string
  createdAt: string
  updatedAt: string
  answers: QnaAnswerSummary[]
}

export interface LectureReflectionResponse {
  id: number
  lectureId: number
  userId: number
  content: string
  createdAt: string
  updatedAt: string
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
  progress: number
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

export type SignupRole = 'USER' | 'SELLER'

export interface SignupRequest {
  email: string
  password: string
  nickname: string
  profileImage?: string
  userRole: SignupRole
}

export interface CourseCreateRequest {
  title: string
  description: string
  categoryId: number
  price: number
  thumbnail: string
}

export interface LectureUpdateRequest {
  id: number | null
  title: string
  durationSeconds: number
  orderNo: number
  isFreePreview: boolean
}

export interface ChapterUpdateRequest {
  id: number | null
  title: string
  orderNo: number
  lectures: LectureUpdateRequest[]
}

export interface CourseUpdateRequest extends CourseCreateRequest {
  chapters: ChapterUpdateRequest[]
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
}

export type StudyStatus = 'DRAFT' | 'ACTIVE' | 'READONLY' | 'INACTIVE'
export type StudyRole = 'owner' | 'member' | 'OWNER' | 'MEMBER'

export interface StudyMember {
  id: string
  name: string
  progress: number
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
  aiFeedback?: string
}

export interface Study {
  id: string
  courseId: string
  name: string
  intro: string
  status: StudyStatus
  ownerName: string
  myRole: StudyRole
  progress: number
  members: StudyMember[]
  applicants: StudyApplicant[]
  announcements: StudyAnnouncement[]
  posts: BoardPost[]
}

export interface StudySummaryResponse {
  studyId: number
  title: string
  description: string
  ownerNickname: string
}

export interface StudyDetailResponse {
  studyId: number
  courseId: number
  title: string
  description: string
  status: string
  ownerNickname: string
  myRole: string
}

export interface StudyActivityResponse {
  activityId: number
  studyId: number
  authorId: number
  content: string
  createdAt: string
}

export interface StructuredFeedback {
  summary: string
  strengths: string
  improvements: string
  additionalStudy: string
}

export interface AiFeedbackResponse {
  id: number
  studyId: number
  activityId: number
  status: 'REQUESTED' | 'GENERATING' | 'COMPLETED' | 'FAILED'
  feedback: StructuredFeedback | null
  createdAt: string
  updatedAt: string
}

export interface LearningEventResponse {
  id: number
  userId: number
  eventType: string
  courseId: number
  chapterId: number | null
  lectureId: number | null
  createdAt: string
}

export interface AttendanceResponse {
  userId: number
  date: string
  continuousDays: number
  totalDays: number
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
