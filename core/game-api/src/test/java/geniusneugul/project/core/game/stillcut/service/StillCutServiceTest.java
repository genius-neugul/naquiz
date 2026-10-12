package geniusneugul.project.core.game.stillcut.service;

import static org.assertj.core.api.Assertions.assertThat;

import geniusneugul.project.core.common.domain.GameType;
import geniusneugul.project.core.game.common.service.GameService;
import geniusneugul.project.core.game.common.service.GameStartResult;
import geniusneugul.project.core.game.common.service.StartGameCommand;
import geniusneugul.project.core.room.service.CreateRoomCommand;
import geniusneugul.project.core.room.service.RoomService;
import geniusneugul.project.core.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class StillCutServiceTest extends IntegrationTestSupport {

    private static final long MOVIE_ID = 7L;

    @Autowired
    private StillCutService stillCutService;

    @Autowired
    private GameService gameService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("라운드 문제의 영화 스틸컷을 노출 순서대로 공개하고, 마지막 컷 뒤에는 더 공개하지 않는다.")
    @Test
    void startReel() {
        // given
        jdbcTemplate.update("""
                INSERT INTO question (game_type, content_id, answer, sub_answer, active, review_status)
                VALUES ('MOVIE_STILL_CUT', ?, '기생충', 'Parasite', TRUE, 'APPROVED')
                """, MOVIE_ID);
        // 노출 순서와 저장 순서를 엇갈리게 넣는다.
        insertStillCut("cut-3", 3);
        insertStillCut("cut-1", 1);
        insertStillCut("cut-2", 2);
        GameStartResult game = startGame();

        // when
        StillCutRevealResult first = startReel(game);
        StillCutRevealResult second = revealNext(game);
        StillCutRevealResult third = revealNext(game);

        // then
        assertThat(first.imageUrl()).isEqualTo("cut-1");
        assertThat(second.imageUrl()).isEqualTo("cut-2");
        assertThat(third.imageUrl()).isEqualTo("cut-3");
        assertThat(third.hasNext()).isFalse();
        assertThat(stillCutService.revealNext(game.roomId(), game.gameId(), game.roundNo())).isEmpty();
    }

    private StillCutRevealResult startReel(GameStartResult game) {
        return stillCutService.startReel(game.roomId(), game.gameId(), game.roundNo(), game.questionId()).orElseThrow();
    }

    private StillCutRevealResult revealNext(GameStartResult game) {
        return stillCutService.revealNext(game.roomId(), game.gameId(), game.roundNo()).orElseThrow();
    }

    private void insertStillCut(String imageUrl, int displayOrder) {
        jdbcTemplate.update("INSERT INTO still_cut (movie_id, image_url, display_order) VALUES (?, ?, ?)",
                MOVIE_ID, imageUrl, displayOrder);
    }

    // 방은 메모리에 남아 테스트 사이에 지워지지 않으므로 연결 토큰을 테스트마다 새로 만든다.
    private GameStartResult startGame() {
        String hostToken = UUID.randomUUID().toString();
        roomService.create(new CreateRoomCommand("방장", hostToken));
        return gameService.start(new StartGameCommand(GameType.MOVIE_STILL_CUT, 3, hostToken));
    }
}
