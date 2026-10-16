package geniusneugul.project.core.game.common.implement;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.question.domain.Question;

/**
 * 출제하기 전에 문제를 준비하는 게임별 확장점(예: 노래 게임의 음원 준비). 준비가 필요 없는 게임은 구현하지 않는다.
 * 외부 API를 기다릴 수 있으므로 트랜잭션과 방 락 밖에서 부른다.
 */
public interface QuestionPreparer {

    boolean supports(GameType gameType);

    /**
     * @return 출제할 수 있게 준비했으면 true. false면 이 문제는 출제하지 않는다
     */
    boolean prepare(Question question);
}
