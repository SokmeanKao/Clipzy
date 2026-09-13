package com.clipzy.repository;

import com.clipzy.domain.Subscription;
import com.clipzy.domain.Subscription.SubscriptionId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, SubscriptionId> {

  boolean existsByIdSubscriberIdAndIdChannelId(UUID subscriberId, UUID channelId);

  void deleteByIdSubscriberIdAndIdChannelId(UUID subscriberId, UUID channelId);

  long countByIdChannelId(UUID channelId);
}
