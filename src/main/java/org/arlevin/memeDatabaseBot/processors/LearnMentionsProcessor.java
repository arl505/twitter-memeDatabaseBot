package org.arlevin.memeDatabaseBot.processors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.domain.UserMemesEntity;
import org.arlevin.memeDatabaseBot.repositories.UserMemesRepository;
import org.arlevin.memeDatabaseBot.services.PostTweetService;
import org.arlevin.memeDatabaseBot.utilities.MediaFileUtility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LearnMentionsProcessor {

  private final UserMemesRepository userMemesRepository;
  private final MediaFileUtility mediaFileUtility;
  private final PostTweetService postTweetService;

  public LearnMentionsProcessor(UserMemesRepository userMemesRepository,
      MediaFileUtility mediaFileUtility,
      PostTweetService postTweetService) {
    this.userMemesRepository = userMemesRepository;
    this.mediaFileUtility = mediaFileUtility;
    this.postTweetService = postTweetService;
  }

  void process(JSONObject tweet, String description) {
    String userId = tweet.getJSONObject("user").getString("id_str");
    if (!userMemesRepository.findAllByUserIdAndDescription(userId, description).isPresent()) {
      JSONArray medias = tweet.getJSONObject("extended_entities").getJSONArray("media");
      for (int i = 0; i < medias.length(); i++) {
        JSONObject media = medias.getJSONObject(i);
        String twitterMediaUrl = getMediaUrl(media);
        String sequenceNumber = mediaFileUtility.getSequenceNumber();

        UserMemesEntity userMemesEntity = UserMemesEntity.builder()
            .userId(userId)
            .description(description)
            .sequenceNumber(sequenceNumber)
            .twitterMediaUrl(twitterMediaUrl)
            .isGif(media.getString("type").equals("animated_gif"))
            .build();

        userMemesRepository.save(userMemesEntity);

        String fileSuffix = twitterMediaUrl.substring(twitterMediaUrl.lastIndexOf('.'));
        if (fileSuffix.contains("?")) {
          fileSuffix = fileSuffix.substring(0, fileSuffix.indexOf('?'));
        }
        downloadFile(twitterMediaUrl, mediaFileUtility.getFileName(sequenceNumber, fileSuffix));
      }
      postTweetService.postTweet('@' + tweet.getJSONObject("user").getString("screen_name") + "✅️",
          tweet.getString("id_str"), null);
    }
    log.info("Received a learn request with an already in use description {} from userId {}", description, userId);
  }

  private String getMediaUrl(JSONObject media) {
    String twitterMediaUrl = "";
    if (media.getString("type").equals("video") || media.getString("type")
        .equals("animated_gif")) {
      JSONArray variantsArray = media
          .getJSONObject("video_info")
          .getJSONArray("variants");
      boolean foundMp4 = false;
      for (int j = 0; j < variantsArray.length() && !foundMp4; j++) {
        if (variantsArray.getJSONObject(j).getString("content_type").equals("video/mp4")
            || j == variantsArray.length() - 1) {
          foundMp4 = true;
          twitterMediaUrl = variantsArray.getJSONObject(j).getString("url");
        }
      }
    } else {
      twitterMediaUrl = media.getString("media_url_https");
    }
    return twitterMediaUrl;
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