package geniusneugul.project.core.room.fixture;

import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import java.time.LocalDateTime;

public class RoomFixture {

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 10, 1, 12, 0);

    public static Participant addGuest(Room room, String nickname, String participantToken) {
        return room.join(nickname, participantToken, JOINED_AT);
    }

    /** 저장소 없이 만든 참가자에 ID를 붙인다. 점수·승자처럼 참가자 ID로 찾는 규칙을 볼 때 쓴다 */
    public static void id(Participant participant, Long id) {
        participant.assignId(id);
    }
}
