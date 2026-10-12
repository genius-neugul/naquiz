package geniusneugul.project.core.common.exception;

import geniusneugul.project.core.common.domain.GameType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import tools.jackson.databind.exc.InvalidFormatException;

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

    /** 본문을 요청 DTO로 바꾸지 못했다. 없는 게임 종류면 게임 종류 오류로, 그 밖에는 요청 형식 오류로 돌려준다 */
    @MessageExceptionHandler(MessageConversionException.class)
    @SendToUser(destinations = ERROR_QUEUE, broadcast = false)
    public ErrorResponse handleMessageConversion(MessageConversionException e) {
        ErrorCode errorCode = isInvalidGameType(e) ? ErrorCode.GAME_INVALID_TYPE : ErrorCode.COMMON_INVALID_REQUEST;
        log.warn("[StompExceptionHandler.handleMessageConversion] code={}, message={}", errorCode.name(), e.getMessage());
        return ErrorResponse.of(errorCode);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = ERROR_QUEUE, broadcast = false)
    public ErrorResponse handleException(Exception e) {
        log.error("[StompExceptionHandler.handleException] {}", e.getMessage(), e);
        return ErrorResponse.of(ErrorCode.COMMON_INTERNAL_ERROR);
    }

    private boolean isInvalidGameType(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof InvalidFormatException invalid && invalid.getTargetType() == GameType.class) {
                return true;
            }
        }
        return false;
    }
}
