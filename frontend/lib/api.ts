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
  StudyIdResponse,
  StudyReport,
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
} from './types'

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'

const getAuthHeaders = async (): Promise<Record<string, string>> => {
  return {}
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
      pathname === '/cart'

    if (!isPublicRoute) {
      window.location.href = '/login'
    }
  }
}

async function request<T>(
  path: string,
  defaultData: T,
  includeAuth = true,
  handleAuthError = true,
): Promise<T> {
  if (!BASE_URL) return defaultData
  try {
    const authHeaders = includeAuth ? await getAuthHeaders() : {}
    const res = await fetch(`${BASE_URL}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...authHeaders,
      },
      credentials: includeAuth ? 'include' : 'same-origin',
      cache: 'no-store',
    })

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

async function mutate<T>(
  path: string,
  method: string,
  body?: any,
  isMultipart = false,
  includeCredentials = true,
): Promise<T> {
  if (!BASE_URL) throw new Error('API BASE_URL is not defined')

  const headers = await getAuthHeaders()
  if (!isMultipart) {
    headers['Content-Type'] = 'application/json'
  }

  const options: RequestInit = {
    method,
    headers,
  }
  if (includeCredentials || !!headers.Authorization) {
    options.credentials = 'include'
  }
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
    if (res.status === 401) {
      handleUnauthorized()
    }
    const errorData = await res.json().catch(() => ({}))
    throw new Error(errorData.message || errorData.detail || errorData.error || `Error ${res.status}`)
  }
  if (res.status === 204 || (res.status === 201 && res.headers.get('content-length') === '0')) {
    return {} as T
  }
  return await res.json() as T
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
  rating: 0,
  reviewCount: 0,
  studentCount: card.viewCount || 0,
  level: '입문',
  tags: [],
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
  rating: 0,
  reviewCount: 0,
  studentCount: detail.viewCount || 0,
  level: '입문',
  tags: [],
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
  members: [
    {
      id: 'owner',
      name: detail.ownerNickname,
      progress: 0,
      role: 'owner',
      joinedAt: '',
    },
  ],
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
  const progressData = (raw.dailyProgress ?? []).map((d) => ({
    day: d.date?.slice(5) ?? '', // MM-DD
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
  signup: (data: SignupRequest) => mutate<void>('/api/auth/signup', 'POST', data, false, false),
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

  requestCourseReview: (courseId: string | number) =>
    mutate<void>(`/api/courses/${courseId}/reviews`, 'POST'),

  cancelCourseReview: (courseId: string | number) =>
    mutate<void>(`/api/courses/${courseId}/reviews`, 'DELETE'),

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
  confirmMockPayment: (orderId: number, paymentKey: string, method: string, amount: number, issuedCouponId?: number | null) =>
    mutate<ConfirmPaymentResponse>(`/api/payments/${orderId}/confirm`, 'POST', {
      paymentKey,
      method,
      amount,
      issuedCouponId,
    }),
  confirmTossPayment: (orderId: number, request: ConfirmTossPaymentRequest) =>
    mutate<ConfirmPaymentResponse>(`/api/payments/${orderId}/toss/confirm`, 'POST', request),
  refundPayment: (orderId: number | string) =>
    mutate<PaymentResponse>(`/api/payments/${orderId}/refund`, 'POST'),
  confirmPayment: (orderId: number, paymentKey: string, method: string, amount: number, issuedCouponId?: number | null) =>
    api.confirmMockPayment(orderId, paymentKey, method, amount, issuedCouponId),

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

  createStudyActivity: (studyId: number | string, content: string) =>
    mutate<StudyActivityResponse>(`/api/studies/${studyId}/activities`, 'POST', { content }),

  getStudy: async (studyId: string | number): Promise<Study | undefined> => {
    const detail = await request<StudyDetailResponse | undefined>(`/api/studies/${studyId}`, undefined)

    if (!detail) return undefined

    return mapStudyDetailToStudy(detail)
  },

  getStudyForEntry: async (studyId: string | number): Promise<Study | undefined> => {
    const study = await api.getStudy(studyId)
    if (!study) return undefined

    const enrolled = await api.isCourseEnrollmentActive(study.courseId)
    if (!enrolled) return undefined

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
    eventTime?: string
    eventKey?: string
  }) =>
    mutate<LearningEventResponse>('/api/learning-events', 'POST', {
      ...data,
      // 백엔드 RecordLearningEventRequest 는 eventTime(@NotNull) 을 요구한다.
      eventTime: data.eventTime ?? new Date().toISOString().slice(0, 19),
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
      '/api/admin/coupons',
      null,
      true,
      false,
    )
    const content = Array.isArray(result) ? result : (result?.content ?? [])
    return content.map(mapAdminCouponPolicyToUserCoupon)
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
    mutate<void>(`/api/admin/coupons/${couponId}`, 'DELETE'),

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

  // Admin Course APIs
  approveCourseByAdmin: (courseId: number | string) =>
    mutate<void>(`/api/admin/courses/${courseId}/approve`, 'POST'),
  rejectCourseByAdmin: (courseId: number | string, reason: string) =>
    mutate<void>(`/api/admin/courses/${courseId}/reject`, 'POST', { reason }),
  suspendCourseByAdmin: (courseId: number | string, reason: string) =>
    mutate<void>(`/api/admin/courses/${courseId}/suspension`, 'POST', { reason }),
  deleteCourseByAdmin: (courseId: number | string) =>
    mutate<void>(`/api/admin/courses/${courseId}`, 'DELETE'),
}
