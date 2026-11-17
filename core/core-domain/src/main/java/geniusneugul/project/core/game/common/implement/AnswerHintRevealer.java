package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.AnswerHintPriority;
import geniusneugul.project.core.game.common.domain.AnswerHintString;
import geniusneugul.project.core.game.common.infra.AnswerHintStringRepository;
import geniusneugul.project.core.room.implement.RoomLock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 라운드의 정답 힌트를 공개 우선순위대로 연다. 공개 상태는 메모리에 두고, 정답이 나오거나 라운드가 바뀐 뒤
 * 늦게 도착한 요청이 끝난 라운드의 힌트를 열지 않도록 라운드가 진행 중인지 확인과 공개를 방 락 안에서 한 번에 한다.
 * 우선순위 하나의 공개 규칙은 AnswerHintPriorityRevealer 구현이 맡고, 여기서는 다음에 열 우선순위를 고른다.
 * 공개 조건(투표 통과, 차례)은 호출하는 쪽이 먼저 확인한다.
 */
@Component
public class AnswerHintRevealer {

    private final RoomLock roomLock;
    private final RoundInPlayChecker roundInPlayChecker;
    private final AnswerHintStringRepository answerHintStringRepository;
    private final Map<AnswerHintPriority, AnswerHintPriorityRevealer> priorityRevealers;

    public AnswerHintRevealer(RoomLock roomLock, RoundInPlayChecker roundInPlayChecker,
            AnswerHintStringRepository answerHintStringRepository, List<AnswerHintPriorityRevealer> priorityRevealers) {
        this.roomLock = roomLock;
        this.roundInPlayChecker = roundInPlayChecker;
        this.answerHintStringRepository = answerHintStringRepository;
        this.priorityRevealers = byPriority(priorityRevealers);
    }

    /** 라운드의 정답 힌트 공개 상태를 올리고 기본 힌트를 연다. 라운드가 이미 끝났으면 비어 있다 */
    public Optional<AnswerHintReveal> start(Long roomId, Long gameId, int roundNo, String answer) {
        return roomLock.withLock(roomId, () -> {
            if (!roundInPlayChecker.isPlaying(roomId, gameId, roundNo)) {
                return Optional.empty();
            }
            AnswerHintString answerHintString = AnswerHintString.of(roomId, gameId, roundNo, answer);
            revealNextPriority(answerHintString);
            answerHintStringRepository.save(answerHintString);
            return Optional.of(AnswerHintReveal.of(answerHintString));
        });
    }

    /**
     * 다음 우선순위의 정답 힌트를 연다. 라운드가 끝났거나 바뀌었으면 비어 있다.
     *
     * @throws BusinessException 더 열 우선순위가 없을 때(GAME_NO_MORE_ANSWER_HINT)
     */
    public Optional<AnswerHintReveal> revealNext(Long roomId, Long gameId, int roundNo) {
        return roomLock.withLock(roomId, () -> answerHintStringRepository.findByRoomId(roomId)
                .filter(answerHintString -> answerHintString.isFor(gameId, roundNo))
                .filter(answerHintString -> roundInPlayChecker.isPlaying(roomId, gameId, roundNo))
                .map(this::reveal));
    }

    /**
     * 게임이 끝났거나 방이 닫혀 더 공개할 일이 없다. 지우기만 하므로 락을 잡지 않는다.
     * 닫힌 방의 락을 다시 만들지 않기 위해서다. 지운 뒤 도착한 공개 요청은 공개 상태가 없어 아무것도 하지 않는다.
     */
    public void clear(Long roomId) {
        answerHintStringRepository.deleteByRoomId(roomId);
    }

    private AnswerHintReveal reveal(AnswerHintString answerHintString) {
        revealNextPriority(answerHintString)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NO_MORE_ANSWER_HINT));
        return AnswerHintReveal.of(answerHintString);
    }

    /**
     * 마지막으로 공개한 우선순위 다음부터 열 수 있는 첫 우선순위를 공개한다. 앞선 우선순위는 다시 보지 않고, 열 수 없는 우선순위는 건너뛴다.
     * 아직 아무것도 공개하지 않았으면 첫 우선순위(기본 힌트)부터 본다.
     *
     * @return 공개한 우선순위. 더 열 수 있는 우선순위가 없으면 빈 값
     */
    private Optional<AnswerHintPriority> revealNextPriority(AnswerHintString answerHintString) {
        AnswerHintPriority current = answerHintString.getCurrent();
        AnswerHintPriority candidate = current == null ? AnswerHintPriority.first() : current.next();
        while (!priorityRevealers.get(candidate).canReveal(answerHintString)) {
            if (candidate.next() == candidate) {
                return Optional.empty();
            }
            candidate = candidate.next();
        }
        priorityRevealers.get(candidate).reveal(answerHintString);
        answerHintString.changeCurrent(candidate);
        return Optional.of(candidate);
    }

    /** 우선순위마다 공개 클래스가 정확히 하나씩 있어야 한다. 앱이 뜰 때 바로 알 수 있게 생성할 때 확인한다 */
    private static Map<AnswerHintPriority, AnswerHintPriorityRevealer> byPriority(List<AnswerHintPriorityRevealer> revealers) {
        Map<AnswerHintPriority, AnswerHintPriorityRevealer> byPriority = new EnumMap<>(AnswerHintPriority.class);
        for (AnswerHintPriorityRevealer revealer : revealers) {
            if (byPriority.put(revealer.priority(), revealer) != null) {
                throw new IllegalStateException(ErrorCode.GAME_ANSWER_HINT_REVEALER_DUPLICATED.getMessage()
                        + " priority=" + revealer.priority());
            }
        }
        for (AnswerHintPriority priority : AnswerHintPriority.values()) {
            if (!byPriority.containsKey(priority)) {
                throw new IllegalStateException(ErrorCode.GAME_ANSWER_HINT_REVEALER_MISSING.getMessage()
                        + " priority=" + priority);
            }
        }
        return byPriority;
    }
}
