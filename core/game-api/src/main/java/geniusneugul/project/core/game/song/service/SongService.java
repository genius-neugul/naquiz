package geniusneugul.project.core.game.song.service;

import geniusneugul.project.core.question.domain.Question;
import geniusneugul.project.core.question.implement.QuestionReader;
import geniusneugul.project.core.song.implement.SongReader;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 노래 맞추기. 라운드 노래의 재생·공개 정보를 읽는다. 음원은 출제할 때 이미 준비했으므로 외부 API를 부르지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {

    private final QuestionReader questionReader;
    private final SongReader songReader;

    /** 음원이 없는 노래면 비어 있다. 출제할 때 준비하므로 정상 흐름에서는 생기지 않는다 */
    @Transactional(readOnly = true)
    public Optional<SongPlaybackResult> playback(Long questionId) {
        Question question = questionReader.read(questionId);
        Optional<SongPlaybackResult> playback = songReader.readPlayback(question.getContentId())
                .map(SongPlaybackResult::from);
        if (playback.isEmpty()) {
            log.warn("[SongService.playback] Song has no audio. questionId={}, songId={}", questionId,
                    question.getContentId());
        }
        return playback;
    }
}
