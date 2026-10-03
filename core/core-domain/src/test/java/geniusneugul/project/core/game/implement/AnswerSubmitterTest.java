package geniusneugul.project.core.game.implement;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.domain.GameProgress;
import geniusneugul.project.core.game.infra.MemoryGameProgressRepository;
import geniusneugul.project.core.question.domain.Answer;
import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.domain.RoomStatus;
import geniusneugul.project.core.room.fixture.RoomFixture;
import geniusneugul.project.core.room.implement.RoomGameUpdater;
import geniusneugul.project.core.room.implement.RoomLock;
import geniusneugul.project.core.room.implement.RoomMemberReader;
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
 * 판정과 라운드 풀림 사이로 다른 정답이 끼어들면 정답자가 둘이 된다. 경합은 확률적으로 일어나므로 여러 번 반복한다.
 */
class AnswerSubmitterTest {

    private static final int REPEAT = 50;
    private static final int GUEST_COUNT = 9;
    private static final String HOST_TOKEN = "host-token";
    private static final Answer ANSWER = new Answer("기생충", "Parasite");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    private MemoryRoomRepository roomRepository;
    private MemoryGameProgressRepository gameProgressRepository;
    private AnswerSubmitter answerSubmitter;

    @DisplayName("여러 참가자가 동시에 정답을 내도 정답자는 한 명이고 그 사람만 1점을 얻는다.")
    @Test
    void submit_concurrently() throws Exception {
        for (int i = 0; i < REPEAT; i++) {
            // given
            Room room = setUpGame(3);

            // when
            List<Optional<RoundSolve>> results = runConcurrently(GUEST_COUNT,
                    guest -> answerSubmitter.submit("guest-" + guest, "기생충"));

            // then
            assertThat(results).filteredOn(Optional::isPresent).hasSize(1);
            assertThat(room.getParticipants()).extracting(Participant::getRoundScore).containsOnlyOnce(1);
            assertThat(room.getParticipants()).extracting(Participant::getRoundScore).filteredOn(score -> score == 0)
                    .hasSize(GUEST_COUNT);
        }
    }

    @DisplayName("정답으로 목표 점수에 도달하면 승자가 확정되고 방은 대기 상태로 돌아간다.")
    @Test
    void submit_reachTargetScore() {
        // given
        Room room = setUpGame(1);

        // when
        Optional<RoundSolve> solve = answerSubmitter.submit("guest-0", "parasite");

        // then
        assertThat(solve).get().extracting(RoundSolve::gameFinished).isEqualTo(true);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(room.findParticipant("guest-0")).get().extracting(Participant::getGameWins).isEqualTo(1);
        assertThat(gameProgressRepository.findByRoomId(room.getId())).isEmpty();
    }

    private Room setUpGame(int targetScore) {
        roomRepository = new MemoryRoomRepository();
        gameProgressRepository = new MemoryGameProgressRepository();
        RoomLock roomLock = new RoomLock();
        answerSubmitter = new AnswerSubmitter(new RoomMemberReader(roomRepository, roomLock), roomLock,
                new RoomGameUpdater(roomRepository, roomLock), gameProgressRepository, CLOCK);

        Room room = Room.create(new InviteCode("ABC123"), "방장", HOST_TOKEN, LocalDateTime.now(CLOCK));
        IntStream.range(0, GUEST_COUNT).forEach(guest -> RoomFixture.addGuest(room, "감자", "guest-" + guest));
        roomRepository.save(room);
        room.startGame(HOST_TOKEN);
        gameProgressRepository.save(new GameProgress(room.getId(), 1L, GameType.MOVIE_STILL_CUT, targetScore, 1, 10L, ANSWER));
        return room;
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
