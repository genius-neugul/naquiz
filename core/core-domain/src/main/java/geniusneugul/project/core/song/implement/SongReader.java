package geniusneugul.project.core.song.implement;

import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.song.domain.Song;
import geniusneugul.project.core.song.domain.SongPlayback;
import geniusneugul.project.core.song.infra.SongRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SongReader {

    private final SongRepository songRepository;

    /** 가수·음원까지 읽는다. 문제의 콘텐츠 ID로 읽으므로 없으면 코드 오류다 */
    public Song readWithAudio(Long songId) {
        return songRepository.findWithAudioById(songId)
                .orElseThrow(() -> new IllegalStateException(
                        ErrorCode.SONG_NOT_FOUND.getMessage() + " songId=" + songId));
    }

    /** 음원이 없는 노래면 빈 값이다 */
    public Optional<SongPlayback> readPlayback(Long songId) {
        Song song = readWithAudio(songId);
        return song.isPlayable() ? Optional.of(SongPlayback.from(song)) : Optional.empty();
    }
}
