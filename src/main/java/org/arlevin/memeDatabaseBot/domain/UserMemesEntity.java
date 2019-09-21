package org.arlevin.memeDatabaseBot.domain;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.arlevin.memeDatabaseBot.domain.UserMemesEntity.UserMemesId;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_memes")
@IdClass(UserMemesId.class)
public class UserMemesEntity {

  @Id
  @Column(name = "user_id")
  private String userId;

  @Id
  @Column(name = "description")
  private String description;

  @Id
  @Column(name = "sequence_number")
  private String sequenceNumber;

  @Column(name = "twitter_media_url")
  private String twitterMediaUrl;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserMemesId implements Serializable {
    private String userId;
    private String description;
    private String sequenceNumber;
  }
}