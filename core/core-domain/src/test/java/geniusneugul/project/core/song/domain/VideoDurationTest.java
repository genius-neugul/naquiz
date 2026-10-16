package geniusneugul.project.core.song.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class VideoDurationTest {

    @DisplayName("m:ss와 h:mm:ss를 초로 바꾼다.")
    @ParameterizedTest
    @CsvSource({
            "2:58, 178",
            "12:05, 725",
            "1:02:03, 3723"
    })
    void parseSeconds(String text, int expected) {
        assertThat(VideoDuration.parseSeconds(text)).hasValue(expected);
    }

    @DisplayName("형식이 맞지 않거나 0초면 재생 시간이 없다.")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"LIVE", "3:7", "3:60", "178", "0:00"})
    void parseSeconds_invalid(String text) {
        assertThat(VideoDuration.parseSeconds(text)).isEmpty();
    }
}
