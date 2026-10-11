package geniusneugul.project.core.common.exception;

import java.util.Map;
import lombok.Getter;

/**
 * 비즈니스 규칙 위반. 사용자 메시지는 ErrorCode만 쓰고, context는 로그에만 남기는 디버깅 정보다(docs/EXCEPTION.md).
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> context;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, Map.of(), null);
    }

    public BusinessException(ErrorCode errorCode, Map<String, Object> context) {
        this(errorCode, context, null);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, Map.of(), cause);
    }

    public BusinessException(ErrorCode errorCode, Map<String, Object> context, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.context = Map.copyOf(context);
    }
}
