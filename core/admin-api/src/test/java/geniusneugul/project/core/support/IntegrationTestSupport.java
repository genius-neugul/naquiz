package geniusneugul.project.core.support;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 통합 테스트 공통 상위 클래스. 모든 통합 테스트가 같은 설정을 공유해야 Spring context가 한 번만 뜬다.
 * 외부 협력 객체의 @MockitoBean 은 여기에만 선언한다(docs/TEST.md 「Spring Context 재사용」).
 * 테스트 격리는 @Transactional 롤백이 아니라 매 테스트 전 테이블 비우기로 한다(docs/TEST.md 「DB 테스트 독립 환경 설정」).
 * 매 테스트 전에 테이블을 비우므로 초기 데이터 SQL은 실행하지 않는다.
 */
@SpringBootTest(properties = "naquiz.initial-data.enabled=false")
public abstract class IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUpDatabase() {
        List<String> tableNames = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class);

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        tableNames.forEach(tableName -> jdbcTemplate.execute("TRUNCATE TABLE " + tableName + " RESTART IDENTITY"));
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
