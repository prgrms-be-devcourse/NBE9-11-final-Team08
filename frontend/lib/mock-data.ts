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
} from './types'

const lecture = (id: string, title: string, completed = false, progress = 0) => ({
  id,
  title,
  duration: '15:08',
  progress,
  completed,
})

const jpaChapters = [
  {
    id: 'ch1',
    title: 'chapter01. 기초',
    lectures: [
      lecture('l1', '1-1 영속성의 이해', true, 100),
      lecture('l2', '1-2 엔티티 매핑', true, 100),
      lecture('l3', '1-3 연관관계 매핑', false, 40),
    ],
  },
  {
    id: 'ch2',
    title: 'chapter02. 심화',
    lectures: [
      lecture('l4', '2-1 영속성 컨텍스트'),
      lecture('l5', '2-2 프록시와 지연로딩'),
      lecture('l6', '2-3 값 타입'),
      lecture('l7', '2-4 JPQL 기초'),
      lecture('l8', '2-5 페치 조인'),
    ],
  },
]

export const courses: Course[] = [
  {
    id: 'jpa',
    title: '자바 ORM 표준기술 JPA for beginner',
    subtitle: '자바 백엔드 개발자라면 반드시 알아야 할 JPA 기초부터 실무까지',
    description:
      '영속성(Persistence)이란 프로그램이 종료되거나 시스템이 재시작되더라도 데이터가 사라지지 않고 저장되는 특성을 의미합니다. 이 강좌에서는 JPA의 핵심 개념인 영속성 컨텍스트부터 연관관계 매핑, 성능 최적화까지 단계별로 학습합니다.',
    category: '개발·프로그래밍',
    subCategory: '백엔드',
    thumbnailUrl: '/courses/jpa.png',
    price: 88000,
    discountRate: 20,
    rating: 4.9,
    reviewCount: 214,
    studentCount: 1203,
    level: '입문',
    tags: ['Java', 'JPA', 'Spring', 'ORM'],
    instructor: { id: 'i1', name: '최지바', title: 'JPA 전문 강사 · 10년차 백엔드 개발자' },
    chapters: jpaChapters,
    badges: ['답변 활발', '미션'],
    status: 'PUBLISHED',
  },
  {
    id: 'spring',
    title: 'Spring Boot 입문 — 처음 시작하는 백엔드 개발',
    subtitle: '스프링 부트로 만드는 첫 REST API 서버',
    description:
      'Spring Boot의 핵심 개념과 자동 설정, 의존성 주입, REST API 작성 방법을 실습 위주로 학습합니다. 자바 백엔드 입문자를 위한 강좌입니다.',
    category: '개발·프로그래밍',
    subCategory: '백엔드',
    thumbnailUrl: '/courses/spring.png',
    price: 99000,
    discountRate: 30,
    rating: 4.8,
    reviewCount: 530,
    studentCount: 3120,
    level: '입문',
    tags: ['Java', 'Spring Boot', 'REST API'],
    instructor: { id: 'i2', name: '김개발', title: '시니어 백엔드 엔지니어' },
    chapters: jpaChapters,
    badges: ['New', '답변 활발'],
    status: 'PUBLISHED',
  },
  {
    id: 'frontend',
    title: '미국 빅테크 프론트엔드 시스템 디자인 완성',
    subtitle: '단순 구현자로 남지 않기 위한 프론트엔드 아키텍처',
    description:
      '확장 가능한 상태 관리, 성능 관측, 런타임 모델까지 — 빅테크 프론트엔드 면접과 실무에서 통하는 시스템 디자인 역량을 기릅니다.',
    category: '개발·프로그래밍',
    subCategory: '프론트엔드',
    thumbnailUrl: '/courses/frontend.png',
    price: 132000,
    discountRate: 30,
    rating: 5.0,
    reviewCount: 96,
    studentCount: 540,
    level: '심화',
    tags: ['React', 'System Design', 'Frontend'],
    instructor: { id: 'i3', name: '치트키맨', title: '프론트엔드 시스템 디자인 멘토' },
    chapters: jpaChapters,
    badges: ['New', '답변 활발', '미션'],
    status: 'PUBLISHED',
  },
  {
    id: 'ai',
    title: 'AI 업무자동화를 쉽게, 2026 업무 자동화 마스터 클래스',
    subtitle: '아침이 설레는 AI 완전 자동화 워크플로우',
    description:
      '반복 업무를 AI 워크플로우로 자동화하는 방법을 8주 과정으로 학습합니다. 노코드 도구부터 API 연동까지 실전 중심으로 다룹니다.',
    category: 'AI 활용(AX)',
    subCategory: '업무 생산성',
    thumbnailUrl: '/courses/ai.png',
    price: 198000,
    discountRate: 70,
    rating: 5.0,
    reviewCount: 3,
    studentCount: 110,
    level: '왕초보',
    tags: ['AI', 'Automation', 'Workflow'],
    instructor: { id: 'i4', name: 'AI LAB', title: 'AI 업무 자동화 전문가' },
    chapters: jpaChapters,
    badges: ['New'],
    status: 'PUBLISHED',
  },
  {
    id: 'docker',
    title: 'Docker 배포 기초 — 컨테이너로 배포하기',
    subtitle: '처음 배우는 컨테이너 기반 배포',
    description:
      'Docker의 기본 개념부터 이미지 빌드, 컨테이너 실행, 배포 자동화까지 입문자가 알아야 할 핵심을 다룹니다.',
    category: '개발·프로그래밍',
    subCategory: '데브옵스',
    thumbnailUrl: '/courses/docker.png',
    price: 66000,
    rating: 4.7,
    reviewCount: 142,
    studentCount: 880,
    level: '초급',
    tags: ['Docker', 'DevOps', '배포'],
    instructor: { id: 'i5', name: '이도커', title: '인프라 엔지니어' },
    chapters: jpaChapters,
    badges: ['답변 활발'],
    status: 'PUBLISHED',
  },
  {
    id: 'python',
    title: 'Advanced Python Mastery — 파이썬 심화',
    subtitle: '객체지향부터 자료구조 심화까지',
    description:
      '파이썬을 한 단계 더 깊이 있게 다루기 위한 강좌입니다. 자료구조, 객체지향 프로그래밍, 성능 최적화를 다룹니다.',
    category: '개발·프로그래밍',
    subCategory: '프로그래밍 언어',
    thumbnailUrl: '/courses/python.png',
    price: 110000,
    discountRate: 15,
    rating: 4.6,
    reviewCount: 78,
    studentCount: 410,
    level: '중급',
    tags: ['Python', 'OOP', 'Data Structures'],
    instructor: { id: 'i6', name: '박파이', title: '파이썬 시니어 개발자' },
    chapters: jpaChapters,
    badges: [],
    status: 'PUBLISHED',
  },
]

export const categories = [
  '전체',
  'AI 기술',
  'AI 활용(AX)',
  '개발·프로그래밍',
  '게임 개발',
  '데이터 사이언스',
  '보안·네트워크',
  '하드웨어',
  '디자인·아트',
  '기획·경영·마케팅',
  '외국어',
  '업무 생산성',
  '커리어·자기계발',
  '대학 교육',
]

export const qnaPosts: QnaPost[] = [
  {
    id: 'q1',
    author: '김아무개',
    role: 'student',
    timestamp: '3:58',
    content: '이부분 도무지 이해가 안됩니다. 다들 이해 되시나요?',
    createdAt: '2026.06.07 19:28',
    answers: [
      {
        id: 'a1',
        author: '선생님',
        role: 'instructor',
        timestamp: '3:59',
        content: '이 부분 설명 다시 들어보시면 이해되실 거예요. 영속성 컨텍스트가 1차 캐시 역할을 한다고 생각하시면 됩니다.',
        createdAt: '2026.06.07 19:29',
      },
    ],
  },
  {
    id: 'q2',
    author: '이학생',
    role: 'student',
    timestamp: '8:12',
    content: 'flush가 일어나는 시점이 정확히 언제인가요?',
    createdAt: '2026.06.07 20:10',
  },
]

export const activityFeed: ActivityFeedItem[] = [
  {
    id: 'f1',
    author: '김아무개',
    date: '2026.06.07',
    content: '오늘 영속성 컨텍스트 개념을 정리했습니다. 1차 캐시와 쓰기 지연 SQL 저장소의 동작 방식을 회고로 작성합니다.',
    hasAiFeedback: true,
    aiFeedback:
      '영속성 컨텍스트의 핵심을 잘 짚으셨습니다. 추가로 "동일성 보장(identity)"과 "변경 감지(dirty checking)"까지 연결해서 정리하면 이해가 더 탄탄해집니다.',
  },
  {
    id: 'f2',
    author: '이학생',
    date: '2026.06.06',
    content: '연관관계 매핑에서 mappedBy 방향을 헷갈렸는데, 연관관계의 주인 개념을 다시 학습했습니다.',
    hasAiFeedback: false,
  },
]

// --- Studies (스터디) ---
// 강좌별로 1개의 스터디가 존재한다. 강좌 id를 그대로 스터디 id로 사용한다.

const sharedBoardPosts: BoardPost[] = [
  {
    id: 'p1',
    title: '영속성 컨텍스트 1차 캐시 정리해봤어요',
    author: '김아무개',
    createdAt: '2026.06.07 19:28',
    content:
      '오늘 영속성 컨텍스트 강의를 듣고 1차 캐시와 쓰기 지연 SQL 저장소의 동작 방식을 정리했습니다.\n\nfind()를 호출하면 먼저 1차 캐시를 조회하고, 없으면 DB에서 가져와 캐시에 저장한다는 점이 인상 깊었어요. 같은 트랜잭션 안에서는 동일성(identity)이 보장된다는 것도 알게 됐습니다.',
    comments: [
      {
        id: 'c1',
        author: '이학생',
        content: '저도 1차 캐시 부분이 헷갈렸는데 정리 감사합니다!',
        createdAt: '2026.06.07 20:02',
      },
      {
        id: 'c2',
        author: '최지바',
        content: '잘 정리하셨어요. 변경 감지(dirty checking)까지 연결해보면 좋아요.',
        createdAt: '2026.06.07 21:10',
      },
    ],
    aiFeedback:
      '영속성 컨텍스트의 핵심을 정확히 짚으셨습니다. 1차 캐시·쓰기 지연을 설명하셨으니, 추가로 "변경 감지(dirty checking)"가 flush 시점에 어떻게 동작하는지 본인의 언어로 정리해보면 개념이 더 단단해집니다.',
  },
  {
    id: 'p2',
    title: '연관관계의 주인(mappedBy) 다시 학습',
    author: '이학생',
    createdAt: '2026.06.06 14:12',
    content:
      '연관관계 매핑에서 mappedBy 방향을 자주 헷갈렸는데, 연관관계의 주인은 외래 키를 가진 쪽이라는 기준으로 다시 정리했습니다. 주인이 아닌 쪽은 읽기 전용이라는 점을 기억하려고 합니다.',
    comments: [
      {
        id: 'c3',
        author: '박참여',
        content: '읽기 전용이라는 표현이 이해에 도움이 되네요.',
        createdAt: '2026.06.06 15:40',
      },
    ],
  },
  {
    id: 'p3',
    title: '오늘 학습 회고 - 프록시와 지연로딩',
    author: '박참여',
    createdAt: '2026.06.05 09:30',
    content:
      '프록시 객체가 실제 사용 시점에 초기화된다는 개념을 학습했습니다. 지연로딩과 즉시로딩의 차이, 그리고 N+1 문제가 왜 발생하는지 감을 잡았어요.',
    comments: [],
  },
]

const sharedMembers = [
  { id: 'm1', name: '김아무개', progress: 30, role: 'owner' as const, joinedAt: '2026.05.20' },
  { id: 'm2', name: '이학생', progress: 64, role: 'member' as const, joinedAt: '2026.05.22' },
  { id: 'm3', name: '박참여', progress: 48, role: 'member' as const, joinedAt: '2026.05.25' },
  { id: 'm4', name: '정수강', progress: 12, role: 'member' as const, joinedAt: '2026.06.01' },
  { id: 'm5', name: '한열공', progress: 88, role: 'member' as const, joinedAt: '2026.06.02' },
]

const sharedApplicants = [
  { id: 'ap1', name: '신청자A', appliedAt: '2026.06.08' },
  { id: 'ap2', name: '신청자B', appliedAt: '2026.06.09' },
]

const studyProgressById: Record<string, number> = {
  jpa: 30,
  spring: 64,
  frontend: 18,
  docker: 100,
  python: 0,
  ai: 0,
}

export const studies: Record<string, Study> = Object.fromEntries(
  courses.map((course, i) => {
    return [
      course.id,
      {
        id: course.id,
        courseId: course.id,
        name: `${course.title} 스터디`,
        intro: course.subtitle,
        status: 'ACTIVE',
        ownerName: course.instructor.name,
        myRole: i === 0 ? 'owner' : 'member',
        progress: studyProgressById[course.id] ?? 0,
        members: sharedMembers,
        applicants: i === 0 ? sharedApplicants : [],
        announcements: [
          {
            id: 'an1',
            title: '6월 셋째 주 스터디 진행 안내',
            content:
              '이번 주는 영속성 컨텍스트와 연관관계 매핑까지 진행합니다. 학습 후 게시판에 회고를 남겨주세요.',
            createdAt: '2026.06.07',
          },
          {
            id: 'an2',
            title: '학습 회고 작성 시 AI 코치를 활용해보세요',
            content:
              '게시글 작성 시 AI 피드백을 요청하면 학습 기록의 구체성과 보완점을 안내받을 수 있어요. (1일 3회)',
            createdAt: '2026.06.03',
          },
        ],
        posts: sharedBoardPosts,
      } satisfies Study,
    ]
  }),
)

export const coupons: Coupon[] = [
  {
    id: 'c1',
    name: '매일 오전 10시 선착순 쿠폰',
    amount: '5,000원 할인',
    condition: '선착순 1000명',
    category: '진행 중인 이벤트',
    type: 'firstcome',
  },
  {
    id: 'c2',
    name: '매일매일 출석 이벤트',
    amount: '출석 포인트 적립',
    condition: '매일 출석 시',
    category: '진행 중인 이벤트',
    type: 'attendance',
  },
  {
    id: 'c3',
    name: '작가의 날 도서 쿠폰 이벤트',
    amount: '도서 전체 10% 할인',
    condition: '7월 01일 ~ 7월 15일',
    category: '진행 중인 이벤트',
    type: 'discount',
  },
  {
    id: 'c4',
    name: '어린이날 기념 쿠폰 이벤트',
    amount: '5,000원 할인',
    condition: '종료됨',
    category: '종료된 이벤트',
    type: 'discount',
  },
  {
    id: 'c5',
    name: '스승의 날 기념 쿠폰 이벤트',
    amount: '10% 할인',
    condition: '종료됨',
    category: '종료된 이벤트',
    type: 'discount',
  },
  {
    id: 'c6',
    name: '어버이날 기념 쿠폰 이벤트',
    amount: '10% 할인',
    condition: '종료됨',
    category: '종료된 이벤트',
    type: 'discount',
  },
]

export const userProfile: UserProfile = {
  id: 'u1',
  name: '김아무개',
  email: 'emailforemail@email.com',
  studyCount: 4,
  courseCount: 7,
  isSeller: true,
}

export const orders: Order[] = [
  {
    id: 'ord1',
    orderNumber: 'ORD-2024-000120',
    orderedAt: '2024.05.15 09:15',
    status: '부분 환불',
    paymentStatus: '결제 완료/성공',
    paymentMethod: '신용/체크카드 (Mock)',
    items: [
      { courseId: 'spring', title: 'Spring Boot 입문', instructor: '김개발 강사', price: 50000, status: '정상 구매' },
      { courseId: 'jpa', title: 'JPA 기본 강의', instructor: '최지바 강사', price: 40000, status: '환불 가능' },
      { courseId: 'docker', title: 'Docker 배포 기초', instructor: '이도커 강사', price: 30000, status: '환불 완료' },
    ],
    productTotal: 120000,
    discount: 0,
    paidAmount: 120000,
    refundAmount: 30000,
  },
]

export const enrolledCourses: EnrolledCourse[] = [
  {
    id: 'jpa',
    title: '자바 ORM 표준기술 JPA for beginner',
    instructor: '최지바',
    thumbnailUrl: '/courses/jpa.png',
    progress: 30,
    totalLectures: 8,
    completedLectures: 2,
    dday: 28,
    status: '진행 중',
  },
  {
    id: 'spring',
    title: 'Spring Boot 입문 — 처음 시작하는 백엔드 개발',
    instructor: '김개발',
    thumbnailUrl: '/courses/spring.png',
    progress: 64,
    totalLectures: 8,
    completedLectures: 5,
    dday: 12,
    status: '진행 중',
  },
  {
    id: 'frontend',
    title: '미국 빅테크 프론트엔드 시스템 디자인 완성',
    instructor: '치트키맨',
    thumbnailUrl: '/courses/frontend.png',
    progress: 18,
    totalLectures: 8,
    completedLectures: 1,
    dday: 40,
    status: '진행 중',
  },
  {
    id: 'docker',
    title: 'Docker 배포 기초 — 컨테이너로 배포하기',
    instructor: '이도커',
    thumbnailUrl: '/courses/docker.png',
    progress: 100,
    totalLectures: 8,
    completedLectures: 8,
    status: '완료',
  },
]

export const purchasedCourses: EnrolledCourse[] = [
  ...enrolledCourses,
  {
    id: 'python',
    title: 'Advanced Python Mastery — 파이썬 심화',
    instructor: '박파이',
    thumbnailUrl: '/courses/python.png',
    progress: 0,
    totalLectures: 8,
    completedLectures: 0,
    status: '진행 중',
  },
  {
    id: 'ai',
    title: 'AI 업무자동화를 쉽게, 2026 업무 자동화 마스터 클래스',
    instructor: 'AI LAB',
    thumbnailUrl: '/courses/ai.png',
    progress: 0,
    totalLectures: 8,
    completedLectures: 0,
    status: '진행 중',
  },
]

export const myComments: MyComment[] = [
  {
    id: 'cm1',
    courseTitle: '자바 ORM 표준기술 JPA for beginner',
    lectureTitle: '1-1 영속성의 이해',
    content: '이부분 도무지 이해가 안됩니다. 다들 이해 되시나요?',
    createdAt: '2026.06.07 19:28',
  },
  {
    id: 'cm2',
    courseTitle: 'Spring Boot 입문',
    lectureTitle: '2-3 의존성 주입',
    content: '생성자 주입과 필드 주입의 차이가 명확해졌어요. 감사합니다!',
    createdAt: '2026.06.05 14:02',
  },
  {
    id: 'cm3',
    courseTitle: 'Docker 배포 기초',
    lectureTitle: '3-1 이미지 빌드',
    content: 'Dockerfile 캐시 레이어 설명이 정말 유익했습니다.',
    createdAt: '2026.05.30 21:47',
  },
]

export const studyReport: StudyReport = {
  studyName: '[8th Layer] 스터디',
  userName: '김아무개',
  period: '2026.06.07 - 2026.07.07',
  totalStudyTime: '32h 15m',
  commentCount: 128,
  studyDays: 24,
  progressData: [
    { day: '1주차', progress: 12, minutes: 320 },
    { day: '2주차', progress: 30, minutes: 480 },
    { day: '3주차', progress: 48, minutes: 410 },
    { day: '4주차', progress: 67, minutes: 520 },
    { day: '5주차', progress: 82, minutes: 460 },
    { day: '6주차', progress: 100, minutes: 545 },
  ],
  // 최근 1년치(53주 = 371일) 데이터. 일요일에서 시작하도록 정렬해 한 열이 한 주가 된다.
  calendar: (() => {
    const days = 371
    const today = new Date()
    const start = new Date(today)
    start.setDate(today.getDate() - (days - 1))
    // 시작일을 그 주의 일요일로 맞춰 첫 열이 온전한 한 주가 되도록 한다.
    start.setDate(start.getDate() - start.getDay())
    return Array.from({ length: days }, (_, i) => {
      const d = new Date(start)
      d.setDate(start.getDate() + i)
      // 결정적인 의사난수로 활동 여부와 강도를 생성
      const seed = (i * 1103515245 + 12345) & 0x7fffffff
      const level = d > today ? 0 : seed % 5 === 0 ? 0 : seed % 7 === 0 ? 0 : (seed % 3) + 1
      return {
        date: d.toISOString().slice(0, 10),
        active: level > 0,
        level,
      }
    })
  })(),
  topLectures: ['Spring Core', 'JPA 기초', 'Docker 입문'],
}

// --- Admin: coupon policies ---
export const adminCoupons: AdminCoupon[] = [
  {
    id: 1001,
    name: '신규가입 1만원 할인',
    totalQuantity: null,
    type: 'AUTO',
    target: 'ALL',
    useType: 'SINGLE',
    stackable: false,
    discountType: 'AMOUNT',
    discountValue: 10000,
    maxDiscount: 10000,
    minOrderAmount: 30000,
    validDays: 30,
    startAt: '2026-06-01T00:00',
    endAt: '2026-12-31T23:59',
    status: 'ACTIVE',
    issuedCount: 1342,
  },
  {
    id: 1002,
    name: '여름 개발강좌 20% 할인',
    totalQuantity: 500,
    type: 'NORMAL',
    target: 'CATEGORY',
    useType: 'SINGLE',
    stackable: false,
    discountType: 'RATE',
    discountValue: 20,
    maxDiscount: 30000,
    minOrderAmount: 50000,
    validDays: 14,
    startAt: '2026-07-01T00:00',
    endAt: '2026-07-31T23:59',
    status: 'SCHEDULED',
    issuedCount: 0,
    targetCategories: ['개발·프로그래밍', 'AI 기술'],
  },
  {
    id: 1003,
    name: '선착순 5천원 쿠폰 (FCFS)',
    totalQuantity: 1000,
    type: 'FCFS',
    target: 'ALL',
    useType: 'SINGLE',
    stackable: true,
    discountType: 'AMOUNT',
    discountValue: 5000,
    maxDiscount: 5000,
    minOrderAmount: 20000,
    validDays: 7,
    startAt: '2026-06-10T10:00',
    endAt: '2026-06-30T23:59',
    status: 'ACTIVE',
    issuedCount: 743,
  },
  {
    id: 1004,
    name: 'JPA 마스터 강좌 단독 할인',
    totalQuantity: 200,
    type: 'NORMAL',
    target: 'COURSE',
    useType: 'MULTI',
    stackable: false,
    discountType: 'RATE',
    discountValue: 15,
    maxDiscount: 20000,
    minOrderAmount: 0,
    validDays: null,
    startAt: '2026-03-01T00:00',
    endAt: '2026-05-31T23:59',
    status: 'ENDED',
    issuedCount: 200,
    targetCourses: ['JPA 완전정복', 'Spring Boot 실전'],
  },
  {
    id: 1005,
    name: '출석 이벤트 적립 쿠폰',
    totalQuantity: null,
    type: 'AUTO',
    target: 'ALL',
    useType: 'MULTI',
    stackable: true,
    discountType: 'AMOUNT',
    discountValue: 3000,
    maxDiscount: 3000,
    minOrderAmount: 10000,
    validDays: 60,
    startAt: '2026-05-01T00:00',
    endAt: '2026-09-30T23:59',
    status: 'ACTIVE',
    issuedCount: 2890,
  },
]
