package geniusneugul.project.core.game.common.domain;

/**
 * 정답 힌트를 만들 때 쓰는 글자 종류. 한글은 완성형 음절(가~힣)만 해당하고, 자모만 있는 글자(ㅋ 등)는 특수문자로 본다.
 */
public enum CharacterKind {
    HANGUL,
    ALPHABET,
    SPACE,
    NUMBER,
    SPECIAL_CHARACTER;

    static final char HANGUL_FIRST = '가';
    static final char HANGUL_LAST = '힣';

    /** ch는 코드 포인트 하나로 된 글자다 */
    public static CharacterKind of(String ch) {
        int c = ch.codePointAt(0);
        if (Character.isWhitespace(c)) {
            return SPACE;
        }
        if (c >= HANGUL_FIRST && c <= HANGUL_LAST) {
            return HANGUL;
        }
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            return ALPHABET;
        }
        if (c >= '0' && c <= '9') {
            return NUMBER;
        }
        return SPECIAL_CHARACTER;
    }
}
