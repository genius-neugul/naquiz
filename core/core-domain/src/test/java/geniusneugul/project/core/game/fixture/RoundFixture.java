package geniusneugul.project.core.game.fixture;

import geniusneugul.project.core.game.domain.Round;
import org.springframework.beans.BeanUtils;

/**
 * 라운드 시작 로직이 아직 없어 Round를 만들 생성자가 없다. 기본 생성자로만 만든다.
 */
public class RoundFixture {

    public static Round round() {
        return BeanUtils.instantiateClass(Round.class);
    }
}
