package geniusneugul.project.core.movie.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String kobisMovieCode;

    @Column(nullable = false)
    private String title;

    private String titleEn;

    // 역대 박스오피스 밖에서 추가한 영화는 관객 수가 없다.
    private Long audienceCount;

    @Column(nullable = false)
    private LocalDate releaseDate;

    @ElementCollection
    @CollectionTable(name = "movie_director", joinColumns = @JoinColumn(name = "movie_id"))
    @OrderColumn(name = "item_order")
    @Column(name = "director", nullable = false)
    private List<String> directors = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "movie_actor", joinColumns = @JoinColumn(name = "movie_id"))
    @OrderColumn(name = "item_order")
    @Column(name = "actor", nullable = false)
    private List<String> actors = new ArrayList<>();

    // 공개할 때 answer·subAnswer를 ○○○로 가린다. 원문은 그대로 저장한다.
    @Lob
    private String synopsis;

    @ElementCollection
    @CollectionTable(name = "movie_genre", joinColumns = @JoinColumn(name = "movie_id"))
    @OrderColumn(name = "item_order")
    @Column(name = "genre", nullable = false)
    private List<String> genres = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "movie_nation", joinColumns = @JoinColumn(name = "movie_id"))
    @OrderColumn(name = "item_order")
    @Column(name = "nation", nullable = false)
    private List<String> nations = new ArrayList<>();

    @Column(nullable = false)
    private String rating;

    private Movie(String kobisMovieCode, String title, String titleEn, Long audienceCount, LocalDate releaseDate,
                  List<String> directors, List<String> actors, String synopsis, List<String> genres,
                  List<String> nations, String rating) {
        this.kobisMovieCode = kobisMovieCode;
        this.title = title;
        this.titleEn = titleEn;
        this.audienceCount = audienceCount;
        this.releaseDate = releaseDate;
        this.directors = new ArrayList<>(directors);
        this.actors = new ArrayList<>(actors);
        this.synopsis = synopsis;
        this.genres = new ArrayList<>(genres);
        this.nations = new ArrayList<>(nations);
        this.rating = rating;
    }

    public static Movie create(String kobisMovieCode, String title, String titleEn, Long audienceCount,
                               LocalDate releaseDate, List<String> directors, List<String> actors, String synopsis,
                               List<String> genres, List<String> nations, String rating) {
        return new Movie(kobisMovieCode, title, titleEn, audienceCount, releaseDate, directors, actors, synopsis,
                genres, nations, rating);
    }
}
