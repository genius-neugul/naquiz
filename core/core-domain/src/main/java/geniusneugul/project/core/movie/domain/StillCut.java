package geniusneugul.project.core.movie.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스틸컷 애그리거트 루트. 영화와는 ID로만 연결한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StillCut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long movieId;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder;

    private StillCut(Long movieId, String imageUrl, int displayOrder) {
        this.movieId = movieId;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    public static StillCut create(Long movieId, String imageUrl, int displayOrder) {
        return new StillCut(movieId, imageUrl, displayOrder);
    }
}
