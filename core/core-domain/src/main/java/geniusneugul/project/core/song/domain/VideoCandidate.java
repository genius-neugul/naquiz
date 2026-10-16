package geniusneugul.project.core.song.domain;

import java.util.OptionalInt;

/**
 * YouTube 검색 결과에서 꺼낸 음원 후보 영상 하나(MUSIC_SELECTION_RULE 2-2). 값은 응답 원문 그대로다.
 *
 * @param lengthText        재생 시간. {@code m:ss} 또는 {@code h:mm:ss}
 * @param channelName       채널명. 응답에 없으면 null
 * @param viewCountText     조회수 원문(예: {@code 254,341,635 views}). 응답에 없으면 null
 * @param publishedTimeText 업로드 시기 원문(예: {@code 4 years ago}). 응답에 없으면 null
 */
public record VideoCandidate(String videoId, String title, String lengthText, String channelName,
                             String viewCountText, String publishedTimeText) {

    public OptionalInt durationSeconds() {
        return VideoDuration.parseSeconds(lengthText);
    }

    /**
     * @return 조회수 원문에서 숫자만 남긴 값. 숫자가 없으면 null
     */
    public Long viewCount() {
        if (viewCountText == null) {
            return null;
        }
        String digits = viewCountText.replaceAll("\\D", "");
        return digits.isEmpty() ? null : Long.parseLong(digits);
    }
}
