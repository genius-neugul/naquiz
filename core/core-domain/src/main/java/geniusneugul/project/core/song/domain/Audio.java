package geniusneugul.project.core.song.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 노래의 음원(YouTube 영상). 노래 애그리거트의 내부 엔티티로 노래당 하나다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Audio {

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
}
