package geniusneugul.project.core.crawl.infra;

import geniusneugul.project.core.crawl.domain.CrawlRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlRunRepository extends JpaRepository<CrawlRun, Long> {
}
