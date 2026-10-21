package geniusneugul.project.core.movie.implement;

import geniusneugul.project.core.movie.domain.StillCut;
import geniusneugul.project.core.movie.infra.StillCutRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StillCutReader {

    private final StillCutRepository stillCutRepository;

    /** 영화의 스틸컷 이미지 URL. 스틸컷 게임에서 보여줄 순서(노출 순서)대로 */
    public List<String> readImageUrls(Long movieId) {
        return stillCutRepository.findByMovieIdOrderByDisplayOrderAsc(movieId).stream()
                .map(StillCut::getImageUrl)
                .toList();
    }
}
