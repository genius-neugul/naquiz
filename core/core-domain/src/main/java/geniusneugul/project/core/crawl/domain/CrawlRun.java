package geniusneugul.project.core.crawl.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import geniusneugul.project.core.song.domain.SourceProgram;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 데일리 크롤링 한 번의 프로그램별 실행 기록.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceProgram sourceProgram;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrawlRunStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Integer lastSeq;

    private LocalDate lastBroadcastDate;

    private Integer addedSongCount;

    private Integer duplicateSongCount;

    @ElementCollection
    @CollectionTable(name = "crawl_run_failed_seq", joinColumns = @JoinColumn(name = "crawl_run_id"))
    @Column(name = "seq", nullable = false)
    private List<Integer> failedSeqs = new ArrayList<>();
}
