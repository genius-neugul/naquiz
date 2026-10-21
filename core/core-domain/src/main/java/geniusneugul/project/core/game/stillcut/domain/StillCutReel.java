package geniusneugul.project.core.game.stillcut.domain;

import java.util.List;
import java.util.Optional;
import lombok.Getter;

/**
 * 스틸컷 게임 한 라운드에서 보여줄 스틸컷 묶음. 노출 순서대로 하나씩 공개하고, 새 컷은 이전 컷을 대체한다(docs/DOMAIN.md 3-2).
 * 공개 위치는 진행 중에만 쓰므로 메모리에 둔다. 스스로 동기화하지 않으므로 방 락 안에서만 읽고 바꾼다.
 */
@Getter
public class StillCutReel {

    private final Long roomId;
    private final Long gameId;
    private final int roundNo;
    private final List<String> imageUrls;
    private int revealedCount;

    /** imageUrls는 노출 순서대로 담는다 */
    public StillCutReel(Long roomId, Long gameId, int roundNo, List<String> imageUrls) {
        if (imageUrls.isEmpty()) {
            throw new IllegalArgumentException("스틸컷이 없는 영화는 스틸컷 문제로 출제하지 않습니다. gameId=" + gameId);
        }
        this.roomId = roomId;
        this.gameId = gameId;
        this.roundNo = roundNo;
        this.imageUrls = List.copyOf(imageUrls);
    }

    /** 다음 스틸컷을 공개한다. 모두 공개했으면 비어 있다 */
    public Optional<ShownStillCut> revealNext() {
        if (!hasNext()) {
            return Optional.empty();
        }
        revealedCount++;
        return Optional.of(new ShownStillCut(revealedCount, imageUrls.size(), imageUrls.get(revealedCount - 1)));
    }

    public boolean hasNext() {
        return revealedCount < imageUrls.size();
    }

    public boolean isFor(Long gameId, int roundNo) {
        return this.gameId.equals(gameId) && this.roundNo == roundNo;
    }
}
