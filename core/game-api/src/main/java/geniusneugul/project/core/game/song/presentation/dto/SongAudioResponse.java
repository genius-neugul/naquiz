package geniusneugul.project.core.game.song.presentation.dto;

import geniusneugul.project.core.game.song.service.SongPlaybackResult;
import java.time.LocalDateTime;

/**
 * 방 토픽으로 나가는 노래 재생 이벤트. 라운드 시작(startedAt)부터 노래를 처음부터 반복 재생한다고 보고,
 * 모두가 (서버 기준 지금 - startedAt) mod durationSeconds 위치를 듣는다.
 * serverTime은 보낸 시각이다. 클라이언트는 받은 시각과 비교해 자기 시계와 서버 시계의 차이를 구한다.
 */
public record SongAudioResponse(String type, Long gameId, int roundNo, String videoId, int durationSeconds,
                                LocalDateTime startedAt, LocalDateTime serverTime) {

    private static final String TYPE = "SONG_AUDIO";

    public static SongAudioResponse of(Long gameId, int roundNo, LocalDateTime startedAt, SongPlaybackResult playback,
                                       LocalDateTime serverTime) {
        return new SongAudioResponse(TYPE, gameId, roundNo, playback.videoId(), playback.durationSeconds(), startedAt,
                serverTime);
    }
}
