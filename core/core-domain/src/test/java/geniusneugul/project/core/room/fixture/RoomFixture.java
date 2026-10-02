package geniusneugul.project.core.room.fixture;

import geniusneugul.project.core.room.domain.Participant;
import geniusneugul.project.core.room.domain.Room;
import geniusneugul.project.core.room.domain.RoomStatus;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class RoomFixture {

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 10, 1, 12, 0);

    public static Participant addGuest(Room room, String nickname, String participantToken) {
        return room.join(nickname, participantToken, JOINED_AT);
    }

    /**
     * 방 상태를 바꾼다. 게임 시작 기능이 아직 없어 PLAYING을 만들 방법이 없으므로 필드에 직접 넣는다.
     * 게임 시작(Room.startGame 등)이 생기면 그 메서드로 바꾼다.
     */
    public static void status(Room room, RoomStatus status) {
        try {
            Field field = Room.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(room, status);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
