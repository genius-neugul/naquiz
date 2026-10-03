package geniusneugul.project.core.game.common.presentation;

import java.time.LocalDateTime;

/**
 * 시작한 카운트다운. deadline은 화면에 마감 시각(nextAt 등)으로 내려가 남은 시간을 그리는 데 쓴다.
 */
public record Countdown(LocalDateTime startedAt, LocalDateTime deadline) {
}
