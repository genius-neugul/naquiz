package geniusneugul.project.core.vote.domain;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.HintType;
import java.util.Arrays;
import java.util.Map;

/**
 * 투표 종류. 힌트 투표는 공개할 힌트 종류(대상)가 있어야 하고, 스킵 투표는 대상이 없다.
 */
public enum VoteType {
    HINT(true),
    SKIP(false);

    private final boolean targeted;

    VoteType(boolean targeted) {
        this.targeted = targeted;
    }

    public static VoteType from(String code) {
        return Arrays.stream(values())
                .filter(voteType -> voteType.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_INVALID_TYPE, Map.of("voteType", String.valueOf(code))));
    }

    /** 대상 힌트 종류 코드. 대상이 없으면 null이다 */
    public static HintType targetFrom(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(HintType.values())
                .filter(hintType -> hintType.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_INVALID_TARGET, Map.of("targetHintType", code)));
    }

    public boolean isValidTarget(HintType targetHintType) {
        return targeted == (targetHintType != null);
    }
}
