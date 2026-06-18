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
  CourseUpdateRequest
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

const mapCouponListToCoupon = (coupon: CouponListResponse): Coupon => {
  const isRate = coupon.discountType === 'RATE'
  const status = coupon.status === 'ACTIVE' ? '진행 중인 이벤트' : '종료된 이벤트'

  return {
    id: coupon.issuedCouponId.toString(),
    name: coupon.policyName,
    amount: isRate
      ? `${coupon.discountValue}% 할인`
      : `${coupon.discountValue.toLocaleString()}원 할인`,
    condition: coupon.validUntil ? `${coupon.validUntil.slice(0, 10)}까지` : coupon.status,
    category: status,
    type: isRate ? 'discount' : 'firstcome',
  }
}

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
    const coupons = await request<CouponListResponse[]>('/api/coupons', [])
    return coupons.map(mapCouponListToCoupon)
  },
  getAdminCoupons: async (): Promise<AdminCoupon[]> => {
    const result = await request<AdminCoupon[] | null>('/api/admin/coupons', null)
    return Array.isArray(result) ? result : []
  },
  
  // User Profile
  getProfile: () => request<UserProfile | null>('/api/me', null),
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
