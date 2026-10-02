package geniusneugul.project.core.room.implement;

import geniusneugul.project.core.room.domain.InviteCode;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 무작위 초대 코드를 만든다. 초대 코드를 아는 사람만 들어올 수 있으므로 추측하기 어려운 SecureRandom을 쓴다.
 */
@Component
public class InviteCodeGenerator {

    private final SecureRandom random = new SecureRandom();

    public InviteCode generate() {
        StringBuilder code = new StringBuilder(InviteCode.LENGTH);
        for (int i = 0; i < InviteCode.LENGTH; i++) {
            code.append(InviteCode.CHARACTERS.charAt(random.nextInt(InviteCode.CHARACTERS.length())));
        }
        return new InviteCode(code.toString());
    }
}
