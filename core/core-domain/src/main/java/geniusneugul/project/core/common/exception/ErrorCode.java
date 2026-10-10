package geniusneugul.project.core.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 프로젝트의 모든 에러 코드. 형식은 {DOMAIN}_{REASON}이고 도메인별로 모아 적는다(docs/EXCEPTION.md).
 */
@Getter
public enum ErrorCode {

    // common
    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다."),

    // game
    GAME_NO_MORE_ANSWER_HINT(HttpStatus.CONFLICT, "더 공개할 정답 힌트가 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
