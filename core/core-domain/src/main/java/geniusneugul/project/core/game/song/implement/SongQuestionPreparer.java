package geniusneugul.project.core.game.song.implement;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.common.implement.QuestionPreparer;
import geniusneugul.project.core.question.domain.Question;
import geniusneugul.project.core.song.implement.SongEnricher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 노래 문제를 출제하기 전에 음원과 Spotify 정보를 준비한다. 음원을 준비하지 못한 노래는 출제하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class SongQuestionPreparer implements QuestionPreparer {

    private final SongEnricher songEnricher;

    @Override
    public boolean supports(GameType gameType) {
        return gameType == GameType.SONG;
    }

    @Override
    public boolean prepare(Question question) {
        return songEnricher.prepare(question.getContentId()).isPresent();
    }
}
