package geniusneugul.project.core.room.domain;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 방 입장용 초대 코드. 숫자·영문 대문자 6자리다(docs/DOMAIN.md 3-1). 내부 식별자인 방 ID와 구분한다.
 */
public record InviteCode(String value) {

    public static final int LENGTH = 6;
    public static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9]{" + LENGTH + "}");

    /** 사용자가 입력한 코드. 앞뒤 공백을 빼고 대문자로 바꿔 받는다 */
    public static InviteCode from(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.ROOM_INVALID_INVITE_CODE);
        }
        return new InviteCode(raw.strip().toUpperCase(Locale.ROOT));
    }

    public InviteCode {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.ROOM_INVALID_INVITE_CODE);
        }
    }
}
