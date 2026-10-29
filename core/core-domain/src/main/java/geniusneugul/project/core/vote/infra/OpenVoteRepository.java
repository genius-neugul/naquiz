package geniusneugul.project.core.vote.infra;

import geniusneugul.project.core.vote.domain.Vote;
import java.util.List;

/**
 * 진행 중인(OPEN) 투표 저장소. 판정 시점의 접속 인원으로 방 락 안에서 판정하려고 메모리에 두고, 방 락 안에서만 쓴다.
 * 끝난 투표는 여기서 꺼내 VoteRepository(DB)에 남긴다.
 */
public interface OpenVoteRepository {

    void save(Long roomId, Vote vote);

    List<Vote> findByRoomId(Long roomId);

    void delete(Long roomId, Vote vote);

    /** 그 방의 열린 투표를 모두 꺼낸다 */
    List<Vote> deleteByRoomId(Long roomId);
}
