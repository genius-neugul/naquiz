package geniusneugul.project.core.room.domain;

import geniusneugul.project.core.common.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 방 애그리거트의 내부 엔티티. 비회원이고 재접속하지 않으므로 메모리에만 둔다.
 * participantToken은 실시간 연결마다 발급되는 식별자다. 연결이 끊기면 퇴장한 것으로 본다.
 * 닉네임은 방 안에서 겹칠 수 있고, 방 안에서 유일한 태그(입장 순서 번호)로 구분해 "닉네임#태그"로 보여준다.
 */
@Getter
public class Participant {

    private Long id;
    private String nickname;
    private int tag;
    private ParticipantRole role;
    private String participantToken;
    private int gameWins;
    private int roundScore;
    private LocalDateTime joinedAt;

    private Participant(String nickname, int tag, ParticipantRole role, String participantToken, LocalDateTime joinedAt) {
        this.nickname = nickname;
        this.tag = tag;
        this.role = role;
        this.participantToken = participantToken;
        this.joinedAt = joinedAt;
    }

    public static Participant host(String nickname, int tag, String participantToken, LocalDateTime joinedAt) {
        return new Participant(nickname, tag, ParticipantRole.HOST, participantToken, joinedAt);
    }

    public static Participant guest(String nickname, int tag, String participantToken, LocalDateTime joinedAt) {
        return new Participant(nickname, tag, ParticipantRole.GUEST, participantToken, joinedAt);
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException(ErrorCode.ROOM_PARTICIPANT_ID_ALREADY_ASSIGNED.getMessage() + " participantId=" + this.id);
        }
        this.id = id;
    }

    public boolean isHost() {
        return role == ParticipantRole.HOST;
    }

    public boolean hasToken(String participantToken) {
        return this.participantToken.equals(participantToken);
    }
}
