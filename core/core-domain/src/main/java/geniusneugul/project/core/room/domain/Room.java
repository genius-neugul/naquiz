package geniusneugul.project.core.room.domain;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

/**
 * 방 애그리거트 루트. 통계에 쓰이지 않아 DB에 저장하지 않고 메모리에만 둔다.
 * 스스로 동기화하지 않는다. 저장소에 들어간 방은 방 락(RoomLock) 안에서만 읽고 바꾼다(docs/ARCHITECTURE.md 「동시성」).
 */
@Getter
public class Room {

    private static final int MAX_PARTICIPANTS = 10;
    private static final int FIRST_TAG = 1;

    private Long id;
    private InviteCode inviteCode;
    private RoomStatus status;
    private int maxParticipants;
    private LocalDateTime createdAt;
    /** 다음 입장자에게 줄 태그. 입장 순서대로 늘고, 나간 참가자의 태그는 다시 쓰지 않는다 */
    private int nextTag;
    private final List<Participant> participants = new ArrayList<>();

    private Room(InviteCode inviteCode, String hostNickname, String hostToken, LocalDateTime createdAt) {
        this.inviteCode = inviteCode;
        this.status = RoomStatus.WAITING;
        this.maxParticipants = MAX_PARTICIPANTS;
        this.createdAt = createdAt;
        this.nextTag = FIRST_TAG;
        this.participants.add(Participant.host(hostNickname, issueTag(), hostToken, createdAt));
    }

    public static Room create(InviteCode inviteCode, String hostNickname, String hostToken, LocalDateTime now) {
        return new Room(inviteCode, hostNickname, hostToken, now);
    }

    /**
     * 초대 코드로 들어온 참가자를 게스트로 추가한다. 닉네임은 겹쳐도 되고, 입장 순서대로 태그를 붙인다.
     * 닫힌 방(방장이 나가 지워지는 중)은 없는 방으로 본다. 게임 중에는 들어올 수 없다.
     */
    public Participant join(String nickname, String participantToken, LocalDateTime now) {
        if (isClosed()) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }
        if (status == RoomStatus.PLAYING) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_PLAYING);
        }
        if (participants.size() >= maxParticipants) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }
        Participant guest = Participant.guest(nickname, issueTag(), participantToken, now);
        participants.add(guest);
        return guest;
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException(ErrorCode.ROOM_ID_ALREADY_ASSIGNED.getMessage() + " roomId=" + this.id);
        }
        this.id = id;
    }

    /**
     * 참가자를 방에서 내보낸다. 방장은 위임하지 않으므로 방장이 나가면 방을 닫는다.
     * 이미 나간 참가자거나 닫힌 방이면 아무것도 하지 않는다(나가기 요청 뒤 연결 끊김이 또 들어오는 경우).
     */
    public Optional<Participant> leave(String participantToken) {
        Optional<Participant> leaving = findParticipant(participantToken);
        leaving.ifPresent(participant -> {
            if (participant.isHost()) {
                status = RoomStatus.CLOSED;
                return;
            }
            participants.remove(participant);
        });
        return leaving;
    }

    public boolean hasParticipant(String participantToken) {
        return findParticipant(participantToken).isPresent();
    }

    /** 닫힌 방이거나 이미 나간 참가자면 비어 있다 */
    public Optional<Participant> findParticipant(String participantToken) {
        if (isClosed()) {
            return Optional.empty();
        }
        return participants.stream()
                .filter(participant -> participant.hasToken(participantToken))
                .findFirst();
    }

    public List<Participant> getParticipants() {
        return List.copyOf(participants);
    }

    public Participant getHost() {
        return participants.stream()
                .filter(Participant::isHost)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(ErrorCode.ROOM_HOST_NOT_FOUND.getMessage() + " roomId=" + id));
    }

    public Long getHostId() {
        return getHost().getId();
    }

    public boolean isClosed() {
        return status == RoomStatus.CLOSED;
    }

    private int issueTag() {
        return nextTag++;
    }
}
