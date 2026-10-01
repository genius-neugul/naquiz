package geniusneugul.project.core.report.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportSource reportSource;

    // 비회원이라 참가자 ID 대신 닉네임을 스냅샷으로 저장한다.
    private String reporterNickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    private String suggestedAnswer;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Long handledByAdminId;

    private LocalDateTime handledAt;

    @Column(length = 1000)
    private String handleNote;
}
