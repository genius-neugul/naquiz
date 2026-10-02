package geniusneugul.project.core.movie.domain;

import java.util.List;

/**
 * 아직 저장하지 않은 영화와 스틸컷 이미지 URL. 스틸컷은 영화 ID가 생긴 뒤에 만들 수 있어 URL로 들고 다닌다.
 * URL 순서가 스틸컷 노출 순서다.
 */
public record MovieWithStillCuts(Movie movie, List<String> stillCutImageUrls) {
}
