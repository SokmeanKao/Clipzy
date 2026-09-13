package com.clipzy.repository;

import com.clipzy.domain.JobStatus;
import com.clipzy.domain.TranscodeJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscodeJobRepository extends JpaRepository<TranscodeJob, UUID> {

  List<TranscodeJob> findByStatusOrderByCreatedAtAsc(JobStatus status, Pageable pageable);
}
