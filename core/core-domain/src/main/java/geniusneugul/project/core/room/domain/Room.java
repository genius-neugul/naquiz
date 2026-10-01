package geniusneugul.project.core.room.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * 방 애그리거트 루트. 통계에 쓰이지 않아 DB에 저장하지 않고 메모리에만 둔다.
 */
@Getter
public class Room {

    private Long id;
    private String inviteCode;
    private Long hostId;
    private RoomStatus status;
    private int maxParticipants;
    private LocalDateTime createdAt;
    private final List<Participant> participants = new ArrayList<>();
}
