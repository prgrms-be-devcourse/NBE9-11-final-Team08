// frontend/lib/hooks/use-learning-session.ts
'use client'

import { useCallback } from 'react'
import { api } from '@/lib/api'
import type {
  LearningEventType,
  LectureEnterResponse,
  LectureProgressResponse,
} from '@/lib/types'

interface UseLearningSessionOptions {
  courseId: number
  // 강의 ID로 챕터 ID를 찾는다(이벤트 적재에 chapterId가 필요).
  chapterIdOf: (lectureId?: string) => string | undefined
}

/**
 * 한 학습 세션(입장→재생→하트비트→완료→퇴장)에서 "어떤 API를, 어떤 순서로 호출하는가"를
 * 한 곳에 모은 훅. 컴포넌트는 의미 단위 메서드만 호출하고, 호출 구성/순서는 여기서 소유한다.
 *
 * 채널 분리는 그대로 유지한다:
 *  - 하트비트/진행 → 전용 PATCH(updateLectureProgress)  (learning_events 에는 안 씀)
 *  - 이산 이벤트(입장/재생/완료/퇴장) → POST(recordLearningEvent)
 *  - 입장 → GET(enterLecture, 메타·이어보기·수강권 응답이 필요한 쿼리)
 */
export function useLearningSession({ courseId, chapterIdOf }: UseLearningSessionOptions) {
  // 학습 이벤트 적재(best-effort) — 어떤 동작이 어떤 이벤트를 남기는지 이 모듈이 소유한다.
  const record = useCallback(
    (lectureId: string, eventType: LearningEventType, positionSeconds: number) => {
      const chapterId = chapterIdOf(lectureId)
      api
        .recordLearningEvent({
          eventType,
          lectureId: Number(lectureId),
          courseId: Number.isFinite(courseId) ? courseId : undefined,
          chapterId: chapterId ? Number(chapterId) : undefined,
          positionSeconds,
        })
        .catch(() => {
          /* 로그 적재 실패는 학습 흐름을 막지 않는다 */
        })
    },
    [chapterIdOf, courseId],
  )

  // 입장: 메타/진행 정보 로드 + LECTURE_ENTER 기록. null 이면 입장 불가(수강권 없음).
  const enter = useCallback(
    async (lectureId: string): Promise<LectureEnterResponse | null> => {
      const res = await api.enterLecture(courseId, chapterIdOf(lectureId) ?? '', lectureId)
      if (res) record(lectureId, 'LECTURE_ENTER', 0)
      return res
    },
    [courseId, chapterIdOf, record],
  )

  // 재생 시작 / 일시정지 — VIDEO_START / VIDEO_END
  const play = useCallback(
    (lectureId: string, position: number) => record(lectureId, 'VIDEO_START', position),
    [record],
  )
  const pause = useCallback(
    (lectureId: string, position: number) => record(lectureId, 'VIDEO_END', position),
    [record],
  )

  // 하트비트: 진행분만 전용 PATCH 로 보고(이벤트 적재 없음). 실패는 null.
  const beat = useCallback(
    (lectureId: string, position: number, delta: number): Promise<LectureProgressResponse | null> =>
      api.updateLectureProgress(lectureId, position, delta).catch(() => null),
    [],
  )

  // 수강 완료: 진행 PATCH 먼저, 그 다음 LECTURE_COMPLETE. (실패는 호출측에서 처리하도록 전파)
  const complete = useCallback(
    async (
      lectureId: string,
      position: number,
      delta: number,
    ): Promise<LectureProgressResponse> => {
      const res = await api.updateLectureProgress(lectureId, position, delta)
      record(lectureId, 'LECTURE_COMPLETE', position)
      return res
    },
    [record],
  )

  // 퇴장: 마지막 위치 flush(PATCH delta=0) 먼저, 그 다음 LECTURE_EXIT.
  const exit = useCallback(
    (lectureId: string, position: number) => {
      api.updateLectureProgress(lectureId, position, 0).catch(() => {})
      record(lectureId, 'LECTURE_EXIT', position)
    },
    [record],
  )

  return { enter, play, pause, beat, complete, exit }
}
