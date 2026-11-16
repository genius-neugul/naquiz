package geniusneugul.project.core.game.twentyquestions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClueBoardTest {

    private static final ClueContents CONTENTS = new ClueContents(Arrays.stream(ClueType.values())
            .collect(Collectors.toMap(Function.identity(), ClueType::name)));
    private static final List<Long> TURN_ORDER = List.of(3L, 1L, 2L);
    private static final List<Long> ALL = List.of(1L, 2L, 3L);

    @DisplayName("차례는 정해진 순서대로 돌고, 끝까지 가면 처음으로 돌아간다.")
    @Test
    void nextTurn_cycles() {
        // given
        ClueBoard board = board();

        // when & then
        assertThat(board.nextTurn(ALL)).contains(3L);
        assertThat(board.nextTurn(ALL)).contains(1L);
        assertThat(board.nextTurn(ALL)).contains(2L);
        assertThat(board.nextTurn(ALL)).contains(3L);
        assertThat(board.getTurnNo()).isEqualTo(4);
    }

    @DisplayName("나간 참가자의 차례는 건너뛴다.")
    @Test
    void nextTurn_skipsLeft() {
        // given
        ClueBoard board = board();
        board.nextTurn(ALL);

        // when & then
        assertThat(board.nextTurn(List.of(2L, 3L))).contains(2L);
    }

    @DisplayName("차례 참가자가 단서를 고르면 차례가 비고, 공개 전까지는 열린 단서가 아니다.")
    @Test
    void select() {
        // given
        ClueBoard board = board();
        board.nextTurn(ALL);

        // when
        board.select(3L, ClueType.DIRECTOR);

        // then
        assertThat(board.getTurnHolderId()).isNull();
        assertThat(board.getSelectedClue()).isEqualTo(ClueType.DIRECTOR);
        assertThat(board.getRevealed()).isEmpty();
    }

    @DisplayName("고른 단서를 공개하면 공개 순서와 내용을 돌려준다.")
    @Test
    void revealSelected() {
        // given
        ClueBoard board = board();
        board.nextTurn(ALL);
        board.select(3L, ClueType.DIRECTOR);

        // when & then
        assertThat(board.revealSelected())
                .contains(new ShownClue(ClueType.DIRECTOR, "DIRECTOR", 1, ClueType.values().length));
        assertThat(board.getSelectedClue()).isNull();
    }

    @DisplayName("고른 단서가 없으면 공개하지 않는다.")
    @Test
    void revealSelected_nothingSelected() {
        // given
        ClueBoard board = board();
        board.nextTurn(ALL);

        // when & then
        assertThat(board.revealSelected()).isEmpty();
    }

    @DisplayName("단서를 고른 뒤 공개 전에는 고른 참가자도 다른 단서를 고를 수 없다.")
    @Test
    void select_afterSelected() {
        // given
        ClueBoard board = board();
        board.nextTurn(ALL);
        board.select(3L, ClueType.DIRECTOR);

        // when & then
        assertThatThrownBy(() -> board.select(3L, ClueType.GENRE))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.GAME_NOT_YOUR_TURN.getMessage());
    }

    @DisplayName("차례가 아닌 참가자는 단서를 열 수 없다.")
    @Test
    void reveal_notYourTurn() {
        // given
        ClueBoard board = board();
        board.nextTurn(ALL);

        // when & then
        assertThatThrownBy(() -> board.select(1L, ClueType.DIRECTOR))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.GAME_NOT_YOUR_TURN.getMessage());
    }

    @DisplayName("이미 열린 단서는 다시 열 수 없다.")
    @Test
    void reveal_alreadyRevealed() {
        // given
        ClueBoard board = board();
        board.nextTurn(ALL);
        board.select(3L, ClueType.DIRECTOR);
        board.revealSelected();
        board.nextTurn(ALL);

        // when & then
        assertThatThrownBy(() -> board.select(1L, ClueType.DIRECTOR))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.GAME_CLUE_ALREADY_REVEALED.getMessage());
    }

    @DisplayName("단서를 모두 열면 더 차례를 주지 않는다.")
    @Test
    void nextTurn_allRevealed() {
        // given
        ClueBoard board = board();
        for (ClueType clueType : ClueType.values()) {
            Long holder = board.nextTurn(ALL).orElseThrow();
            board.select(holder, clueType);
            board.revealSelected();
        }

        // when & then
        assertThat(board.allRevealed()).isTrue();
        assertThat(board.nextTurn(ALL)).isEmpty();
    }

    @DisplayName("시간이 지나 대신 고를 때는 아직 열리지 않은 단서만 고르고, 그 차례 참가자가 고른 것으로 남는다.")
    @Test
    void selectAtRandom() {
        // given
        ClueBoard board = board();
        ClueType[] clueTypes = ClueType.values();
        for (int i = 0; i < clueTypes.length - 1; i++) {
            Long holder = board.nextTurn(ALL).orElseThrow();
            board.select(holder, clueTypes[i]);
            board.revealSelected();
        }
        Long holder = board.nextTurn(ALL).orElseThrow();

        // when
        ClueType selected = board.selectAtRandom(new Random());

        // then
        assertThat(selected).isEqualTo(clueTypes[clueTypes.length - 1]);
        assertThat(board.getSelectedById()).isEqualTo(holder);
        assertThat(board.getTurnHolderId()).isNull();
    }

    @DisplayName("지나간 차례 번호로는 지금 차례인지 확인되지 않는다.")
    @Test
    void isCurrentTurn() {
        // given
        ClueBoard board = board();
        board.nextTurn(ALL);
        int first = board.getTurnNo();
        board.nextTurn(ALL);

        // when & then
        assertThat(board.isCurrentTurn(first)).isFalse();
        assertThat(board.isCurrentTurn(board.getTurnNo())).isTrue();
    }

    private ClueBoard board() {
        return new ClueBoard(1L, 10L, 1, TURN_ORDER, CONTENTS);
    }
}
