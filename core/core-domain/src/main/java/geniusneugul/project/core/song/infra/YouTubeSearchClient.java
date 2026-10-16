package geniusneugul.project.core.song.infra;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.song.domain.VideoCandidate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * YouTube에서 음원 후보 영상을 검색한다(MUSIC_SELECTION_RULE 2-1, 2-2).
 * 비공개 innertube API라 요청 값이나 응답 구조가 예고 없이 바뀔 수 있다. 바뀌면 이 클래스와 규칙 문서 2-1을 함께 고친다.
 * 결과가 없거나 응답 구조를 읽을 수 없으면 빈 목록을, 호출 자체가 실패하면 {@link ErrorCode#SONG_LOOKUP_FAILED}를 던진다.
 */
@Slf4j
@Component
public class YouTubeSearchClient {

    private static final String SEARCH_URL = "https://youtubei.googleapis.com/youtubei/v1/search";
    private static final Map<String, Object> CLIENT = Map.of(
            "clientName", "ANDROID_VR",
            "clientVersion", "1.60.19",
            "androidSdkVersion", 32,
            "clientScreen", "EMBED");
    // 동영상만 검색하는 필터
    private static final String VIDEO_FILTER = "EgIQAfABAQ==";
    private static final int MAX_CANDIDATES = 5;

    private final RestClient restClient;

    public YouTubeSearchClient(@Qualifier("songApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @param query {@code "{정제한 제목} {정제한 아티스트}"}
     * @return 첫 번째 섹션의 영상을 검색 순서대로 최대 5개. videoId나 재생 시간이 없는 항목은 뺀다
     */
    public List<VideoCandidate> search(String query) {
        long startedAt = System.nanoTime();
        try {
            SearchResponse response = restClient.post()
                    .uri(SEARCH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("context", Map.of("client", CLIENT), "query", query, "params", VIDEO_FILTER))
                    .retrieve()
                    .body(SearchResponse.class);
            return candidatesOf(response);
        } catch (RestClientException e) {
            if (e.getCause() instanceof HttpMessageNotReadableException) {
                log.warn("[YouTubeSearchClient.search] YouTube response unreadable. provider=youtube, elapsedMs={}",
                        elapsedMs(startedAt), e);
                return List.of();
            }
            log.warn("[YouTubeSearchClient.search] YouTube search failed. provider=youtube, statusCode={}, elapsedMs={}",
                    statusCodeOf(e), elapsedMs(startedAt), e);
            throw new BusinessException(ErrorCode.SONG_LOOKUP_FAILED, Map.of("provider", "youtube"), e);
        }
    }

    private static List<VideoCandidate> candidatesOf(SearchResponse response) {
        List<Item> items = Optional.ofNullable(response)
                .map(SearchResponse::contents)
                .map(Contents::sectionListRenderer)
                .map(SectionList::contents)
                .flatMap(sections -> sections.stream().findFirst())
                .map(Section::itemSectionRenderer)
                .map(ItemSection::contents)
                .orElse(List.of());
        return items.stream()
                .map(Item::compactVideoRenderer)
                .filter(Objects::nonNull)
                .filter(video -> video.videoId() != null && Text.firstOf(video.lengthText()) != null)
                .limit(MAX_CANDIDATES)
                .map(video -> new VideoCandidate(
                        video.videoId(),
                        Text.firstOf(video.title()),
                        Text.firstOf(video.lengthText()),
                        Text.firstOf(video.longBylineText()),
                        Text.firstOf(video.viewCountText()),
                        Text.firstOf(video.publishedTimeText())))
                .toList();
    }

    private static Object statusCodeOf(RestClientException e) {
        return e instanceof RestClientResponseException response ? response.getStatusCode().value() : "none";
    }

    private static long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    record SearchResponse(Contents contents) {
    }

    record Contents(SectionList sectionListRenderer) {
    }

    record SectionList(List<Section> contents) {
    }

    record Section(ItemSection itemSectionRenderer) {
    }

    record ItemSection(List<Item> contents) {
    }

    record Item(CompactVideo compactVideoRenderer) {
    }

    record CompactVideo(String videoId, Text title, Text lengthText, Text longBylineText, Text viewCountText,
                        Text publishedTimeText) {
    }

    record Text(List<Run> runs) {

        static String firstOf(Text text) {
            return Optional.ofNullable(text)
                    .map(Text::runs)
                    .flatMap(runs -> runs.stream().findFirst())
                    .map(Run::text)
                    .orElse(null);
        }
    }

    record Run(String text) {
    }
}
