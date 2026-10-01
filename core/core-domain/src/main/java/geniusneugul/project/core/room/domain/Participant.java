package geniusneugul.project.core.room.domain;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 방 애그리거트의 내부 엔티티. 비회원이고 재접속하지 않으므로 메모리에만 둔다.
 */
@Getter
public class Participant {

    private Long id;
    private String nickname;
    private ParticipantRole role;
    private String participantToken;
    private int gameWins;
    private int roundScore;
    private LocalDateTime joinedAt;
}
