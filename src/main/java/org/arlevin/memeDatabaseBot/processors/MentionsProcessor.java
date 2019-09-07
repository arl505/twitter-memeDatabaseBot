package org.arlevin.memeDatabaseBot.processors;

import java.util.List;
import javax.transaction.Transactional;
import org.arlevin.memeDatabaseBot.domain.ProcessedMentionsEntity;
import org.arlevin.memeDatabaseBot.domain.SequenceNumberEntity;
import org.arlevin.memeDatabaseBot.domain.UserMemesEntity;
import org.arlevin.memeDatabaseBot.repositories.SequenceNumberRepository;
import org.arlevin.memeDatabaseBot.repositories.ProcessedMentionsRepository;
import org.arlevin.memeDatabaseBot.repositories.UserMemesRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class MentionsProcessor {

  private final ProcessedMentionsRepository processedMentionsRepository;
  private final SequenceNumberRepository sequenceNumberRepository;
  private final UserMemesRepository userMemesRepository;

  public MentionsProcessor(ProcessedMentionsRepository processedMentionsRepository,
      SequenceNumberRepository sequenceNumberRepository,
      UserMemesRepository userMemesRepository) {
    this.processedMentionsRepository = processedMentionsRepository;
    this.sequenceNumberRepository = sequenceNumberRepository;
    this.userMemesRepository = userMemesRepository;
  }

  @Transactional
  public void process(List<JSONObject> tweets) {
    for (JSONObject tweet : tweets) {
      addMentionRecordToDb(tweet);

      if (isValidMention(tweet)) {
        String description = getDescription(tweet);
        String userId = tweet.getJSONObject("user").getString("id_str");

        JSONArray medias = tweet.getJSONObject("entities").getJSONArray("media");
        for(int i = 0; i < medias.length(); i++) {
          JSONObject media = medias.getJSONObject(i);
          String twitterMediaUrl = media.getString("media_url_https");

          SequenceNumberEntity sequenceNumberEntity = sequenceNumberRepository.save(new SequenceNumberEntity());
          sequenceNumberRepository.deleteBySequenceNumberLessThan(sequenceNumberEntity.getSequenceNumber());
          String sequenceNumber = sequenceNumberEntity.getSequenceNumber().toString();

          UserMemesEntity userMemesEntity = UserMemesEntity.builder()
              .userId(userId)
              .description(description)
              .sequenceNumber(sequenceNumber)
              .twitterMediaUrl(twitterMediaUrl)
              .build();

          userMemesRepository.save(userMemesEntity);

          // use the sequence number to save media to filesystem
        }
      }
    }
  }

  private void addMentionRecordToDb(JSONObject tweet) {
    ProcessedMentionsEntity processedMentionsEntity = ProcessedMentionsEntity.builder()
        .tweetId(tweet.getString("id_str"))
        .build();
    processedMentionsRepository.save(processedMentionsEntity);
  }

  private boolean isValidMention(JSONObject tweet) {
    return tweet.getString("text")
        .matches("@memeDatabaseBot\\s+learn\\s*\\S+.*https://t.co/\\S+.*");
  }

  private String getDescription(JSONObject tweet) {
    String tweetText = tweet.getString("text");
    tweetText = tweetText.substring(tweetText.indexOf("learn") + 5);
    tweetText = tweetText.replaceAll("\\s*https://t.co/\\S+.*", "");
    tweetText = tweetText.replaceFirst("\\s+", "");
    return tweetText;
  }
}