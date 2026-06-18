// API client for the PlayLearn React frontend.
//
// In production this talks to the Java/Spring backend over REST. Set
// NEXT_PUBLIC_API_BASE_URL (e.g. https://api.playlearn.io) to point at the
// Spring server. When it is not set, the client falls back to bundled mock
// data so the UI can be developed and previewed standalone.
//
// Each method documents the Spring endpoint it maps to.

import {
  activityFeed,
  adminCoupons,
  coupons,
  courses,
  enrolledCourses,
  myComments,
  orders,
  purchasedCourses,
  qnaPosts,
  studies,
  studyReport,
  userProfile,
} from './mock-data'
import type {
  ActivityFeedItem,
  AdminCoupon,
  BoardPost,
  Coupon,
  Course,
  EnrolledCourse,
  MyComment,
  Order,
  QnaPost,
  Study,
  StudyReport,
  UserProfile,
  SignupRequest,
} from './types'

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? ''

async function request<T>(path: string, fallback: T): Promise<T> {
  if (!BASE_URL) return fallback
  try {
    const res = await fetch(`${BASE_URL}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      cache: 'no-store',
    })
    if (!res.ok) throw new Error(`API ${res.status}`)
    return (await res.json()) as T
  } catch (err) {
    console.log('[v0] API request failed, using fallback:', path, err)
    return fallback
  }
}

async function mutate<T>(path: string, method: string, body: any): Promise<T> {
  if (!BASE_URL) return {} as T
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}))
    throw new Error(errorData.message || `Error ${res.status}`)
  }
  if (res.status === 204 || (res.status === 201 && res.headers.get('content-length') === '0')) {
    return {} as T
  }
  try {
    return (await res.json()) as T
  } catch (e) {
    return {} as T
  }
}

export const api = {
  // POST /api/auth/signup
  signup: (data: SignupRequest) => mutate<void>('/api/auth/signup', 'POST', data),

  // GET /api/courses
  getCourses: () => request<Course[]>('/api/courses', courses),

  // GET /api/courses/{id}
  getCourse: (id: string) =>
    request<Course | undefined>(
      `/api/courses/${id}`,
      courses.find((c) => c.id === id),
    ),

  // GET /api/courses/{id}/qna
  getQna: (_courseId: string) =>
    request<QnaPost[]>(`/api/courses/${_courseId}/qna`, qnaPosts),

  // GET /api/studies/{id}/feed
  getActivityFeed: (_studyId: string) =>
    request<ActivityFeedItem[]>(`/api/studies/${_studyId}/feed`, activityFeed),

  // GET /api/studies/{id}
  getStudy: (id: string) =>
    request<Study | undefined>(`/api/studies/${id}`, studies[id]),

  // GET /api/studies/{id}/posts
  getBoardPosts: (studyId: string) =>
    request<BoardPost[]>(
      `/api/studies/${studyId}/posts`,
      studies[studyId]?.posts ?? [],
    ),

  // GET /api/studies/{id}/posts/{postId}
  getBoardPost: (studyId: string, postId: string) =>
    request<BoardPost | undefined>(
      `/api/studies/${studyId}/posts/${postId}`,
      studies[studyId]?.posts.find((p) => p.id === postId),
    ),

  // GET /api/coupons
  getCoupons: () => request<Coupon[]>('/api/coupons', coupons),

  // GET /api/admin/coupons
  getAdminCoupons: () =>
    request<AdminCoupon[]>('/api/admin/coupons', adminCoupons),

  // GET /api/me
  getProfile: () => request<UserProfile>('/api/me', userProfile),

  // GET /api/me/studies
  getEnrolledCourses: () =>
    request<EnrolledCourse[]>('/api/me/studies', enrolledCourses),

  // GET /api/me/courses
  getPurchasedCourses: () =>
    request<EnrolledCourse[]>('/api/me/courses', purchasedCourses),

  // GET /api/me/comments
  getMyComments: () => request<MyComment[]>('/api/me/comments', myComments),

  // GET /api/orders
  getOrders: () => request<Order[]>('/api/orders', orders),

  // GET /api/orders/{id}
  getOrder: (id: string) =>
    request<Order | undefined>(
      `/api/orders/${id}`,
      orders.find((o) => o.id === id) ?? orders[0],
    ),

  // GET /api/studies/{id}/report
  getStudyReport: (_studyId: string) =>
    request<StudyReport>(`/api/studies/${_studyId}/report`, studyReport),
}
