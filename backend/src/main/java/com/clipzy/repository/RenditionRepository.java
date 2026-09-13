package com.clipzy.repository;

import com.clipzy.domain.Rendition;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RenditionRepository extends JpaRepository<Rendition, UUID> {

  List<Rendition> findByVideoId(UUID videoId);
}
