package geniusneugul.project.core.game.twentyquestions.domain;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import java.util.Arrays;
import java.util.Map;

/**
 * 영화 스무고개 단서 종류.
 */
public enum ClueType {
    AUDIENCE,
    RELEASE_DATE,
    DIRECTOR,
    CAST,
    SYNOPSIS,
    GENRE,
    NATION,
    RATING;

    public static ClueType from(String code) {
        return Arrays.stream(values())
                .filter(clueType -> clueType.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_INVALID_CLUE_TYPE, Map.of("clueType", String.valueOf(code))));
    }
}
