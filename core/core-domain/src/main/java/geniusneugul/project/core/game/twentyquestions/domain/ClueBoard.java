package geniusneugul.project.core.game.twentyquestions.domain;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;
import lombok.Getter;

/**
 * 영화 스무고개 한 라운드의 차례와 단서 공개 상태(docs/DOMAIN.md 3-2). 차례 참가자가 아직 열리지 않은 단서 하나를 고르면
 * 잠시 뒤(3초) 공개하고, 정해진 시간 안에 고르지 않으면 남은 단서 중 하나를 무작위로 대신 고른다. 나간 참가자의 차례는 건너뛴다.
 * 진행 중에만 쓰므로 메모리에 둔다. 스스로 동기화하지 않으므로 방 락 안에서만 읽고 바꾼다.
 */
@Getter
public class ClueBoard {

    private final Long roomId;
    private final Long gameId;
    private final int roundNo;
    private final List<Long> turnOrder;
    private final ClueContents contents;
    private final Set<ClueType> revealed = EnumSet.noneOf(ClueType.class);
    private int turnOrderIndex = -1;
    /** 차례가 바뀔 때마다 1씩 는다. 늦게 도착한 차례 마감 타이머가 이미 지나간 차례에 단서를 고르지 않게 확인한다 */
    private int turnNo;
    /** 지금 차례 참가자. 단서를 모두 열었거나 차례를 줄 참가자가 없으면 비어 있다 */
    private Long turnHolderId;
    /** 골랐지만 아직 공개하지 않은 단서. 고른 뒤 공개까지는 차례가 비어 아무도 고를 수 없다 */
    private ClueType selectedClue;
    private Long selectedById;

    /** turnOrder는 라운드마다 무작위로 섞은 참가자 순서다 */
    public ClueBoard(Long roomId, Long gameId, int roundNo, List<Long> turnOrder, ClueContents contents) {
        this.roomId = roomId;
        this.gameId = gameId;
        this.roundNo = roundNo;
        this.turnOrder = List.copyOf(turnOrder);
        this.contents = contents;
    }

    /**
     * 순서상 다음 참가자에게 차례를 준다. 지금 방에 없는(나간) 참가자는 건너뛴다.
     * 단서를 모두 열었거나 남은 참가자가 없으면 차례를 비우고 빈 값을 돌려준다.
     */
    public Optional<Long> nextTurn(Collection<Long> presentIds) {
        turnHolderId = null;
        if (allRevealed()) {
            return Optional.empty();
        }
        for (int step = 1; step <= turnOrder.size(); step++) {
            int index = (turnOrderIndex + step) % turnOrder.size();
            Long participantId = turnOrder.get(index);
            if (presentIds.contains(participantId)) {
                turnOrderIndex = index;
                turnNo++;
                turnHolderId = participantId;
                return Optional.of(participantId);
            }
        }
        return Optional.empty();
    }

    /** 차례 참가자가 아직 열리지 않은 단서를 고른다. 고른 뒤에는 차례가 비고, 공개는 revealSelected로 한다 */
    public void select(Long participantId, ClueType clueType) {
        if (!isTurnHolder(participantId)) {
            throw new BusinessException(ErrorCode.GAME_NOT_YOUR_TURN, Map.of("participantId", participantId));
        }
        if (revealed.contains(clueType)) {
            throw new BusinessException(ErrorCode.GAME_CLUE_ALREADY_REVEALED, Map.of("clueType", clueType));
        }
        selectedClue = clueType;
        selectedById = participantId;
        turnHolderId = null;
    }

    /**
     * 차례 참가자가 시간 안에 고르지 않아 아직 열리지 않은 단서 중 하나를 무작위로 대신 고른다.
     * 고른 참가자는 그 차례 참가자로 남기고, 공개는 직접 고를 때와 같이 revealSelected로 한다.
     */
    public ClueType selectAtRandom(RandomGenerator random) {
        List<ClueType> unrevealed = Arrays.stream(ClueType.values())
                .filter(clueType -> !revealed.contains(clueType))
                .toList();
        ClueType clueType = unrevealed.get(random.nextInt(unrevealed.size()));
        select(turnHolderId, clueType);
        return clueType;
    }

    /** 고른 단서를 공개한다. 고른 단서가 없으면 비어 있다. 다음 차례는 nextTurn으로 준다 */
    public Optional<ShownClue> revealSelected() {
        if (selectedClue == null) {
            return Optional.empty();
        }
        ClueType clueType = selectedClue;
        revealed.add(clueType);
        selectedClue = null;
        return Optional.of(new ShownClue(clueType, contents.get(clueType), revealed.size(), contents.size()));
    }

    public boolean isTurnHolder(Long participantId) {
        return turnHolderId != null && turnHolderId.equals(participantId);
    }

    /** 그 차례가 아직 지나가지 않았다(단서를 고르지 않았다) */
    public boolean isCurrentTurn(int turnNo) {
        return turnHolderId != null && this.turnNo == turnNo;
    }

    public boolean allRevealed() {
        return revealed.size() == contents.size();
    }

    public boolean isFor(Long gameId, int roundNo) {
        return Objects.equals(this.gameId, gameId) && this.roundNo == roundNo;
    }
}
