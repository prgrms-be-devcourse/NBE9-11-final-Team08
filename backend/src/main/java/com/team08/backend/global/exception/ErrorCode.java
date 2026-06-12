package com.team08.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 ──────────────────────────────────────────────────────────────
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 파라미터 요청입니다. 입력 값을 확인해주세요."),

    // ── Auth ─────────────────────────────────────────────────────────────
    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "AUTH_001", "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_002", "이미 존재하는 이메일입니다."),
    INVALID_SIGNUP_ROLE(HttpStatus.BAD_REQUEST, "AUTH_003", "회원가입에 불가능한 역할입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_004", "인증이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_005", "유효하지 않은 refresh token입니다."),

    // ── User ─────────────────────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,   "USER_001", "사용자를 찾을 수 없습니다."),

    // ── Study ────────────────────────────────────────────────────────────
    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY_001", "스터디를 찾을 수 없습니다."),
    STUDY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,       "STUDY_002", "스터디 멤버를 찾을 수 없습니다."),
    STUDY_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND,       "STUDY_003", "참여 신청을 찾을 수 없습니다."),
    STUDY_POST_NOT_FOUND(HttpStatus.NOT_FOUND,       "STUDY_004", "게시글을 찾을 수 없습니다."),
    STUDY_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND,       "STUDY_005", "댓글을 찾을 수 없습니다."),
    STUDY_ACCESS_DENIED(HttpStatus.FORBIDDEN,       "STUDY_006", "스터디에 접근할 권한이 없습니다."),
    STUDY_ALREADY_MEMBER(HttpStatus.CONFLICT,        "STUDY_007", "이미 참여 중인 스터디입니다."),
    STUDY_APPLICATION_ALREADY_EXISTS(HttpStatus.CONFLICT,        "STUDY_008", "이미 참여 신청한 스터디입니다."),
    KICKED_MEMBER_CANNOT_APPLY(HttpStatus.FORBIDDEN,       "STUDY_009", "강퇴된 사용자는 다시 참여 신청할 수 없습니다."),
    STUDY_OWNER_CANNOT_BE_KICKED(HttpStatus.BAD_REQUEST,     "STUDY_010", "스터디 생성자는 강퇴할 수 없습니다."),
    STUDY_OWNER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST,     "STUDY_011", "스터디 생성자는 탈퇴할 수 없습니다."),
    STUDY_NOT_RECRUITING(HttpStatus.CONFLICT,        "STUDY_012", "모집 중인 스터디가 아닙니다."),
    STUDY_NOT_EDITABLE(HttpStatus.BAD_REQUEST,     "STUDY_013", "수정할 수 없는 상태입니다."),
    INVALID_STUDY_STATUS_TRANSITION(HttpStatus.BAD_REQUEST,     "STUDY_014", "잘못된 스터디 상태 변경입니다."),
    EMPTY_STUDY_TITLE(HttpStatus.BAD_REQUEST,     "STUDY_015", "스터디 제목은 빈값을 허용하지 않습니다."),
    INVALID_STUDY_DATE_RANGE(HttpStatus.BAD_REQUEST,     "STUDY_016", "스터디 종료일자는 시작일자 이후여야 합니다."),
    STUDY_APPLICATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST,     "STUDY_017", "이미 처리된 참여 신청입니다."),
    INVALID_MEMBER_ROLE_CHANGE(HttpStatus.BAD_REQUEST,     "STUDY_018", "변경할 수 없는 멤버 역할입니다."),
    INVALID_MEMBER_STATUS(HttpStatus.BAD_REQUEST,     "STUDY_019", "처리할 수 없는 멤버 상태입니다."),
    DUPLICATE_STUDY(HttpStatus.CONFLICT, "STUDY_020", "코스의 스터디가 이미 존재합니다."),
    STUDY_NOT_ACTIVE(HttpStatus.CONFLICT, "STUDY_021", "활성화된 스터디가 아닙니다."),
    STUDY_ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY_022", "스터디 활동을 찾을 수 없습니다."),
    STUDY_ACTIVITY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "STUDY_023", "스터디 활동을 수정할 권한이 없습니다."),
    AI_FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "STUDY_024", "AI 피드백을 찾을 수 없습니다."),
    AI_FEEDBACK_REQUEST_DENIED(HttpStatus.FORBIDDEN, "STUDY_025", "AI 피드백을 요청할 권한이 없습니다."),
    AI_FEEDBACK_GENERATION_IN_PROGRESS(HttpStatus.CONFLICT, "STUDY_026", "AI 피드백을 생성 중입니다."),
    AI_FEEDBACK_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "STUDY_027", "AI 피드백 생성에 실패했습니다."),

    // ── Course ───────────────────────────────────────────────────────────
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND,      "COURSE_001", "강좌를 찾을 수 없습니다."),
    COURSE_NOT_ON_SALE(HttpStatus.CONFLICT, "COURSE_002", "판매 중인 강의만 장바구니에 담을 수 있습니다."),
    UNAUTHORIZED_COURSE_OWNER(HttpStatus.FORBIDDEN, "COURSE_003", "해당 강좌의 소유자가 아닙니다."),
    INVALID_COURSE_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "COURSE_004", "변경할 수 없는 강좌 상태입니다."),
    COURSE_CURRICULUM_EMPTY(HttpStatus.BAD_REQUEST, "COURSE_005", "강좌에 최소 1개 이상의 챕터와 강의가 존재해야 심사를 요청할 수 있습니다."),
    REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "COURSE_006", "강좌 심사 반려 시 사유는 필수입니다."),

    // ── Lecture ──────────────────────────────────────────────────────────
    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND,     "LECTURE_001", "강의를 찾을 수 없습니다."),
    CHAPTER_NOT_FOUND(HttpStatus.NOT_FOUND,     "LECTURE_002", "챕터를 찾을 수 없습니다."),
    LECTURE_NOT_FOUND_IN_CHAPTER(HttpStatus.NOT_FOUND,     "LECTURE_003", "챕터에 강의가 없습니다."),
    RECENT_LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND,     "LECTURE_004", "최근 수강한 강의가 없습니다."),
    RETROSPECTION_NOT_FOUND(HttpStatus.NOT_FOUND,     "LECTURE_005", "회고를 찾을 수 없습니다."),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST,   "LECTURE_006", "부모 댓글이 올바르지 않습니다."),
    REFLECTION_ALREADY_EXISTS(HttpStatus.CONFLICT,    "LECTURE_007", "이미 작성된 회고가 있습니다."),
    REFLECTION_ACCESS_DENIED(HttpStatus.FORBIDDEN,    "LECTURE_008", "회고를 수정할 권한이 없습니다."),

    // ── Order ────────────────────────────────────────────────────────────
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND,       "ORDER_001", "주문을 찾을 수 없습니다."),
    EMPTY_CART(HttpStatus.BAD_REQUEST,     "ORDER_002", "장바구니가 비어 있습니다."),
    LECTURE_ALREADY_ENROLLED(HttpStatus.CONFLICT,        "ORDER_003", "이미 수강 중인 강의입니다."),
    PAID_ORDER_CANNOT_BE_CANCELED(HttpStatus.CONFLICT,        "ORDER_004", "결제 완료 주문은 취소할 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT,        "ORDER_005", "취소할 수 없는 주문 상태입니다."),
    INVALID_ORDER_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "ORDER_006", "잘못된 주문 상태 전이입니다."),

    // ── Enrollment ───────────────────────────────────────────────────────
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND,  "ENROLLMENT_001", "수강 중인 강의를 찾을 수 없습니다."),
    INVALID_ENROLLMENT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "ENROLLMENT_002", "잘못된 수강권 상태 전이입니다."),

    // ── Payment ──────────────────────────────────────────────────────────
    INVALID_PAYMENT_ORDER_STATUS(HttpStatus.CONFLICT,      "PAYMENT_001", "결제할 수 없는 주문 상태입니다."),
    ORDER_ALREADY_PAID(HttpStatus.CONFLICT,      "PAYMENT_002", "이미 결제 완료된 주문입니다."),
    INVALID_PAYMENT_FAILURE_STATUS(HttpStatus.CONFLICT,      "PAYMENT_003", "결제 실패 처리할 수 없는 주문 상태입니다."),
    INVALID_PAYMENT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "PAYMENT_004", "잘못된 결제 상태 전이입니다."),

    // ── Cart ─────────────────────────────────────────────────────────────
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND,        "CART_001", "장바구니 항목을 찾을 수 없습니다."),
    LECTURE_ALREADY_IN_CART(HttpStatus.CONFLICT,         "CART_002", "이미 장바구니에 담긴 강의입니다."),

    // ── QnA ──────────────────────────────────────────────────────────────
    QNA_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND,   "QNA_001", "QnA 질문을 찾을 수 없습니다."),
    QNA_ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND,     "QNA_002", "QnA 답변을 찾을 수 없습니다."),
    QNA_ACCESS_DENIED(HttpStatus.FORBIDDEN,        "QNA_003", "QnA에 접근할 권한이 없습니다."),
    QNA_ANSWER_ALREADY_EXISTS(HttpStatus.CONFLICT, "QNA_004", "이미 답변이 존재합니다."),
    QNA_ALREADY_ANSWERED(HttpStatus.CONFLICT,      "QNA_004", "이미 답변된 질문은 수정/삭제할 수 없습니다."),
    INSTRUCTOR_ONLY(HttpStatus.FORBIDDEN,          "QNA_005", "강사만 접근 가능합니다."),

    // ── Report ───────────────────────────────────────────────────────────
    NOT_STUDY_MEMBER(HttpStatus.FORBIDDEN,      "REPORT_001", "스터디 구성원이 아닙니다."),
    NOT_CURRENT_STUDY_MEMBER(HttpStatus.FORBIDDEN,      "REPORT_002", "현재 소속된 스터디 구성원이 아닙니다."),
    STUDY_NOT_FINISHED(HttpStatus.BAD_REQUEST,    "REPORT_003", "스터디 기간이 아직 완료되지 않았습니다."),

    // ── Attendance ───────────────────────────────────────────────────────
    ATTENDANCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "ATTENDANCE_001", "오늘은 이미 출석하셨습니다."),

    // ── Coupon ───────────────────────────────────────────────────────────
    COUPON_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_001", "쿠폰 정책을 찾을 수 없습니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "COUPON_002", "이미 발급받은 쿠폰입니다."),
    INVALID_COUPON_TYPE(HttpStatus.BAD_REQUEST, "COUPON_003", "해당 발급 방식에 적합한 쿠폰이 아닙니다."),
    COUPON_EXHAUSTED(HttpStatus.CONFLICT, "COUPON_004", "선착순 쿠폰이 모두 소진되었습니다."),
    COUPON_ISSUE_PERIOD_NOT_STARTED(HttpStatus.BAD_REQUEST, "COUPON_005", "아직 쿠폰 발급 기간이 시작되지 않았습니다."),
    COUPON_ISSUE_PERIOD_ENDED(HttpStatus.BAD_REQUEST, "COUPON_006", "쿠폰 발급 기간이 종료되었습니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_007", "존재하지 않는 쿠폰입니다."),
    COUPON_NOT_OWNED(HttpStatus.FORBIDDEN, "COUPON_008", "본인의 쿠폰만 사용할 수 있습니다."),
    COUPON_ALREADY_USED_OR_EXPIRED(HttpStatus.BAD_REQUEST, "COUPON_009", "사용할 수 없는 쿠폰 상태입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}