package org.arlevin.memeDatabaseBot.processors;

import java.util.List;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.domain.ProcessedMentionsEntity;
import org.arlevin.memeDatabaseBot.repositories.ProcessedMentionsRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MentionsProcessor {

  @Value("${pathPrefix}")
  private String pathPrefix;

  private final ProcessedMentionsRepository processedMentionsRepository;
  private final LearnMentionsProcessor learnMentionsProcessor;
  private final PostMentionsProcessor postMentionsProcessor;

  public MentionsProcessor(ProcessedMentionsRepository processedMentionsRepository,
      LearnMentionsProcessor learnMentionsProcessor, PostMentionsProcessor postMentionsProcessor) {
    this.processedMentionsRepository = processedMentionsRepository;
    this.learnMentionsProcessor = learnMentionsProcessor;
    this.postMentionsProcessor = postMentionsProcessor;
  }

  @Transactional
  public void process(List<JSONObject> tweets) {
    for (JSONObject tweet : tweets) {
      addMentionRecordToDb(tweet);

      if (isValidLearnMention(tweet)) {
        learnMentionsProcessor.process(tweet, getLearnDescription(tweet));
      } else if (isValidPostMention(tweet)) {
        postMentionsProcessor.process(tweet, getPostDescription(tweet));
      } else {
        log.info("Received an invalid mention: {}", tweet.getString("text"));
      }
    }
  }

  private void addMentionRecordToDb(JSONObject tweet) {
    ProcessedMentionsEntity processedMentionsEntity = ProcessedMentionsEntity.builder()
        .tweetId(tweet.getString("id_str"))
        .build();
    processedMentionsRepository.save(processedMentionsEntity);
  }

  private boolean isValidLearnMention(JSONObject tweet) {
    return tweet.getString("text")
        .matches("(?i:@memeDatabaseBot\\s+learn\\s*\\S+.*https://t.co/\\S+.*)");
  }

  private boolean isValidPostMention(JSONObject tweet) {
    return tweet.getString("text").matches("(?i:@memeDatabaseBot\\s+post\\s*\\S+.*)");
  }

  private String getLearnDescription(JSONObject tweet) {
    String tweetText = tweet.getString("text");
    tweetText = tweetText.substring(tweetText.indexOf("learn") + 5);
    tweetText = tweetText.replaceAll("(?i:\\s*https://t.co/\\S+.*)", "");
    if(tweetText.matches("\\s+\\S+.*")) {
      tweetText = tweetText.replaceFirst("\\s+", "");
    }
    return tweetText;
  }

  private String getPostDescription(JSONObject tweet) {
    String tweetText = tweet.getString("text");
    tweetText = tweetText.substring(tweetText.toLowerCase().indexOf("post") + 4);
    if(tweetText.matches("\\s+\\S+.*")) {
      tweetText = tweetText.replaceFirst("\\s+", "");
    }
    return tweetText;
  }
}