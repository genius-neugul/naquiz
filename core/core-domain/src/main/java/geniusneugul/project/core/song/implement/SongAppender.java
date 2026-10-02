package geniusneugul.project.core.song.implement;

import geniusneugul.project.core.song.domain.Song;
import geniusneugul.project.core.song.domain.SongSourceKey;
import geniusneugul.project.core.song.infra.SongRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SongAppender {

    // 묶음마다 따로 커밋해서, 중간에 실패해도 다시 실행하면 저장된 곡은 건너뛰고 이어서 넣는다.
    private static final int SAVE_CHUNK_SIZE = 1_000;

    private final SongRepository songRepository;

    /**
     * 같은 방송 행에서 온 곡이 이미 있으면 건너뛰고 나머지만 저장한다.
     *
     * @return 새로 저장한 곡 수
     */
    public int appendNew(List<Song> songs) {
        Set<SongSourceKey> savedKeys = new HashSet<>(songRepository.findAllSourceKeys());
        List<Song> newSongs = songs.stream()
                .filter(song -> savedKeys.add(song.sourceKey()))
                .toList();
        for (int from = 0; from < newSongs.size(); from += SAVE_CHUNK_SIZE) {
            songRepository.saveAll(newSongs.subList(from, Math.min(from + SAVE_CHUNK_SIZE, newSongs.size())));
        }
        return newSongs.size();
    }
}
