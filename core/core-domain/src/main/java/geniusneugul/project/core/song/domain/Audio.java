package geniusneugul.project.core.song.domain;

import geniusneugul.project.core.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 노래의 음원(YouTube 영상). 노래 애그리거트의 내부 엔티티로 노래당 하나다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Audio {

    private static final String WATCH_URL = "https://www.youtube.com/watch?v=";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String videoId;

    @Column(nullable = false)
    private String url;

    // 정답이 들어 있을 수 있어 화면에 보여주지 않는다.
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int durationSeconds;

    private String channelName;

    private Long viewCount;

    private String publishedTimeText;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    private Audio(String videoId, String title, int durationSeconds, String channelName, Long viewCount,
                  String publishedTimeText, LocalDateTime fetchedAt) {
        this.videoId = videoId;
        this.url = WATCH_URL + videoId;
        this.title = title;
        this.durationSeconds = durationSeconds;
        this.channelName = channelName;
        this.viewCount = viewCount;
        this.publishedTimeText = publishedTimeText;
        this.fetchedAt = fetchedAt;
    }

    /**
     * 영상 선택 규칙으로 고른 후보로 음원을 만든다. 재생 시간을 읽을 수 있는 후보만 넘긴다({@link AudioSelector}).
     *
     * @param fetchedAt YouTube를 조회한 시각. 업로드 시기(상대 시간)의 기준이다
     */
    public static Audio create(VideoCandidate candidate, LocalDateTime fetchedAt) {
        int durationSeconds = candidate.durationSeconds()
                .orElseThrow(() -> new IllegalArgumentException(
                        ErrorCode.SONG_AUDIO_WITHOUT_DURATION.getMessage() + " videoId=" + candidate.videoId()));
        return new Audio(candidate.videoId(), Objects.requireNonNullElse(candidate.title(), ""), durationSeconds,
                candidate.channelName(), candidate.viewCount(), candidate.publishedTimeText(), fetchedAt);
    }
}
