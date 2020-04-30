package org.arlevin.memedatabasebot.entity;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "processed_mentions")
public class ProcessedMentionsEntity {

  @Id
  @Column(name = "tweet_id")
  private String tweetId;

  @CreationTimestamp
  @Column(name = "processed_timestamp")
  private Instant processedTimestamp;
}
