package geniusneugul.project.core.game.fixture;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.domain.Game;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 게임 시작 로직이 아직 없어 Game을 만들 생성자가 없다. 테스트에 필요한 값만 리플렉션으로 채운다.
 */
public class GameFixture {

    public static Game game(GameType gameType) {
        Game game = BeanUtils.instantiateClass(Game.class);
        ReflectionTestUtils.setField(game, "gameType", gameType);
        return game;
    }
}
