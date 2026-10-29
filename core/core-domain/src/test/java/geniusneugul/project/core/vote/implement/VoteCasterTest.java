package geniusneugul.project.core.vote.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.GameProgress;
import geniusneugul.project.core.game.common.domain.HintType;
import geniusneugul.project.core.game.common.infra.MemoryGameProgressRepository;
import geniusneugul.project.core.question.domain.Answer;
import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.fixture.RoomFixture;
import geniusneugul.project.core.room.implement.RoomLock;
import geniusneugul.project.core.room.implement.RoomMemberReader;
import geniusneugul.project.core.room.infra.MemoryRoomRepository;
import geniusneugul.project.core.vote.domain.VoteStatus;
import geniusneugul.project.core.vote.domain.VoteType;
import geniusneugul.project.core.vote.infra.MemoryOpenVoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VoteCasterTest {

    private static final String HOST_TOKEN = "host-token";
    private static final String GUEST_TOKEN = "guest-token";
    private static final String OTHER_GUEST_TOKEN = "other-guest-token";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneId.of("Asia/Seoul"));

    private MemoryRoomRepository roomRepository;
    private MemoryGameProgressRepository gameProgressRepository;
    private MemoryOpenVoteRepository openVoteRepository;
    private VoteCaster voteCaster;
    private Room room;

    @BeforeEach
    void setUp() {
        roomRepository = new MemoryRoomRepository();
        gameProgressRepository = new MemoryGameProgressRepository();
        openVoteRepository = new MemoryOpenVoteRepository();
        RoomLock roomLock = new RoomLock();
        voteCaster = new VoteCaster(new RoomMemberReader(roomRepository, roomLock), roomLock, gameProgressRepository,
                openVoteRepository, List.of(new SkipVotePassHandler()), CLOCK);

        room = Room.create(new InviteCode("ABC123"), "방장", HOST_TOKEN, LocalDateTime.now(CLOCK));
        RoomFixture.addGuest(room, "감자", GUEST_TOKEN);
        RoomFixture.addGuest(room, "고구마", OTHER_GUEST_TOKEN);
        roomRepository.save(room);
        room.startGame(HOST_TOKEN);
    }

    @DisplayName("진행 중인 라운드가 없으면 투표를 열 수 없다.")
    @Test
    void open_roundNotInProgress() {
        // given 정답자가 나와 다음 라운드를 기다리는 중이다
        GameProgress progress = startRound();
        progress.submit("기생충");

        // when & then
        assertThatThrownBy(() -> voteCaster.open(HOST_TOKEN, VoteType.SKIP, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VOTE_ROUND_NOT_IN_PROGRESS);
    }

    @DisplayName("통과 처리가 없는 종류의 투표는 열 수 없다.")
    @Test
    void open_unsupportedType() {
        // given 스킵 통과 처리만 등록돼 있다
        startRound();

        // when & then
        assertThatThrownBy(() -> voteCaster.open(HOST_TOKEN, VoteType.HINT, HintType.ALBUM))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VOTE_UNSUPPORTED_TYPE);
    }

    @DisplayName("같은 대상의 투표가 열려 있으면 투표 열기는 그 투표에 찬성한 것으로 처리한다.")
    @Test
    void open_alreadyOpened() {
        // given
        startRound();
        voteCaster.open(HOST_TOKEN, VoteType.SKIP, null);

        // when
        VoteCast cast = voteCaster.open(GUEST_TOKEN, VoteType.SKIP, null);

        // then 3명 중 2명 찬성으로 통과한다
        assertThat(cast.passed()).isTrue();
        assertThat(cast.approverIds()).hasSize(2);
    }

    @DisplayName("스킵 투표가 통과하면 라운드가 끝나 이후 정답을 인정하지 않고 다음 라운드를 열 수 있다.")
    @Test
    void approve_skipPassed() {
        // given
        GameProgress progress = startRound();
        voteCaster.open(HOST_TOKEN, VoteType.SKIP, null);

        // when
        VoteCast cast = voteCaster.approve(GUEST_TOKEN, VoteType.SKIP, null).orElseThrow();

        // then
        assertThat(cast.pass()).isInstanceOfSatisfying(RoundSkipPass.class,
                pass -> assertThat(pass.roundSkip().answer().answer()).isEqualTo("기생충"));
        assertThat(cast.endedVotes()).extracting("status").containsExactly(VoteStatus.PASSED);
        assertThat(progress.submit("기생충")).isFalse();
        assertThat(progress.isRoundSolved()).isTrue();
        assertThat(openVoteRepository.findByRoomId(room.getId())).isEmpty();
    }

    @DisplayName("참가자가 나가 줄어든 인원으로 다시 판정해 통과 기준을 채우면 스킵된다.")
    @Test
    void rejudge_afterLeave() {
        // given 3명 중 2명이 찬성해야 하는데 1명만 찬성했다
        startRound();
        VoteCast opened = voteCaster.open(GUEST_TOKEN, VoteType.SKIP, null);
        room.leave(OTHER_GUEST_TOKEN);
        room.leave(GUEST_TOKEN);

        // when 방장 혼자 남았고 발의자는 나갔다: 방장이 찬성하지 않았으므로 통과하지 않는다
        List<VoteCast> afterHostAlone = voteCaster.rejudge(room.getId());

        // then
        assertThat(opened.passed()).isFalse();
        assertThat(afterHostAlone).singleElement()
                .satisfies(cast -> {
                    assertThat(cast.passed()).isFalse();
                    assertThat(cast.approverIds()).isEmpty();
                    assertThat(cast.requiredApprovals()).isEqualTo(1);
                });
    }

    @DisplayName("참가자가 나가 남은 찬성만으로 통과 기준을 채우면 다시 판정할 때 스킵된다.")
    @Test
    void rejudge_passed() {
        // given 3명 중 방장만 찬성했다
        startRound();
        voteCaster.open(HOST_TOKEN, VoteType.SKIP, null);
        room.leave(OTHER_GUEST_TOKEN);
        room.leave(GUEST_TOKEN);

        // when
        List<VoteCast> casts = voteCaster.rejudge(room.getId());

        // then
        assertThat(casts).singleElement().satisfies(cast -> assertThat(cast.pass()).isInstanceOf(RoundSkipPass.class));
    }

    private GameProgress startRound() {
        GameProgress progress = new GameProgress(room.getId(), 1L, GameType.MOVIE_STILL_CUT, 3, 1, 10L,
                new Answer("기생충", "Parasite"));
        gameProgressRepository.save(progress);
        return progress;
    }
}
