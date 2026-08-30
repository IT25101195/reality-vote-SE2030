package com.slit.realityvote.repository;

import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-layer integration tests (Module 6.4).
 * Uses H2 in-memory with ddl-auto=create-drop so the schema is generated
 * automatically from JPA annotations, keeping tests fully self-contained.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class AuditLogRepositoryTest {

    @Autowired
    AuditLogRepository repo;

    private AuditLog save(AuditEventType type, String actor, boolean flagged) {
        AuditLog log = AuditLog.builder()
                .eventType(type)
                .description("test event")
                .actorEmail(actor)
                .flagged(flagged)
                .build();
        return repo.save(log);
    }

    @Test
    void countByFlaggedTrue_returnsOnlyFlaggedRows() {
        save(AuditEventType.LOGIN_SUCCESS, "a@test.com", false);
        save(AuditEventType.SUSPICIOUS_ACTIVITY, "b@test.com", true);
        save(AuditEventType.VOTE_REJECTED, "c@test.com", true);

        assertThat(repo.countByFlaggedTrue()).isEqualTo(2);
    }

    @Test
    void countByEventType_returnsCorrectCount() {
        save(AuditEventType.VOTE_CAST, "a@test.com", false);
        save(AuditEventType.VOTE_CAST, "b@test.com", false);
        save(AuditEventType.LOGIN_FAILURE, "c@test.com", false);

        assertThat(repo.countByEventType(AuditEventType.VOTE_CAST)).isEqualTo(2);
        assertThat(repo.countByEventType(AuditEventType.LOGIN_FAILURE)).isEqualTo(1);
    }

    @Test
    void countByFlaggedTrueAndCreatedDateAfter_returnsRecentFlaggedOnly() {
        save(AuditEventType.SUSPICIOUS_ACTIVITY, "x@test.com", true);

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        assertThat(repo.countByFlaggedTrueAndCreatedDateAfter(oneHourAgo)).isEqualTo(1);

        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        assertThat(repo.countByFlaggedTrueAndCreatedDateAfter(futureTime)).isEqualTo(0);
    }

    @Test
    void findByEntityTypeAndEntityId_returnsMatchingRows() {
        AuditLog log = AuditLog.builder()
                .eventType(AuditEventType.VOTE_CAST)
                .description("vote cast")
                .actorEmail("voter@test.com")
                .entityType("VotingSession")
                .entityId(5L)
                .build();
        repo.save(log);
        save(AuditEventType.LOGIN_SUCCESS, "other@test.com", false);

        List<AuditLog> results = repo.findByEntityTypeAndEntityId("VotingSession", 5L);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getActorEmail()).isEqualTo("voter@test.com");
    }

    @Test
    void findTopFlaggedActors_returnsTopByFlaggedCount() {
        save(AuditEventType.SUSPICIOUS_ACTIVITY, "bad@test.com", true);
        save(AuditEventType.VOTE_REJECTED, "bad@test.com", true);
        save(AuditEventType.SUSPICIOUS_ACTIVITY, "worse@test.com", true);

        List<AuditLogRepository.ActorProjection> top =
                repo.findTopFlaggedActors(PageRequest.of(0, 5));
        assertThat(top).isNotEmpty();
        assertThat(top.get(0).getActorEmail()).isEqualTo("bad@test.com");
    }

    @Test
    void deleteByCreatedDateBefore_doesNotDeleteVoteRows() {
        save(AuditEventType.VOTE_CAST, "v@test.com", false);
        save(AuditEventType.LOGIN_SUCCESS, "a@test.com", false);

        LocalDateTime future = LocalDateTime.now().plusMinutes(1);
        int deleted = repo.deleteByCreatedDateBeforeExcludingVoteEvents(future);

        assertThat(deleted).isEqualTo(1);
        assertThat(repo.count()).isEqualTo(1);
    }
}
