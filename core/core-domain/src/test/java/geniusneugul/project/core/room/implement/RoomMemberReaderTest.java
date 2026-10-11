package geniusneugul.project.core.room.implement;

import static geniusneugul.project.core.common.exception.ErrorCode.ROOM_NOT_JOINED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.fixture.RoomFixture;
import geniusneugul.project.core.room.infra.MemoryRoomRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoomMemberReaderTest {

    private static final String HOST_TOKEN = "host-token";
    private static final String GUEST_TOKEN = "guest-token";

    private MemoryRoomRepository roomRepository;
    private RoomMemberReader roomMemberReader;
    private Room room;

    @BeforeEach
    void setUp() {
        roomRepository = new MemoryRoomRepository();
        RoomLock roomLock = new RoomLock();
        roomMemberReader = new RoomMemberReader(roomRepository, roomLock);
        room = roomRepository.save(
                Room.create(new InviteCode("ABC123"), "방장", HOST_TOKEN, LocalDateTime.of(2026, 10, 3, 12, 0)));
        RoomFixture.addGuest(room, "감자", GUEST_TOKEN);
        roomRepository.save(room);
    }

    @DisplayName("방에 들어가 있는 연결이면 방 ID와 참가자의 닉네임·태그를 읽는다.")
    @Test
    void read() {
        // when
        RoomMember member = roomMemberReader.read(GUEST_TOKEN);

        // then
        Participant guest = room.findParticipant(GUEST_TOKEN).orElseThrow();
        assertThat(member).isEqualTo(new RoomMember(room.getId(), guest.getId(), "감자", 2));
    }

    @DisplayName("어느 방에도 들어가 있지 않은 연결이면 방 미참가 오류가 난다.")
    @Test
    void read_notJoined() {
        // when & then
        assertThatThrownBy(() -> roomMemberReader.read("stranger-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ROOM_NOT_JOINED.getMessage());
    }

    @DisplayName("방에서 나간 연결이면 방 미참가 오류가 난다.")
    @Test
    void read_left() {
        // given
        new RoomLeaver(roomRepository, new RoomLock()).leave(GUEST_TOKEN);

        // when & then
        assertThatThrownBy(() -> roomMemberReader.read(GUEST_TOKEN))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ROOM_NOT_JOINED.getMessage());
    }
}
