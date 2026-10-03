package geniusneugul.project.core.common.infra.initialdata;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * 초기 데이터(노래·영화·스틸컷) SQL을 song 테이블이 비어 있을 때만 넣는다.
 * 매번 넣으면 MySQL에서 지운 콘텐츠가 재시작할 때 같은 ID로 되살아나므로, 첫 적재 뒤에는 실행하지 않는다.
 * Hibernate가 테이블을 만든 뒤(entityManagerFactory 생성 뒤), 앱이 요청을 받기 전에 실행된다.
 */
@Slf4j
@Component
@DependsOn("entityManagerFactory")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "naquiz.initial-data.enabled", havingValue = "true")
public class InitialDataLoader implements InitializingBean {

    private static final String SCRIPT_LOCATION = "sql/initial-data.sql";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {
        long songCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM song", Long.class);
        if (songCount > 0) {
            log.info("[InitialDataLoader.afterPropertiesSet] Initial data load skipped. songCount={}", songCount);
            return;
        }

        load();
        log.info("[InitialDataLoader.afterPropertiesSet] Initial data loaded. script={}", SCRIPT_LOCATION);
    }

    /**
     * 한 트랜잭션으로 실행해, 중간에 실패하면 모두 되돌리고 다음 시작 때 처음부터 다시 넣는다.
     * 여러 앱이 빈 DB에 동시에 처음 떠도 SQL의 INSERT IGNORE가 같은 행을 건너뛴다.
     */
    private void load() throws Exception {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(SCRIPT_LOCATION));
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());

        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                populator.populate(connection);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }
}
