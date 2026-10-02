package geniusneugul.project.core.room.implement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoomLockTest {

    private static final int THREAD_COUNT = 16;
    private static final long ROOM_ID = 1L;

    private final RoomLock roomLock = new RoomLock();

    @DisplayName("같은 방의 작업은 동시에 실행되지 않는다.")
    @Test
    void withLock_sameRoom() throws Exception {
        // given
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        // when
        try (ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT)) {
            var futures = IntStream.range(0, THREAD_COUNT)
                    .mapToObj(i -> executor.submit(() -> {
                        start.await();
                        return roomLock.withLock(ROOM_ID, () -> {
                            maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max);
                            Thread.yield();
                            return running.decrementAndGet();
                        });
                    }))
                    .toList();
            start.countDown();
            for (Future<Integer> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        }

        // then
        assertThat(maxRunning.get()).isEqualTo(1);
    }
}
