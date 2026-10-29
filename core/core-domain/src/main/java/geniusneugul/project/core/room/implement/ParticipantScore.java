package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.room.domain.Room;
import java.util.List;

/**
 * 참가자 한 명의 현재 게임 누적 점수. 방 락 안에서 만든 스냅샷이다.
 */
public record ParticipantScore(Long participantId, int score) {

    static List<ParticipantScore> listOf(Room room) {
        return room.getParticipants().stream()
                .map(participant -> new ParticipantScore(participant.getId(), participant.getRoundScore()))
                .toList();
    }
}
