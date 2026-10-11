package geniusneugul.project.core.room.implement;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.common.infra.event.EventPublisher;
import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.fixture.RoomFixture;
import geniusneugul.project.core.room.infra.MemoryRoomRepository;
import java.time.Clock;
import java.time.LocalDateTime;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 같은 방에 퇴장이 동시에 몰려도 방 상태와 저장소 색인이 깨지지 않는지 본다.
 * 경합은 확률적으로 일어나므로 같은 상황을 여러 번 반복한다.
 */
class RoomLeaverTest {

    private static final int REPEAT = 50;
    private static final String HOST_TOKEN = "host-token";
    private static final InviteCode INVITE_CODE = new InviteCode("ABC123");

    private MemoryRoomRepository roomRepository;
    private RoomLeaver roomLeaver;

    @BeforeEach
    void setUp() {
        roomRepository = new MemoryRoomRepository();
        roomLeaver = new RoomLeaver(roomRepository, new RoomLock(), new EventPublisher(event -> {
        }), Clock.systemDefaultZone());
    }

    @DisplayName("방장의 퇴장이 동시에 여러 번 들어와도 퇴장은 한 번만 처리되고 방이 사라진다.")
    @Test
    void leave_hostConcurrently() throws Exception {
        for (int i = 0; i < REPEAT; i++) {
            // given
            setUp();
            roomRepository.save(createRoom());

            // when
            List<Optional<RoomLeave>> results = runConcurrently(8, thread -> roomLeaver.leave(HOST_TOKEN));

            // then
            assertThat(results).filteredOn(Optional::isPresent).hasSize(1);
            assertThat(roomRepository.findByInviteCode(INVITE_CODE)).isEmpty();
            assertThat(roomRepository.findByParticipantToken(HOST_TOKEN)).isEmpty();
        }
    }

    @DisplayName("게스트들이 동시에 나가도 빠지는 참가자 없이 모두 퇴장하고 방장만 남는다.")
    @Test
    void leave_guestsConcurrently() throws Exception {
        int guestCount = 9;
        for (int i = 0; i < REPEAT; i++) {
            // given
            setUp();
            Room room = createRoom();
            IntStream.range(0, guestCount).forEach(guest -> RoomFixture.addGuest(room, "게스트" + guest, guestToken(guest)));
            roomRepository.save(room);

            // when
            List<Optional<RoomLeave>> results = runConcurrently(guestCount, guest -> roomLeaver.leave(guestToken(guest)));

            // then
            assertThat(results).allMatch(Optional::isPresent);
            assertThat(room.getParticipants()).extracting(Participant::getParticipantToken).containsExactly(HOST_TOKEN);
            assertThat(IntStream.range(0, guestCount).mapToObj(guest -> roomRepository.findByParticipantToken(guestToken(guest))))
                    .allMatch(Optional::isEmpty);
        }
    }

    private Room createRoom() {
        return Room.create(INVITE_CODE, "방장", HOST_TOKEN, LocalDateTime.of(2026, 10, 1, 12, 0));
    }

    private String guestToken(int guest) {
        return "guest-token-" + guest;
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
