package org.arlevin.memeDatabaseBot.repositories;

import java.util.Optional;
import org.arlevin.memeDatabaseBot.entity.ProcessedMentionsEntity;
import org.springframework.data.repository.CrudRepository;

public interface ProcessedMentionsRepository extends CrudRepository<ProcessedMentionsEntity, Long> {

  Optional<ProcessedMentionsEntity> getAllByTweetId(String tweetId);
}