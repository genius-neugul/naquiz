package geniusneugul.project.core.report.domain;

/**
 * 오류 유형. SPOTIFY_NOT_FOUND, AUDIO_NOT_FOUND는 데일리 크롤링만 쓴다.
 */
public enum ReportType {
    WRONG_ANSWER,
    WRONG_HINT,
    WRONG_AUDIO,
    NOT_ORIGINAL_AUDIO,
    SPOTIFY_NOT_FOUND,
    AUDIO_NOT_FOUND,
    OTHER
}
