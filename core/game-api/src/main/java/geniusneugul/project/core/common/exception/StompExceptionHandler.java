package geniusneugul.project.core.common.exception;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * STOMP 메시지 처리 중 난 예외를 메시지를 보낸 참가자에게만 돌려준다(docs/EXCEPTION.md 「실시간 메시지(STOMP) 에러 처리」).
 */
@Slf4j
@ControllerAdvice
public class StompExceptionHandler {

    private static final String ERROR_QUEUE = "/queue/errors";

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser(destinations = ERROR_QUEUE, broadcast = false)
    public ErrorResponse handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[StompExceptionHandler.handleBusinessException] code={}, context={}",
                errorCode.name(), e.getContext(), e.getCause());
        return ErrorResponse.of(errorCode);
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(destinations = ERROR_QUEUE, broadcast = false)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<ErrorResponse.FieldError> errors = bindingResult == null ? List.of()
                : bindingResult.getFieldErrors().stream()
                        .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                        .toList();
        log.warn("[StompExceptionHandler.handleMethodArgumentNotValid] errors={}", errors);
        return ErrorResponse.of(ErrorCode.COMMON_INVALID_REQUEST, errors);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = ERROR_QUEUE, broadcast = false)
    public ErrorResponse handleException(Exception e) {
        log.error("[StompExceptionHandler.handleException] {}", e.getMessage(), e);
        return ErrorResponse.of(ErrorCode.COMMON_INTERNAL_ERROR);
    }
}
