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

    // room
    ROOM_INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "초대 코드는 숫자·영문 6자리입니다."),
    ROOM_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 다른 방에 들어가 있습니다."),
    ROOM_INVITE_CODE_EXHAUSTED(HttpStatus.SERVICE_UNAVAILABLE, "초대 코드를 만들지 못했습니다. 잠시 후 다시 시도해 주세요."),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다. 초대 코드를 확인해 주세요."),
    ROOM_FULL(HttpStatus.CONFLICT, "방 인원이 가득 찼습니다."),
    ROOM_ALREADY_PLAYING(HttpStatus.CONFLICT, "게임이 진행 중인 방에는 들어갈 수 없습니다."),

    // room - 불변식 위반. 응답에는 쓰지 않고 IllegalStateException 메시지로만 쓴다(docs/EXCEPTION.md 「불변식 위반」)
    ROOM_ID_ALREADY_ASSIGNED(HttpStatus.INTERNAL_SERVER_ERROR, "방 ID는 한 번만 부여합니다."),
    ROOM_PARTICIPANT_ID_ALREADY_ASSIGNED(HttpStatus.INTERNAL_SERVER_ERROR, "참가자 ID는 한 번만 부여합니다."),
    ROOM_HOST_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "방장이 없는 방입니다."),
    ROOM_INVITE_CODE_DUPLICATED(HttpStatus.INTERNAL_SERVER_ERROR, "이미 쓰고 있는 초대 코드입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
