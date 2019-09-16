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

      String tweetText = tweet.getString("text");
      tweetText = tweetText.substring(tweetText.toLowerCase().indexOf("@memestorebot"));

      if (isValidLearnMention(tweetText)) {
        learnMentionsProcessor.process(tweet, getLearnDescription(tweetText));
      } else if (isValidPostMention(tweetText)) {
        boolean removeEndUrl = tweet.get("is_quote_status").equals(true);
        postMentionsProcessor.process(tweet, getPostDescription(tweetText, removeEndUrl));
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

  private boolean isValidLearnMention(String tweetText) {
    return tweetText.matches("(?i:@memestorebot\\s+learn\\s*\\S+.*https://t.co/\\S+.*)");
  }

  private boolean isValidPostMention(String tweetText) {
    return tweetText.matches("(?i:@memestorebot\\s+post\\s*\\S+.*)");
  }

  private String getLearnDescription(String tweetText) {
    tweetText = tweetText.substring(tweetText.indexOf("learn") + 5);
    tweetText = tweetText.replaceAll("(?i:\\s*https://t.co/\\S+.*)", "");
    if(tweetText.matches("\\s+\\S+.*")) {
      tweetText = tweetText.replaceFirst("\\s+", "");
    }
    return tweetText;
  }

  private String getPostDescription(String tweetText, boolean removeEndUrl) {
    tweetText = tweetText.substring(tweetText.toLowerCase().indexOf("post") + 4);

    if(tweetText.matches("\\s+\\S+.*")) {
      tweetText = tweetText.replaceFirst("\\s+", "");
    }

    if(removeEndUrl) {
      tweetText = tweetText.replaceAll("(?i:\\s*https://t.co/\\S+.*)", "");
    }
    return tweetText;
  }
}