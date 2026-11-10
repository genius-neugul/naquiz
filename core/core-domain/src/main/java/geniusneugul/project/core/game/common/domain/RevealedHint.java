package geniusneugul.project.core.game.common.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 라운드에서 공개된 힌트 기록. 1차 MVP에서는 저장하지 않고, 백오피스 통계를 구현할 때 저장한다.
 * 진행 중인 정답 힌트 공개 상태는 메모리의 AnswerHintString이 가진다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevealedHint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id")
    private Round round;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HintType hintType;

    // 무작위 공개가 재요청마다 바뀌지 않도록 공개 시점에 확정해 저장한다.
    @Column(nullable = false, length = 1000)
    private String revealedContent;

    @Column(nullable = false)
    private int revealOrder;

    @Column(nullable = false)
    private LocalDateTime revealedAt;

    static RevealedHint of(Round round, HintType hintType, String revealedContent, int revealOrder,
                           LocalDateTime revealedAt) {
        RevealedHint hint = new RevealedHint();
        hint.round = round;
        hint.hintType = hintType;
        hint.revealedContent = revealedContent;
        hint.revealOrder = revealOrder;
        hint.revealedAt = revealedAt;
        return hint;
    }
}