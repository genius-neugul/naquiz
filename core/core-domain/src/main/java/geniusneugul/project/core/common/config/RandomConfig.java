package geniusneugul.project.core.common.config;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 게임 규칙에 쓰는 무작위 소스. 참가자가 결과를 미리 추측하기 어렵도록 SecureRandom을 쓴다.
 */
@Configuration
public class RandomConfig {

    @Bean
    public RandomGenerator random() {
        return new SecureRandom();
    }
}
