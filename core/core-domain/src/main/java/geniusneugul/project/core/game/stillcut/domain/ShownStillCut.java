package geniusneugul.project.core.game.stillcut.domain;

/**
 * 지금 보여주는 스틸컷. order는 1부터 센 공개 순서다.
 */
public record ShownStillCut(int order, int total, String imageUrl) {
}
