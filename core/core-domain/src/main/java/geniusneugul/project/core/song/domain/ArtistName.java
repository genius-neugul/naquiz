package geniusneugul.project.core.song.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가수 이름 값 객체. 병기가 없으면 artistSub는 한글 이름과 같다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistName {

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false)
    private String artistSub;

    public ArtistName(String artist, String artistSub) {
        this.artist = artist;
        this.artistSub = artistSub;
    }
}
