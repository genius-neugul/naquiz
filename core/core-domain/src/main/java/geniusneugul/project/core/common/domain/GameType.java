package geniusneugul.project.core.common.domain;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import java.util.Arrays;
import java.util.Map;

public enum GameType {
    SONG,
    MOVIE_TWENTY_QUESTIONS,
    MOVIE_STILL_CUT;

    public static GameType from(String code) {
        return Arrays.stream(values())
                .filter(gameType -> gameType.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_INVALID_TYPE, Map.of("gameType", String.valueOf(code))));
    }
}
