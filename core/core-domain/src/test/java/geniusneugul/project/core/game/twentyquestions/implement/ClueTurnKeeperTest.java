package geniusneugul.project.core.game.twentyquestions.implement;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.common.domain.GameProgress;
import geniusneugul.project.core.game.common.implement.RoundInPlayChecker;
import geniusneugul.project.core.game.common.infra.MemoryGameProgressRepository;
import geniusneugul.project.core.game.twentyquestions.domain.ClueContents;
import geniusneugul.project.core.game.twentyquestions.domain.ClueType;
import geniusneugul.project.core.game.twentyquestions.infra.MemoryClueBoardRepository;
import geniusneugul.project.core.question.domain.Answer;
import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.fixture.RoomFixture;
import geniusneugul.project.core.room.implement.RoomLock;
import geniusneugul.project.core.room.implement.RoomMemberReader;
import geniusneugul.project.core.room.infra.MemoryRoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 차례 패스 타이머와 단서 요청은 정답·다음 라운드·다른 차례와 엇갈려 늦게 도착할 수 있다. 늦은 요청이 끝난 차례·라운드를
 * 건드리지 않는지 본다. 차례 순서는 무작위이므로 지금 차례 참가자를 보드에서 읽어 쓴다.
 */
class ClueTurnKeeperTest {

    private static final Long GAME_ID = 10L;
    private static final ClueContents CONTENTS = new ClueContents(Arrays.stream(ClueType.values())
            .collect(Collectors.toMap(Function.identity(), ClueType::name)));
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-31T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    private MemoryRoomRepository roomRepository;
    private MemoryClueBoardRepository clueBoardRepository;
    private ClueTurnKeeper clueTurnKeeper;
    private GameProgress progress;
    private Room room;

    @BeforeEach
    void setUp() {
        roomRepository = new MemoryRoomRepository();
        clueBoardRepository = new MemoryClueBoardRepository();
        MemoryGameProgressRepository gameProgressRepository = new MemoryGameProgressRepository();
        RoomLock roomLock = new RoomLock();
        clueTurnKeeper = new ClueTurnKeeper(roomLock, new RoomMemberReader(roomRepository, roomLock),
                new RoundInPlayChecker(gameProgressRepository), clueBoardRepository, CLOCK);

        room = Room.create(new InviteCode("ABC123"), "방장", "host-token", LocalDateTime.now(CLOCK));
        RoomFixture.addGuest(room, "감자", "guest-token");
        roomRepository.save(room);
        room.startGame("host-token");
        progress = new GameProgress(room.getId(), GAME_ID, GameType.MOVIE_TWENTY_QUESTIONS, 3, 1, 100L,
                new Answer("기생충", "Parasite"));
        gameProgressRepository.save(progress);
    }

    @DisplayName("라운드가 열리면 방에 있는 참가자 중 한 명에게 첫 차례를 준다.")
    @Test
    void startBoard() {
        // when & then
        assertThat(clueTurnKeeper.startBoard(room.getId(), GAME_ID, 1, CONTENTS))
                .get().extracting(TurnStart::participantId)
                .isIn(room.getParticipants().stream().map(Participant::getId).toList());
    }

    @DisplayName("고른 단서를 공개하면 공개 결과와 다음 차례를 돌려준다.")
    @Test
    void revealSelected() {
        // given
        TurnStart first = clueTurnKeeper.startBoard(room.getId(), GAME_ID, 1, CONTENTS).orElseThrow();
        clueTurnKeeper.select(tokenOf(first.participantId()), ClueType.GENRE);

        // when
        ClueReveal reveal = clueTurnKeeper.revealSelected(room.getId(), GAME_ID, 1).orElseThrow();

        // then
        assertThat(reveal).extracting(ClueReveal::clueType, ClueReveal::content, ClueReveal::participantId,
                ClueReveal::order).containsExactly(ClueType.GENRE, "GENRE", first.participantId(), 1);
        assertThat(reveal.nextTurn().participantId()).isNotEqualTo(first.participantId());
    }

    @DisplayName("정답자가 나온 라운드에는 단서를 고를 수 없다.")
    @Test
    void select_roundSolved() {
        // given
        TurnStart first = clueTurnKeeper.startBoard(room.getId(), GAME_ID, 1, CONTENTS).orElseThrow();
        progress.submit("기생충");

        // when & then
        assertThat(clueTurnKeeper.select(tokenOf(first.participantId()), ClueType.GENRE)).isEmpty();
    }

    @DisplayName("단서를 고른 뒤 공개 전에 정답자가 나오면 공개하지 않는다.")
    @Test
    void revealSelected_roundSolved() {
        // given
        TurnStart first = clueTurnKeeper.startBoard(room.getId(), GAME_ID, 1, CONTENTS).orElseThrow();
        clueTurnKeeper.select(tokenOf(first.participantId()), ClueType.GENRE);
        progress.submit("기생충");

        // when & then
        assertThat(clueTurnKeeper.revealSelected(room.getId(), GAME_ID, 1)).isEmpty();
    }

    @DisplayName("시간이 지나면 차례 참가자 대신 단서를 무작위로 고르고, 차례는 넘기지 않는다.")
    @Test
    void selectOnTimeout() {
        // given
        TurnStart first = clueTurnKeeper.startBoard(room.getId(), GAME_ID, 1, CONTENTS).orElseThrow();

        // when
        ClueSelect select = clueTurnKeeper.selectOnTimeout(room.getId(), GAME_ID, 1, first.turnNo()).orElseThrow();

        // then
        assertThat(select).extracting(ClueSelect::participantId, ClueSelect::timedOut)
                .containsExactly(first.participantId(), true);
        assertThat(clueTurnKeeper.revealSelected(room.getId(), GAME_ID, 1))
                .get().extracting(ClueReveal::clueType, ClueReveal::participantId)
                .containsExactly(select.clueType(), first.participantId());
    }

    @DisplayName("직접 고른 뒤 그 차례의 마감 타이머가 와도 또 고르지 않는다.")
    @Test
    void selectOnTimeout_afterSelect() {
        // given
        TurnStart first = clueTurnKeeper.startBoard(room.getId(), GAME_ID, 1, CONTENTS).orElseThrow();
        clueTurnKeeper.select(tokenOf(first.participantId()), ClueType.GENRE);

        // when & then
        assertThat(clueTurnKeeper.selectOnTimeout(room.getId(), GAME_ID, 1, first.turnNo())).isEmpty();
    }

    @DisplayName("다음 라운드가 열린 뒤 이전 라운드의 차례 마감 타이머가 오면 고르지 않는다.")
    @Test
    void selectOnTimeout_otherRound() {
        // given
        TurnStart first = clueTurnKeeper.startBoard(room.getId(), GAME_ID, 1, CONTENTS).orElseThrow();
        progress.submit("기생충");
        progress.startRound(2, 200L, new Answer("괴물", "The Host"));
        clueTurnKeeper.startBoard(room.getId(), GAME_ID, 2, CONTENTS);

        // when & then
        assertThat(clueTurnKeeper.selectOnTimeout(room.getId(), GAME_ID, 1, first.turnNo())).isEmpty();
    }

    @DisplayName("차례 참가자가 나가면 바로 남은 참가자에게 차례를 준다.")
    @Test
    void participantLeft() {
        // given
        TurnStart first;
        do {
            // 게스트가 차례를 받을 때까지 넘긴다.
            first = clueTurnKeeper.startBoard(room.getId(), GAME_ID, 1, CONTENTS).orElseThrow();
        } while (first.participantId().equals(room.getHostId()));
        Long guestId = first.participantId();
        room.leave("guest-token");

        // when & then
        assertThat(clueTurnKeeper.participantLeft(room.getId(), guestId))
                .get().extracting(TurnStart::participantId).isEqualTo(room.getHostId());
    }

    @DisplayName("차례가 아닌 참가자가 나가면 차례는 그대로다.")
    @Test
    void participantLeft_notTurnHolder() {
        // given
        TurnStart first = clueTurnKeeper.startBoard(room.getId(), GAME_ID, 1, CONTENTS).orElseThrow();
        Long otherId = room.getParticipants().stream().map(Participant::getId)
                .filter(id -> !id.equals(first.participantId())).findFirst().orElseThrow();

        // when & then
        assertThat(clueTurnKeeper.participantLeft(room.getId(), otherId)).isEmpty();
    }

    private String tokenOf(Long participantId) {
        return participantId.equals(room.getHostId()) ? "host-token" : "guest-token";
    }
}
