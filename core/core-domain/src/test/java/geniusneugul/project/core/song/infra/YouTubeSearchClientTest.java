package geniusneugul.project.core.song.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import geniusneugul.project.core.song.domain.VideoCandidate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class YouTubeSearchClientTest {

    private static final String SEARCH_URL = "https://youtubei.googleapis.com/youtubei/v1/search";

    private MockRestServiceServer server;
    private YouTubeSearchClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new YouTubeSearchClient(builder.build());
    }

    @DisplayName("첫 번째 섹션의 영상만 검색 순서대로 최대 5개 읽는다.")
    @Test
    void search_firstSectionTopFive() {
        // given
        String firstSection = IntStream.rangeClosed(1, 6)
                .mapToObj(i -> video("video-" + i, "3:0" + i))
                .collect(Collectors.joining(","));
        respond(twoSections(firstSection, video("other-section", "3:00")));

        // when
        List<VideoCandidate> candidates = client.search("밤편지 아이유");

        // then
        assertThat(candidates).extracting(VideoCandidate::videoId)
                .containsExactly("video-1", "video-2", "video-3", "video-4", "video-5");
    }

    @DisplayName("영상의 videoId, 제목, 재생 시간, 채널명, 조회수, 업로드 시기를 원문 그대로 읽는다.")
    @Test
    void search_readsVideoFields() {
        // given
        respond(sections(video("video-1", "4:14")));

        // when
        List<VideoCandidate> candidates = client.search("밤편지 아이유");

        // then
        assertThat(candidates).containsExactly(new VideoCandidate("video-1", "제목 video-1", "4:14", "채널",
                "254,341,635 views", "4 years ago"));
    }

    @DisplayName("채널명·조회수·업로드 시기가 없는 영상은 그 값만 비우고 후보로 둔다.")
    @Test
    void search_optionalFieldsMissing() {
        // given
        respond(sections("""
                {"compactVideoRenderer": {
                  "videoId": "video-1",
                  "title": {"runs": [{"text": "밤편지"}]},
                  "lengthText": {"runs": [{"text": "4:14"}]}
                }}
                """));

        // when
        List<VideoCandidate> candidates = client.search("밤편지 아이유");

        // then
        assertThat(candidates).containsExactly(new VideoCandidate("video-1", "밤편지", "4:14", null, null, null));
    }

    @DisplayName("videoId나 재생 시간이 없는 항목, 영상이 아닌 항목은 후보에서 뺀다.")
    @Test
    void search_skipsUnplayableItems() {
        // given
        respond(sections(
                """
                        {"compactVideoRenderer": {"title": {"runs": [{"text": "videoId 없음"}]}, "lengthText": {"runs": [{"text": "3:00"}]}}}
                        """,
                """
                        {"compactVideoRenderer": {"videoId": "live-stream", "title": {"runs": [{"text": "재생 시간 없음"}]}}}
                        """,
                """
                        {"compactChannelRenderer": {"channelId": "channel-1"}}
                        """,
                video("video-1", "4:14")));

        // when
        List<VideoCandidate> candidates = client.search("밤편지 아이유");

        // then
        assertThat(candidates).extracting(VideoCandidate::videoId).containsExactly("video-1");
    }

    @DisplayName("검색 결과가 비었거나 응답 구조가 다르면 빈 목록을 돌려준다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"contents\": {\"sectionListRenderer\": {\"contents\": []}}}",
            "{\"contents\": {\"twoColumnSearchResultsRenderer\": {}}}",
            "{\"contents\": []}"
    })
    void search_emptyOrUnknownStructure(String body) {
        // given
        server.expect(requestTo(SEARCH_URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        // when
        List<VideoCandidate> candidates = client.search("밤편지 아이유");

        // then
        assertThat(candidates).isEmpty();
    }

    private void respond(String body) {
        server.expect(requestTo(SEARCH_URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private static String sections(String... firstSectionItems) {
        return twoSections(String.join(",", firstSectionItems), null);
    }

    private static String twoSections(String firstSectionItems, String secondSectionItems) {
        String second = secondSectionItems == null ? ""
                : ", {\"itemSectionRenderer\": {\"contents\": [" + secondSectionItems + "]}}";
        return """
                {"contents": {"sectionListRenderer": {"contents": [
                  {"itemSectionRenderer": {"contents": [%s]}}%s
                ]}}}
                """.formatted(firstSectionItems, second);
    }

    private static String video(String videoId, String length) {
        return """
                {"compactVideoRenderer": {
                  "videoId": "%s",
                  "title": {"runs": [{"text": "제목 %s"}]},
                  "lengthText": {"runs": [{"text": "%s"}]},
                  "longBylineText": {"runs": [{"text": "채널"}]},
                  "viewCountText": {"runs": [{"text": "254,341,635 views"}]},
                  "publishedTimeText": {"runs": [{"text": "4 years ago"}]}
                }}
                """.formatted(videoId, videoId, length);
    }
}
