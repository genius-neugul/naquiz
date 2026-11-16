package geniusneugul.project.core.game.twentyquestions.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 영화 스무고개에서 실제로 공개된 단서(docs/DOMAIN.md 3-2). 통계에 쓰므로 DB에 남긴다.
 * 스무고개에서만 쓰므로 게임 애그리거트에 넣지 않고 게임 ID로만 게임을 참조한다.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "round_no", "clue_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevealedClue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    @Column(nullable = false)
    private int roundNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClueType clueType;

    // 시놉시스는 제목을 가린 뒤 저장한다.
    @Lob
    @Column(nullable = false)
    private String revealedContent;

    @Column(nullable = false)
    private int revealOrder;

    // 참가자는 저장하지 않으므로 FK 없이 ID만 남긴다.
    @Column(nullable = false)
    private Long revealedByParticipantId;

    @Column(nullable = false)
    private LocalDateTime revealedAt;

    private RevealedClue(Long gameId, int roundNo, ClueType clueType, String revealedContent, int revealOrder,
            Long revealedByParticipantId, LocalDateTime revealedAt) {
        this.gameId = gameId;
        this.roundNo = roundNo;
        this.clueType = clueType;
        this.revealedContent = revealedContent;
        this.revealOrder = revealOrder;
        this.revealedByParticipantId = revealedByParticipantId;
        this.revealedAt = revealedAt;
    }

    /** 공개 순번은 그 라운드에서 연 순서다 */
    public static RevealedClue create(Long gameId, int roundNo, ClueType clueType, String revealedContent,
            int revealOrder, Long revealedByParticipantId, LocalDateTime revealedAt) {
        return new RevealedClue(gameId, roundNo, clueType, revealedContent, revealOrder, revealedByParticipantId,
                revealedAt);
    }
}
