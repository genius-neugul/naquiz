package geniusneugul.project.core.room.domain;

import static geniusneugul.project.core.common.exception.ErrorCode.ROOM_ALREADY_PLAYING;
import static geniusneugul.project.core.common.exception.ErrorCode.ROOM_FULL;
import static geniusneugul.project.core.common.exception.ErrorCode.ROOM_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.room.fixture.RoomFixture;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoomTest {

    private static final String HOST_TOKEN = "host-token";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 12, 0);

    @DisplayName("방장이 나가면 방이 종료된다.")
    @Test
    void leave_host() {
        // given
        Room room = createRoom();

        // when
        Optional<Participant> left = room.leave(HOST_TOKEN);

        // then
        assertThat(left).get().extracting(Participant::getRole).isEqualTo(ParticipantRole.HOST);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
    }

    @DisplayName("게스트가 나가면 그 참가자만 빠지고 방은 대기 상태로 남는다.")
    @Test
    void leave_guest() {
        // given
        Room room = createRoom();
        RoomFixture.addGuest(room, "게스트", "guest-token");

        // when
        room.leave("guest-token");

        // then
        assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(room.getParticipants()).extracting(Participant::getParticipantToken).containsExactly(HOST_TOKEN);
    }

    @DisplayName("이미 나간 참가자가 다시 나가면 아무 일도 일어나지 않는다.")
    @Test
    void leave_alreadyLeft() {
        // given
        Room room = createRoom();
        room.leave(HOST_TOKEN);

        // when
        Optional<Participant> left = room.leave(HOST_TOKEN);

        // then
        assertThat(left).isEmpty();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
    }

    @DisplayName("방장은 #1, 이후 입장한 참가자는 입장 순서대로 태그를 받고 같은 닉네임도 들어올 수 있다.")
    @Test
    void join() {
        // given
        Room room = createRoom();

        // when
        Participant first = room.join("감자", "guest-1", NOW);
        Participant second = room.join("감자", "guest-2", NOW);

        // then
        assertThat(room.getHost().getTag()).isEqualTo(1);
        assertThat(first.getTag()).isEqualTo(2);
        assertThat(second.getTag()).isEqualTo(3);
        assertThat(room.getParticipants()).extracting(Participant::getNickname).containsExactly("방장", "감자", "감자");
    }

    @DisplayName("나간 참가자의 태그는 다시 쓰지 않는다.")
    @Test
    void join_afterLeave() {
        // given
        Room room = createRoom();
        RoomFixture.addGuest(room, "감자", "guest-1");
        room.leave("guest-1");

        // when
        Participant next = room.join("고구마", "guest-2", NOW);

        // then
        assertThat(next.getTag()).isEqualTo(3);
    }

    @DisplayName("방 인원이 가득 차면 입장에 실패한다.")
    @Test
    void join_roomIsFull() {
        // given
        Room room = createRoom();
        IntStream.range(0, room.getMaxParticipants() - 1)
                .forEach(i -> RoomFixture.addGuest(room, "게스트" + i, "guest-" + i));

        // when & then
        assertThatThrownBy(() -> room.join("늦은 사람", "late", NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ROOM_FULL.getMessage());
    }

    @DisplayName("게임이 진행 중인 방에는 들어갈 수 없다.")
    @Test
    void join_roomIsPlaying() {
        // given
        Room room = createRoom();
        RoomFixture.status(room, RoomStatus.PLAYING);

        // when & then
        assertThatThrownBy(() -> room.join("감자", "guest-1", NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ROOM_ALREADY_PLAYING.getMessage());
    }

    @DisplayName("방장이 나가 닫힌 방에는 들어갈 수 없다.")
    @Test
    void join_roomIsClosed() {
        // given
        Room room = createRoom();
        room.leave(HOST_TOKEN);

        // when & then
        assertThatThrownBy(() -> room.join("감자", "guest-1", NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ROOM_NOT_FOUND.getMessage());
    }

    private Room createRoom() {
        return Room.create(new InviteCode("ABC123"), "방장", HOST_TOKEN, NOW);
    }
}
