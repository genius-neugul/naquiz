package geniusneugul.project.core.game.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "game_round", uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "round_no"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private Game game;

    @Column(nullable = false)
    private int roundNo;

    @Column(nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStatus status;

    // 참가자는 저장하지 않으므로 FK 없이 ID만 남긴다.
    private Long solverParticipantId;

    // 차례 정보는 영화 스무고개 진행 중에만 쓰고 통계에 필요 없어 저장하지 않는다.
    @Transient
    private List<Long> turnOrder = new ArrayList<>();

    @Transient
    private Integer currentTurnOrderIndex;

    @Transient
    private LocalDateTime turnDeadline;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("revealOrder")
    private List<RevealedHint> revealedHints = new ArrayList<>();

    public RevealedHint revealHint(HintType hintType, String revealedContent, LocalDateTime revealedAt) {
        RevealedHint hint = RevealedHint.of(this, hintType, revealedContent, revealedHints.size() + 1, revealedAt);
        revealedHints.add(hint);
        return hint;
    }

    public Optional<RevealedHint> lastAnswerHint() {
        return revealedHints.stream()
                .filter(hint -> hint.getHintType().isAnswerHint())
                .max(Comparator.comparingInt(RevealedHint::getRevealOrder));
    }
}
