package geniusneugul.project.core.room.service;

import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import java.util.List;

public record ParticipantResult(Long participantId, String nickname, int tag, String role) {

    static ParticipantResult from(Participant participant) {
        return new ParticipantResult(participant.getId(), participant.getNickname(), participant.getTag(),
                participant.getRole().name());
    }

    static List<ParticipantResult> listOf(Room room) {
        return listOf(room.getParticipants());
    }

    static List<ParticipantResult> listOf(List<Participant> participants) {
        return participants.stream().map(ParticipantResult::from).toList();
    }
}
