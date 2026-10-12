package geniusneugul.project.core.game.domain;

/**
 * 정답 힌트를 만들 때 쓰는 글자 분류. 한글은 완성형 음절(가~힣)만 해당하고, 자모만 있는 글자는 특수문자로 본다.
 */
enum AnswerCharKind {
    SPACE,
    HANGUL,
    ENGLISH,
    DIGIT,
    SYMBOL;

    static final int HANGUL_FIRST = 0xAC00;
    static final int HANGUL_LAST = 0xD7A3;

    static AnswerCharKind of(int codePoint) {
        if (Character.isWhitespace(codePoint)) {
            return SPACE;
        }
        if (codePoint >= HANGUL_FIRST && codePoint <= HANGUL_LAST) {
            return HANGUL;
        }
        if ((codePoint >= 'a' && codePoint <= 'z') || (codePoint >= 'A' && codePoint <= 'Z')) {
            return ENGLISH;
        }
        if (codePoint >= '0' && codePoint <= '9') {
            return DIGIT;
        }
        return SYMBOL;
    }
}
