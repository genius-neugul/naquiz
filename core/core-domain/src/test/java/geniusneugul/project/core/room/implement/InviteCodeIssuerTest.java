package geniusneugul.project.core.room.implement;

import static geniusneugul.project.core.common.exception.ErrorCode.ROOM_INVITE_CODE_EXHAUSTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.infra.MemoryRoomRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InviteCodeIssuerTest {

    private static final InviteCode IN_USE = new InviteCode("AAAAAA");
    private static final InviteCode FREE = new InviteCode("BBBBBB");

    private InviteCodeGenerator inviteCodeGenerator;
    private InviteCodeIssuer inviteCodeIssuer;

    @BeforeEach
    void setUp() {
        // 무작위 생성기만 고정하고, 저장소는 실제 메모리 저장소를 쓴다.
        inviteCodeGenerator = mock(InviteCodeGenerator.class);
        MemoryRoomRepository roomRepository = new MemoryRoomRepository();
        roomRepository.save(Room.create(IN_USE, "방장", "host-token", LocalDateTime.of(2026, 10, 1, 12, 0)));
        inviteCodeIssuer = new InviteCodeIssuer(inviteCodeGenerator, roomRepository);
    }

    @DisplayName("활성 방이 쓰고 있는 초대 코드가 나오면 다시 만들어 겹치지 않는 코드를 발급한다.")
    @Test
    void issue_collidesWithActiveRoom() {
        // given
        given(inviteCodeGenerator.generate()).willReturn(IN_USE, FREE);

        // when
        InviteCode issued = inviteCodeIssuer.issue();

        // then
        assertThat(issued).isEqualTo(FREE);
    }

    @DisplayName("다시 만들어도 계속 겹치면 발급에 실패한다.")
    @Test
    void issue_exhausted() {
        // given
        given(inviteCodeGenerator.generate()).willReturn(IN_USE);

        // when & then
        assertThatThrownBy(() -> inviteCodeIssuer.issue())
                .isInstanceOf(BusinessException.class)
                .hasMessage(ROOM_INVITE_CODE_EXHAUSTED.getMessage());
    }
}
