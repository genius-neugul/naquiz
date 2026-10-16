package geniusneugul.project.core.game.song.service;

import geniusneugul.project.core.song.domain.SongPlayback;
import java.util.List;

/**
 * 라운드 노래의 재생·공개 정보.
 *
 * @param artists       가수의 한글 이름. 원본 순서대로다
 * @param albumImageUrl Spotify 정보가 없으면 null
 */
public record SongPlaybackResult(String videoId, int durationSeconds, List<String> artists, String albumImageUrl) {

    static SongPlaybackResult from(SongPlayback playback) {
        return new SongPlaybackResult(playback.videoId(), playback.durationSeconds(), playback.artists(),
                playback.albumImageUrl());
    }
}
