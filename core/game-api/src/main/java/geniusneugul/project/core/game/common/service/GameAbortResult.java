package geniusneugul.project.core.game.common.service;

import java.util.List;

/**
 * 다음 라운드 문제를 준비하지 못해 게임이 승자 없이 끝났다. 방은 대기 상태로 돌아갔다.
 */
public record GameAbortResult(Long roomId, Long gameId, List<ScoreResult> scores) {
}
