package geniusneugul.project.core.game.domain;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.question.domain.Answer;
import lombok.Getter;

/**
 * 진행 중인 게임에서 정답 판정에 필요한 값(현재 라운드의 정답, 목표 점수). 선착순 정답자를 방 락 안에서 확정하려고
 * DB가 아닌 메모리에 둔다. 스스로 동기화하지 않으므로 방 락(RoomLock) 안에서만 읽고 바꾼다.
 * 게임·라운드 기록은 DB의 Game이 가지고, 점수는 방의 참가자(roundScore)가 가진다.
 */
@Getter
public class GameProgress {

    private final Long roomId;
    private final Long gameId;
    private final GameType gameType;
    private final int targetScore;
    private int roundNo;
    private Long questionId;
    private Answer answer;
    private boolean roundSolved;

    public GameProgress(Long roomId, Long gameId, GameType gameType, int targetScore, int roundNo, Long questionId,
            Answer answer) {
        this.roomId = roomId;
        this.gameId = gameId;
        this.gameType = gameType;
        this.targetScore = targetScore;
        startRound(roundNo, questionId, answer);
    }

    public void startRound(int roundNo, Long questionId, Answer answer) {
        this.roundNo = roundNo;
        this.questionId = questionId;
        this.answer = answer;
        this.roundSolved = false;
    }

    /**
     * 정답 제출을 판정한다. 라운드가 진행 중이고 정답이면 라운드를 풀고 true를 돌려준다.
     * 이미 풀린 라운드에 들어온 제출은 정답이어도 false다(먼저 맞힌 한 명만 정답자).
     */
    public boolean submit(String text) {
        if (roundSolved || !answer.isCorrect(text)) {
            return false;
        }
        roundSolved = true;
        return true;
    }

    public boolean reachedTarget(int score) {
        return score >= targetScore;
    }
}
