package geniusneugul.project.core.question.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import geniusneugul.project.core.common.domain.GameType;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    // 노래 게임은 songId, 영화 게임(스무고개·스틸컷)은 movieId
    @Column(nullable = false)
    private Long contentId;

    @Column(nullable = false)
    private String answer;

    private String subAnswer;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus reviewStatus;

    private Long reviewedByAdminId;

    private LocalDateTime reviewedAt;

    public Answer toAnswer() {
        return new Answer(answer, subAnswer);
    }
}
