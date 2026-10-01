package geniusneugul.project.core.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * API 에러 응답. errors는 요청 값 검증 실패일 때만 내려간다(docs/EXCEPTION.md).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String code, String message, List<FieldError> errors) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errors);
    }

    public record FieldError(String field, String message) {
    }
}
