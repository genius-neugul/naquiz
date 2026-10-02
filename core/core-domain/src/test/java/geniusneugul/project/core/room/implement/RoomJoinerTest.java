package geniusneugul.project.core.room.implement;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.infra.MemoryRoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 인원 확인과 추가 사이로 다른 입장이 끼어들면 상한을 넘는다. 경합은 확률적으로 일어나므로 여러 번 반복한다.
 */
class RoomJoinerTest {

    private static final int REPEAT = 50;
    private static final int JOINER_COUNT = 12;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-01T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @DisplayName("방장만 있는 방에 동시에 여러 명이 들어와도 최대 인원까지만 들어오고 태그는 겹치지 않는다.")
    @Test
    void join_concurrently() throws Exception {
        for (int i = 0; i < REPEAT; i++) {
            // given
            MemoryRoomRepository roomRepository = new MemoryRoomRepository();
            RoomJoiner roomJoiner = new RoomJoiner(roomRepository, new RoomLock(), CLOCK);
            Room room = roomRepository.save(
                    Room.create(new InviteCode("ABC123"), "방장", "host-token", LocalDateTime.now(CLOCK)));

            // when
            List<Optional<RoomJoin>> results = runConcurrently(JOINER_COUNT, joiner -> {
                try {
                    return Optional.of(roomJoiner.join(room, "감자", "guest-" + joiner));
                } catch (BusinessException e) {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ROOM_FULL);
                    return Optional.empty();
                }
            });

            // then
            assertThat(results).filteredOn(Optional::isPresent).hasSize(room.getMaxParticipants() - 1);
            assertThat(room.getParticipants())
                    .extracting(Participant::getTag)
                    .containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(1, room.getMaxParticipants()).boxed().toList());
        }
    }

    // 모든 스레드를 준비시킨 뒤 한꺼번에 출발시켜 경합을 만든다.
    private <T> List<T> runConcurrently(int threadCount, IntFunction<T> task) throws Exception {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            List<Future<T>> futures = IntStream.range(0, threadCount)
                    .mapToObj(thread -> executor.submit((Callable<T>) () -> {
                        ready.countDown();
                        start.await();
                        return task.apply(thread);
                    }))
                    .toList();
            ready.await();
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }
            return results;
        }
    }
}
