package geniusneugul.project.core.support;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * 통합 테스트 공통 상위 클래스. 모든 통합 테스트가 같은 설정을 공유해야 Spring context가 한 번만 뜬다.
 * 외부 협력 객체의 @MockitoBean 은 여기에만 선언한다(docs/TEST.md 「Spring Context 재사용」).
 */
@SpringBootTest
public abstract class IntegrationTestSupport {
}
