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
  chapterId?: string
  durationSeconds?: number
  m3u8Path?: string | null
  summary?: string
  hasVideo?: boolean
  lastPositionSeconds?: number
  watchedSeconds?: number
  isFreePreview?: boolean
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
  status?: 'PUBLISHED' | 'DRAFT' | 'REVIEW' | 'ON_SALE' | 'IN_REVIEW' | 'CLOSED' | 'SUSPENDED' | 'DELETED'
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

export interface CategoryResponse {
  id: number
  name: string
  parentCategoryId: number | null
  depth: number
}

export interface LectureInfoResponse {
  id: number
  title: string
  summary?: string
  m3u8Path: string | null
  durationSeconds: number
  orderNo: number
  isFreePreview: boolean
  hasVideo?: boolean
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
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'IN_REVIEW' | 'ON_SALE' | 'SUSPENDED' | 'DELETED'
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

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'CANCELED'
  | 'REFUNDED'
  | 'EXPIRED'

export type PaymentStatus =
  | 'READY'
  | 'PROCESSING'
  | 'SUCCESS'
  | 'DECLINED'
  | 'UNKNOWN'
  | 'CANCELED'
  | 'REFUNDED'

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
  status: OrderStatus
  orderedAt: string
  canceledAt?: string
  items: OrderItemResponse[]
}

export interface OrderSummaryResponse {
  orderId: number
  orderNumber: string
  orderedAt: string
  status: OrderStatus
  totalPrice: number
  finalPrice: number
}

export interface ConfirmPaymentResponse {
  paymentId: number
  orderId: number
  orderNumber: string
  amount: number
  paymentStatus: PaymentStatus
  orderStatus: OrderStatus
  paidAt?: string | null
  enrolledCourseIds?: number[]
}

export interface ConfirmTossPaymentRequest {
  paymentKey: string
  method: string
  amount: number
  issuedCouponId?: number | null
  idempotencyKey?: string | null
}

export interface PaymentResponse {
  paymentId: number
  orderId: number
  amount: number
  paymentStatus: PaymentStatus
  orderStatus: OrderStatus
  paidAt?: string | null
  failedReason?: string | null
  canceledAt?: string | null
  refundedAt?: string | null
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

export type CouponPolicyType = 'NORMAL' | 'FCFS' | 'AUTO' | 'ADMIN'
export type AutoIssueType = 'SIGNUP' | 'ATTENDANCE_STREAK' | 'MONTHLY_ATTENDANCE'
export type CouponApplyTarget = 'ALL' | 'CATEGORY' | 'COURSE'
export type CouponUseType = 'SINGLE' | 'MULTI'
export type CouponDiscountType = 'AMOUNT' | 'RATE'
export type CouponStatus = 'SCHEDULED' | 'ACTIVE' | 'ENDED'

export interface AdminCoupon {
  id: number
  name: string
  totalQuantity: number | null
  type: CouponPolicyType
  autoIssueType?: AutoIssueType | null
  target: CouponApplyTarget
  useType: CouponUseType
  stackable: boolean
  discountType: CouponDiscountType
  discountValue: number
  maxDiscount: number | null
  minOrderAmount: number | null
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
  autoIssueType?: AutoIssueType | null
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
  autoIssueType?: AutoIssueType | null
  totalQuantity: number | null
  usageType: CouponUseType
  isStackable: boolean
  discountType: CouponDiscountType
  discountValue: number
  maxDiscountAmount: number | null
  minOrderAmount: number | null
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
  maxDiscountAmount: number | null
  minOrderAmount: number | null
  couponTarget: 'ALL' | 'CATEGORY' | 'COURSE'
  categoryIds: number[]
  courseIds: number[]
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

// 백엔드 QnaAnswerSummary(record(id, content, createdAt))와 동일한 형태
export interface QnaAnswerSummary {
  id: number
  content: string
  createdAt: string
}

// 백엔드 QnaQuestionResponse(record)와 동일한 형태. 답변(answer)은 단일이며 없으면 null.
export interface QnaQuestionResponse {
  id: number
  lectureId: number
  userId: number
  title: string
  content: string
  createdAt: string
  updatedAt: string
  answer: QnaAnswerSummary | null
}

export interface LectureReflectionResponse {
  id: number | null
  userId: number | null
  lectureId: number | null
  content: string | null
}

// ── 강의 입장/진행 ──────────────────────────────────────────────
export interface LectureProgressSummary {
  lastPositionSeconds: number
  watchedSeconds: number
  progressRate: number
  completed: boolean
}

export interface LectureEnterResponse {
  lectureId: number
  title: string
  m3u8Path: string | null
  durationSeconds: number
  orderNo: number
  chapterId: number
  progress: LectureProgressSummary | null
}

export interface LectureProgressResponse {
  lectureId: number
  lastPositionSeconds: number
  watchedSeconds: number
  progressRate: number
  completed: boolean
}

export type LearningEventType =
  | 'LECTURE_ENTER'
  | 'VIDEO_PAUSE'
  | 'LECTURE_EXIT'
  | 'LECTURE_COMPLETE'

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
  status: OrderStatus
}

export interface Order {
  id: string
  orderNumber: string
  orderedAt: string
  status: OrderStatus
  paymentStatus: PaymentStatus
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
  isAdmin: boolean
}

export interface EnrolledCourse {
  id: string
  courseId?: string
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
  id: number
  lectureId: number
  courseTitle: string
  lectureTitle: string
  title: string
  content: string
  createdAt: string
  answered: boolean
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
  summary?: string
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

export type StudyStatus = 'DRAFT' | 'ACTIVE' | 'READONLY' | 'INACTIVE'
export type StudyRole = 'owner' | 'member' | 'viewer' | 'OWNER' | 'MEMBER'

export interface StudyMember {
  id: string
  name: string
  avatarUrl?: string | null
  progress: number
  role: StudyRole
  joinedAt: string
}

export interface StudyMemberResponse {
  userId: number
  nickname: string
  profileImage: string | null
  role: 'OWNER' | 'MEMBER'
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
  progressRate: number
  completedLectures: number
  totalLectures: number
}

export interface StudyDetailResponse {
  studyId: number
  courseId: number
  title: string
  description: string
  status: string
  ownerNickname: string
  myRole: string | null
}

export interface StudyIdResponse {
  studyId: number
}

export interface StudyActivityResponse {
  activityId: number
  studyId: number
  authorId: number
  authorNickname: string
  content: string
  createdAt: string
}

export type FeedItemType = 'STUDY_ACTIVITY' | 'LECTURE_ENTER' | 'LECTURE_COMPLETE'
export type BackendDateTime = string | number[]

export interface FeedItemResponse {
  id: number
  studyId: number
  actorId: number
  actorNickname: string
  type: FeedItemType
  sourceId: number
  content: string
  occurredAt: BackendDateTime
}

export interface FeedCursor {
  occurredAt: BackendDateTime
  id: number
}

export interface FeedCursorResponse {
  items: FeedItemResponse[]
  nextCursor: FeedCursor | null
  hasNext: boolean
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
  consecutiveDays: number
  monthlyTotalDays: number
}

export interface AttendanceStatusResponse {
  checkedToday: boolean
  consecutiveDays: number
  monthlyTotalDays: number
  checkedDays: number[]
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

// 마이페이지 리포트 화면용: 화면 리포트 + 스터디 식별자(개별 갱신/구분용)
export interface MyStudyReport extends StudyReport {
  studyId: string
}

// 백엔드 StudyReportResponse(record)와 동일한 형태
export interface StudyReportTopLectureEntry {
  lectureId: number
  title: string
  watchTimeSeconds: number
}

export interface StudyReportDailyProgressEntry {
  date: string
  progressRate: number
}

export interface StudyReportResponse {
  studyId: number
  totalWatchTime: number
  totalQnaCount: number
  progressRate: number
  studyDays: number
  topLectures: StudyReportTopLectureEntry[]
  dailyProgress: StudyReportDailyProgressEntry[]
  dailyActivityMap: Record<string, number>
  updatedAt: string
  status: 'LOADED' | 'REGENERATED' | 'COOLDOWN'
  nextRegenerableAt: string | null
}

// ── 관리자 대시보드 ────────────────────────────────────────────────────
export interface AdminOverview {
  totalUsers: number
  sellerCount: number
  regularUserCount: number
  onSaleCourseCount: number
  activeEnrollmentCount: number
  totalLearningEvents: number
  totalCompletions: number
  todaySessions: number
  todayActiveLearners: number
}

export interface DailySessionPoint {
  date: string
  sessions: number
  distinctLearners: number
}

export interface SellerAnalytics {
  totalRevenue: number
  totalOrders: number
  totalStudents: number
  totalCourses: number
  onSaleCourses: number
  revenueDelta: number
  ordersDelta: number
  monthly: { month: string; revenue: number; orders: number }[]
  categories: { name: string; value: number }[]
  topCourses: { courseId: number; title: string; price: number; studentCount: number; revenue: number }[]
  courseBreakdown: {
    courseId: number
    title: string
    status: string
    price: number
    studentCount: number
    orders: number
    revenue: number
  }[]
}

export interface LectureEngagement {
  lectureId: number
  chapterTitle: string
  title: string
  durationSeconds: number
  enterCount: number
  completeCount: number
  viewerCount: number
  avgWatchSeconds: number
}

export interface SellerCourseDetail {
  courseId: number
  title: string
  status: string
  price: number
  totalRevenue: number
  totalOrders: number
  activeStudents: number
  completions: number
  revenueDelta: number
  ordersDelta: number
  monthly: { month: string; revenue: number; orders: number }[]
  lectures: LectureEngagement[]
}

export interface LecturePauses {
  lectureId: number
  title: string
  durationSeconds: number
  binSeconds: number
  totalPauses: number
  viewerCount: number
  bins: { index: number; startSeconds: number; endSeconds: number; count: number; heat: number }[]
  hotspots: { startSeconds: number; endSeconds: number; count: number; heat: number }[]
}

export interface CourseStatRow {
  courseId: number
  title: string
  instructorId: number
  status: string
  enrollees: number
  enterCount: number
  completionCount: number
  incompletionRate: number
}

export interface LectureStatRow {
  lectureId: number
  chapterTitle: string
  title: string
  enterCount: number
  completeCount: number
  avgWatchSeconds: number
}

export interface EnrolleeRow {
  userId: number
  nickname: string
  completedLectures: number
  totalLectures: number
  progressRate: number
  lastEventTime: string | null
}

export interface LearningEventRow {
  id: number
  userId: number
  courseId: number | null
  chapterId: number | null
  lectureId: number | null
  eventType: string
  positionSeconds: number | null
  eventTime: string
}

export interface DuplicateBurst {
  userId: number
  lectureId: number
  eventType: string
  bucketMinute: string
  count: number
}

export interface AnomalyResponse {
  incompletionThreshold: number
  burstThreshold: number
  windowMinutes: number
  highIncompletionCourses: CourseStatRow[]
  duplicateBursts: DuplicateBurst[]
}

export interface AuditResponse {
  retention: {
    learningEventCount: number
    oldestEventTime: string | null
    newestEventTime: string | null
    courseStatusHistoryCount: number
    lectureProgressCount: number
  }
  accessHistory: {
    source: string
    description: string
    actorId: number | null
    occurredAt: string | null
  }[]
  integrityErrors: {
    type: string
    description: string
    count: number
    sampleIds: number[]
  }[]
}

export interface CouponIssueUsersRequest {
  requestKey: string
  userIds: number[]
}

export interface CouponIssueAllUsersRequest {
  requestKey: string
}

export type CouponIssueRequestType = 'TARGETED' | 'ALL_USERS'
export type CouponIssueRequestStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'

export interface CouponIssueRequestResponse {
  id: number
  policyId: number
  requestKey: string
  issueType: CouponIssueRequestType
  status: CouponIssueRequestStatus
  requestedCount: number
  successCount: number
  failedCount: number
  skippedCount: number
  targetUserMaxId: number | null
  requestedBy: number
  requestedAt: string | number[]
  startedAt: string | number[] | null
  completedAt: string | number[] | null
  failureReason: string | null
}
