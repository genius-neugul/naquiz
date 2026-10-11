package geniusneugul.project.core.common.domain;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import java.util.Arrays;
import java.util.Map;

/**
 * 게임 종류. 게임과 문제가 함께 쓰므로 common에 둔다.
 */
public enum GameType {
    SONG,
    MOVIE_TWENTY_QUESTIONS,
    MOVIE_STILL_CUT;

    /** 요청으로 받은 게임 종류 코드 */
    public static GameType from(String code) {
        return Arrays.stream(values())
                .filter(gameType -> gameType.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_INVALID_TYPE, Map.of("gameType", String.valueOf(code))));
    }
}
