package geniusneugul.project.core.game.common.service;

import geniusneugul.project.core.game.common.implement.GameReader;
import geniusneugul.project.core.game.common.implement.GameReader.AskedQuestions;
import geniusneugul.project.core.game.common.implement.PreparedQuestionSelector;
import geniusneugul.project.core.room.implement.RoomGameUpdater;
import geniusneugul.project.core.room.implement.RoomMember;
import geniusneugul.project.core.room.implement.RoomMemberReader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 라운드에 낼 문제를 고르고 준비한다(노래 게임은 음원 준비). 외부 API를 기다릴 수 있으므로 트랜잭션 없이 실행하고,
 * 라운드는 준비된 문제로 GameService가 연다. 준비하지 못하면 GAME_QUESTION_UNAVAILABLE이다.
 */
@Service
@RequiredArgsConstructor
public class RoundQuestionService {

    private final RoomMemberReader roomMemberReader;
    private final RoomGameUpdater roomGameUpdater;
    private final GameReader gameReader;
    private final PreparedQuestionSelector preparedQuestionSelector;

    /** 첫 라운드 문제. 방장이 아니거나 이미 게임 중이면 문제를 준비하기 전에 실패한다 */
    public PreparedQuestion prepareFirst(StartGameCommand command) {
        RoomMember starter = roomMemberReader.read(command.participantToken());
        roomGameUpdater.validateStartGame(starter.roomId(), command.participantToken());
        return new PreparedQuestion(preparedQuestionSelector.select(command.gameType(), List.of()).getId());
    }

    /** 다음 라운드 문제. 이 게임에서 아직 내지 않은 문제를 먼저 고른다 */
    public PreparedQuestion prepareNext(Long gameId) {
        AskedQuestions asked = gameReader.readAskedQuestions(gameId);
        return new PreparedQuestion(preparedQuestionSelector.select(asked.gameType(), asked.questionIds()).getId());
    }
}
