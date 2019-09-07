package org.arlevin.memeDatabaseBot.processors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.domain.ProcessedMentionsEntity;
import org.arlevin.memeDatabaseBot.domain.SequenceNumberEntity;
import org.arlevin.memeDatabaseBot.domain.UserMemesEntity;
import org.arlevin.memeDatabaseBot.repositories.SequenceNumberRepository;
import org.arlevin.memeDatabaseBot.repositories.ProcessedMentionsRepository;
import org.arlevin.memeDatabaseBot.repositories.UserMemesRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MentionsProcessor {

  @Value("${pathPrefix}")
  private String pathPrefix;

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

        JSONArray medias = tweet.getJSONObject("extended_entities").getJSONArray("media");
        for(int i = 0; i < medias.length(); i++) {
          JSONObject media = medias.getJSONObject(i);
          String twitterMediaUrl = media.getString("media_url_https");

          SequenceNumberEntity sequenceNumberEntity = sequenceNumberRepository.save(new SequenceNumberEntity());
          sequenceNumberRepository.deleteLessThanHighNum(sequenceNumberEntity.getSequenceNumber());
          String sequenceNumber = sequenceNumberEntity.getSequenceNumber().toString();

          UserMemesEntity userMemesEntity = UserMemesEntity.builder()
              .userId(userId)
              .description(description)
              .sequenceNumber(sequenceNumber)
              .twitterMediaUrl(twitterMediaUrl)
              .build();

          userMemesRepository.save(userMemesEntity);

          StringBuilder stringBuilder = new StringBuilder();
          for(int j = 0; j < 12 - sequenceNumber.length(); j++) {
            stringBuilder.append("0");
          }
          sequenceNumber = stringBuilder.toString() + sequenceNumber;

          String fileName = pathPrefix + '/'
              + sequenceNumber.substring(0, 3) + '/'
              + sequenceNumber.substring(3, 6) + '/'
              + sequenceNumber.substring(6, 9) + '/'
              + sequenceNumber.substring(9, 12) + '/'
              + sequenceNumber
              + twitterMediaUrl.substring(twitterMediaUrl.lastIndexOf('.'));

          File file = new File(fileName);
          file.getParentFile().mkdirs();
          FileWriter writer = null;
          try {
            writer.write("test");
            writer.close();

          } catch (IOException e) {
            log.error("Could not open fileWriter: {}", e.toString());
          }
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