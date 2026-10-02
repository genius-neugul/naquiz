package geniusneugul.project.core.room.presentation.dto;

import geniusneugul.project.core.room.service.ParticipantResult;
import java.util.List;

/**
 * 참가자. 화면에는 "nickname#tag"로 보여준다(tag는 방 안에서 유일한 입장 순서 번호).
 */
public record ParticipantResponse(Long participantId, String nickname, int tag, String role) {

    static List<ParticipantResponse> listOf(List<ParticipantResult> results) {
        return results.stream()
                .map(result -> new ParticipantResponse(result.participantId(), result.nickname(), result.tag(), result.role()))
                .toList();
    }
}
