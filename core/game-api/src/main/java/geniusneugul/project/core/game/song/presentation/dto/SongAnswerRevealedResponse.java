package geniusneugul.project.core.game.song.presentation.dto;

import geniusneugul.project.core.game.song.service.SongPlaybackResult;
import java.util.List;

/**
 * 방 토픽으로 나가는 노래 정답 공개 정보. ROUND_SOLVED 직후에 온다. 노래 제목은 ROUND_SOLVED.answer다.
 * 발매일은 정답 공개에 보여주지 않는다. albumImageUrl은 Spotify 정보가 없으면 null이다.
 */
public record SongAnswerRevealedResponse(String type, Long gameId, int roundNo, List<String> artists,
                                         String albumImageUrl) {

    private static final String TYPE = "SONG_ANSWER_REVEALED";

    public static SongAnswerRevealedResponse of(Long gameId, int roundNo, SongPlaybackResult playback) {
        return new SongAnswerRevealedResponse(TYPE, gameId, roundNo, playback.artists(), playback.albumImageUrl());
    }
}
