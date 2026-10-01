package geniusneugul.project.core.admin.infra;

import geniusneugul.project.core.admin.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
}
