package geniusneugul.project.core.report.infra;

import geniusneugul.project.core.report.domain.ErrorReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorReportRepository extends JpaRepository<ErrorReport, Long> {
}
