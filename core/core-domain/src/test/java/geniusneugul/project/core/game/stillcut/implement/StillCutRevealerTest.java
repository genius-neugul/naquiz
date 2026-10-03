package geniusneugul.project.core.game.stillcut.implement;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.common.domain.GameProgress;
import geniusneugul.project.core.game.common.implement.RoundInPlayChecker;
import geniusneugul.project.core.game.common.infra.MemoryGameProgressRepository;
import geniusneugul.project.core.game.stillcut.infra.MemoryStillCutReelRepository;
import geniusneugul.project.core.question.domain.Answer;
import geniusneugul.project.core.room.implement.RoomLock;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 스틸컷 교체 타이머는 정답·다음 라운드와 엇갈려 늦게 도착할 수 있다. 늦은 공개가 끝난 라운드를 건드리지 않는지 본다.
 */
class StillCutRevealerTest {

    private static final Long ROOM_ID = 1L;
    private static final Long GAME_ID = 10L;
    private static final List<String> IMAGE_URLS = List.of("cut-1", "cut-2", "cut-3");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-04T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    private MemoryGameProgressRepository gameProgressRepository;
    private StillCutRevealer stillCutRevealer;
    private GameProgress progress;

    @BeforeEach
    void setUp() {
        gameProgressRepository = new MemoryGameProgressRepository();
        stillCutRevealer = new StillCutRevealer(new RoomLock(), new RoundInPlayChecker(gameProgressRepository),
                new MemoryStillCutReelRepository(), CLOCK);
        progress = new GameProgress(ROOM_ID, GAME_ID, GameType.MOVIE_STILL_CUT, 3, 1, 100L, new Answer("기생충", "Parasite"));
        gameProgressRepository.save(progress);
    }

    @DisplayName("라운드가 진행 중이면 다음 스틸컷을 공개한다.")
    @Test
    void revealNext() {
        // given
        stillCutRevealer.startReel(ROOM_ID, GAME_ID, 1, IMAGE_URLS);

        // when & then
        assertThat(stillCutRevealer.revealNext(ROOM_ID, GAME_ID, 1))
                .get().extracting(StillCutReveal::order, StillCutReveal::imageUrl).containsExactly(2, "cut-2");
    }

    @DisplayName("정답자가 나온 라운드에는 늦게 도착한 공개 요청이 와도 스틸컷을 공개하지 않는다.")
    @Test
    void revealNext_roundSolved() {
        // given
        stillCutRevealer.startReel(ROOM_ID, GAME_ID, 1, IMAGE_URLS);
        progress.submit("기생충");

        // when & then
        assertThat(stillCutRevealer.revealNext(ROOM_ID, GAME_ID, 1)).isEmpty();
    }

    @DisplayName("다음 라운드가 열린 뒤 이전 라운드의 공개 요청이 오면 스틸컷을 공개하지 않는다.")
    @Test
    void revealNext_otherRound() {
        // given
        stillCutRevealer.startReel(ROOM_ID, GAME_ID, 1, IMAGE_URLS);
        progress.submit("기생충");
        progress.startRound(2, 200L, new Answer("괴물", "The Host"));
        stillCutRevealer.startReel(ROOM_ID, GAME_ID, 2, IMAGE_URLS);

        // when & then
        assertThat(stillCutRevealer.revealNext(ROOM_ID, GAME_ID, 1)).isEmpty();
    }

    @DisplayName("이미 끝난 라운드에는 스틸컷 묶음을 올리지 않는다.")
    @Test
    void startReel_roundSolved() {
        // given
        progress.submit("기생충");

        // when & then
        assertThat(stillCutRevealer.startReel(ROOM_ID, GAME_ID, 1, IMAGE_URLS)).isEmpty();
    }
}
