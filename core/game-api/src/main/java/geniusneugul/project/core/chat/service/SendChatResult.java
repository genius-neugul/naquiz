package geniusneugul.project.core.chat.service;

/**
 * 채팅 한 건을 보낸 결과. 라운드 진행 중 가장 먼저 맞힌 채팅이면 정답자 확정, 아니면 일반 채팅이다.
 */
public sealed interface SendChatResult permits ChatResult, AnswerSolvedResult {
}
