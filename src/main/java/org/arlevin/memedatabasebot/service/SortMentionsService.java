package org.arlevin.memedatabasebot.service;

import java.util.List;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memedatabasebot.entity.ProcessedMentionsEntity;
import org.arlevin.memedatabasebot.repository.ProcessedMentionsRepository;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SortMentionsService {

  private final ProcessedMentionsRepository processedMentionsRepository;
  private final ProcessLearnMemeMentionsService processLearnMemeMentionsService;
  private final ProcessPostMemeMentionsService processPostMemeMentionsService;

  public SortMentionsService(final ProcessedMentionsRepository processedMentionsRepository,
      final ProcessLearnMemeMentionsService processLearnMemeMentionsService,
      final ProcessPostMemeMentionsService processPostMemeMentionsService) {
    this.processedMentionsRepository = processedMentionsRepository;
    this.processLearnMemeMentionsService = processLearnMemeMentionsService;
    this.processPostMemeMentionsService = processPostMemeMentionsService;
  }

  @Transactional
  public void process(final List<JSONObject> tweets) {
    for (final JSONObject tweet : tweets) {
      final String tweetId = tweet.getString("id_str");
      log.info("Saving processed mention with id {} to database...", tweetId);
      addMentionRecordToDb(tweetId);

      String tweetText = tweet.getString("full_text");
      tweetText = tweetText.substring(tweetText.toLowerCase().indexOf("@memestorebot"));

      if (isValidLearnMention(tweetText)) {
        log.info("Found valid learn mention with id {}, processing...", tweetId);
        processLearnMemeMentionsService.process(tweet, getLearnDescription(tweetText));
      } else if (isValidPostMention(tweetText)) {
        log.info("Found valid post mention with id {}, processing...", tweetId);
        boolean removeEndUrl = tweet.get("is_quote_status").equals(true);
        processPostMemeMentionsService.process(tweet, getPostDescription(tweetText, removeEndUrl));
      } else {
        log.info("Received an invalid mention: {}", tweet.getString("full_text"));
      }
    }
  }

  private void addMentionRecordToDb(final String tweetId) {
    final ProcessedMentionsEntity processedMentionsEntity = ProcessedMentionsEntity.builder()
        .tweetId(tweetId)
        .build();
    processedMentionsRepository.save(processedMentionsEntity);
    log.info("Saved processed mention record for tweet id {}", tweetId);
  }

  private boolean isValidLearnMention(final String tweetText) {
    return tweetText.matches("(?i:.*@memestorebot\\s+learn\\s*\\S+[\\s\\S]*)");
  }

  private boolean isValidPostMention(final String tweetText) {
    return tweetText.matches("(?i:@memestorebot\\s+post\\s*\\S+.*)");
  }

  private String getLearnDescription(String tweetText) {
    tweetText = tweetText.substring(tweetText.toLowerCase().indexOf("learn") + 5);
    tweetText = tweetText.replaceAll("(?i:\\s*https://t.co/\\S+.*)", "");
    if (tweetText.matches("\\s+\\S+[\\s\\S]*")) {
      tweetText = tweetText.replaceFirst("\\s+", "");
    }
    return tweetText;
  }

  private String getPostDescription(String tweetText, final boolean removeEndUrl) {
    tweetText = tweetText.substring(tweetText.toLowerCase().indexOf("post") + 4);

    if (tweetText.matches("\\s+\\S+.*")) {
      tweetText = tweetText.replaceFirst("\\s+", "");
    }

    if (removeEndUrl) {
      tweetText = tweetText.replaceAll("(?i:\\s*https://t.co/\\S+.*)", "");
    }
    return tweetText;
  }
}