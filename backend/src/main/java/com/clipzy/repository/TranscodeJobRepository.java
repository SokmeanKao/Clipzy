package com.clipzy.repository;

import com.clipzy.domain.JobStatus;
import com.clipzy.domain.TranscodeJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TranscodeJobRepository extends JpaRepository<TranscodeJob, UUID> {

  List<TranscodeJob> findByStatusOrderByCreatedAtAsc(JobStatus status, Pageable pageable);

  /**
   * Atomically claim up to {@code limit} pending jobs using Postgres
   * {@code FOR UPDATE SKIP LOCKED} so concurrent pollers never double-claim.
   *
   * @return claimed job ids now in {@code running} status
   */
  @Query(value = """
      WITH cte AS (
        SELECT id
        FROM transcode_jobs
        WHERE status = 'pending'
        ORDER BY created_at ASC
        FOR UPDATE SKIP LOCKED
        LIMIT :limit
      )
      UPDATE transcode_jobs j
      SET status = 'running', updated_at = NOW()
      FROM cte
      WHERE j.id = cte.id
      RETURNING j.id
      """, nativeQuery = true)
  List<UUID> claimPendingJobs(@Param("limit") int limit);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
      UPDATE transcode_jobs
      SET status = 'pending', updated_at = NOW(), error_message = NULL
      WHERE id = :id AND status = 'running'
      """, nativeQuery = true)
  int releaseClaim(@Param("id") UUID id);
}
