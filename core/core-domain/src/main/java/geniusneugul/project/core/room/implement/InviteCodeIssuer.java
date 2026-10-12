package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.room.domain.InviteCode;
import geniusneugul.project.core.room.infra.RoomRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 활성 방 사이에서 겹치지 않는 초대 코드를 발급한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InviteCodeIssuer {

    static final int MAX_ATTEMPTS = 10;

    private final InviteCodeGenerator inviteCodeGenerator;
    private final RoomRepository roomRepository;

    public InviteCode issue() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            InviteCode inviteCode = inviteCodeGenerator.generate();
            if (roomRepository.findByInviteCode(inviteCode).isEmpty()) {
                return inviteCode;
            }
            log.debug("[InviteCodeIssuer.issue] Invite code collided. attempt={}", attempt);
        }
        throw new BusinessException(ErrorCode.ROOM_INVITE_CODE_EXHAUSTED, Map.of("attempts", MAX_ATTEMPTS));
    }
}
