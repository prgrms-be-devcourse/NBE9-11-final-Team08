package com.team08.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 ──────────────────────────────────────────────────────────────
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다."),

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

    // ── Course ───────────────────────────────────────────────────────────
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND,      "COURSE_001", "강좌를 찾을 수 없습니다."),

    // ── Lecture ──────────────────────────────────────────────────────────
    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND,     "LECTURE_001", "강의를 찾을 수 없습니다."),
    CHAPTER_NOT_FOUND(HttpStatus.NOT_FOUND,     "LECTURE_002", "챕터를 찾을 수 없습니다."),
    LECTURE_NOT_FOUND_IN_CHAPTER(HttpStatus.NOT_FOUND,     "LECTURE_003", "챕터에 강의가 없습니다."),
    RECENT_LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND,     "LECTURE_004", "최근 수강한 강의가 없습니다."),
    RETROSPECTION_NOT_FOUND(HttpStatus.NOT_FOUND,     "LECTURE_005", "회고를 찾을 수 없습니다."),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST,   "LECTURE_006", "부모 댓글이 올바르지 않습니다."),

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

    // ── Report ───────────────────────────────────────────────────────────
    NOT_STUDY_MEMBER(HttpStatus.FORBIDDEN,      "REPORT_001", "스터디 구성원이 아닙니다."),
    NOT_CURRENT_STUDY_MEMBER(HttpStatus.FORBIDDEN,      "REPORT_002", "현재 소속된 스터디 구성원이 아닙니다."),
    STUDY_NOT_FINISHED(HttpStatus.BAD_REQUEST,    "REPORT_003", "스터디 기간이 아직 완료되지 않았습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}