package geniusneugul.project.core.game.common.service;

import geniusneugul.project.core.game.common.domain.Game;
import geniusneugul.project.core.game.common.domain.RevealedHint;
import geniusneugul.project.core.game.common.implement.AnswerHintRevealer;
import geniusneugul.project.core.game.common.implement.GameAborter;
import geniusneugul.project.core.game.common.implement.GameAppender;
import geniusneugul.project.core.game.common.implement.GameProgressRemover;
import geniusneugul.project.core.game.common.implement.GameReader;
import geniusneugul.project.core.game.common.implement.GameStart;
import geniusneugul.project.core.game.common.implement.GameStarter;
import geniusneugul.project.core.game.common.implement.RoundStarter;
import geniusneugul.project.core.question.domain.Question;
import geniusneugul.project.core.question.implement.QuestionReader;
import geniusneugul.project.core.room.implement.RoomMember;
import geniusneugul.project.core.room.implement.RoomMemberReader;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게임 시작·다음 라운드·승자 없는 종료. 판정·점수·승자 결정은 메모리(방 락 안)에서 하고, 그 결과를 DB에 남긴다.
 * 라운드에 낼 문제는 RoundQuestionService가 트랜잭션 밖에서 미리 준비해 넘긴다.
 * 정답 제출은 채팅으로 들어오므로 ChatService가 game implement로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final RoomMemberReader roomMemberReader;
    private final QuestionReader questionReader;
    private final GameAppender gameAppender;
    private final GameStarter gameStarter;
    private final GameReader gameReader;
    private final RoundStarter roundStarter;
    private final GameProgressRemover gameProgressRemover;
    private final GameAborter gameAborter;
    private final AnswerHintRevealer answerHintRevealer;
    private final Clock clock;

    /**
     * 방장이 게임을 시작하고 첫 라운드를 연다. 방장이 아니거나 이미 게임 중이면 방 락 안에서 실패하고,
     * 먼저 저장한 게임은 트랜잭션과 함께 되돌린다.
     */
    @Transactional
    public GameStartResult start(StartGameCommand command, PreparedQuestion prepared) {
        RoomMember starter = roomMemberReader.read(command.participantToken());
        Question firstQuestion = questionReader.read(prepared.questionId());
        Game game = gameAppender.append(starter.roomId(), command.gameType(), command.targetScore(), firstQuestion,
                LocalDateTime.now(clock));
        GameStart start = gameStarter.start(command.participantToken(), game, firstQuestion);
        AnswerHintResult answerHint = revealBaseHint(game, start.roundNo(), firstQuestion);

        log.info("[GameService.start] Game started. roomId={}, gameId={}, gameType={}, targetScore={}",
                start.roomId(), start.gameId(), start.gameType(), start.targetScore());
        log.info("[GameService.start] Round started. roomId={}, gameId={}, roundNo={}, answer={}, subAnswer={}",
                start.roomId(), start.gameId(), start.roundNo(), firstQuestion.getAnswer(), firstQuestion.getSubAnswer());
        return GameStartResult.from(start, answerHint);
    }

    /** 정답자가 나온 뒤 다음 라운드를 연다. 그 사이 게임이 끝났으면 빈 값이다 */
    @Transactional
    public Optional<RoundStartResult> startNextRound(Long roomId, Long gameId, PreparedQuestion prepared) {
        Game game = gameReader.readForUpdate(gameId);
        if (!game.isInProgress()) {
            return Optional.empty();
        }
        Question question = questionReader.read(prepared.questionId());
        LocalDateTime now = LocalDateTime.now(clock);
        return roundStarter.start(roomId, gameId, question, now)
                .map(start -> {
                    game.startRound(start.questionId(), start.startedAt());
                    AnswerHintResult answerHint = revealBaseHint(game, start.roundNo(), question);
                    log.info("[GameService.startNextRound] Round started. roomId={}, gameId={}, roundNo={}, answer={}, subAnswer={}",
                            roomId, gameId, start.roundNo(), question.getAnswer(), question.getSubAnswer());
                    return RoundStartResult.from(start, answerHint);
                });
    }

    // 라운드가 열리면 기본 힌트(ANSWER_MASK)를 투표 없이 공개한다(docs/DOMAIN.md 정답 힌트).
    private AnswerHintResult revealBaseHint(Game game, int roundNo, Question question) {
        RevealedHint hint = answerHintRevealer.reveal(game, roundNo, question.getAnswer());
        return AnswerHintResult.of(hint, answerHintRevealer.nextHintType(question.getAnswer(), hint));
    }

    /**
     * 다음 라운드 문제를 준비하지 못했다. 게임을 승자 없이 끝내고 방을 대기 상태로 되돌린다.
     * 그 사이 게임이 끝났으면(방장 퇴장 등) 빈 값이다.
     */
    @Transactional
    public Optional<GameAbortResult> abort(Long roomId, Long gameId) {
        return gameAborter.abort(roomId, gameId).map(scores -> {
            Game game = gameReader.readForUpdate(gameId);
            if (game.isInProgress()) {
                game.finishWithoutWinner(LocalDateTime.now(clock));
            }
            log.info("[GameService.abort] Game ended without winner, no question prepared. roomId={}, gameId={}",
                    roomId, gameId);
            return new GameAbortResult(roomId, gameId, ScoreResult.listOf(scores));
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
}
