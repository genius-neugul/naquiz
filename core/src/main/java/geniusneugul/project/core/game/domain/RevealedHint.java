package geniusneugul.project.core.game.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevealedHint {

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
    private HintType hintType;

    // 무작위 공개가 재요청마다 바뀌지 않도록 공개 시점에 확정해 저장한다.
    @Column(nullable = false, length = 1000)
    private String revealedContent;

    @Column(nullable = false)
    private int revealOrder;

    @Column(nullable = false)
    private LocalDateTime revealedAt;
}
