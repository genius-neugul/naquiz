package geniusneugul.project.core.song.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SongTest {

    @DisplayName("음원이 없는 노래는 출제할 수 없고, 음원을 붙이면 출제할 수 있다.")
    @Test
    void isPlayable() {
        // given
        Song song = Song.create("밤편지", null, List.of(new ArtistName("아이유", "IU")), "밤편지", "아이유(IU)", 1,
                SourceProgram.MUSIC_PARTY, 1, LocalDate.of(2026, 10, 1));
        boolean beforeAttach = song.isPlayable();

        // when
        song.attachAudio(Audio.create(new VideoCandidate("video-1", "밤편지 가사", "4:14", null, null, null),
                LocalDateTime.of(2026, 10, 16, 0, 0)));

        // then
        assertThat(beforeAttach).isFalse();
        assertThat(song.isPlayable()).isTrue();
    }
}
