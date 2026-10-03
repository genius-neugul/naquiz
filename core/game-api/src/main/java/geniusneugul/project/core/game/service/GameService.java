package geniusneugul.project.core.game.service;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.domain.Game;
import geniusneugul.project.core.game.implement.AnswerSubmitter;
import geniusneugul.project.core.game.implement.GameAppender;
import geniusneugul.project.core.game.implement.GameProgressRemover;
import geniusneugul.project.core.game.implement.GameReader;
import geniusneugul.project.core.game.implement.GameStart;
import geniusneugul.project.core.game.implement.GameStarter;
import geniusneugul.project.core.game.implement.RoundSolve;
import geniusneugul.project.core.game.implement.RoundStarter;
import geniusneugul.project.core.question.domain.Question;
import geniusneugul.project.core.question.implement.QuestionPicker;
import geniusneugul.project.core.room.implement.RoomMember;
import geniusneugul.project.core.room.implement.RoomMemberReader;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게임 진행. 판정·점수·승자 결정은 메모리(방 락 안)에서 하고, 그 결과를 게임·라운드 기록으로 DB에 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final RoomMemberReader roomMemberReader;
    private final QuestionPicker questionPicker;
    private final GameAppender gameAppender;
    private final GameStarter gameStarter;
    private final GameReader gameReader;
    private final AnswerSubmitter answerSubmitter;
    private final RoundStarter roundStarter;
    private final GameProgressRemover gameProgressRemover;
    private final Clock clock;

    /**
     * 방장이 게임을 시작하고 첫 라운드를 연다. 방장이 아니거나 이미 게임 중이면 방 락 안에서 실패하고,
     * 먼저 저장한 게임은 트랜잭션과 함께 되돌린다.
     */
    @Transactional
    public GameStartResult start(StartGameCommand command) {
        RoomMember starter = roomMemberReader.read(command.participantToken());
        GameType gameType = GameType.from(command.gameType());
        Question firstQuestion = questionPicker.pick(gameType, List.of());
        Game game = gameAppender.append(starter.roomId(), gameType, command.targetScore(), firstQuestion,
                LocalDateTime.now(clock));
        GameStart start = gameStarter.start(command.participantToken(), game, firstQuestion);

        log.info("[GameService.start] Game started. roomId={}, gameId={}, gameType={}, targetScore={}",
                start.roomId(), start.gameId(), start.gameType(), start.targetScore());
        return GameStartResult.from(start);
    }

    /** 채팅을 정답 제출로 판정한다. 정답자가 확정되면 그 결과를, 아니면 빈 값을 돌려준다 */
    @Transactional
    public Optional<RoundSolvedResult> submit(SubmitAnswerCommand command) {
        return answerSubmitter.submit(command.participantToken(), command.text())
                .map(solve -> {
                    record(solve);
                    return RoundSolvedResult.from(solve);
                });
    }

    /** 정답자가 나온 뒤 다음 라운드를 연다. 그 사이 게임이 끝났으면 빈 값이다 */
    @Transactional
    public Optional<RoundStartResult> startNextRound(Long roomId, Long gameId) {
        Game game = gameReader.readForUpdate(gameId);
        if (!game.isInProgress()) {
            return Optional.empty();
        }
        Question question = questionPicker.pick(game.getGameType(), game.getAskedQuestionIds());
        LocalDateTime now = LocalDateTime.now(clock);
        return roundStarter.start(roomId, gameId, question, now)
                .map(start -> {
                    game.startRound(start.questionId(), start.startedAt());
                    return RoundStartResult.from(start);
                });
    }

    /** 방장이 나가 방이 닫혔다. 진행 중이던 게임은 승자 없이 끝낸다 */
    @Transactional
    public void endWithoutWinner(Long roomId) {
        gameProgressRemover.remove(roomId).ifPresent(gameId -> {
            Game game = gameReader.readForUpdate(gameId);
            if (game.isInProgress()) {
                game.finishWithoutWinner(LocalDateTime.now(clock));
                log.info("[GameService.endWithoutWinner] Game ended without winner. roomId={}, gameId={}", roomId, gameId);
            }
        });
    }

    private void record(RoundSolve solve) {
        Game game = gameReader.readForUpdate(solve.gameId());
        game.solveRound(solve.roundNo(), solve.solverId(), solve.solvedAt());
        log.info("[GameService.submit] Round solved. roomId={}, gameId={}, roundNo={}, participantId={}",
                solve.roomId(), solve.gameId(), solve.roundNo(), solve.solverId());
        if (solve.gameFinished()) {
            game.finish(solve.solverId(), solve.solvedAt());
            log.info("[GameService.submit] Game finished. roomId={}, gameId={}, winnerId={}",
                    solve.roomId(), solve.gameId(), solve.solverId());
        }
    }
}
