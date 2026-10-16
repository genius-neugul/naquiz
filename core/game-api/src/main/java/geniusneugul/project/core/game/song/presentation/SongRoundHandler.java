package geniusneugul.project.core.game.song.presentation;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.common.presentation.EndedRound;
import geniusneugul.project.core.game.common.presentation.RoundStartedHandler;
import geniusneugul.project.core.game.common.presentation.StartedRound;
import geniusneugul.project.core.game.song.presentation.dto.SongAnswerRevealedResponse;
import geniusneugul.project.core.game.song.presentation.dto.SongAudioResponse;
import geniusneugul.project.core.game.song.service.SongService;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 노래 라운드가 열리면 재생할 음원(SONG_AUDIO)을, 끝나면 가수·앨범 이미지(SONG_ANSWER_REVEALED)를 보낸다.
 */
@Component
@RequiredArgsConstructor
public class SongRoundHandler implements RoundStartedHandler {

    private final SongService songService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    @Override
    public boolean supports(GameType gameType) {
        return gameType == GameType.SONG;
    }

    @Override
    public void onRoundStarted(StartedRound round) {
        songService.playback(round.questionId()).ifPresent(playback -> messagingTemplate.convertAndSend(
                topic(round.roomId()),
                SongAudioResponse.of(round.gameId(), round.roundNo(), round.startedAt(), playback,
                        LocalDateTime.now(clock))));
    }

    @Override
    public void onRoundEnded(EndedRound round) {
        songService.playback(round.questionId()).ifPresent(playback -> messagingTemplate.convertAndSend(
                topic(round.roomId()), SongAnswerRevealedResponse.of(round.gameId(), round.roundNo(), playback)));
    }

    @Override
    public void onGameFinished(Long roomId) {
        // 노래 게임은 방별 진행 상태를 따로 두지 않는다.
    }

    private String topic(Long roomId) {
        return "/topic/rooms/" + roomId;
    }
}
