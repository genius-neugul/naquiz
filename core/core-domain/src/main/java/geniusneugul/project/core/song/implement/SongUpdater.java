package geniusneugul.project.core.song.implement;

import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.song.domain.Audio;
import geniusneugul.project.core.song.domain.Song;
import geniusneugul.project.core.song.domain.SpotifyTrack;
import geniusneugul.project.core.song.infra.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 출제할 때 조회한 음원·Spotify 정보를 노래에 저장한다.
 * 외부 호출을 기다리는 동안 DB 커넥션을 잡지 않도록 노래 준비는 트랜잭션 밖에서 돌고, 저장만 짧은 독립 트랜잭션으로 한다.
 * 여러 방이 같은 노래를 동시에 준비할 수 있으므로 먼저 저장된 값을 덮어쓰지 않는다.
 */
@Component
@RequiredArgsConstructor
public class SongUpdater {

    private final SongRepository songRepository;

    @Transactional
    public void attachAudio(Long songId, Audio audio) {
        Song song = read(songId);
        if (!song.isPlayable()) {
            song.attachAudio(audio);
        }
    }

    @Transactional
    public void enrich(Long songId, SpotifyTrack track) {
        Song song = read(songId);
        if (!song.isEnriched()) {
            song.enrich(track);
        }
    }

    private Song read(Long songId) {
        return songRepository.findById(songId)
                .orElseThrow(() -> new IllegalStateException(
                        ErrorCode.SONG_NOT_FOUND.getMessage() + " songId=" + songId));
    }
}
