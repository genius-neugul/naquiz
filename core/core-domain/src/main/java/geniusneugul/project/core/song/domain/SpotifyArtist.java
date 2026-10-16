package geniusneugul.project.core.song.domain;

/**
 * Spotify 아티스트 검색 결과 하나. 트랙을 고를 때 아티스트 id를 맞춰 본다(MUSIC_SELECTION_RULE 1-2).
 */
public record SpotifyArtist(String id, String name) {
}
