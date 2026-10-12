package geniusneugul.project.core.game.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import geniusneugul.project.core.common.domain.GameType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Column(nullable = false)
    private int targetScore;

    // 참가자는 저장하지 않으므로 FK 없이 ID만 남긴다.
    private Long winnerId;

    @Column(nullable = false)
    private int currentRoundNo;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("roundNo")
    private List<Round> rounds = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("roundNo, revealOrder")
    private List<RevealedHint> revealedHints = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("roundNo, revealOrder")
    private List<RevealedClue> revealedClues = new ArrayList<>();

    public RevealedHint revealHint(int roundNo, HintType hintType, String revealedContent, LocalDateTime revealedAt) {
        int revealOrder = (int) revealedHints.stream().filter(hint -> hint.getRoundNo() == roundNo).count() + 1;
        RevealedHint hint = RevealedHint.of(this, roundNo, hintType, revealedContent, revealOrder, revealedAt);
        revealedHints.add(hint);
        return hint;
    }

    public Optional<RevealedHint> lastAnswerHint(int roundNo) {
        return revealedHints.stream()
                .filter(hint -> hint.getRoundNo() == roundNo && hint.getHintType().isAnswerHint())
                .max(Comparator.comparingInt(RevealedHint::getRevealOrder));
    }
}
