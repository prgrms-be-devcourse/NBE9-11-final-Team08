//package com.team08.backend.global.error;
//
//import jakarta.validation.ConstraintViolationException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.method.annotation.HandlerMethodValidationException;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.List;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
//        List<ErrorResponse.FieldErrorResponse> fieldErrors = exception.getBindingResult()
//                .getFieldErrors()
//                .stream()
//                .map(this::toFieldErrorResponse)
//                .toList();
//
//        return ResponseEntity.badRequest()
//                .body(ErrorResponse.of(
//                        HttpStatus.BAD_REQUEST.value(),
//                        "VALIDATION_ERROR",
//                        "요청 본문 값이 올바르지 않습니다.",
//                        fieldErrors
//                ));
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
//        List<ErrorResponse.FieldErrorResponse> fieldErrors = exception.getConstraintViolations()
//                .stream()
//                .map(violation -> new ErrorResponse.FieldErrorResponse(
//                        violation.getPropertyPath().toString(),
//                        String.valueOf(violation.getInvalidValue()),
//                        violation.getMessage()
//                ))
//                .toList();
//
//        return ResponseEntity.badRequest()
//                .body(ErrorResponse.of(
//                        HttpStatus.BAD_REQUEST.value(),
//                        "VALIDATION_ERROR",
//                        "요청 파라미터 값이 올바르지 않습니다.",
//                        fieldErrors
//                ));
//    }
//
//    @ExceptionHandler(HandlerMethodValidationException.class)
//    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
//        List<ErrorResponse.FieldErrorResponse> fieldErrors = exception.getParameterValidationResults()
//                .stream()
//                .flatMap(result -> result.getResolvableErrors()
//                        .stream()
//                        .map(error -> new ErrorResponse.FieldErrorResponse(
//                                result.getMethodParameter().getParameterName(),
//                                String.valueOf(result.getArgument()),
//                                error.getDefaultMessage()
//                        )))
//                .toList();
//
//        return ResponseEntity.badRequest()
//                .body(ErrorResponse.of(
//                        HttpStatus.BAD_REQUEST.value(),
//                        "VALIDATION_ERROR",
//                        "요청 파라미터 값이 올바르지 않습니다.",
//                        fieldErrors
//                ));
//    }
//
//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable() {
//        return ResponseEntity.badRequest()
//                .body(ErrorResponse.of(
//                        HttpStatus.BAD_REQUEST.value(),
//                        "INVALID_REQUEST_BODY",
//                        "요청 본문 형식이 올바르지 않습니다.",
//                        List.of()
//                ));
//    }
//
//    @ExceptionHandler(ResponseStatusException.class)
//    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception) {
//        int status = exception.getStatusCode().value();
//        String reason = exception.getReason() == null ? "요청을 처리할 수 없습니다." : exception.getReason();
//
//        return ResponseEntity.status(exception.getStatusCode())
//                .body(ErrorResponse.of(
//                        status,
//                        exception.getStatusCode().toString(),
//                        reason,
//                        List.of()
//                ));
//    }
//
//    private ErrorResponse.FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
//        return new ErrorResponse.FieldErrorResponse(
//                fieldError.getField(),
//                String.valueOf(fieldError.getRejectedValue()),
//                fieldError.getDefaultMessage()
//        );
//    }
//}
