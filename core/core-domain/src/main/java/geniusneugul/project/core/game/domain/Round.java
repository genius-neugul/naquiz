package geniusneugul.project.core.game.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    Round(Game game, int roundNo, Long questionId, LocalDateTime startedAt) {
        this.game = game;
        this.roundNo = roundNo;
        this.questionId = questionId;
        this.status = RoundStatus.IN_PROGRESS;
        this.startedAt = startedAt;
    }

    void solve(Long solverParticipantId, LocalDateTime now) {
        this.status = RoundStatus.SOLVED;
        this.solverParticipantId = solverParticipantId;
        this.endedAt = now;
    }
}
