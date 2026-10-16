package geniusneugul.project.core.song.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 음원 후보 중 음원(AR)에 가장 가까운 영상 하나를 고른다(MUSIC_SELECTION_RULE 2-3).
 * 가사 영상은 대개 원곡 음원을 쓰고, 재생 시간이 짧을수록 뮤직비디오 인트로·아웃트로나 긴 편집본이 아닐 가능성이 높다.
 */
public final class AudioSelector {

    private static final Pattern LYRICS = Pattern.compile("가사|lyrics", Pattern.CASE_INSENSITIVE);
    // 단어 단위로 비교한다. 부분 문자열로 보면 Deliver, Oliver도 라이브가 된다.
    private static final Pattern LIVE = Pattern.compile("\\blive\\b", Pattern.CASE_INSENSITIVE);

    private AudioSelector() {
    }

    /**
     * @return 재생 시간을 읽을 수 있는 후보가 없으면 빈 값
     */
    public static Optional<VideoCandidate> select(List<VideoCandidate> candidates) {
        List<VideoCandidate> playable = candidates.stream()
                .filter(candidate -> candidate.durationSeconds().isPresent())
                .toList();

        Optional<VideoCandidate> lyrics = playable.stream()
                .filter(candidate -> matches(LYRICS, candidate.title()))
                .findFirst();
        if (lyrics.isPresent()) {
            return lyrics;
        }

        List<VideoCandidate> notLive = playable.stream()
                .filter(candidate -> !matches(LIVE, candidate.title()))
                .toList();
        List<VideoCandidate> pool = notLive.isEmpty() ? playable : notLive;
        return pool.stream()
                .min(Comparator.comparingInt(candidate -> candidate.durationSeconds().getAsInt()));
    }

    private static boolean matches(Pattern pattern, String title) {
        return title != null && pattern.matcher(title).find();
    }
}
