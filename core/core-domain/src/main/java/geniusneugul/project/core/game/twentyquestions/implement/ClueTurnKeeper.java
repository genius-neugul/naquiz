package geniusneugul.project.core.game.twentyquestions.implement;

import geniusneugul.project.core.game.common.implement.RoundInPlayChecker;
import geniusneugul.project.core.game.twentyquestions.domain.ClueBoard;
import geniusneugul.project.core.game.twentyquestions.domain.ClueContents;
import geniusneugul.project.core.game.twentyquestions.domain.ClueType;
import geniusneugul.project.core.game.twentyquestions.infra.ClueBoardRepository;
import geniusneugul.project.core.room.implement.RoomLock;
import geniusneugul.project.core.room.implement.RoomMember;
import geniusneugul.project.core.room.implement.RoomMemberReader;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 영화 스무고개 라운드의 차례를 돌리고, 고른 단서를 잠시 뒤 연다. 정답이 나오거나 라운드가 바뀐 뒤 늦게 도착한 요청·차례 마감 타이머가
 * 끝난 라운드를 건드리지 않도록, 라운드가 진행 중인지 확인과 변경을 방 락 안에서 한 번에 한다.
 */
@Component
@RequiredArgsConstructor
public class ClueTurnKeeper {

    private final RoomLock roomLock;
    private final RoomMemberReader roomMemberReader;
    private final RoundInPlayChecker roundInPlayChecker;
    private final ClueBoardRepository clueBoardRepository;
    private final Clock clock;

    /** 라운드마다 참가자 순서를 무작위로 정하고 첫 차례를 준다. 라운드가 이미 끝났으면 비어 있다 */
    public Optional<TurnStart> startBoard(Long roomId, Long gameId, int roundNo, ClueContents contents) {
        return roomLock.withLock(roomId, () -> {
            if (!roundInPlayChecker.isPlaying(roomId, gameId, roundNo)) {
                return Optional.empty();
            }
            List<Long> participantIds = roomMemberReader.readParticipantIds(roomId);
            List<Long> turnOrder = new ArrayList<>(participantIds);
            Collections.shuffle(turnOrder, ThreadLocalRandom.current());
            ClueBoard board = new ClueBoard(roomId, gameId, roundNo, turnOrder, contents);
            clueBoardRepository.save(board);
            return board.nextTurn(participantIds).map(holder -> TurnStart.of(board));
        });
    }

    /**
     * 차례 참가자가 단서를 고른다. 진행 중인 스무고개 라운드가 없으면(정답 직후 늦게 누른 경우 등) 비어 있다.
     * 차례가 아니거나(고른 뒤 공개 전 포함) 이미 열린 단서면 예외다.
     */
    public Optional<ClueSelect> select(String participantToken, ClueType clueType) {
        RoomMember member = roomMemberReader.read(participantToken);
        Long roomId = member.roomId();
        return roomLock.withLock(roomId, () -> clueBoardRepository.findByRoomId(roomId)
                .filter(board -> roundInPlayChecker.isPlaying(roomId, board.getGameId(), board.getRoundNo()))
                .map(board -> {
                    board.select(member.participantId(), clueType);
                    return ClueSelect.of(board, false, LocalDateTime.now(clock));
                }));
    }

    /**
     * 고른 단서를 공개하고 다음 차례를 준다. 고른 사이 정답이 나왔거나 라운드가 바뀌었으면 공개하지 않고 비어 있다.
     * 고른 참가자가 나갔어도 공개한다.
     */
    public Optional<ClueReveal> revealSelected(Long roomId, Long gameId, int roundNo) {
        return roomLock.withLock(roomId, () -> clueBoardRepository.findByRoomId(roomId)
                .filter(board -> board.isFor(gameId, roundNo))
                .filter(board -> roundInPlayChecker.isPlaying(roomId, gameId, roundNo))
                .flatMap(board -> {
                    Long participantId = board.getSelectedById();
                    return board.revealSelected().map(shown -> {
                        TurnStart nextTurn = board.nextTurn(roomMemberReader.readParticipantIds(roomId))
                                .map(holder -> TurnStart.of(board))
                                .orElse(null);
                        return ClueReveal.of(board, shown, participantId, LocalDateTime.now(clock), nextTurn);
                    });
                }));
    }

    /**
     * 차례 참가자가 시간 안에 고르지 않아 남은 단서 중 하나를 무작위로 대신 고른다. 공개는 직접 고를 때와 같다.
     * 그 차례가 이미 지나갔거나(직접 골랐거나) 라운드가 끝났으면 비어 있다.
     */
    public Optional<ClueSelect> selectOnTimeout(Long roomId, Long gameId, int roundNo, int turnNo) {
        return roomLock.withLock(roomId, () -> clueBoardRepository.findByRoomId(roomId)
                .filter(board -> board.isFor(gameId, roundNo) && board.isCurrentTurn(turnNo))
                .filter(board -> roundInPlayChecker.isPlaying(roomId, gameId, roundNo))
                .map(board -> {
                    board.selectAtRandom(ThreadLocalRandom.current());
                    return ClueSelect.of(board, true, LocalDateTime.now(clock));
                }));
    }

    /** 차례 참가자가 나갔으면 바로 다음 차례를 준다. 차례가 아니었거나 라운드가 끝났으면 비어 있다 */
    public Optional<TurnStart> participantLeft(Long roomId, Long participantId) {
        return roomLock.withLock(roomId, () -> clueBoardRepository.findByRoomId(roomId)
                .filter(board -> board.isTurnHolder(participantId))
                .filter(board -> roundInPlayChecker.isPlaying(roomId, board.getGameId(), board.getRoundNo()))
                .flatMap(board -> board.nextTurn(roomMemberReader.readParticipantIds(roomId))
                        .map(holder -> TurnStart.of(board))));
    }

    /**
     * 게임이 끝났거나 방이 닫혀 더 진행할 일이 없다. 지우기만 하므로 락을 잡지 않는다.
     * 닫힌 방의 락을 다시 만들지 않기 위해서다. 지운 뒤 도착한 요청은 보드가 없어 아무것도 하지 않는다.
     */
    public void clear(Long roomId) {
        clueBoardRepository.deleteByRoomId(roomId);
    }
}
