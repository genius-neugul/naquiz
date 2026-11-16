package geniusneugul.project.core.game.twentyquestions.domain;


/**
 * 막 공개한 단서. order는 라운드 안에서 1부터 센 공개 순서, total은 단서 종류 수다.
 */
public record ShownClue(ClueType clueType, String content, int order, int total) {
}
