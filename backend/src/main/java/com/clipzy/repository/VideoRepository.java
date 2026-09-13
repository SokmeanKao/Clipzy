package com.clipzy.repository;

import com.clipzy.domain.Video;
import com.clipzy.domain.VideoStatus;
import com.clipzy.domain.VideoVisibility;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<Video, UUID> {

  Page<Video> findByStatusAndVisibilityOrderByCreatedAtDesc(
      VideoStatus status, VideoVisibility visibility, Pageable pageable);

  Page<Video> findByOwnerIdAndStatusAndVisibilityOrderByCreatedAtDesc(
      UUID ownerId, VideoStatus status, VideoVisibility visibility, Pageable pageable);

  @Query(
      value = """
          SELECT * FROM videos v
          WHERE v.status = 'ready'
            AND v.visibility = 'public'
            AND v.search_vector @@ plainto_tsquery('english', :q)
          ORDER BY ts_rank(v.search_vector, plainto_tsquery('english', :q)) DESC,
                   v.created_at DESC
          """,
      countQuery = """
          SELECT count(*) FROM videos v
          WHERE v.status = 'ready'
            AND v.visibility = 'public'
            AND v.search_vector @@ plainto_tsquery('english', :q)
          """,
      nativeQuery = true)
  Page<Video> searchPublicReady(@Param("q") String q, Pageable pageable);

  @Modifying
  @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :id")
  int incrementViewCount(@Param("id") UUID id);
}
