package org.arlevin.memedatabasebot.repository;

import java.util.Optional;
import org.arlevin.memedatabasebot.entity.ProcessedMentionsEntity;
import org.springframework.data.repository.CrudRepository;

public interface ProcessedMentionsRepository extends CrudRepository<ProcessedMentionsEntity, Long> {

  Optional<ProcessedMentionsEntity> getAllByTweetId(String tweetId);
}