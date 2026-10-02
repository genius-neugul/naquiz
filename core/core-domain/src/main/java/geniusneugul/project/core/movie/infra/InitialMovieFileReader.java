package geniusneugul.project.core.movie.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import geniusneugul.project.core.movie.domain.Movie;
import geniusneugul.project.core.movie.domain.MovieWithStillCuts;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * 초기 데이터 영화 목록(movies.json)을 읽는다. 형식은 initial_crawler/README.md 「영화 크롤링」.
 */
@Component
@RequiredArgsConstructor
public class InitialMovieFileReader {

    private static final String FILE_NAME = "movies.json";
    // KOBIS 요약 정보는 제작 나라 여러 개를 "미국, 영국"처럼 한 문자열로 준다.
    private static final String NATION_DELIMITER = ",";

    private final JsonMapper jsonMapper;

    public List<MovieWithStillCuts> read(Path dataDir) {
        List<MovieJson> movies = jsonMapper.readValue(dataDir.resolve(FILE_NAME).toFile(), new TypeReference<>() {
        });
        return movies.stream()
                .map(MovieJson::toMovieWithStillCuts)
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MovieJson(
            String movieCd,
            String titleKo,
            String titleEn,
            Long audiAcc,
            LocalDate openDt,
            List<PersonJson> directors,
            List<PersonJson> actors,
            String synopsis,
            SummaryJson summary,
            String rating,
            List<String> stills
    ) {

        MovieWithStillCuts toMovieWithStillCuts() {
            Movie movie = Movie.create(movieCd, titleKo, titleEn, audiAcc, openDt, names(directors), names(actors),
                    synopsis, summary.genres(), summary.nations(), rating);
            return new MovieWithStillCuts(movie, stills);
        }

        private static List<String> names(List<PersonJson> people) {
            return people.stream()
                    .map(PersonJson::name)
                    .toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PersonJson(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SummaryJson(List<String> genres, String nation) {

        List<String> nations() {
            return Arrays.stream(nation.split(NATION_DELIMITER))
                    .map(String::strip)
                    .filter(name -> !name.isEmpty())
                    .toList();
        }
    }
}
