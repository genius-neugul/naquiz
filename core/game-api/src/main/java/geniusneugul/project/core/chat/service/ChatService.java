package geniusneugul.project.core.chat.service;

import geniusneugul.project.core.game.implement.AnswerSubmitter;
import geniusneugul.project.core.game.implement.GameRecorder;
import geniusneugul.project.core.game.implement.RoundSolve;
import geniusneugul.project.core.room.implement.RoomMember;
import geniusneugul.project.core.room.implement.RoomMemberReader;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 방 안 채팅. 메시지는 저장하지 않는다. 라운드 진행 중 채팅은 정답 제출이므로(docs/DOMAIN.md) 먼저 판정하고,
 * 가장 먼저 맞힌 채팅이면 일반 채팅 대신 정답자 확정 결과를 돌려준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RoomMemberReader roomMemberReader;
    private final AnswerSubmitter answerSubmitter;
    private final GameRecorder gameRecorder;
    private final Clock clock;

    public SendChatResult send(SendChatCommand command) {
        return answerSubmitter.submit(command.participantToken(), command.text())
                .<SendChatResult>map(this::solve)
                .orElseGet(() -> message(command));
    }

    private AnswerSolvedResult solve(RoundSolve solve) {
        log.info("[ChatService.send] Round solved. roomId={}, gameId={}, roundNo={}, participantId={}",
                solve.roomId(), solve.gameId(), solve.roundNo(), solve.solverId());
        if (solve.gameFinished()) {
            log.info("[ChatService.send] Game finished. roomId={}, gameId={}, winnerId={}",
                    solve.roomId(), solve.gameId(), solve.solverId());
        }
        try {
            gameRecorder.recordSolve(solve);
        } catch (RuntimeException e) {
            // 정답자·점수는 메모리에서 이미 확정됐다. 기록 실패로 정답 공개와 다음 라운드까지 막으면 게임이 멈춘다.
            log.error("[ChatService.send] Failed to record solved round. roomId={}, gameId={}, roundNo={}",
                    solve.roomId(), solve.gameId(), solve.roundNo(), e);
        }
        return AnswerSolvedResult.from(solve);
    }

    private ChatResult message(SendChatCommand command) {
        RoomMember sender = roomMemberReader.read(command.participantToken());
        return ChatResult.of(sender, command.text(), LocalDateTime.now(clock));
    }
}
