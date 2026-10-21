package geniusneugul.project.core.game.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import geniusneugul.project.core.support.IntegrationTestSupport;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CountdownTimerTest extends IntegrationTestSupport {

    private static final Duration COUNTDOWN = Duration.ofMillis(100);
    /** 콜백이 실행되지 않았음을 확인하려고 지켜보는 시간. 카운트다운보다 충분히 길게 둔다 */
    private static final Duration WATCH = Duration.ofMillis(500);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Autowired
    private CountdownTimer countdownTimer;

    @DisplayName("카운트다운 시간이 지나면 콜백이 실행되고, 마감 시각은 시작 시각에서 그 시간만큼 뒤다.")
    @Test
    void start() {
        // given
        AtomicInteger ended = new AtomicInteger();

        // when
        Countdown countdown = countdownTimer.start(1001L, CountdownKind.STILL_CUT, COUNTDOWN, ended::incrementAndGet);

        // then
        assertThat(countdown.deadline()).isEqualTo(countdown.startedAt().plus(COUNTDOWN));
        await().atMost(TIMEOUT).until(() -> ended.get() == 1);
    }

    @DisplayName("같은 방에서 같은 종류의 카운트다운을 다시 걸면 이전 카운트다운은 실행되지 않는다.")
    @Test
    void start_restart() {
        // given
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        countdownTimer.start(1002L, CountdownKind.STILL_CUT, COUNTDOWN, first::incrementAndGet);

        // when
        countdownTimer.start(1002L, CountdownKind.STILL_CUT, COUNTDOWN, second::incrementAndGet);

        // then
        await().atMost(TIMEOUT).until(() -> second.get() == 1);
        await().during(WATCH).atMost(TIMEOUT).until(() -> first.get() == 0);
    }

    @DisplayName("방의 카운트다운을 취소하면 그 방의 카운트다운은 모두 실행되지 않는다.")
    @Test
    void cancelRoom() {
        // given
        AtomicInteger ended = new AtomicInteger();
        countdownTimer.start(1003L, CountdownKind.STILL_CUT, COUNTDOWN, ended::incrementAndGet);
        countdownTimer.start(1003L, CountdownKind.NEXT_ROUND, COUNTDOWN, ended::incrementAndGet);

        // when
        countdownTimer.cancelRoom(1003L);

        // then
        await().during(WATCH).atMost(TIMEOUT).until(() -> ended.get() == 0);
    }
}
