package geniusneugul.project.core.vote.service;

/**
 * 투표 열기·찬성 요청. targetHintType은 힌트 투표일 때만 있다.
 */
public record VoteCommand(String voteType, String targetHintType, String participantToken) {
}
