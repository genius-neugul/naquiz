package geniusneugul.project.core.game.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "round_no", "clue_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevealedClue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private Game game;

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
}
