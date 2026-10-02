package geniusneugul.project.core.song.domain;

/**
 * 곡을 수집한 방송 행을 가리키는 키. 같은 초기 데이터를 다시 적재할 때 이미 넣은 곡을 건너뛰는 데 쓴다.
 * 같은 곡인지(제목·가수 키) 판정하는 키와는 다르다.
 */
public record SongSourceKey(SourceProgram sourceProgram, int sourceSeq, String rawTitle, String rawArtist) {
}
