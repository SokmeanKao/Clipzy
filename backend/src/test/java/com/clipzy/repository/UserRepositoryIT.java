package com.clipzy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.clipzy.domain.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserRepositoryIT {

  @Autowired
  UserRepository userRepository;

  @Test
  void savesAndFetchesUser() {
    User user = new User();
    user.setEmail("it-user-" + UUID.randomUUID() + "@clipzy.test");
    user.setPasswordHash("$2a$10$placeholderhashplaceholderhash12");
    user.setDisplayName("Integration User");

    User saved = userRepository.saveAndFlush(user);
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();

    User found = userRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getEmail()).isEqualTo(user.getEmail());
    assertThat(found.getDisplayName()).isEqualTo("Integration User");
  }
}
