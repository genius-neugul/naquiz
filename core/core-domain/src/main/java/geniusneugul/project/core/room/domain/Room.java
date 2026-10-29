package geniusneugul.project.core.room.domain;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    /**
     * 방장이 게임을 시작한다. 대기 중인 방에서만 시작할 수 있고, 모든 참가자의 점수를 0으로 되돌린다.
     * 혼자 있어도 시작할 수 있다.
     */
    public void startGame(String participantToken) {
        Participant starter = findParticipant(participantToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_JOINED));
        if (!starter.isHost()) {
            throw new BusinessException(ErrorCode.GAME_NOT_HOST);
        }
        if (status == RoomStatus.PLAYING) {
            throw new BusinessException(ErrorCode.GAME_ALREADY_PLAYING);
        }
        status = RoomStatus.PLAYING;
        participants.forEach(Participant::resetScore);
    }

    /** 정답자에게 1점을 주고 누적 점수를 돌려준다 */
    public int addScore(Long participantId) {
        return findParticipant(participantId)
                .orElseThrow(() -> new IllegalStateException(
                        ErrorCode.ROOM_PARTICIPANT_NOT_FOUND.getMessage() + " roomId=" + id + ", participantId=" + participantId))
                .addScore();
    }

    /** 승자가 나와 게임이 끝났다. 방은 대기 상태로 돌아가고 승자의 승리 횟수가 늘어난다 */
    public void endGame(Long winnerId) {
        if (isClosed()) {
            return;
        }
        status = RoomStatus.WAITING;
        findParticipant(winnerId).ifPresent(Participant::winGame);
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

    private Optional<Participant> findParticipant(Long participantId) {
        return participants.stream()
                .filter(participant -> Objects.equals(participant.getId(), participantId))
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
