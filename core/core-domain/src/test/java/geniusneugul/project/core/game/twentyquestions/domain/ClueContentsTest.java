package geniusneugul.project.core.game.twentyquestions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.movie.domain.Movie;
import geniusneugul.project.core.question.domain.Answer;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClueContentsTest {

    private static final Answer ANSWER = new Answer("기생충", "Parasite");

    @DisplayName("영화 정보를 단서 종류별 공개 내용으로 바꾼다.")
    @Test
    void of() {
        // given
        Movie movie = Movie.create("20183782", "기생충", "Parasite", 10_313_086L, LocalDate.of(2019, 5, 30),
                List.of("봉준호"), List.of("송강호", "이선균", "조여정", "최우식", "박소담", "이정은"),
                "기택 가족의 이야기", List.of("드라마", "스릴러"), List.of("한국"), "15세이상관람가");

        // when
        ClueContents contents = ClueContents.of(movie, ANSWER);

        // then
        assertThat(contents.get(ClueType.AUDIENCE)).isEqualTo("약 1,031만 명");
        assertThat(contents.get(ClueType.RELEASE_DATE)).isEqualTo("2019-05-30");
        assertThat(contents.get(ClueType.DIRECTOR)).isEqualTo("봉준호");
        assertThat(contents.get(ClueType.CAST)).isEqualTo("송강호, 이선균, 조여정, 최우식, 박소담");
        assertThat(contents.get(ClueType.SYNOPSIS)).isEqualTo("기택 가족의 이야기");
        assertThat(contents.get(ClueType.GENRE)).isEqualTo("드라마, 스릴러");
        assertThat(contents.get(ClueType.NATION)).isEqualTo("한국");
        assertThat(contents.get(ClueType.RATING)).isEqualTo("15세이상관람가");
    }

    @DisplayName("관객 수는 1만 명 이상이면 만 명 단위로 반올림하고, 없으면 집계 X로 보여준다.")
    @ParameterizedTest
    @CsvSource(value = {
            "10313086, '약 1,031만 명'",
            "10315000, '약 1,032만 명'",
            "10000, 약 1만 명",
            "9999, '9,999명'",
            "NULL, 집계 X"
    }, nullValues = "NULL")
    void of_audience(Long audienceCount, String expected) {
        // when
        ClueContents contents = ClueContents.of(movie(audienceCount, "줄거리"), ANSWER);

        // then
        assertThat(contents.get(ClueType.AUDIENCE)).isEqualTo(expected);
    }

    @DisplayName("시놉시스에 나오는 정답·보조 정답은 대소문자·공백 차이를 무시하고 ○○○로 가린다.")
    @Test
    void of_masksTitleInSynopsis() {
        // given
        Movie movie = movie(1L, "기 생충은 PARASITE라는 뜻이다. 기생충 가족의 parasite 이야기");

        // when
        ClueContents contents = ClueContents.of(movie, ANSWER);

        // then
        assertThat(contents.get(ClueType.SYNOPSIS)).isEqualTo("○○○은 ○○○라는 뜻이다. ○○○ 가족의 ○○○ 이야기");
    }

    @DisplayName("보조 정답이 없어도 정답만 가린다.")
    @Test
    void of_withoutSubAnswer() {
        // when
        ClueContents contents = ClueContents.of(movie(1L, "괴물이 나타났다"), new Answer("괴물", null));

        // then
        assertThat(contents.get(ClueType.SYNOPSIS)).isEqualTo("○○○이 나타났다");
    }

    @DisplayName("시놉시스가 없으면 정보 없음으로 보여준다.")
    @Test
    void of_withoutSynopsis() {
        // when
        ClueContents contents = ClueContents.of(movie(1L, null), ANSWER);

        // then
        assertThat(contents.get(ClueType.SYNOPSIS)).isEqualTo("정보 없음");
    }

    private Movie movie(Long audienceCount, String synopsis) {
        return Movie.create("20183782", "기생충", "Parasite", audienceCount, LocalDate.of(2019, 5, 30),
                List.of("봉준호"), List.of("송강호"), synopsis, List.of("드라마"), List.of("한국"), "15세이상관람가");
    }
}
