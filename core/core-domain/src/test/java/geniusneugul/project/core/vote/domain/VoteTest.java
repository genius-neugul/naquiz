package geniusneugul.project.core.vote.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.HintType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VoteTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 21, 0);
    private static final Long INITIATOR_ID = 1L;

    @DisplayName("투표를 연 사람은 자동으로 찬성한다.")
    @Test
    void open() {
        // when
        Vote vote = openSkip();

        // then
        assertThat(vote.approverIds(List.of(INITIATOR_ID, 2L))).containsExactly(INITIATOR_ID);
        assertThat(vote.isOpen()).isTrue();
    }

    @DisplayName("이미 찬성한 참가자가 다시 찬성해도 한 번만 센다.")
    @Test
    void approve_alreadyApproved() {
        // given
        Vote vote = openSkip();

        // when
        boolean approved = vote.approve(INITIATOR_ID, NOW);

        // then
        assertThat(approved).isFalse();
        assertThat(vote.approverIds(List.of(INITIATOR_ID, 2L, 3L))).hasSize(1);
    }

    @DisplayName("나간 참가자의 찬성은 세지 않고 남은 인원으로 판정한다.")
    @Test
    void judge_leftApproverNotCounted() {
        // given 3명 중 1·2번이 찬성했지만 2번이 나갔다
        Vote vote = openSkip();
        vote.approve(2L, NOW);

        // when 남은 1·3번 중 1명 찬성: 2명 이하는 전원 찬성이 필요하다
        boolean passed = vote.judge(List.of(INITIATOR_ID, 3L));

        // then
        assertThat(passed).isFalse();
        assertThat(vote.getStatus()).isEqualTo(VoteStatus.OPEN);
    }

    @DisplayName("참가자가 나가 줄어든 인원으로 통과 기준을 채우면 통과한다.")
    @Test
    void judge_passAfterLeave() {
        // given
        Vote vote = openSkip();

        // when 혼자 남았다
        boolean passed = vote.judge(List.of(INITIATOR_ID));

        // then
        assertThat(passed).isTrue();
        assertThat(vote.getStatus()).isEqualTo(VoteStatus.PASSED);
    }

    @DisplayName("스킵 투표에 대상 힌트가 있거나 힌트 투표에 대상 힌트가 없으면 열 수 없다.")
    @ParameterizedTest
    @CsvSource(value = {"SKIP, ALBUM", "HINT, null"}, nullValues = "null")
    void open_invalidTarget(VoteType voteType, HintType targetHintType) {
        // when & then
        assertThatThrownBy(() -> Vote.open(1L, 1, voteType, targetHintType, INITIATOR_ID, NOW))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VOTE_INVALID_TARGET);
    }

    private Vote openSkip() {
        return Vote.open(1L, 1, VoteType.SKIP, null, INITIATOR_ID, NOW);
    }
}
