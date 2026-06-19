// frontend/lib/api.ts
import type {
  AdminCoupon,
  BoardPost,
  Coupon,
  Course,
  CourseCardResponse,
  CourseDetailResponse,
  PageResponse,
  CartResponse,
  CartItemResponse,
  OrderDetailResponse,
  EnrolledCourse,
  MyComment,
  Order,
  QnaPost,
  Study,
  StudyDetailResponse,
  StudyReport,
  UserProfile,
  SignupRequest,
  LoginRequest,
  LoginResponse,
  StudySummaryResponse,
  StudyActivityResponse,
  AiFeedbackResponse,
  AttendanceResponse,
  CouponListResponse,
  CourseCreateRequest,
  CourseUpdateRequest,
  AdminCouponPolicyRequest,
  AdminCouponPolicyResponse,
} from './types'

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'

const getAuthHeaders = async (): Promise<Record<string, string>> => {
  let token = ''
  if (typeof window !== 'undefined') {
    token = localStorage.getItem('accessToken') || ''
    if (!token) {
      const match = document.cookie.match(/(^| )accessToken=([^;]+)/)
      if (match) token = match[2]
    }
  } else {
    try {
      const { cookies } = await import('next/headers')
      const cookieStore = cookies()
      const store = cookieStore instanceof Promise ? await cookieStore : cookieStore
      token = store.get('accessToken')?.value || ''
    } catch (e) {}
  }
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const handleUnauthorized = () => {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('accessToken')
    document.cookie = 'accessToken=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT'
    window.location.href = '/login'
  }
}

async function request<T>(path: string, defaultData: T): Promise<T> {
  if (!BASE_URL) return defaultData
  try {
    const authHeaders = await getAuthHeaders()
    const res = await fetch(`${BASE_URL}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...authHeaders,
      },
      cache: 'no-store',
    })
    
    if (!res.ok) {
      console.warn(`[API 에러] ${res.status} on ${path}`)
      if (res.status === 401) {
        handleUnauthorized()
      }
      return defaultData
    }
    
    return await res.json() as T
  } catch (err) {
    console.error(`[API 통신 에러] ${path}:`, err)
    return defaultData
  }
}

async function mutate<T>(path: string, method: string, body?: any, isMultipart = false): Promise<T> {
  if (!BASE_URL) throw new Error('API BASE_URL is not defined')
  
  const headers = await getAuthHeaders()
  if (!isMultipart) {
    headers['Content-Type'] = 'application/json'
  }
  
  const options: RequestInit = {
    method,
    headers,
  }
  if (body) {
    options.body = isMultipart ? body : JSON.stringify(body)
  }
  
  const res = await fetch(`${BASE_URL}${path}`, options)
  if (!res.ok) {
    if (res.status === 401) {
      handleUnauthorized()
    }
    const errorData = await res.json().catch(() => ({}))
    throw new Error(errorData.message || `Error ${res.status}`)
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
    })) || [],
  })) || [],
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
  myRole: detail.myRole.toLowerCase() as Study['myRole'],
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
    id: post.id?.toString() || '0',
    title: post.content.split('\n')[0] || '학습 활동',
    author: post.authorName,
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

const mapCouponListToCoupon = (coupon: CouponListResponse): Coupon => {
  const isRate = coupon.discountType === 'RATE' || coupon.discountType === 'PERCENT'
  const isAvailable = coupon.status === 'ISSUED'
  const category = isAvailable ? '진행 중인 이벤트' : '종료된 이벤트'

  return {
    id: coupon.issuedCouponId.toString(),
    name: coupon.couponName,
    amount: isRate
      ? `${coupon.discountValue}% 할인`
      : `${coupon.discountValue.toLocaleString()}원 할인`,
    condition: coupon.expiredAt ? `${coupon.expiredAt.slice(0, 10)}까지` : coupon.status,
    category: category,
    type: isRate ? 'discount' : 'firstcome',
    status: isAvailable ? 'ACTIVE' : 'ENDED',
  }
}

const toLocalDateTime = (value: string | null | undefined) =>
  value ? value.slice(0, 16) : ''

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
  } else if (policy.issueEndDate) {
    condition = `${policy.issueEndDate.slice(0, 10)}까지`
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
    startDate: policy.issueStartDate,
    endDate: policy.issueEndDate,
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
    minOrderAmount: policy.minOrderAmount ?? 0,
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

export const api = {
  // Auth
  signup: (data: SignupRequest) => mutate<void>('/api/auth/signup', 'POST', data),
  login: (data: LoginRequest) => mutate<LoginResponse>('/api/auth/login', 'POST', data),
  logout: async () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken')
      document.cookie = 'accessToken=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT'
    }
    return mutate<void>('/api/auth/logout', 'POST')
  },

  // Courses
  getCourses: async (page = 0, size = 10, sort = 'VIEW_DESC'): Promise<PageResponse<Course>> => {
    const res = await request<PageResponse<CourseCardResponse>>(`/api/courses?page=${page}&size=${size}&sort=${sort}`, {
      content: [],
      pageable: { pageNumber: page, pageSize: size },
      totalElements: 0,
      totalPages: 0,
      last: true
    })
    return {
      ...res,
      content: res.content ? res.content.map(c => mapCourseCardToCourse(c)) : []
    }
  },

  getCourse: async (id: string | number): Promise<Course | undefined> => {
    const detail = await request<CourseDetailResponse | undefined>(`/api/courses/${id}`, undefined)
    if (detail && detail.id) return mapCourseDetailToCourse(detail)
    return undefined
  },

  createCourse: (data: CourseCreateRequest) => mutate<number>('/api/courses', 'POST', data),
  
  updateCourse: (courseId: string | number, data: CourseUpdateRequest) => 
    mutate<void>(`/api/courses/${courseId}`, 'PUT', data),

  uploadLectureVideo: (lectureId: string | number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return mutate<void>(`/api/courses/lectures/${lectureId}/videos`, 'POST', formData, true)
  },

  // QnA
  getQna: (lectureId: string | number) => request<any[]>(`/api/lectures/${lectureId}/qna`, []),

  // Cart & Order
  getCart: () => request<CartResponse>('/api/cart', { items: [], totalPrice: 0 }),
  addToCart: (courseId: number) => mutate<CartResponse>('/api/cart/items', 'POST', { courseId }),
  removeFromCart: (cartItemId: number) => mutate<CartResponse>(`/api/cart/items/${cartItemId}`, 'DELETE'),
  clearCart: () => mutate<CartResponse>('/api/cart', 'DELETE'),

  createOrderFromCart: () => mutate<OrderDetailResponse>('/api/orders/cart', 'POST'),
  createDirectOrder: (courseId: number) => mutate<OrderDetailResponse>('/api/orders/direct', 'POST', { courseId }),

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
      // Note: progress, totalLectures, and completedLectures cannot be accurately
      // populated from StudySummaryResponse or StudyDetailResponse without
      // further backend changes, so they remain hardcoded to 0.
      return {
        id: study.studyId.toString(),
        title: study.title,
        instructor: study.ownerNickname,
        thumbnailUrl: '/placeholder.svg',
        progress: 0,
        totalLectures: 0,
        completedLectures: 0,
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

  getStudy: async (studyId: string | number): Promise<Study | undefined> => {
    const numericStudyId = Number(studyId)
    let detail = await request<StudyDetailResponse | undefined>(`/api/studies/${studyId}`, undefined)
    if (!detail) {
      detail = await request<StudyDetailResponse | undefined>(`/api/studies/by-course/${studyId}`, undefined)
    }
    if (!detail) return undefined

    const numericResolvedStudyId = Number(detail.studyId)
    const activities = Number.isFinite(numericResolvedStudyId)
      ? (await api.getStudyActivities(numericResolvedStudyId)).content
      : []

    return mapStudyDetailToStudy(detail, activities)
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
    const posts = await api.getBoardPosts(studyId)
    return posts.find((post) => post.id?.toString() === postId.toString())
  },

  generateAiFeedback: (studyId: number, activityId: number) =>
    mutate<AiFeedbackResponse>(`/api/studies/${studyId}/activities/${activityId}/ai-feedback`, 'POST'),

  getAiFeedback: (studyId: number, activityId: number) =>
    request<AiFeedbackResponse | null>(`/api/studies/${studyId}/activities/${activityId}/ai-feedback`, null),

  // Attendance
  getAttendance: () => request<AttendanceResponse | null>('/api/attendances/me', null),
  checkAttendance: () => mutate<AttendanceResponse>('/api/attendances', 'POST'),

  recordLearningEvent: (data: { eventType: string, courseId: number, chapterId?: number, lectureId?: number }) =>
    mutate<void>('/api/learning-events', 'POST', data),

  // Coupons & Admin
  getCoupons: async () => {
    let authHeaders = await getAuthHeaders()
    if (!authHeaders.Authorization) {
      try {
        const loginRes = await fetch(`${BASE_URL}/api/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: 'admin@test.com', password: 'Test1234!' }),
        })
        if (loginRes.ok) {
          const loginData = await loginRes.json()
          if (loginData.accessToken) {
            authHeaders = { Authorization: `Bearer ${loginData.accessToken}` }
          }
        }
      } catch (e) {
        console.error('Failed to log in as system admin to fetch public coupons:', e)
      }
    }
    
    const res = await fetch(`${BASE_URL}/api/admin/coupons`, {
      headers: {
        'Content-Type': 'application/json',
        ...authHeaders,
      },
      cache: 'no-store',
    })
    
    if (!res.ok) {
      console.warn(`[API 에러] ${res.status} on /api/admin/coupons`)
      return []
    }
    
    const result = await res.json() as any
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
    const authHeaders = await getAuthHeaders()
    const token = authHeaders.Authorization?.split(' ')[1]
    if (!token) return null
    try {
      const payloadPart = token.split('.')[1]
      if (!payloadPart) return null
      const base64 = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
      const rawPayload = typeof window !== 'undefined'
        ? atob(base64)
        : Buffer.from(base64, 'base64').toString('utf8')
      const jsonPayload = typeof window !== 'undefined'
        ? decodeURIComponent(escape(rawPayload))
        : rawPayload
      const claims = JSON.parse(jsonPayload)
      return {
        id: String(claims.sub || ''),
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
  getPurchasedCourses: () => request<EnrolledCourse[]>('/api/me/courses', []),
  getMyComments: () => request<MyComment[]>('/api/me/comments', []),

  // Orders
  getOrders: async () => {
    const res = await request<any>('/api/orders', { content: [] })
    return Array.isArray(res) ? res : (res?.content || [])
  },

  getOrder: async (id: string) => request<OrderDetailResponse | undefined>(`/api/orders/${id}`, undefined),

  getStudyReport: (_studyId: string) => request<StudyReport | null>(`/api/studies/${_studyId}/report`, null),
}
