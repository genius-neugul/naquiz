package geniusneugul.project.core.vote.infra;

import geniusneugul.project.core.vote.domain.Vote;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryOpenVoteRepository implements OpenVoteRepository {

    private final Map<Long, List<Vote>> votes = new ConcurrentHashMap<>();

    @Override
    public void save(Long roomId, Vote vote) {
        votes.computeIfAbsent(roomId, id -> new ArrayList<>()).add(vote);
    }

    @Override
    public List<Vote> findByRoomId(Long roomId) {
        return List.copyOf(votes.getOrDefault(roomId, List.of()));
    }

    @Override
    public void delete(Long roomId, Vote vote) {
        votes.computeIfPresent(roomId, (id, roomVotes) -> {
            roomVotes.remove(vote);
            return roomVotes.isEmpty() ? null : roomVotes;
        });
    }

    @Override
    public List<Vote> deleteByRoomId(Long roomId) {
        List<Vote> removed = votes.remove(roomId);
        return removed == null ? List.of() : List.copyOf(removed);
    }
}
