package geniusneugul.project.core.game.twentyquestions.presentation;

import geniusneugul.project.core.game.common.presentation.Countdown;
import geniusneugul.project.core.game.common.presentation.CountdownKind;
import geniusneugul.project.core.game.common.presentation.CountdownTimer;
import geniusneugul.project.core.game.common.presentation.RoundStartedHandler;
import geniusneugul.project.core.game.common.presentation.StartedRound;
import geniusneugul.project.core.game.twentyquestions.presentation.dto.ClueRevealedResponse;
import geniusneugul.project.core.game.twentyquestions.presentation.dto.ClueSelectedResponse;
import geniusneugul.project.core.game.twentyquestions.presentation.dto.TurnStartedResponse;
import geniusneugul.project.core.game.twentyquestions.service.ClueRevealResult;
import geniusneugul.project.core.game.twentyquestions.service.ClueSelectResult;
import geniusneugul.project.core.game.twentyquestions.service.TurnStartResult;
import geniusneugul.project.core.game.twentyquestions.service.TwentyQuestionsService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 영화 스무고개 라운드가 열리면 첫 차례를 알리고, 차례마다 서버 카운트다운(10초)을 건다. 시간이 지나면 남은 단서 중 하나를 무작위로 대신 고른다.
 * 단서를 고르면 고른 칸만 알리고 카운트다운(3초) 뒤 공개한 다음 차례를 준다.
 * 정답이 나오면 공통 흐름이 그 방의 카운트다운을 취소하고, 늦게 도착한 차례 마감·공개는 서비스가 차례·라운드를 다시 확인해 막는다.
 */
@Component
public class TwentyQuestionsRoundHandler implements RoundStartedHandler {

    private static final String GAME_TYPE = "MOVIE_TWENTY_QUESTIONS";

    private final TwentyQuestionsService twentyQuestionsService;
    private final CountdownTimer countdownTimer;
    private final SimpMessagingTemplate messagingTemplate;
    private final Duration turnDuration;
    private final Duration clueRevealDelay;

    public TwentyQuestionsRoundHandler(TwentyQuestionsService twentyQuestionsService, CountdownTimer countdownTimer,
            SimpMessagingTemplate messagingTemplate, @Value("${app.game.turn-duration}") Duration turnDuration,
            @Value("${app.game.clue-reveal-delay}") Duration clueRevealDelay) {
        this.twentyQuestionsService = twentyQuestionsService;
        this.countdownTimer = countdownTimer;
        this.messagingTemplate = messagingTemplate;
        this.turnDuration = turnDuration;
        this.clueRevealDelay = clueRevealDelay;
    }

    @Override
    public boolean supports(String gameType) {
        return GAME_TYPE.equals(gameType);
    }

    @Override
    public void onRoundStarted(StartedRound round) {
        twentyQuestionsService.startBoard(round.roomId(), round.gameId(), round.roundNo(), round.questionId())
                .ifPresent(this::startTurn);
    }

    @Override
    public void onGameFinished(Long roomId) {
        twentyQuestionsService.clear(roomId);
    }

    /** 고른 단서를 알리고 공개 카운트다운을 건다 */
    void showSelect(ClueSelectResult select) {
        Countdown countdown = countdownTimer.start(select.roomId(), CountdownKind.CLUE_REVEAL, clueRevealDelay,
                () -> revealSelected(select));
        messagingTemplate.convertAndSend(topic(select.roomId()), ClueSelectedResponse.of(select, countdown.deadline()));
    }

    private void revealSelected(ClueSelectResult select) {
        twentyQuestionsService.revealSelected(select.roomId(), select.gameId(), select.roundNo())
                .ifPresent(this::showReveal);
    }

    /** 단서 공개를 알리고, 이어서 시작한 차례가 있으면 알린다 */
    private void showReveal(ClueRevealResult reveal) {
        messagingTemplate.convertAndSend(topic(reveal.roomId()), ClueRevealedResponse.from(reveal));
        if (reveal.nextTurn() != null) {
            startTurn(reveal.nextTurn());
        }
    }

    /** 차례를 알리고 차례 마감 카운트다운을 건다. 같은 방의 이전 차례 카운트다운은 취소된다 */
    void startTurn(TurnStartResult turn) {
        Countdown countdown = countdownTimer.start(turn.roomId(), CountdownKind.TURN, turnDuration,
                () -> selectOnTimeout(turn));
        messagingTemplate.convertAndSend(topic(turn.roomId()), TurnStartedResponse.of(turn, countdown.deadline()));
    }

    private void selectOnTimeout(TurnStartResult turn) {
        twentyQuestionsService.selectOnTimeout(turn.roomId(), turn.gameId(), turn.roundNo(), turn.turnNo())
                .ifPresent(this::showSelect);
    }

    private String topic(Long roomId) {
        return "/topic/rooms/" + roomId;
    }
}
