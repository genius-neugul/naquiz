package geniusneugul.project.core.game.common.presentation;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

/**
 * 게임 서버 카운트다운. 정해진 시간이 지나면 콜백을 실행한다(다음 라운드, 스틸컷 교체, 이후 스무고개 차례 패스).
 * 한 방에서 같은 종류는 하나만 돌고, 다시 걸면 이전 것을 취소한다.
 * 취소는 정리용이다. 취소와 실행이 엇갈릴 수 있으므로 콜백은 실행될 때 라운드가 그대로인지 다시 확인해야 한다.
 */
@Slf4j
@Component
public class CountdownTimer {

    private final TaskScheduler taskScheduler;
    private final Clock clock;
    private final Map<Key, ScheduledFuture<?>> countdowns = new ConcurrentHashMap<>();

    public CountdownTimer(@Qualifier("gameTaskScheduler") TaskScheduler taskScheduler, Clock clock) {
        this.taskScheduler = taskScheduler;
        this.clock = clock;
    }

    public Countdown start(Long roomId, CountdownKind kind, Duration duration, Runnable onEnd) {
        Key key = new Key(roomId, kind);
        LocalDateTime startedAt = LocalDateTime.now(clock);
        AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
        ScheduledFuture<?> future = taskScheduler.schedule(() -> end(key, self.get(), onEnd),
                taskScheduler.getClock().instant().plus(duration));
        self.set(future);
        ScheduledFuture<?> previous = countdowns.put(key, future);
        if (previous != null) {
            previous.cancel(false);
        }
        return new Countdown(startedAt, startedAt.plus(duration));
    }

    /** 라운드가 끝났다. 그 방의 카운트다운을 모두 취소한다 */
    public void cancelRoom(Long roomId) {
        countdowns.keySet().stream()
                .filter(key -> key.roomId().equals(roomId))
                .forEach(key -> {
                    ScheduledFuture<?> future = countdowns.remove(key);
                    if (future != null) {
                        future.cancel(false);
                    }
                });
    }

    private void end(Key key, ScheduledFuture<?> self, Runnable onEnd) {
        // 이 카운트다운만 지운다(그 사이 다시 건 것은 남긴다). 콜백 안에서 같은 종류를 다시 걸 수 있으므로 콜백보다 먼저 지운다.
        if (self != null) {
            countdowns.remove(key, self);
        }
        try {
            onEnd.run();
        } catch (RuntimeException e) {
            // 타이머 스레드의 예외는 호출자가 없어 여기서 남기지 않으면 사라진다.
            log.error("[CountdownTimer.end] Countdown callback failed. roomId={}, kind={}", key.roomId(), key.kind(), e);
        }
    }

    private record Key(Long roomId, CountdownKind kind) {
    }
}
