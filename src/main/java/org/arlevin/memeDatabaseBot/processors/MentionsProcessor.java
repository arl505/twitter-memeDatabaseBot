package org.arlevin.memeDatabaseBot.processors;

import java.util.List;
import org.arlevin.memeDatabaseBot.domain.ProcessedMentionsEntity;
import org.arlevin.memeDatabaseBot.repositories.ProcessedMentionsRepository;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class MentionsProcessor {

  private final ProcessedMentionsRepository processedMentionsRepository;

  public MentionsProcessor(ProcessedMentionsRepository processedMentionsRepository) {
    this.processedMentionsRepository = processedMentionsRepository;
  }

  public void process(List<JSONObject> tweets) {
    for (JSONObject tweet : tweets) {
      saveTweetToDb(tweet);

      if (isValidMention(tweet)) {
        String twitterMediaUrl = getUrl(tweet);
        String description = getDescription(tweet);

        // save the description with a sequence number which will correlate to the local path
      }
    }
  }

  private void saveTweetToDb(JSONObject tweet) {
    ProcessedMentionsEntity processedMentionsEntity = ProcessedMentionsEntity.builder()
        .tweetId(tweet.getString("id_str"))
        .build();
    processedMentionsRepository.save(processedMentionsEntity);
  }

  private boolean isValidMention(JSONObject tweet) {
    return tweet.getString("text")
        .matches("@memeDatabaseBot\\s+learn\\s*\\S+.*https://t.co/\\S+.*");
  }

  private String getUrl(JSONObject tweet) {
    String tweetText = tweet.getString("text");
    tweetText = tweetText.substring(tweetText.indexOf("https://t.co/"));
    tweetText = tweetText.replaceAll("\\s+.*", "");
    return tweetText;
  }

  private String getDescription(JSONObject tweet) {
    String tweetText = tweet.getString("text");
    tweetText = tweetText.substring(tweetText.indexOf("learn") + 5);
    tweetText = tweetText.replaceAll("\\s*https://t.co/\\S+.*", "");
    tweetText = tweetText.replaceFirst("\\s+", "");
    return tweetText;
  }
}