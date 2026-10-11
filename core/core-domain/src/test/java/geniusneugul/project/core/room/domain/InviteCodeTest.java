package geniusneugul.project.core.room.domain;

import static geniusneugul.project.core.common.exception.ErrorCode.ROOM_INVALID_INVITE_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geniusneugul.project.core.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InviteCodeTest {

    @DisplayName("숫자·영문 대문자 6자리면 초대 코드가 된다.")
    @Test
    void create() {
        // when
        InviteCode inviteCode = new InviteCode("A1B2C3");

        // then
        assertThat(inviteCode.value()).isEqualTo("A1B2C3");
    }

    @DisplayName("사용자가 입력한 코드는 앞뒤 공백을 빼고 대문자로 바꿔 받는다.")
    @ParameterizedTest
    @ValueSource(strings = {"a1b2c3", " A1b2C3 ", "A1B2C3"})
    void from(String raw) {
        // when
        InviteCode inviteCode = InviteCode.from(raw);

        // then
        assertThat(inviteCode.value()).isEqualTo("A1B2C3");
    }

    @DisplayName("숫자·영문 대문자 6자리가 아니면 초대 코드를 만들 수 없다.")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"A1B2C", "A1B2C3D", "a1b2c3", "A1B2C!", "가1B2C3"})
    void create_invalidFormat(String value) {
        // when & then
        assertThatThrownBy(() -> new InviteCode(value))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ROOM_INVALID_INVITE_CODE.getMessage());
    }
}
