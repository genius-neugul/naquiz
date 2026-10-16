package geniusneugul.project.core.game.common.service;

import static geniusneugul.project.core.common.exception.ErrorCode.GAME_NOT_HOST;
import static geniusneugul.project.core.common.exception.ErrorCode.GAME_QUESTION_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.room.service.CreateRoomCommand;
import geniusneugul.project.core.room.service.JoinRoomCommand;
import geniusneugul.project.core.room.service.RoomResult;
import geniusneugul.project.core.room.service.RoomService;
import geniusneugul.project.core.song.domain.ArtistName;
import geniusneugul.project.core.song.domain.Song;
import geniusneugul.project.core.song.domain.SourceProgram;
import geniusneugul.project.core.song.domain.VideoCandidate;
import geniusneugul.project.core.song.infra.SongRepository;
import geniusneugul.project.core.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class RoundQuestionServiceTest extends IntegrationTestSupport {

    // YouTube 검색어는 "{제목} {가수}"다. 이 제목으로 시작하는 노래만 음원을 찾는다.
    private static final String PLAYABLE_TITLE = "음원있음";
    private static final VideoCandidate VIDEO = new VideoCandidate("video-1", "노래 가사", "3:30", null, null, null);

    @Autowired
    private RoundQuestionService roundQuestionService;

    @Autowired
    private GameService gameService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("음원을 준비하지 못한 노래는 건너뛰고 준비한 노래로 게임을 시작한다.")
    @Test
    void prepareFirst_skipsUnplayableSong() {
        // given
        insertSongQuestion("음원없음1");
        insertSongQuestion("음원없음2");
        Long playableQuestionId = insertSongQuestion(PLAYABLE_TITLE);
        givenOnlyPlayableTitleHasVideo();
        StartGameCommand command = new StartGameCommand(GameType.SONG, 3, createRoom().hostToken());

        // when
        GameStartResult result = gameService.start(command, roundQuestionService.prepareFirst(command));

        // then
        assertThat(result.questionId()).isEqualTo(playableQuestionId);
    }

    @DisplayName("노래 3곡의 음원 준비가 연속으로 실패하면 게임을 시작할 수 없고 방은 대기 상태로 남는다.")
    @Test
    void prepareFirst_threeConsecutiveFailures() {
        // given
        insertSongQuestion("음원없음1");
        insertSongQuestion("음원없음2");
        insertSongQuestion("음원없음3");
        givenOnlyPlayableTitleHasVideo();
        TestRoom room = createRoom();
        StartGameCommand command = new StartGameCommand(GameType.SONG, 3, room.hostToken());

        // when & then
        assertThatThrownBy(() -> roundQuestionService.prepareFirst(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage(GAME_QUESTION_UNAVAILABLE.getMessage());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM game", Integer.class)).isZero();
        // 게임 중인 방이면 들어올 수 없으므로, 들어올 수 있으면 대기 상태다.
        roomService.join(new JoinRoomCommand(room.inviteCode(), "감자", token()));
    }

    @DisplayName("영화 게임은 외부 API를 부르지 않고 시작한다.")
    @Test
    void prepareFirst_movieGame() {
        // given
        jdbcTemplate.update("""
                INSERT INTO question (game_type, content_id, answer, sub_answer, active, review_status)
                VALUES ('MOVIE_STILL_CUT', 1, '기생충', NULL, TRUE, 'APPROVED')
                """);
        StartGameCommand command = new StartGameCommand(GameType.MOVIE_STILL_CUT, 3, createRoom().hostToken());

        // when
        gameService.start(command, roundQuestionService.prepareFirst(command));

        // then
        verifyNoInteractions(youTubeSearchClient, spotifyApiClient);
    }

    @DisplayName("방장이 아닌 참가자가 시작하면 음원을 준비하기 전에 실패한다.")
    @Test
    void prepareFirst_notHost() {
        // given
        insertSongQuestion(PLAYABLE_TITLE);
        TestRoom room = createRoom();
        String guestToken = token();
        roomService.join(new JoinRoomCommand(room.inviteCode(), "감자", guestToken));

        // when & then
        assertThatThrownBy(() -> roundQuestionService.prepareFirst(new StartGameCommand(GameType.SONG, 3, guestToken)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(GAME_NOT_HOST.getMessage());
        verifyNoInteractions(youTubeSearchClient, spotifyApiClient);
    }

    private void givenOnlyPlayableTitleHasVideo() {
        given(youTubeSearchClient.search(anyString())).willAnswer(invocation ->
                invocation.getArgument(0, String.class).startsWith(PLAYABLE_TITLE) ? List.of(VIDEO) : List.of());
    }

    private Long insertSongQuestion(String title) {
        Song song = songRepository.save(Song.create(title, null, List.of(new ArtistName("가수", "Singer")), title,
                "가수", 1, SourceProgram.MUSIC_PARTY, 1, LocalDate.of(2026, 10, 1)));
        jdbcTemplate.update("""
                INSERT INTO question (game_type, content_id, answer, sub_answer, active, review_status)
                VALUES ('SONG', ?, ?, NULL, TRUE, 'APPROVED')
                """, song.getId(), title);
        return jdbcTemplate.queryForObject("SELECT id FROM question WHERE content_id = ? AND game_type = 'SONG'",
                Long.class, song.getId());
    }

    // 방은 메모리에 남아 테스트 사이에 지워지지 않으므로 연결 토큰을 테스트마다 새로 만든다.
    private TestRoom createRoom() {
        String hostToken = token();
        RoomResult room = roomService.create(new CreateRoomCommand("방장", hostToken));
        return new TestRoom(room.inviteCode(), hostToken);
    }

    private String token() {
        return UUID.randomUUID().toString();
    }

    private record TestRoom(String inviteCode, String hostToken) {
    }
}
