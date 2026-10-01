package geniusneugul.project.core.vote.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"vote_id", "participant_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_id")
    private Vote vote;

    // 참가자는 저장하지 않으므로 FK 없이 ID만 남긴다.
    @Column(nullable = false)
    private Long participantId;

    @Column(nullable = false)
    private LocalDateTime approvedAt;
}
