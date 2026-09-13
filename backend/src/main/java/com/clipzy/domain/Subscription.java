package com.clipzy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription {

  @EmbeddedId
  private SubscriptionId id = new SubscriptionId();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("subscriberId")
  @JoinColumn(name = "subscriber_id", nullable = false)
  private User subscriber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("channelId")
  @JoinColumn(name = "channel_id", nullable = false)
  private User channel;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public SubscriptionId getId() {
    return id;
  }

  public void setId(SubscriptionId id) {
    this.id = id;
  }

  public User getSubscriber() {
    return subscriber;
  }

  public void setSubscriber(User subscriber) {
    this.subscriber = subscriber;
    if (id == null) {
      id = new SubscriptionId();
    }
    id.setSubscriberId(subscriber.getId());
  }

  public User getChannel() {
    return channel;
  }

  public void setChannel(User channel) {
    this.channel = channel;
    if (id == null) {
      id = new SubscriptionId();
    }
    id.setChannelId(channel.getId());
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Embeddable
  public static class SubscriptionId implements Serializable {

    @Column(name = "subscriber_id")
    private UUID subscriberId;

    @Column(name = "channel_id")
    private UUID channelId;

    public SubscriptionId() {
    }

    public SubscriptionId(UUID subscriberId, UUID channelId) {
      this.subscriberId = subscriberId;
      this.channelId = channelId;
    }

    public UUID getSubscriberId() {
      return subscriberId;
    }

    public void setSubscriberId(UUID subscriberId) {
      this.subscriberId = subscriberId;
    }

    public UUID getChannelId() {
      return channelId;
    }

    public void setChannelId(UUID channelId) {
      this.channelId = channelId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof SubscriptionId that)) {
        return false;
      }
      return Objects.equals(subscriberId, that.subscriberId)
          && Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(subscriberId, channelId);
    }
  }
}
