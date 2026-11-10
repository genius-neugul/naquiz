package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.AnswerHintString;
import geniusneugul.project.core.game.common.infra.AnswerHintStringRepository;
import geniusneugul.project.core.room.implement.RoomLock;
import java.util.Optional;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 라운드의 정답 힌트를 공개 우선순위대로 연다. 공개 상태는 메모리에 두고, 정답이 나오거나 라운드가 바뀐 뒤
 * 늦게 도착한 요청이 끝난 라운드의 힌트를 열지 않도록 라운드가 진행 중인지 확인과 공개를 방 락 안에서 한 번에 한다.
 * 공개 조건(투표 통과, 차례)은 호출하는 쪽이 먼저 확인한다.
 */
@Component
@RequiredArgsConstructor
public class AnswerHintRevealer {

    private final RoomLock roomLock;
    private final RoundInPlayChecker roundInPlayChecker;
    private final AnswerHintStringRepository answerHintStringRepository;
    private final RandomGenerator random;

    /** 라운드의 정답 힌트 공개 상태를 올리고 기본 힌트를 돌려준다. 라운드가 이미 끝났으면 비어 있다 */
    public Optional<AnswerHintReveal> start(Long roomId, Long gameId, int roundNo, String answer) {
        return roomLock.withLock(roomId, () -> {
            if (!roundInPlayChecker.isPlaying(roomId, gameId, roundNo)) {
                return Optional.empty();
            }
            AnswerHintString answerHintString = AnswerHintString.of(roomId, gameId, roundNo, answer);
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
        answerHintString.revealNext(random)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NO_MORE_ANSWER_HINT));
        return AnswerHintReveal.of(answerHintString);
    }
}
