package geniusneugul.project.core.game.stillcut.presentation;

import geniusneugul.project.core.game.common.presentation.Countdown;
import geniusneugul.project.core.game.common.presentation.CountdownKind;
import geniusneugul.project.core.game.common.presentation.CountdownTimer;
import geniusneugul.project.core.game.common.presentation.RoundStartedHandler;
import geniusneugul.project.core.game.common.presentation.StartedRound;
import geniusneugul.project.core.game.stillcut.presentation.dto.StillCutRevealedResponse;
import geniusneugul.project.core.game.stillcut.service.StillCutRevealResult;
import geniusneugul.project.core.game.stillcut.service.StillCutService;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 스틸컷 라운드가 열리면 첫 스틸컷을 보내고, 서버 카운트다운으로 일정 간격(10초)마다 다음 스틸컷을 보낸다.
 * 정답이 나오면 공통 흐름이 그 방의 카운트다운을 취소하고, 늦게 도착한 공개는 서비스가 라운드를 다시 확인해 막는다.
 */
@Component
public class StillCutRoundHandler implements RoundStartedHandler {

    private static final String GAME_TYPE = "MOVIE_STILL_CUT";

    private final StillCutService stillCutService;
    private final CountdownTimer countdownTimer;
    private final SimpMessagingTemplate messagingTemplate;
    private final Duration interval;

    public StillCutRoundHandler(StillCutService stillCutService, CountdownTimer countdownTimer,
            SimpMessagingTemplate messagingTemplate, @Value("${app.game.still-cut-interval}") Duration interval) {
        this.stillCutService = stillCutService;
        this.countdownTimer = countdownTimer;
        this.messagingTemplate = messagingTemplate;
        this.interval = interval;
    }

    @Override
    public boolean supports(String gameType) {
        return GAME_TYPE.equals(gameType);
    }

    @Override
    public void onRoundStarted(StartedRound round) {
        stillCutService.startReel(round.roomId(), round.gameId(), round.roundNo(), round.questionId())
                .ifPresent(this::show);
    }

    @Override
    public void onGameFinished(Long roomId) {
        stillCutService.clear(roomId);
    }

    private void show(StillCutRevealResult reveal) {
        LocalDateTime nextAt = null;
        if (reveal.hasNext()) {
            Countdown countdown = countdownTimer.start(reveal.roomId(), CountdownKind.STILL_CUT, interval,
                    () -> revealNext(reveal.roomId(), reveal.gameId(), reveal.roundNo()));
            nextAt = countdown.deadline();
        }
        messagingTemplate.convertAndSend("/topic/rooms/" + reveal.roomId(), StillCutRevealedResponse.of(reveal, nextAt));
    }

    private void revealNext(Long roomId, Long gameId, int roundNo) {
        stillCutService.revealNext(roomId, gameId, roundNo).ifPresent(this::show);
    }
}
