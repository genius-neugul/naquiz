package geniusneugul.project.core.chat.service;

import geniusneugul.project.core.game.common.implement.SolveHandler;
import geniusneugul.project.core.room.implement.RoomMember;
import geniusneugul.project.core.room.implement.RoomMemberReader;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 방 안 채팅. 메시지는 저장하지 않는다. 라운드 진행 중 채팅은 정답 제출을 겸하므로(docs/DOMAIN.md) 먼저 game의
 * SolveHandler로 판정하고, 가장 먼저 맞힌 채팅이면 일반 채팅 대신 정답자 확정 결과를 돌려준다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RoomMemberReader roomMemberReader;
    private final SolveHandler solveHandler;
    private final Clock clock;

    public SendChatResult send(SendChatCommand command) {
        return solveHandler.handle(command.participantToken(), command.text())
                .<SendChatResult>map(AnswerSolvedResult::from)
                .orElseGet(() -> message(command));
    }

    private ChatResult message(SendChatCommand command) {
        RoomMember sender = roomMemberReader.read(command.participantToken());
        return ChatResult.of(sender, command.text(), LocalDateTime.now(clock));
    }
}
