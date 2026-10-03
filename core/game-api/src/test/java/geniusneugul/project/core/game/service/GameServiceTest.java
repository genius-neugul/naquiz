package geniusneugul.project.core.game.service;

import static geniusneugul.project.core.common.exception.ErrorCode.GAME_QUESTION_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import geniusneugul.project.core.chat.service.AnswerSolvedResult;
import geniusneugul.project.core.chat.service.ChatService;
import geniusneugul.project.core.chat.service.SendChatCommand;
import geniusneugul.project.core.chat.service.SendChatResult;
import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.room.service.CreateRoomCommand;
import geniusneugul.project.core.room.service.JoinRoomCommand;
import geniusneugul.project.core.room.service.RoomResult;
import geniusneugul.project.core.room.service.RoomService;
import geniusneugul.project.core.support.IntegrationTestSupport;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class GameServiceTest extends IntegrationTestSupport {

    private static final String GAME_TYPE = "MOVIE_STILL_CUT";

    @Autowired
    private GameService gameService;

    @Autowired
    private RoomService roomService;

    // 정답 제출은 채팅으로 들어온다.
    @Autowired
    private ChatService chatService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("방장이 게임을 시작하면 승인된 활성 문제로 1라운드가 열린다.")
    @Test
    void start() {
        // given
        Long approvedId = insertQuestion("기생충", "APPROVED", true);
        insertQuestion("괴물", "PENDING", true);
        insertQuestion("마더", "APPROVED", false);
        String hostToken = createRoom().hostToken();

        // when
        GameStartResult result = gameService.start(new StartGameCommand(GAME_TYPE, 3, hostToken));

        // then
        assertThat(result.roundNo()).isEqualTo(1);
        assertThat(gameStatus(result.gameId())).isEqualTo("IN_PROGRESS");
        assertThat(roundQuestionId(result.gameId(), 1)).isEqualTo(approvedId);
    }

    @DisplayName("출제할 문제가 없으면 게임을 시작할 수 없고 방은 대기 상태로 남는다.")
    @Test
    void start_noQuestion() {
        // given
        TestRoom room = createRoom();

        // when & then
        assertThatThrownBy(() -> gameService.start(new StartGameCommand(GAME_TYPE, 3, room.hostToken())))
                .isInstanceOf(BusinessException.class)
                .hasMessage(GAME_QUESTION_NOT_FOUND.getMessage());
        // 게임 중인 방이면 들어올 수 없으므로, 들어올 수 있으면 대기 상태다.
        roomService.join(new JoinRoomCommand(room.inviteCode(), "감자", token()));
    }

    @DisplayName("다음 라운드는 아직 출제하지 않은 문제를 내고, 모두 출제했으면 이미 낸 문제를 다시 낸다.")
    @Test
    void startNextRound() {
        // given
        Long firstId = insertQuestion("기생충", "APPROVED", true);
        Long secondId = insertQuestion("괴물", "APPROVED", true);
        TestRoom room = createRoom();
        GameStartResult start = gameService.start(new StartGameCommand(GAME_TYPE, 10, room.hostToken()));
        solveCurrentRound(room, start.gameId(), 1);

        // when
        RoundStartResult second = gameService.startNextRound(room.roomId(), start.gameId()).orElseThrow();
        solveCurrentRound(room, start.gameId(), 2);
        RoundStartResult third = gameService.startNextRound(room.roomId(), start.gameId()).orElseThrow();

        // then
        assertThat(second.roundNo()).isEqualTo(2);
        assertThat(third.roundNo()).isEqualTo(3);
        assertThat(new Long[]{roundQuestionId(start.gameId(), 1), roundQuestionId(start.gameId(), 2)})
                .containsExactlyInAnyOrder(firstId, secondId);
        assertThat(roundQuestionId(start.gameId(), 3)).isIn(firstId, secondId);
    }

    @DisplayName("정답으로 목표 점수에 도달하면 라운드가 정답으로 끝나고 게임이 그 참가자의 승리로 끝난다.")
    @Test
    void submit_reachTargetScore() {
        // given
        insertQuestion("기생충", "APPROVED", true);
        TestRoom room = createRoom();
        GameStartResult start = gameService.start(new StartGameCommand(GAME_TYPE, 1, room.hostToken()));

        // when
        SendChatResult result = chatService.send(new SendChatCommand("기 생 충", room.hostToken()));

        // then
        assertThat(result).isInstanceOfSatisfying(AnswerSolvedResult.class,
                solved -> assertThat(solved.gameFinished()).isTrue());
        assertThat(roundRow(start.gameId(), 1)).containsEntry("STATUS", "SOLVED")
                .containsEntry("SOLVER_PARTICIPANT_ID", room.hostId());
        assertThat(gameRow(start.gameId())).containsEntry("STATUS", "FINISHED").containsEntry("WINNER_ID", room.hostId());
    }

    @DisplayName("방장이 나가면 진행 중이던 게임은 승자 없이 끝난다.")
    @Test
    void endWithoutWinner_hostLeft() {
        // given
        insertQuestion("기생충", "APPROVED", true);
        TestRoom room = createRoom();
        GameStartResult start = gameService.start(new StartGameCommand(GAME_TYPE, 3, room.hostToken()));

        // when
        roomService.leave(room.hostToken());

        // then
        assertThat(gameRow(start.gameId())).containsEntry("STATUS", "FINISHED").containsEntry("WINNER_ID", null);
    }

    private void solveCurrentRound(TestRoom room, Long gameId, int roundNo) {
        String answer = jdbcTemplate.queryForObject(
                "SELECT q.answer FROM game_round r JOIN question q ON q.id = r.question_id WHERE r.game_id = ? AND r.round_no = ?",
                String.class, gameId, roundNo);
        assertThat(chatService.send(new SendChatCommand(answer, room.hostToken()))).isInstanceOf(AnswerSolvedResult.class);
    }

    private Long insertQuestion(String answer, String reviewStatus, boolean active) {
        jdbcTemplate.update(
                "INSERT INTO question (game_type, content_id, answer, sub_answer, active, review_status) VALUES (?, 1, ?, NULL, ?, ?)",
                GAME_TYPE, answer, active, reviewStatus);
        return jdbcTemplate.queryForObject("SELECT id FROM question WHERE answer = ?", Long.class, answer);
    }

    private String gameStatus(Long gameId) {
        return (String) gameRow(gameId).get("STATUS");
    }

    private Map<String, Object> gameRow(Long gameId) {
        return jdbcTemplate.queryForMap("SELECT status, winner_id FROM game WHERE id = ?", gameId);
    }

    private Map<String, Object> roundRow(Long gameId, int roundNo) {
        return jdbcTemplate.queryForMap(
                "SELECT status, solver_participant_id FROM game_round WHERE game_id = ? AND round_no = ?", gameId, roundNo);
    }

    private Long roundQuestionId(Long gameId, int roundNo) {
        return jdbcTemplate.queryForObject(
                "SELECT question_id FROM game_round WHERE game_id = ? AND round_no = ?", Long.class, gameId, roundNo);
    }

    // 방은 메모리에 남아 테스트 사이에 지워지지 않으므로 연결 토큰을 테스트마다 새로 만든다.
    private TestRoom createRoom() {
        String hostToken = token();
        RoomResult room = roomService.create(new CreateRoomCommand("방장", hostToken));
        return new TestRoom(room.roomId(), room.inviteCode(), hostToken, room.meId());
    }

    private String token() {
        return UUID.randomUUID().toString();
    }

    private record TestRoom(Long roomId, String inviteCode, String hostToken, Long hostId) {
    }
}
