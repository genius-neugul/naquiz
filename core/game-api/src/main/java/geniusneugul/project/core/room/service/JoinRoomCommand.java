package geniusneugul.project.core.room.service;

public record JoinRoomCommand(String inviteCode, String nickname, String participantToken) {
}
