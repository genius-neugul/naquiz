package geniusneugul.project.core.vote.implement;

import geniusneugul.project.core.common.exception.BusinessException;
import geniusneugul.project.core.common.exception.ErrorCode;
import geniusneugul.project.core.game.common.domain.GameProgress;
import geniusneugul.project.core.game.common.domain.HintType;
import geniusneugul.project.core.game.common.infra.GameProgressRepository;
import geniusneugul.project.core.room.implement.RoomLock;
import geniusneugul.project.core.room.implement.RoomMember;
import geniusneugul.project.core.room.implement.RoomMemberReader;
import geniusneugul.project.core.vote.domain.Vote;
import geniusneugul.project.core.vote.domain.VoteType;
import geniusneugul.project.core.vote.infra.OpenVoteRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 투표를 열고 찬성하고 판정한다(docs/DOMAIN.md 3-4). 투표 종류와 무관한 공통 흐름이고, 통과했을 때의 처리는
 * 종류별 VotePassHandler에 맡긴다. 통과 처리가 없는 종류의 투표는 열 수 없다.
 * 통과 기준은 판정 시점에 방에 접속 중인 인원으로 정하므로 판정과 통과 처리를 입장·퇴장·정답 제출과 겹치지 않게
 * 방 락 안에서 한 번에 한다.
 */
@Component
public class VoteCaster {

    private final RoomMemberReader roomMemberReader;
    private final RoomLock roomLock;
    private final GameProgressRepository gameProgressRepository;
    private final OpenVoteRepository openVoteRepository;
    private final Map<VoteType, VotePassHandler> passHandlers;
    private final Clock clock;

    public VoteCaster(RoomMemberReader roomMemberReader, RoomLock roomLock, GameProgressRepository gameProgressRepository,
            OpenVoteRepository openVoteRepository, List<VotePassHandler> passHandlers, Clock clock) {
        this.roomMemberReader = roomMemberReader;
        this.roomLock = roomLock;
        this.gameProgressRepository = gameProgressRepository;
        this.openVoteRepository = openVoteRepository;
        this.passHandlers = passHandlers.stream()
                .collect(Collectors.toUnmodifiableMap(VotePassHandler::voteType, Function.identity()));
        this.clock = clock;
    }

    /** 투표를 연다. 같은 대상의 투표가 이미 열려 있으면 그 투표에 찬성한다 */
    public VoteCast open(String participantToken, VoteType voteType, HintType targetHintType) {
        validateSupported(voteType);
        Long roomId = roomMemberReader.read(participantToken).roomId();
        return roomLock.withLock(roomId, () -> {
            // 방을 찾은 뒤 락을 잡기 전에 나갔을 수 있어 락 안에서 다시 확인한다.
            RoomMember member = roomMemberReader.readWithLock(participantToken);
            GameProgress progress = findPlaying(roomId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_ROUND_NOT_IN_PROGRESS));
            LocalDateTime now = LocalDateTime.now(clock);
            Vote vote = findOpen(roomId, progress, voteType, targetHintType)
                    .orElseGet(() -> openNew(roomId, progress, voteType, targetHintType, member, now));
            vote.approve(member.participantId(), now);
            return judge(roomId, progress, vote, now);
        });
    }

    /** 열린 투표에 찬성한다. 그 사이 통과했거나 라운드가 끝나 열린 투표가 없으면 비어 있다 */
    public Optional<VoteCast> approve(String participantToken, VoteType voteType, HintType targetHintType) {
        Long roomId = roomMemberReader.read(participantToken).roomId();
        return roomLock.withLock(roomId, () -> {
            RoomMember member = roomMemberReader.readWithLock(participantToken);
            LocalDateTime now = LocalDateTime.now(clock);
            return findPlaying(roomId).flatMap(progress -> findOpen(roomId, progress, voteType, targetHintType)
                    .map(vote -> {
                        vote.approve(member.participantId(), now);
                        return judge(roomId, progress, vote, now);
                    }));
        });
    }

    /** 참가자가 나가 인원이 줄었다. 그 방의 열린 투표를 줄어든 인원으로 다시 판정한다 */
    public List<VoteCast> rejudge(Long roomId) {
        return roomLock.withLock(roomId, () -> {
            LocalDateTime now = LocalDateTime.now(clock);
            return findPlaying(roomId)
                    .map(progress -> openVoteRepository.findByRoomId(roomId).stream()
                            // 판정은 하나씩 차례로 한다. 앞선 통과로 라운드가 끝나 함께 만료된 투표는 건너뛴다.
                            .filter(Vote::isOpen)
                            .map(vote -> judge(roomId, progress, vote, now))
                            .toList())
                    .orElse(List.of());
        });
    }

    private void validateSupported(VoteType voteType) {
        if (!passHandlers.containsKey(voteType)) {
            throw new BusinessException(ErrorCode.VOTE_UNSUPPORTED_TYPE, Map.of("voteType", voteType));
        }
    }

    private Optional<GameProgress> findPlaying(Long roomId) {
        return gameProgressRepository.findByRoomId(roomId).filter(progress -> !progress.isRoundSolved());
    }

    /** 같은 대상의 투표는 한 라운드에 하나만 열린다 */
    private Optional<Vote> findOpen(Long roomId, GameProgress progress, VoteType voteType, HintType targetHintType) {
        return openVoteRepository.findByRoomId(roomId).stream()
                .filter(vote -> vote.isSameTarget(progress.getGameId(), progress.getRoundNo(), voteType, targetHintType))
                .findFirst();
    }

    private Vote openNew(Long roomId, GameProgress progress, VoteType voteType, HintType targetHintType,
            RoomMember initiator, LocalDateTime now) {
        Vote vote = Vote.open(progress.getGameId(), progress.getRoundNo(), voteType, targetHintType,
                initiator.participantId(), now);
        openVoteRepository.save(roomId, vote);
        return vote;
    }

    private VoteCast judge(Long roomId, GameProgress progress, Vote vote, LocalDateTime now) {
        List<Long> participantIds = roomMemberReader.readParticipantIds(roomId);
        if (!vote.judge(participantIds)) {
            return VoteCast.of(roomId, vote, participantIds, null, List.of());
        }
        openVoteRepository.delete(roomId, vote);
        VotePass pass = passHandlers.get(vote.getVoteType()).pass(roomId, progress, vote, now);
        List<Vote> endedVotes = new ArrayList<>(List.of(vote));
        if (pass.endsRound()) {
            endedVotes.addAll(expireAll(roomId));
        }
        return VoteCast.of(roomId, vote, participantIds, pass, endedVotes);
    }

    // 라운드가 끝나 남은 투표는 더 통과할 수 없다.
    private List<Vote> expireAll(Long roomId) {
        List<Vote> expired = openVoteRepository.deleteByRoomId(roomId);
        expired.forEach(Vote::expire);
        return expired;
    }
}
