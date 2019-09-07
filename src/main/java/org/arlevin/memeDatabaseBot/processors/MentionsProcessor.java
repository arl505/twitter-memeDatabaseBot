package org.arlevin.memeDatabaseBot.processors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
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

          // store the image using methodology found here:
          // https://serverfault.com/questions/95444/storing-a-million-images-in-the-filesystem:
          /*
          First pad your sequence number with leading zeroes until you have at least 12 digit string.
            This is the name for your file. You may want to add a suffix:
                         12345 -> 000000012345.jpg
          Then split the string to 2 or 3 character blocks where each block denotes a directory level.
            Have a fixed number of directory levels (for example 3):
                          000000012345 -> 000/000/012
          Store the file to under generated directory:
              Thus the full path and file filename for file with sequence id 123 is
                          000/000/012/00000000012345.jpg
              For file with sequence id 12345678901234 the path would be
                          123/456/789/12345678901234.jpg
           */

          // increment sequence by inserting new record
          // delete all records less than new sequence number (should only be 1 record)
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

          downloadFile(twitterMediaUrl, fileName);
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

  private void downloadFile(String url, String fileName) {
    File file = new File(fileName);
    file.getParentFile().mkdirs();

    try {
      URL urlStream = new URL(url);
      ReadableByteChannel readableByteChannel = Channels.newChannel(urlStream.openStream());
      FileOutputStream fileOutputStream = new FileOutputStream(fileName);
      FileChannel fileChannel = fileOutputStream.getChannel();
      fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
      fileOutputStream.close();
      fileChannel.close();
      readableByteChannel.close();
    } catch (IOException e) {
      log.error("Could not open twitter url ({}): {}", url, e.toString());
    }
  }
}