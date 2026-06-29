// frontend/lib/api.ts
import type {
  AdminCoupon,
  BoardPost,
  Coupon,
  Course,
  CourseCardResponse,
  CategoryResponse,
  CourseDetailResponse,
  ChapterInfoResponse,
  PageResponse,
  CartResponse,
  CartItemResponse,
  OrderDetailResponse,
  ConfirmPaymentResponse,
  ConfirmTossPaymentRequest,
  PaymentResponse,
  EnrolledCourse,
  MyComment,
  QnaPost,
  Study,
  StudyDetailResponse,
  StudyMember,
  StudyMemberResponse,
  StudyIdResponse,
  StudyReport,
  MyStudyReport,
  UserProfile,
  SignupRequest,
  LoginRequest,
  StudySummaryResponse,
  StudyActivityResponse,
  AiFeedbackResponse,
  AttendanceResponse,
  AttendanceStatusResponse,
  CouponListResponse,
  CourseCreateRequest,
  CourseUpdateRequest,
  AdminCouponPolicyRequest,
  AdminCouponPolicyResponse,
  LectureEnterResponse,
  LectureProgressResponse,
  LearningEventType,
  LearningEventResponse,
  QnaQuestionResponse,
  StudyReportResponse,
  AdminOverview,
  DailySessionPoint,
  CourseStatRow,
  LectureStatRow,
  EnrolleeRow,
  LearningEventRow,
  AnomalyResponse,
  AuditResponse,
  SellerAnalytics,
  SellerCourseDetail,
  LecturePauses,
  FeedCursor,
  FeedCursorResponse,
  CouponIssueRequestResponse,
} from './types'

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'
const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN'
const REFRESH_EXCLUDED_PATHS = new Set([
  '/api/auth/csrf',
  '/api/auth/login',
  '/api/auth/logout',
  '/api/auth/refresh',
  '/api/auth/signup',
  '/api/auth/seller/signup',
])

let refreshAuthPromise: Promise<boolean> | null = null
let csrfFetchPromise: Promise<string | undefined> | null = null

const decodeCookieValue = (value: string): string => {
  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
}

const getCookieValue = async (name: string): Promise<string | undefined> => {
  if (typeof window !== 'undefined') {
    const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
    return match ? decodeCookieValue(match[1]) : undefined
  }

  try {
    const { cookies } = await import('next/headers')
    const cookieStore = await cookies()
    const value = cookieStore.get(name)?.value
    return value ? decodeCookieValue(value) : undefined
  } catch {
    return undefined
  }
}

const getAuthHeaders = async (): Promise<Record<string, string>> => {
  if (typeof window !== 'undefined') {
    return {}
  }

  try {
    const { cookies } = await import('next/headers')
    const cookieStore = await cookies()
    const cookieHeader = cookieStore.toString()

    return cookieHeader ? { Cookie: cookieHeader } : {}
  } catch {
    return {}
  }
}

const getCredentialHeaders = async (includeCredentials: boolean): Promise<Record<string, string>> => {
  if (!includeCredentials) {
    return {}
  }

  return getAuthHeaders()
}

const getCredentialsMode = (includeCredentials: boolean): RequestCredentials => {
  return includeCredentials ? 'include' : 'same-origin'
}

const toIsoLocalDateTime = (value: string | number[]): string => {
  if (!Array.isArray(value)) {
    return value
  }

  const [year, month = 1, day = 1, hour = 0, minute = 0, second = 0, nano = 0] = value
  const millisecond = Math.floor(nano / 1_000_000)
  const pad = (num: number, length = 2) => String(num).padStart(length, '0')

  return `${pad(year, 4)}-${pad(month)}-${pad(day)}T${pad(hour)}:${pad(minute)}:${pad(second)}.${pad(millisecond, 3)}`
}

const ensureCsrfToken = async (includeCredentials: boolean): Promise<string | undefined> => {
  if (!includeCredentials || !BASE_URL) {
    return undefined
  }

  const currentToken = await getCookieValue(CSRF_COOKIE_NAME)
  if (currentToken) {
    return currentToken
  }

  if (typeof window === 'undefined') {
    return undefined
  }

  // 쿠키가 없을 때만 발급한다. 콜드스타트에 mutation 이 동시에 나가면
  // 발급이 중복되어 토큰이 매번 갱신되고 쿠키-헤더가 어긋날 수 있으므로,
  // refreshAuthPromise 와 동일하게 in-flight 요청을 하나로 합친다.
  if (!csrfFetchPromise) {
    csrfFetchPromise = (async () => {
      await fetch(`${BASE_URL}/api/auth/csrf`, {
        method: 'GET',
        credentials: 'include',
        cache: 'no-store',
      }).catch(() => undefined)

      return getCookieValue(CSRF_COOKIE_NAME)
    })().finally(() => {
      csrfFetchPromise = null
    })
  }

  return csrfFetchPromise
}

const handleUnauthorized = () => {
  if (typeof window !== 'undefined') {
    const pathname = window.location.pathname
    const isPublicRoute =
      pathname === '/' ||
      pathname.startsWith('/courses') ||
      pathname.startsWith('/study') ||
      pathname === '/events' ||
      pathname === '/login' ||
      pathname === '/signup' ||
      pathname === '/seller/signup' ||
      pathname === '/cart'

    if (!isPublicRoute) {
      window.location.href = '/login'
    }
  }
}

const shouldAttemptTokenRefresh = (path: string, includeCredentials: boolean, status: number): boolean => {
  return status === 401
    && includeCredentials
    && typeof window !== 'undefined'
    && !REFRESH_EXCLUDED_PATHS.has(path)
}

const refreshAuthTokens = async (): Promise<boolean> => {
  if (!BASE_URL || typeof window === 'undefined') {
    return false
  }

  if (!refreshAuthPromise) {
    refreshAuthPromise = (async () => {
      const headers: Record<string, string> = {}
      const csrfToken = await ensureCsrfToken(true)
      if (csrfToken) {
        headers[CSRF_HEADER_NAME] = csrfToken
      }

      const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
        method: 'POST',
        headers,
        credentials: 'include',
        cache: 'no-store',
      }).catch(() => null)

      return res?.ok ?? false
    })().finally(() => {
      refreshAuthPromise = null
    })
  }

  return refreshAuthPromise
}

async function request<T>(
  path: string,
  defaultData: T,
  includeAuth = true,
  handleAuthError = true,
): Promise<T> {
  if (!BASE_URL) return defaultData
  try {
    const fetchRequest = async () => {
      // GET 은 백엔드 CsrfFilter 가 검증하지 않으므로 CSRF 토큰을 붙이지 않는다.
      const authHeaders = await getCredentialHeaders(includeAuth)
      return fetch(`${BASE_URL}${path}`, {
        headers: {
          'Content-Type': 'application/json',
          ...authHeaders,
        },
        credentials: getCredentialsMode(includeAuth),
        cache: 'no-store',
      })
    }

    let res = await fetchRequest()
    if (shouldAttemptTokenRefresh(path, includeAuth, res.status) && await refreshAuthTokens()) {
      res = await fetchRequest()
    }

    if (!res.ok) {
      console.warn(`[API 에러] ${res.status} on ${path}`)
      if (res.status === 401 && handleAuthError) {
        handleUnauthorized()
      }
      return defaultData
    }

    return await res.json() as T
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    console.warn(`[API 통신 실패] ${path}: ${message}`)
    return defaultData
  }
}

async function requestText(
  path: string,
  includeAuth = true,
  handleAuthError = true,
): Promise<string> {
  if (!BASE_URL) return ''
  try {
    const fetchRequest = async () => {
      // GET 은 백엔드 CsrfFilter 가 검증하지 않으므로 CSRF 토큰을 붙이지 않는다.
      const authHeaders = await getCredentialHeaders(includeAuth)
      return fetch(`${BASE_URL}${path}`, {
        headers: {
          ...authHeaders,
        },
        credentials: getCredentialsMode(includeAuth),
        cache: 'no-store',
      })
    }

    let res = await fetchRequest()
    if (shouldAttemptTokenRefresh(path, includeAuth, res.status) && await refreshAuthTokens()) {
      res = await fetchRequest()
    }

    if (!res.ok) {
      console.warn(`[API 에러] ${res.status} on ${path}`)
      if (res.status === 401 && handleAuthError) {
        handleUnauthorized()
      }
      return ''
    }

    return await res.text()
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    console.warn(`[API 통신 실패] ${path}: ${message}`)
    return ''
  }
}


async function mutate<T>(
  path: string,
  method: string,
  body?: any,
  isMultipart = false,
  includeCredentials = true,
): Promise<T> {
  if (!BASE_URL) throw new Error('API BASE_URL is not defined')

  const headers = await getCredentialHeaders(includeCredentials)
  if (!isMultipart) {
    headers['Content-Type'] = 'application/json'
  }
  const csrfToken = await ensureCsrfToken(includeCredentials)
  if (csrfToken) {
    headers[CSRF_HEADER_NAME] = csrfToken
  }

  const options: RequestInit = {
    method,
    headers,
  }
  options.credentials = getCredentialsMode(includeCredentials)
  if (body) {
    options.body = isMultipart ? body : JSON.stringify(body)
  }

  let res: Response
  try {
    res = await fetch(`${BASE_URL}${path}`, options)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    console.warn(`[API 통신 실패] ${path}: ${message}`)
    throw new Error('API 서버에 연결할 수 없습니다. 백엔드 서버가 실행 중인지 확인해주세요.')
  }

  if (!res.ok) {
    if (shouldAttemptTokenRefresh(path, includeCredentials, res.status) && await refreshAuthTokens()) {
      res = await fetch(`${BASE_URL}${path}`, options)
    }

    if (!res.ok) {
      if (res.status === 401) {
        handleUnauthorized()
      }
      const errorData = await res.json().catch(() => ({}))
      throw new Error(errorData.message || errorData.detail || errorData.error || `Error ${res.status}`)
    }
  }
  if (res.status === 204 || res.status === 202) {
    return {} as T
  }
  if (res.status === 201 && res.headers.get('content-length') === '0') {
    return {} as T
  }
  const text = await res.text()
  if (!text.trim()) {
    return {} as T
  }
  return JSON.parse(text) as T
}

const mapCourseCardToCourse = (card: CourseCardResponse): Course => ({
  id: card.id?.toString() || '0',
  title: card.title || '',
  subtitle: '',
  description: '',
  category: card.categoryId?.toString() || '0',
  subCategory: '',
  thumbnailUrl: card.thumbnail || '/placeholder.svg',
  price: card.price || 0,
  viewCount: card.viewCount || 0,
  instructor: { id: card.instructorId?.toString() || '0', name: `강사 ${card.instructorId || ''}`, title: '' },
  chapters: [],
})

const mapCourseDetailToCourse = (detail: CourseDetailResponse): Course => ({
  id: detail.id?.toString() || '0',
  title: detail.title || '',
  subtitle: '',
  description: detail.description || '',
  category: detail.categoryId?.toString() || '0',
  subCategory: '',
  thumbnailUrl: detail.thumbnail || '/placeholder.svg',
  price: detail.price || 0,
  viewCount: detail.viewCount || 0,
  instructor: { id: detail.instructorId?.toString() || '0', name: `강사 ${detail.instructorId || ''}`, title: '' },
  chapters: detail.chapters?.map((ch) => ({
    id: ch.id.toString(),
    title: ch.title,
    lectures: ch.lectures?.map((lec) => ({
      id: lec.id.toString(),
      title: lec.title,
      duration: `${Math.floor(lec.durationSeconds / 60)}:${(lec.durationSeconds % 60).toString().padStart(2, '0')}`,
      progress: 0,
      completed: false,
      chapterId: ch.id.toString(),
      durationSeconds: lec.durationSeconds,
      m3u8Path: lec.m3u8Path ?? null,
      summary: lec.summary ?? '',
      hasVideo: lec.hasVideo ?? false,
      isFreePreview: lec.isFreePreview ?? false,
    })) || [],
  })) || [],
  status: detail.status,
})

const mapStudyDetailToStudy = (
  detail: StudyDetailResponse,
  posts: StudyActivityResponse[] = [],
): Study => ({
  id: detail.studyId.toString(),
  courseId: detail.courseId.toString(),
  name: detail.title,
  intro: detail.description,
  status: detail.status as Study['status'],
  ownerName: detail.ownerNickname,
  myRole: (detail.myRole?.toLowerCase() ?? 'viewer') as Study['myRole'],
  progress: 0,
  // 멤버 목록은 별도 엔드포인트(/api/studies/{id}/members)에서 조회한다.
  members: [],
  applicants: [],
  announcements: [],
  posts: posts.map((post) => ({
    id: post.activityId?.toString() || '0',
    title: post.content.split('\n')[0] || '학습 활동',
    author: post.authorId?.toString() || '',
    content: post.content,
    createdAt: post.createdAt,
    comments: [],
  })),
})

const mapStudyMemberResponseToMember = (
  member: StudyMemberResponse,
): StudyMember => ({
  id: member.userId.toString(),
  name: member.nickname,
  avatarUrl: member.profileImage,
  progress: 0,
  role: member.role.toLowerCase() as StudyMember['role'],
  joinedAt: parseDateToString(member.joinedAt),
})

const mapUseTypeToBackend = (useType: string): any => {
  if (useType === 'SINGLE') return 'SINGLE_USE'
  if (useType === 'MULTI') return 'MULTI_USE'
  return useType
}

const mapUseTypeToFrontend = (useType: string): any => {
  if (useType === 'SINGLE_USE') return 'SINGLE'
  if (useType === 'MULTI_USE') return 'MULTI'
  return useType
}

const mapDiscountTypeToBackend = (discountType: string): any => {
  if (discountType === 'RATE') return 'PERCENT'
  return discountType
}

const mapDiscountTypeToFrontend = (discountType: string): any => {
  if (discountType === 'PERCENT') return 'RATE'
  return discountType
}

const parseDateToString = (value: any): string => {
  if (!value) return ''
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${year}-${pad(month)}-${pad(day)}T${pad(hour)}:${pad(minute)}:${pad(second)}`
  }
  if (typeof value === 'string') {
    return value
  }
  if (value instanceof Date) {
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
  }
  return String(value)
}

const toLocalDateTime = (value: any) => {
  const str = parseDateToString(value)
  return str ? str.slice(0, 16) : ''
}

const mapCouponListToCoupon = (coupon: CouponListResponse): Coupon => {
  const isRate = coupon.discountType === 'RATE' || coupon.discountType === 'PERCENT'
  const isAvailable = coupon.status === 'ISSUED'
  const category = isAvailable ? '진행 중인 이벤트' : '종료된 이벤트'
  const expiredAtStr = parseDateToString(coupon.expiredAt)
  const usageTypeStr = coupon.usageType === 'MULTI' || coupon.usageType === 'MULTI_USE' ? '다회용' : '1회용'

  let couponTargetStr = '전체 적용'
  if (coupon.couponTarget === 'CATEGORY') {
    couponTargetStr = coupon.categoryIds && coupon.categoryIds.length > 0
      ? `특정 카테고리 (${coupon.categoryIds.length}개)`
      : '특정 카테고리 적용'
  } else if (coupon.couponTarget === 'COURSE') {
    couponTargetStr = coupon.courseIds && coupon.courseIds.length > 0
      ? `특정 강좌 (${coupon.courseIds.length}개)`
      : '특정 강좌 적용'
  }

  return {
    id: coupon.issuedCouponId.toString(),
    name: coupon.couponName,
    amount: isRate
      ? `${coupon.discountValue}% 할인`
      : `${coupon.discountValue.toLocaleString()}원 할인`,
    condition: expiredAtStr ? `${expiredAtStr.slice(0, 10).replace(/-/g, '.')}까지` : coupon.status,
    category: category,
    type: isRate ? 'discount' : 'firstcome',
    status: isAvailable ? 'ACTIVE' : 'ENDED',
    usageType: usageTypeStr,
    isStackable: coupon.isStackable,
    maxDiscountAmount: coupon.maxDiscountAmount,
    minOrderAmount: coupon.minOrderAmount,
    couponTarget: couponTargetStr,
    categoryIds: coupon.categoryIds || [],
    courseIds: coupon.courseIds || [],
  }
}

const inferCouponStatus = (startAt: string, endAt: string): AdminCoupon['status'] => {
  const now = Date.now()
  const start = startAt ? new Date(startAt).getTime() : Number.NEGATIVE_INFINITY
  const end = endAt ? new Date(endAt).getTime() : Number.POSITIVE_INFINITY

  if (Number.isFinite(start) && now < start) return 'SCHEDULED'
  if (Number.isFinite(end) && now > end) return 'ENDED'
  return 'ACTIVE'
}

const mapAdminCouponPolicyToUserCoupon = (policy: AdminCouponPolicyResponse): Coupon => {
  const isRate = policy.discountType === 'PERCENT' || policy.discountType === 'RATE'
  const startAt = toLocalDateTime(policy.issueStartDate)
  const endAt = toLocalDateTime(policy.issueEndDate)

  const isExhausted = policy.totalQuantity !== null && policy.totalQuantity !== undefined && policy.totalQuantity <= 0
  const status = isExhausted ? 'ENDED' : inferCouponStatus(startAt, endAt)
  const category = (status === 'ACTIVE' || status === 'SCHEDULED') ? '진행 중인 이벤트' : '종료된 이벤트'

  let condition = ''
  if (policy.validDays) {
    condition = `발급 후 ${policy.validDays}일`
  } else if (endAt) {
    condition = `${endAt.slice(0, 10).replace(/-/g, '.')}까지`
  } else {
    condition = '무기한'
  }

  let type: Coupon['type'] = 'discount'
  if (policy.couponType === 'FCFS') {
    type = 'firstcome'
  }

  let couponTargetStr = '전체 적용'
  if (policy.couponTarget === 'CATEGORY') {
    couponTargetStr = policy.categoryIds && policy.categoryIds.length > 0
      ? `특정 카테고리 (${policy.categoryIds.length}개)`
      : '특정 카테고리 적용'
  } else if (policy.couponTarget === 'COURSE') {
    couponTargetStr = policy.courseIds && policy.courseIds.length > 0
      ? `특정 강좌 (${policy.courseIds.length}개)`
      : '특정 강좌 적용'
  }

  const usageTypeStr = policy.usageType === 'MULTI' || policy.usageType === 'MULTI_USE' ? '다회용' : '1회용'

  return {
    id: policy.id.toString(),
    name: policy.name,
    amount: isRate
      ? `${policy.discountValue}% 할인`
      : `${policy.discountValue.toLocaleString()}원 할인`,
    condition,
    category,
    type,
    status,
    startDate: startAt,
    endDate: endAt,
    validDays: policy.validDays,
    couponTarget: couponTargetStr,
    usageType: usageTypeStr,
    isStackable: policy.isStackable,
    maxDiscountAmount: policy.maxDiscountAmount,
    minOrderAmount: policy.minOrderAmount,
    categoryIds: policy.categoryIds || [],
    courseIds: policy.courseIds || [],
    totalQuantity: policy.totalQuantity,
  }
}

const mapAdminCouponPolicyToCoupon = (policy: AdminCouponPolicyResponse): AdminCoupon => {
  const startAt = toLocalDateTime(policy.issueStartDate)
  const endAt = toLocalDateTime(policy.issueEndDate)
  const isExhausted = policy.totalQuantity !== null && policy.totalQuantity !== undefined && policy.totalQuantity <= 0
  const status = isExhausted ? 'ENDED' : inferCouponStatus(startAt, endAt)

  return {
    id: policy.id,
    name: policy.name,
    totalQuantity: policy.totalQuantity ?? null,
    type: policy.couponType,
    autoIssueType: policy.autoIssueType ?? null,
    target: policy.couponTarget,
    useType: mapUseTypeToFrontend(policy.usageType),
    stackable: policy.isStackable,
    discountType: mapDiscountTypeToFrontend(policy.discountType),
    discountValue: policy.discountValue,
    maxDiscount: policy.maxDiscountAmount ?? null,
    minOrderAmount: policy.minOrderAmount ?? null,
    validDays: policy.validDays ?? null,
    startAt,
    endAt,
    status,
    issuedCount: 0,
    targetCategories: policy.categoryIds?.map(String) ?? [],
    targetCourses: policy.courseIds?.map(String) ?? [],
  }
}

const mapAdminCouponToPolicyRequest = (coupon: AdminCoupon): AdminCouponPolicyRequest => ({
  name: coupon.name,
  couponTarget: coupon.target,
  couponType: coupon.type,
  autoIssueType: coupon.type === 'AUTO' ? (coupon.autoIssueType ?? null) : null,
  totalQuantity: coupon.totalQuantity,
  usageType: mapUseTypeToBackend(coupon.useType),
  isStackable: coupon.stackable,
  discountType: mapDiscountTypeToBackend(coupon.discountType),
  discountValue: coupon.discountValue,
  maxDiscountAmount: coupon.maxDiscount,
  minOrderAmount: coupon.minOrderAmount,
  validDays: coupon.validDays,
  issueStartDate: coupon.startAt || null,
  issueEndDate: coupon.endAt || null,
  categoryIds: coupon.target === 'CATEGORY'
    ? (coupon.targetCategories ?? []).map(Number).filter(Number.isFinite)
    : [],
  courseIds: coupon.target === 'COURSE'
    ? (coupon.targetCourses ?? []).map(Number).filter(Number.isFinite)
    : [],
})

const SEEDED_COURSES: Record<number, { title: string; description: string; price: number; categoryId: number; instructorId: number; thumbnail: string; status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' }> = {
  1: {
    title: '강의 1 - 프론트엔드',
    description: '강의 1에 대한 설명입니다.',
    price: 20000,
    categoryId: 5,
    instructorId: 2,
    thumbnail: 'thumb1.jpg',
    status: 'PUBLISHED',
  },
  2: {
    title: '강의 2 - DevOps',
    description: '강의 2에 대한 설명입니다.',
    price: 30000,
    categoryId: 6,
    instructorId: 2,
    thumbnail: 'thumb2.jpg',
    status: 'PUBLISHED',
  },
  3: {
    title: '강의 3 - UI/UX',
    description: '강의 3에 대한 설명입니다.',
    price: 40000,
    categoryId: 7,
    instructorId: 3,
    thumbnail: 'thumb3.jpg',
    status: 'DRAFT',
  },
  4: {
    title: '강의 4 - 마케팅',
    description: '강의 4에 대한 설명입니다.',
    price: 50000,
    categoryId: 8,
    instructorId: 3,
    thumbnail: 'thumb4.jpg',
    status: 'PUBLISHED',
  },
  5: {
    title: '1234',
    description: '1231412412',
    price: 123124,
    categoryId: 1,
    instructorId: 8,
    thumbnail: 'https://via.placeholder.com/800x450',
    status: 'DRAFT',
  }
}

const getCourseDraftFromCookies = async (id: string | number): Promise<any | null> => {
  const cookieName = `course_draft_${id}`
  if (typeof window !== 'undefined') {
    const match = document.cookie.match(new RegExp(`(^| )${cookieName}=([^;]+)`))
    if (match) {
      try {
        return JSON.parse(decodeURIComponent(match[2]))
      } catch (e) { }
    }
    try {
      const local = localStorage.getItem(cookieName)
      if (local) return JSON.parse(local)
    } catch (e) { }
  } else {
    try {
      const { cookies } = await import('next/headers')
      const cookieStore = cookies()
      const store = cookieStore instanceof Promise ? await cookieStore : cookieStore
      const val = store.get(cookieName)?.value
      if (val) {
        return JSON.parse(decodeURIComponent(val))
      }
    } catch (e) { }
  }
  return null
}

const saveCourseDraft = (id: string | number, data: any) => {
  if (typeof window !== 'undefined') {
    const cookieName = `course_draft_${id}`
    const jsonStr = JSON.stringify(data)
    document.cookie = `${cookieName}=${encodeURIComponent(jsonStr)}; path=/; max-age=${7 * 24 * 60 * 60}`
    try {
      localStorage.setItem(cookieName, jsonStr)
      const draftIds = JSON.parse(localStorage.getItem('course_draft_ids') || '[]')
      const nextDraftIds = Array.from(new Set([...draftIds, String(id)]))
      localStorage.setItem('course_draft_ids', JSON.stringify(nextDraftIds))
    } catch (e) { }
  }
}

const removeCourseDraft = (id: string | number) => {
  if (typeof window !== 'undefined') {
    const cookieName = `course_draft_${id}`
    document.cookie = `${cookieName}=; path=/; max-age=0`
    try {
      localStorage.removeItem(cookieName)
      const draftIds = JSON.parse(localStorage.getItem('course_draft_ids') || '[]')
      const nextDraftIds = draftIds.filter((dId: string) => dId !== String(id))
      localStorage.setItem('course_draft_ids', JSON.stringify(nextDraftIds))
    } catch (e) { }
  }
}

const formatWatchTime = (totalSeconds: number): string => {
  if (!totalSeconds || totalSeconds <= 0) return '0분'
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  if (hours > 0) return `${hours}시간 ${minutes}분`
  return `${minutes}분`
}

// 백엔드 StudyReportResponse 를 화면용 StudyReport(studyName/userName 제외)로 변환한다.
const mapStudyReportToDisplay = (
  raw: StudyReportResponse,
): Omit<StudyReport, 'studyName' | 'userName'> => {
  // 백엔드가 LocalDate를 [년,월,일] 배열로 직렬화하므로(WRITE_DATES_AS_TIMESTAMPS),
  // 배열/문자열 양쪽 모두에서 MM-DD를 안전하게 뽑아낸다.
  const toMonthDay = (date: unknown): string => {
    if (Array.isArray(date)) {
      const [, month, day] = date as number[]
      if (month == null || day == null) return ''
      const pad = (n: number) => String(n).padStart(2, '0')
      return `${pad(month)}-${pad(day)}`
    }
    return typeof date === 'string' ? date.slice(5) : ''
  }

  const progressData = (raw.dailyProgress ?? []).map((d) => ({
    day: toMonthDay(d.date), // MM-DD
    progress: Number(d.progressRate ?? 0),
    minutes: 0,
  }))

  const activity = raw.dailyActivityMap ?? {}
  const activeDates = Object.keys(activity).sort()
  let calendar: StudyReport['calendar'] = []
  if (activeDates.length > 0) {
    const start = new Date(activeDates[0])
    const end = new Date()
    const max = Math.max(...Object.values(activity), 1)
    for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
      const key = d.toISOString().slice(0, 10)
      const value = activity[key] ?? 0
      calendar.push({
        date: key,
        active: value > 0,
        level: value > 0 ? Math.min(3, Math.ceil((value / max) * 3)) : 0,
      })
    }
  }

  let period = '기능 없음'
  if (activeDates.length > 0) {
    period = `${activeDates[0]} ~ ${activeDates[activeDates.length - 1]}`
  }

  return {
    period,
    totalStudyTime: formatWatchTime(raw.totalWatchTime ?? 0),
    commentCount: raw.totalQnaCount ?? 0,
    studyDays: raw.studyDays ?? 0,
    progressData,
    calendar,
    topLectures: (raw.topLectures ?? []).map((l) => l.title),
  }
}

// 여러 스터디의 원본 리포트를 하나로 합산한 가상 StudyReportResponse 를 만든다.
// 합산 의미: 총시청시간/QnA수는 단순 합, 학습일수는 날짜 합집합, 일별 활동은 날짜별 합,
// 일별 진도는 날짜별 평균(전체 진척 추세), top 강의는 시청시간 기준 상위 5개.
const buildAggregateReportRaw = (
  raws: StudyReportResponse[],
): StudyReportResponse => {
  const dailyActivityMap: Record<string, number> = {}
  for (const r of raws) {
    for (const [date, count] of Object.entries(r.dailyActivityMap ?? {})) {
      dailyActivityMap[date] = (dailyActivityMap[date] ?? 0) + (count ?? 0)
    }
  }

  // 날짜별 진도율 평균
  const progressSum: Record<string, { sum: number; n: number }> = {}
  for (const r of raws) {
    for (const d of r.dailyProgress ?? []) {
      const key = String(d.date)
      const acc = progressSum[key] ?? { sum: 0, n: 0 }
      acc.sum += Number(d.progressRate ?? 0)
      acc.n += 1
      progressSum[key] = acc
    }
  }
  const dailyProgress = Object.entries(progressSum)
    .map(([date, { sum, n }]) => ({ date, progressRate: n > 0 ? sum / n : 0 }))

  const topLectures = raws
    .flatMap((r) => r.topLectures ?? [])
    .sort((a, b) => (b.watchTimeSeconds ?? 0) - (a.watchTimeSeconds ?? 0))
    .slice(0, 5)

  const studyDays = Object.values(dailyActivityMap).filter((v) => v > 0).length
  const progressValues = raws.map((r) => Number(r.progressRate ?? 0))
  const progressRate = progressValues.length
    ? progressValues.reduce((a, b) => a + b, 0) / progressValues.length
    : 0

  return {
    studyId: 0,
    totalWatchTime: raws.reduce((a, r) => a + (r.totalWatchTime ?? 0), 0),
    totalQnaCount: raws.reduce((a, r) => a + (r.totalQnaCount ?? 0), 0),
    progressRate,
    studyDays,
    topLectures,
    dailyProgress,
    dailyActivityMap,
    updatedAt: raws.map((r) => r.updatedAt).sort().at(-1) ?? '',
    status: 'LOADED',
    nextRegenerableAt: null,
  }
}

const getCurrentUserId = async (): Promise<number> => {
  try {
    const profile = await api.getProfile()
    return Number(profile?.id || 1)
  } catch (e) {
    return 1
  }
}

export const api = {
  // Auth
  signup: (data: SignupRequest) => mutate<void>('/api/auth/signup', 'POST', data, false, true),
  sellerSignup: (data: SignupRequest) => mutate<void>('/api/auth/seller/signup', 'POST', data, false, true),
  login: (data: LoginRequest) => mutate<void>('/api/auth/login', 'POST', data, false, true),
  logout: async () => {
    return mutate<void>('/api/auth/logout', 'POST', undefined, false, true)
  },

  // Courses
  getCourses: async (page = 0, size = 10, sort = 'VIEW_DESC'): Promise<PageResponse<Course>> => {
    const res = await request<PageResponse<CourseCardResponse>>(
      `/api/courses?page=${page}&size=${size}&sort=${sort}`,
      { content: [], pageable: { pageNumber: page, pageSize: size }, totalElements: 0, totalPages: 0, last: true },
      false,
      false,
    )
    return {
      ...res,
      content: res.content ? res.content.map(c => mapCourseCardToCourse(c)) : []
    }
  },

  // Categories
  getCategories: () => request<CategoryResponse[]>('/api/categories', [], false, false),

  getCourse: async (id: string | number): Promise<Course | undefined> => {
    const detail = await request<CourseDetailResponse | undefined>(
      `/api/courses/${id}`,
      undefined,
      true,
      false,
    )
    if (detail && detail.id) return mapCourseDetailToCourse(detail)

    const chapters = await request<ChapterInfoResponse[]>(
      `/api/courses/${id}/chapters`,
      [],
      true,
      false,
    )

    let generalInfo = SEEDED_COURSES[Number(id)]
    if (!generalInfo) {
      generalInfo = await getCourseDraftFromCookies(id)
    }

    if (!generalInfo) {
      try {
        const coursesPage = await api.getCourses(0, 100)
        const found = coursesPage.content.find(c => c.id.toString() === id.toString())
        if (found) {
          generalInfo = {
            title: found.title,
            description: found.description || `${found.title}에 대한 설명입니다.`,
            price: found.price,
            categoryId: Number(found.category),
            instructorId: Number(found.instructor.id),
            thumbnail: found.thumbnailUrl,
            status: 'PUBLISHED'
          }
        }
      } catch (e) {
        console.error('Failed to find course in public list:', e)
      }
    }

    if (!generalInfo) {
      generalInfo = {
        title: `강좌 ${id}`,
        description: `강좌 ${id}에 대한 설명입니다.`,
        price: 0,
        categoryId: 1,
        instructorId: 1,
        thumbnail: 'https://via.placeholder.com/800x450',
        status: 'PUBLISHED'
      }
    }

    const detailResponse: CourseDetailResponse = {
      id: Number(id),
      instructorId: generalInfo.instructorId || 1,
      categoryId: generalInfo.categoryId || 1,
      title: generalInfo.title || '',
      description: generalInfo.description || '',
      thumbnail: generalInfo.thumbnail || '',
      price: generalInfo.price || 0,
      status: generalInfo.status || 'PUBLISHED',
      viewCount: 0,
      chapters: chapters
    }

    return mapCourseDetailToCourse(detailResponse)
  },

  createCourse: async (formData: FormData) => {
    const courseId = await mutate<number>('/api/courses', 'POST', formData, true)
    if (courseId) {
      const instructorId = await getCurrentUserId()

      const requestBlob = formData.get('request') as Blob
      const requestData = requestBlob ? JSON.parse(await requestBlob.text()) : {}

      saveCourseDraft(courseId, {
        instructorId,
        categoryId: Number(requestData.categoryId),
        title: requestData.title || '',
        description: requestData.description || '',
        thumbnail: requestData.thumbnail || '',
        price: Number(requestData.price),
        status: 'DRAFT'
      })
    }
    return courseId
  },

  updateCourse: async (courseId: string | number, formData: FormData) => {
    const res = await mutate<void>(`/api/courses/${courseId}`, 'PUT', formData, true)
    const instructorId = await getCurrentUserId()

    const requestBlob = formData.get('request') as Blob
    const requestData = requestBlob ? JSON.parse(await requestBlob.text()) : {}

    saveCourseDraft(courseId, {
      instructorId,
      categoryId: Number(requestData.categoryId),
      title: requestData.title || '',
      description: requestData.description || '',
      thumbnail: requestData.thumbnail || '',
      price: Number(requestData.price),
      status: 'DRAFT'
    })
    return res
  },

  requestCourseReview: async (courseId: string | number) => {
    const res = await mutate<void>(`/api/courses/${courseId}/reviews`, 'POST')
    const draft = await getCourseDraftFromCookies(courseId)
    if (draft) {
      saveCourseDraft(courseId, { ...draft, status: 'IN_REVIEW' })
    }
    return res
  },

  cancelCourseReview: async (courseId: string | number) => {
    const res = await mutate<void>(`/api/courses/${courseId}/reviews`, 'DELETE')
    const draft = await getCourseDraftFromCookies(courseId)
    if (draft) {
      saveCourseDraft(courseId, { ...draft, status: 'DRAFT' })
    }
    return res
  },

  closeCourse: (courseId: string | number) =>
    mutate<void>(`/api/courses/${courseId}/closing`, 'POST'),

  deleteCourse: (courseId: string | number) =>
    mutate<void>(`/api/courses/${courseId}`, 'DELETE'),

  uploadLectureVideo: (lectureId: string | number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return mutate<void>(`/api/courses/lectures/${lectureId}/videos`, 'POST', formData, true)
  },

  // Lecture enter / progress / last-watched
  enterLecture: (
    courseId: string | number,
    chapterId: string | number,
    lectureId: string | number,
  ) =>
    request<LectureEnterResponse | null>(
      `/api/courses/${courseId}/chapters/${chapterId}/lectures/${lectureId}/enter`,
      null,
    ),

  // New API to fetch lecture details directly (including m3u8Path)
  getLecture: (
    courseId: string | number,
    chapterId: string | number,
    lectureId: string | number,
  ) =>
    request<any>(
      `/api/courses/${courseId}/chapters/${chapterId}/lectures/${lectureId}`,
      null,
    ),

  getVideoStreamUrl: (
    courseId: string | number,
    chapterId: string | number,
    lectureId: string | number,
  ) =>
    requestText(
      `/api/courses/${courseId}/chapters/${chapterId}/lectures/${lectureId}/stream`,
      true,
    ),

  getLastWatched: (courseId: string | number) =>
    request<LectureEnterResponse | null>(`/api/courses/${courseId}/lectures/last-watched`, null),

  enterFirstLecture: (courseId: string | number, chapterId: string | number) =>
    request<LectureEnterResponse | null>(
      `/api/courses/${courseId}/chapters/${chapterId}/lectures/first`,
      null,
    ),

  updateLectureProgress: (
    lectureId: string | number,
    positionSeconds: number,
    watchedDeltaSeconds: number,
  ) =>
    mutate<LectureProgressResponse>(`/api/lectures/${lectureId}/progress`, 'PATCH', {
      positionSeconds,
      watchedDeltaSeconds,
    }),

  // QnA
  getQna: (lectureId: string | number) =>
    request<PageResponse<QnaQuestionResponse>>(`/api/lectures/${lectureId}/qna`, {
      content: [],
      pageable: { pageNumber: 0, pageSize: 20 },
      totalElements: 0,
      totalPages: 0,
      last: true,
    }),
  createQuestion: (lectureId: string | number, title: string, content: string) =>
    mutate<QnaQuestionResponse>(`/api/lectures/${lectureId}/qna/questions`, 'POST', { title, content }),

  // Reflections
  getReflection: (lectureId: string | number) => request<any>(`/api/lectures/${lectureId}/reflections`, null),
  createReflection: (lectureId: string | number, content: string) => mutate<any>(`/api/lectures/${lectureId}/reflections`, 'POST', { content }),
  updateReflection: (lectureId: string | number, reflectionId: number | string, content: string) => mutate<any>(`/api/lectures/${lectureId}/reflections/${reflectionId}`, 'PUT', { content }),

  // Cart & Order
  getCart: () => request<CartResponse>('/api/cart', { items: [], totalPrice: 0 }, true, false),
  addToCart: (courseId: number) => mutate<CartResponse>('/api/cart/items', 'POST', { courseId }),
  removeFromCart: (cartItemId: number) => mutate<CartResponse>(`/api/cart/items/${cartItemId}`, 'DELETE'),
  clearCart: () => mutate<CartResponse>('/api/cart', 'DELETE'),

  isCourseEnrollmentActive: async (courseId: string | number): Promise<boolean> => {
    const res = await request<boolean | { exists?: boolean }>(
      `/api/enrollments/courses/${courseId}/active`,
      false,
      true,
      false,
    )
    return typeof res === 'boolean' ? res : !!res?.exists
  },

  createOrderFromCart: () => mutate<OrderDetailResponse>('/api/orders/cart', 'POST'),
  createDirectOrder: (courseId: number) => mutate<OrderDetailResponse>('/api/orders/direct', 'POST', { courseId }),
  cancelOrder: (orderId: number | string) => mutate<OrderDetailResponse>(`/api/orders/${orderId}/cancel`, 'PATCH'),
  confirmMockPayment: (
    orderId: number,
    paymentKey: string,
    method: string,
    amount: number,
    issuedCouponId?: number | null,
    idempotencyKey?: string | null,
  ) =>
    mutate<ConfirmPaymentResponse>(`/api/payments/${orderId}/confirm`, 'POST', {
      paymentKey,
      method,
      amount,
      issuedCouponId,
      idempotencyKey,
    }),
  confirmTossPayment: (orderId: number, request: ConfirmTossPaymentRequest) =>
    mutate<ConfirmPaymentResponse>(`/api/payments/${orderId}/toss/confirm`, 'POST', request),
  refundPayment: (orderId: number | string) =>
    mutate<PaymentResponse>(`/api/payments/${orderId}/refund`, 'POST'),
  confirmPayment: (
    orderId: number,
    paymentKey: string,
    method: string,
    amount: number,
    issuedCouponId?: number | null,
    idempotencyKey?: string | null,
  ) =>
    api.confirmMockPayment(orderId, paymentKey, method, amount, issuedCouponId, idempotencyKey),

  // Studies
  getMyStudies: async (): Promise<EnrolledCourse[]> => {
    const summaryRes = await request<StudySummaryResponse[]>('/api/studies/me', [])
    if (!Array.isArray(summaryRes)) return []

    const enrolledCoursesPromises = summaryRes.map(async (study) => {
      // Fetch detailed study information to determine accurate status
      const detail = await request<StudyDetailResponse | undefined>(`/api/studies/${study.studyId}`, undefined)
      let status: EnrolledCourse['status'] = '진행 중' // Default to '진행 중'

      if (detail?.status === 'CLOSED') {
        status = '완료' // Map 'CLOSED' from backend to '완료' for frontend display
      }
      // 진행도/강의 수는 백엔드 StudySummaryResponse 에서 집계해 내려준다.
      return {
        id: study.studyId.toString(),
        courseId: detail?.courseId?.toString(),
        title: study.title,
        instructor: study.ownerNickname,
        thumbnailUrl: '/placeholder.svg',
        progress: study.progressRate ?? 0,
        totalLectures: study.totalLectures ?? 0,
        completedLectures: study.completedLectures ?? 0,
        status: status,
      }
    })
    return Promise.all(enrolledCoursesPromises)
  },

  getStudyActivities: (studyId: number, page = 0, size = 10) =>
    request<PageResponse<StudyActivityResponse>>(
      `/api/studies/${studyId}/activities?page=${page}&size=${size}`,
      { content: [], pageable: { pageNumber: page, pageSize: size }, totalElements: 0, totalPages: 0, last: true }
    ),

  getStudyFeed: (studyId: string | number, cursor?: FeedCursor | null, size = 10) => {
    const params = new URLSearchParams({ size: String(size) })
    if (cursor) {
      params.set('cursorOccurredAt', toIsoLocalDateTime(cursor.occurredAt))
      params.set('cursorId', String(cursor.id))
    }

    return request<FeedCursorResponse>(
      `/api/studies/${studyId}/feed?${params.toString()}`,
      { items: [], nextCursor: null, hasNext: false },
    )
  },

  createStudyActivity: (studyId: number | string, content: string) =>
    mutate<StudyActivityResponse>(`/api/studies/${studyId}/activities`, 'POST', { content }),

  getStudy: async (studyId: string | number): Promise<Study | undefined> => {
    const detail = await request<StudyDetailResponse | undefined>(`/api/studies/${studyId}`, undefined)

    if (!detail) return undefined

    return mapStudyDetailToStudy(detail)
  },

  getStudyMembers: async (studyId: string | number): Promise<StudyMember[]> => {
    const members = await request<StudyMemberResponse[]>(
      `/api/studies/${studyId}/members`,
      [],
    )
    if (!Array.isArray(members)) return []
    return members.map(mapStudyMemberResponseToMember)
  },

  getStudyForEntry: async (studyId: string | number): Promise<Study | undefined> => {
    const study = await api.getStudy(studyId)
    if (!study) return undefined

    const profile = await api.getProfile()
    const isAdmin = profile ? profile.isAdmin : false

    if (!isAdmin) {
      const enrolled = await api.isCourseEnrollmentActive(study.courseId)
      if (!enrolled) return undefined
    }

    return study
  },

  getStudyIdByCourseId: async (courseId: string | number): Promise<string | null> => {
    const res = await request<StudyIdResponse | null>(`/api/studies/by-course/${courseId}`, null)

    return res?.studyId ? res.studyId.toString() : null
  },

  getBoardPosts: async (studyId: string | number): Promise<StudyActivityResponse[]> => {
    const numericStudyId = Number(studyId)
    if (!Number.isFinite(numericStudyId)) return []
    return (await api.getStudyActivities(numericStudyId)).content
  },

  getBoardPost: async (
    studyId: string | number,
    postId: string | number,
  ): Promise<StudyActivityResponse | undefined> => {
    const numericStudyId = Number(studyId)
    const numericPostId = Number(postId)
    if (!Number.isFinite(numericStudyId) || !Number.isFinite(numericPostId)) return undefined
    return request<StudyActivityResponse | undefined>(
      `/api/studies/${numericStudyId}/activities/${numericPostId}`,
      undefined,
    )
  },

  generateAiFeedback: (studyId: number, activityId: number) =>
    mutate<AiFeedbackResponse>(`/api/studies/${studyId}/activities/${activityId}/ai-feedback`, 'POST'),

  getAiFeedback: (studyId: number, activityId: number) =>
    request<AiFeedbackResponse | null>(`/api/studies/${studyId}/activities/${activityId}/ai-feedback`, null),

  // Attendance
  getAttendance: () => request<AttendanceStatusResponse | null>('/api/attendances/me', null),
  checkAttendance: () => mutate<AttendanceResponse>('/api/attendances', 'POST'),

  // Learning events
  recordLearningEvent: (data: {
    eventType: LearningEventType
    lectureId: number
    courseId?: number
    chapterId?: number
    positionSeconds?: number
    eventKey?: string
  }) =>
    mutate<LearningEventResponse>('/api/learning-events', 'POST', {
      ...data,
      // 이벤트 발생 시각은 서버가 수신 시각으로 직접 찍는다(클라이언트는 시각을 보내지 않음).
      // 멱등 처리를 위한 클라이언트 고유 키 (중복 이벤트 방지)
      eventKey: data.eventKey ?? (typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `${data.lectureId}-${data.eventType}-${Date.now()}`),
    }),

  getMyLearningActivities: async (page = 0, size = 20): Promise<PageResponse<LearningEventResponse>> => {
    const empty: PageResponse<LearningEventResponse> = {
      content: [], pageable: { pageNumber: page, pageSize: size }, totalElements: 0, totalPages: 0, last: true,
    }
    const userId = await getCurrentUserId()
    return request<PageResponse<LearningEventResponse>>(
      `/api/learning-events/users/${userId}/activities?page=${page}&size=${size}`,
      empty,
    )
  },

  // Coupons & Admin
  getCoupons: async () => {
    const result = await request<PageResponse<AdminCouponPolicyResponse> | AdminCouponPolicyResponse[] | null>(
      '/api/coupons',
      null,
      false,
      false,
    )
    const content = Array.isArray(result) ? result : (result?.content ?? [])
    return content
      .filter((policy) => policy.couponType !== 'AUTO')
      .map(mapAdminCouponPolicyToUserCoupon)
  },
  getMyCoupons: async () => {
    const coupons = await request<CouponListResponse[]>('/api/coupons/me', [])
    return coupons.map(mapCouponListToCoupon)
  },
  downloadCoupon: (policyId: number) =>
    mutate<any>(`/api/coupons/${policyId}/download`, 'POST'),
  getAdminCoupons: async (): Promise<AdminCoupon[]> => {
    const result = await request<PageResponse<AdminCouponPolicyResponse> | AdminCouponPolicyResponse[] | null>('/api/admin/coupons', null)
    const content = Array.isArray(result) ? result : (result?.content ?? [])
    return content.map(mapAdminCouponPolicyToCoupon)
  },
  createAdminCoupon: async (coupon: AdminCoupon) => {
    const result = await mutate<AdminCouponPolicyResponse>(
      '/api/admin/coupons',
      'POST',
      mapAdminCouponToPolicyRequest(coupon),
    )
    return mapAdminCouponPolicyToCoupon(result)
  },
  updateAdminCoupon: async (couponId: number, coupon: AdminCoupon) => {
    const { couponTarget, couponType, ...payload } = mapAdminCouponToPolicyRequest(coupon)
    const result = await mutate<AdminCouponPolicyResponse>(
      `/api/admin/coupons/${couponId}`,
      'PUT',
      payload,
    )
    return mapAdminCouponPolicyToCoupon(result)
  },
  terminateAdminCoupon: (couponId: number) =>
    mutate<void>(`/api/admin/coupons/${couponId}/terminate`, 'PATCH'),
  deleteAdminCoupon: (couponId: number) =>
    mutate(`/api/admin/coupons/${couponId}`, 'DELETE'),

  issueCouponToUsers: async (policyId: number, userIds: number[], requestKey: string) => {
    return await mutate<CouponIssueRequestResponse>(
      `/api/admin/coupons/${policyId}/issue/users`,
      'POST',
      { userIds, requestKey }
    )
  },

  issueCouponToAll: async (policyId: number, requestKey: string) => {
    return await mutate<CouponIssueRequestResponse>(
      `/api/admin/coupons/${policyId}/issue/all`,
      'POST',
      { requestKey }
    )
  },

  issueCouponToInactiveUsers: async (policyId: number, inactiveDays: number, maxInactiveDays: number | undefined, requestKey: string) => {
    return await mutate<CouponIssueRequestResponse>(
      `/api/admin/coupons/${policyId}/issue/inactive`,
      'POST',
      { inactiveDays, maxInactiveDays, requestKey }
    )
  },

  getCouponIssueRequests: async (page = 0, size = 20) => {
    const result = await request<PageResponse<CouponIssueRequestResponse> | null>(
      `/api/admin/coupons/issue-requests?page=${page}&size=${size}`,
      null
    )
    return result
  },

  // User Profile
  getProfile: async (): Promise<UserProfile | null> => {
    try {
      const claims = await request<any>('/api/auth/me', null)
      if (!claims) return null
      return {
        id: String(claims.id || claims.sub || ''),
        name: claims.nickname || claims.name || '',
        email: claims.email || '',
        studyCount: 0,
        courseCount: 0,
        isSeller: claims.role === 'ROLE_SELLER' || claims.role === 'ROLE_ADMIN',
        isAdmin: claims.role === 'ROLE_ADMIN',
      }
    } catch (e) {
      console.error('Failed to decode token:', e)
      return null
    }
  },
  getPurchasedCourses: async (): Promise<EnrolledCourse[]> => {
    try {
      const res = await request<any>('/api/orders', { content: [] })
      const orders = Array.isArray(res) ? res : (res?.content || [])
      const enrolled: EnrolledCourse[] = []
      for (const ord of orders) {
        if (ord.status === 'PAID') {
          for (const item of ord.items ?? []) {
            const courseId = item.courseId.toString()
            if (!enrolled.some(e => (e.courseId ?? e.id) === courseId)) {
              const studyId = await api.getStudyIdByCourseId(courseId)
              if (!studyId) continue
              enrolled.push({
                id: studyId,
                courseId,
                title: item.courseTitle || '',
                instructor: '강사',
                thumbnailUrl: '/placeholder.svg',
                progress: 0,
                totalLectures: 0,
                completedLectures: 0,
                status: '진행 중'
              })
            }
          }
        }
      }
      return enrolled
    } catch (e) {
      console.error('Failed to get purchased courses from orders:', e)
      return []
    }
  },
  getMyComments: () => request<MyComment[]>('/api/me/comments', []),

  // Orders
  getOrders: async () => {
    const res = await request<any>('/api/orders', { content: [] })
    return Array.isArray(res) ? res : (res?.content || [])
  },

  getOrder: async (id: string) => request<OrderDetailResponse | undefined>(`/api/orders/${id}`, undefined),

  // 리포트 조회. 없으면 백엔드에서 즉시 생성한다.
  getStudyReportRaw: (studyId: string | number, refresh = false) =>
    request<StudyReportResponse | null>(
      `/api/studies/${studyId}/report${refresh ? '?refresh=true' : ''}`,
      null,
    ),

  // 학습 이벤트 재집계를 시도한다. 쿨다운 이내면 기존 리포트가 반환된다.
  generateStudyReport: (studyId: string | number) =>
    api.getStudyReportRaw(studyId, true),

  // 조회 후 없으면 백엔드에서 생성까지 처리한다.
  getOrGenerateStudyReport: async (studyId: string | number): Promise<StudyReportResponse | null> => {
    return await api.getStudyReportRaw(studyId)
  },

  getStudyReport: async (_studyId: string): Promise<StudyReport> => {
    let studyTitle = '기능 없음'
    let resolvedStudyId = _studyId

    try {
      // 1. Fetch user's studies to resolve a valid study ID if it's 'study-1' or similar
      const myStudies = await request<StudySummaryResponse[]>('/api/studies/me', [])
      if (myStudies && myStudies.length > 0) {
        if (_studyId === 'study-1' || isNaN(Number(_studyId))) {
          resolvedStudyId = String(myStudies[0].studyId)
          studyTitle = myStudies[0].title
        } else {
          const matched = myStudies.find(s => String(s.studyId) === _studyId)
          if (matched) {
            studyTitle = matched.title
          } else {
            const detail = await request<StudyDetailResponse | undefined>(`/api/studies/${_studyId}`, undefined)
            if (detail) {
              studyTitle = detail.title
            }
          }
        }
      } else {
        studyTitle = '참여 중인 스터디 없음'
      }
    } catch (e) {
      console.warn('Failed to fetch user studies or study detail:', e)
    }

    let userName = '사용자'
    try {
      // 2. Fetch User Profile to get user name
      const profile = await api.getProfile()
      if (profile && profile.name) {
        userName = profile.name
      }
    } catch (e) {
      console.warn('Failed to fetch user profile:', e)
    }

    // 3. 실제 학습 리포트(StudyReportResponse) 조회/생성 후 화면용 StudyReport 로 매핑
    const raw = isNaN(Number(resolvedStudyId))
      ? null
      : await api.getOrGenerateStudyReport(resolvedStudyId)

    if (!raw) {
      return {
        studyName: studyTitle,
        userName,
        period: '기능 없음',
        totalStudyTime: '기능 없음',
        commentCount: -1,
        studyDays: -1,
        progressData: [],
        calendar: [],
        topLectures: [],
      }
    }

    return { studyName: studyTitle, userName, ...mapStudyReportToDisplay(raw) }
  },

  // 마이페이지: 내가 참여한 모든 스터디의 리포트 + 전체 합산 리포트를 한 번에 조회.
  getMyStudyReports: async (): Promise<{
    reports: MyStudyReport[]
    aggregate: MyStudyReport | null
  }> => {
    const myStudies = await request<StudySummaryResponse[]>('/api/studies/me', [])

    let userName = '사용자'
    try {
      const profile = await api.getProfile()
      if (profile?.name) userName = profile.name
    } catch {
      // 프로필 조회 실패는 무시하고 기본값 사용
    }

    if (!myStudies || myStudies.length === 0) {
      return { reports: [], aggregate: null }
    }

    const entries = await Promise.all(
      myStudies.map(async (s) => ({
        study: s,
        raw: await api.getOrGenerateStudyReport(s.studyId).catch(() => null),
      })),
    )

    const emptyDisplay: Omit<StudyReport, 'studyName' | 'userName'> = {
      period: '기능 없음',
      totalStudyTime: '기능 없음',
      commentCount: -1,
      studyDays: -1,
      progressData: [],
      calendar: [],
      topLectures: [],
    }

    const reports: MyStudyReport[] = entries.map(({ study, raw }) => ({
      studyId: String(study.studyId),
      studyName: study.title,
      userName,
      ...(raw ? mapStudyReportToDisplay(raw) : emptyDisplay),
    }))

    const raws = entries
      .map((e) => e.raw)
      .filter((r): r is StudyReportResponse => r != null)

    const aggregate: MyStudyReport | null =
      raws.length > 1
        ? {
          studyId: 'all',
          studyName: `전체 ${reports.length}개 스터디`,
          userName,
          ...mapStudyReportToDisplay(buildAggregateReportRaw(raws)),
        }
        : null

    return { reports, aggregate }
  },

  // Admin Course APIs
  approveCourseByAdmin: async (courseId: number | string) => {
    const res = await mutate<void>(`/api/admin/courses/${courseId}/approve`, 'POST')
    removeCourseDraft(courseId)
    return res
  },
  rejectCourseByAdmin: async (courseId: number | string, reason: string) => {
    const res = await mutate<void>(`/api/admin/courses/${courseId}/reject`, 'POST', { reason })
    const draft = await getCourseDraftFromCookies(courseId)
    if (draft) {
      saveCourseDraft(courseId, { ...draft, status: 'DRAFT' })
    }
    return res
  },
  suspendCourseByAdmin: async (courseId: number | string, reason: string) => {
    const res = await mutate<void>(`/api/admin/courses/${courseId}/suspension`, 'POST', { reason })
    const draft = await getCourseDraftFromCookies(courseId)
    if (draft) {
      saveCourseDraft(courseId, { ...draft, status: 'SUSPENDED' })
    }
    return res
  },
  deleteCourseByAdmin: async (courseId: number | string) => {
    const res = await mutate<void>(`/api/admin/courses/${courseId}`, 'DELETE')
    removeCourseDraft(courseId)
    return res
  },

  // ── Admin Dashboard APIs ────────────────────────────────────────────
  getAdminOverview: () =>
    request<AdminOverview | null>('/api/admin/dashboard/overview', null),
  getAdminDailySessions: (from?: string, to?: string) => {
    const qs = new URLSearchParams()
    if (from) qs.set('from', from)
    if (to) qs.set('to', to)
    const suffix = qs.toString() ? `?${qs.toString()}` : ''
    return request<DailySessionPoint[]>(`/api/admin/dashboard/sessions/daily${suffix}`, [])
  },
  getAdminCourseStats: (page = 0, size = 20, status?: string) => {
    const qs = new URLSearchParams({ page: String(page), size: String(size) })
    if (status) qs.set('status', status)
    return request<PageResponse<CourseStatRow> | null>(
      `/api/admin/dashboard/courses?${qs.toString()}`,
      null,
    )
  },
  getAdminLectureStats: (courseId: number) =>
    request<LectureStatRow[]>(`/api/admin/dashboard/courses/${courseId}/lectures`, []),
  getAdminEnrollees: (courseId: number, page = 0, size = 20) =>
    request<PageResponse<EnrolleeRow> | null>(
      `/api/admin/dashboard/courses/${courseId}/enrollees?page=${page}&size=${size}`,
      null,
    ),
  getAdminUserTimeline: (userId: number, courseId?: number, page = 0, size = 30) => {
    const qs = new URLSearchParams({ page: String(page), size: String(size) })
    if (courseId != null) qs.set('courseId', String(courseId))
    return request<PageResponse<LearningEventRow> | null>(
      `/api/admin/dashboard/users/${userId}/timeline?${qs.toString()}`,
      null,
    )
  },
  getAdminAnomalies: (incompletionThreshold?: number, burstThreshold?: number, windowMinutes?: number) => {
    const qs = new URLSearchParams()
    if (incompletionThreshold != null) qs.set('incompletionThreshold', String(incompletionThreshold))
    if (burstThreshold != null) qs.set('burstThreshold', String(burstThreshold))
    if (windowMinutes != null) qs.set('windowMinutes', String(windowMinutes))
    const suffix = qs.toString() ? `?${qs.toString()}` : ''
    return request<AnomalyResponse | null>(`/api/admin/dashboard/anomalies${suffix}`, null)
  },
  getAdminAudit: () =>
    request<AuditResponse | null>('/api/admin/dashboard/audit/retention', null),

  // ── Seller Dashboard APIs ───────────────────────────────────────────
  getSellerAnalytics: (range: '3m' | '6m' | '1y' = '6m') =>
    request<SellerAnalytics | null>(`/api/seller/dashboard/analytics?range=${range}`, null),
  getSellerCourseDetail: (courseId: number, range: '3m' | '6m' | '1y' = '6m') =>
    request<SellerCourseDetail | null>(
      `/api/seller/dashboard/courses/${courseId}?range=${range}`,
      null,
    ),
  getSellerLecturePauses: (lectureId: number, bins = 40) =>
    request<LecturePauses | null>(
      `/api/seller/dashboard/lectures/${lectureId}/pauses?bins=${bins}`,
      null,
    ),
}
