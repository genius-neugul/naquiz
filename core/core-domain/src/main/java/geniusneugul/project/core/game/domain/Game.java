package geniusneugul.project.core.game.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

    public static final int MIN_TARGET_SCORE = 1;
    public static final int MAX_TARGET_SCORE = 50;

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

    private Game(Long roomId, GameType gameType, int targetScore, LocalDateTime startedAt) {
        this.roomId = roomId;
        this.gameType = gameType;
        this.status = GameStatus.IN_PROGRESS;
        this.targetScore = targetScore;
        this.startedAt = startedAt;
    }

    /** 방장이 정한 목표 점수(1~50점)로 게임을 시작한다. 라운드는 startRound로 연다 */
    public static Game start(Long roomId, GameType gameType, int targetScore, LocalDateTime now) {
        if (targetScore < MIN_TARGET_SCORE || targetScore > MAX_TARGET_SCORE) {
            throw new BusinessException(ErrorCode.GAME_INVALID_TARGET_SCORE, Map.of("targetScore", targetScore));
        }
        return new Game(roomId, gameType, targetScore, now);
    }

    /** 다음 번호의 라운드를 연다 */
    public Round startRound(Long questionId, LocalDateTime now) {
        currentRoundNo++;
        Round round = new Round(this, currentRoundNo, questionId, now);
        rounds.add(round);
        return round;
    }

    public void solveRound(int roundNo, Long solverParticipantId, LocalDateTime now) {
        findRound(roundNo).solve(solverParticipantId, now);
    }

    /** 승자가 목표 점수에 도달해 끝났다 */
    public void finish(Long winnerId, LocalDateTime now) {
        this.status = GameStatus.FINISHED;
        this.winnerId = winnerId;
        this.endedAt = now;
    }

    /** 방장이 나가 승자 없이 끝났다 */
    public void finishWithoutWinner(LocalDateTime now) {
        this.status = GameStatus.FINISHED;
        this.endedAt = now;
    }

    public boolean isInProgress() {
        return status == GameStatus.IN_PROGRESS;
    }

    /** 이 게임에서 출제한 문제 ID */
    public List<Long> getAskedQuestionIds() {
        return rounds.stream().map(Round::getQuestionId).toList();
    }

    private Round findRound(int roundNo) {
        return rounds.stream()
                .filter(round -> round.getRoundNo() == roundNo)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        ErrorCode.GAME_ROUND_NOT_FOUND.getMessage() + " gameId=" + id + ", roundNo=" + roundNo));
    }
}
