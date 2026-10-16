package geniusneugul.project.core.song.domain;

import java.time.LocalDate;

/**
 * Spotify 트랙 검색 결과 하나(MUSIC_SELECTION_RULE 1-3).
 *
 * @param albumArtistId 앨범의 첫 번째 아티스트 id. 1-2에서 정한 아티스트와 같은 트랙을 고를 때 쓴다
 * @param releaseDate   발매일. Spotify가 연도나 연·월까지만 주면 빠진 월·일을 1로 채운다. 없으면 null
 */
public record SpotifyTrack(String id, String name, String albumName, String albumImageUrl, String albumArtistId,
                           LocalDate releaseDate) {
}
