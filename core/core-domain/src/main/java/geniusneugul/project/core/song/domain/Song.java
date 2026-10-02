package geniusneugul.project.core.song.domain;

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
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String subtitle;

    @ElementCollection
    @CollectionTable(name = "song_artist", joinColumns = @JoinColumn(name = "song_id"))
    @OrderColumn(name = "artist_order")
    private List<ArtistName> artists = new ArrayList<>();

    @Column(nullable = false)
    private String rawTitle;

    @Column(nullable = false)
    private String rawArtist;

    @Column(nullable = false)
    private int playCount;

    // 정답 노출 위험이 있어 힌트로 쓰지 않는다.
    private String albumName;

    private String albumImageUrl;

    private LocalDate releaseDate;

    private String spotifyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceProgram sourceProgram;

    @Column(nullable = false)
    private int sourceSeq;

    @Column(nullable = false)
    private LocalDate sourceDate;

    // 초기 데이터 곡은 음원 없이 적재하고 출제할 때 채운다.
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "audio_id")
    private Audio audio;

    private Song(String title, String subtitle, List<ArtistName> artists, String rawTitle, String rawArtist,
                 int playCount, SourceProgram sourceProgram, int sourceSeq, LocalDate sourceDate) {
        this.title = title;
        this.subtitle = subtitle;
        this.artists = new ArrayList<>(artists);
        this.rawTitle = rawTitle;
        this.rawArtist = rawArtist;
        this.playCount = playCount;
        this.sourceProgram = sourceProgram;
        this.sourceSeq = sourceSeq;
        this.sourceDate = sourceDate;
    }

    /**
     * 선곡표에서 정제한 곡을 만든다. Spotify 정보와 음원은 비워 두고 나중에 채운다.
     */
    public static Song create(String title, String subtitle, List<ArtistName> artists, String rawTitle,
                              String rawArtist, int playCount, SourceProgram sourceProgram, int sourceSeq,
                              LocalDate sourceDate) {
        return new Song(title, subtitle, artists, rawTitle, rawArtist, playCount, sourceProgram, sourceSeq,
                sourceDate);
    }

    public SongSourceKey sourceKey() {
        return new SongSourceKey(sourceProgram, sourceSeq, rawTitle, rawArtist);
    }
}
