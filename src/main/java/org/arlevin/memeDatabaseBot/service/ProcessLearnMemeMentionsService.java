package org.arlevin.memeDatabaseBot.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.client.TwitterClient;
import org.arlevin.memeDatabaseBot.entity.UserMemesEntity;
import org.arlevin.memeDatabaseBot.repositories.UserMemesRepository;
import org.arlevin.memeDatabaseBot.utilities.MediaFileUtility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProcessLearnMemeMentionsService {

  @Value("${credentials.consumer.key}")
  private String consumerApiKey;

  @Value("${credentials.access.key}")
  private String accessToken;

  private static final String SHOW_STATUS_API_PATH = "/1.1/statuses/show.json";

  private final UserMemesRepository userMemesRepository;
  private final MediaFileUtility mediaFileUtility;
  private final TwitterClient twitterClient;

  public ProcessLearnMemeMentionsService(final UserMemesRepository userMemesRepository,
      final MediaFileUtility mediaFileUtility,
      final TwitterClient twitterClient) {
    this.userMemesRepository = userMemesRepository;
    this.mediaFileUtility = mediaFileUtility;
    this.twitterClient = twitterClient;
  }

  public void process(final JSONObject tweet, final String description) {
    final Map<String, Boolean> medias = getMediaMapFromTweet(tweet);

    if (!medias.isEmpty()) {
      final String userId = tweet.getJSONObject("user").getString("id_str");
      final String tweetId = tweet.getString("id_str");

      if (!userMemesRepository.findAllByUserIdAndDescription(userId, description).isPresent()) {
        log.info("Received a learn request with tweetId {} from userId {}, with description {}",
            tweetId, userId, description);

        for (final Map.Entry<String, Boolean> entry : medias.entrySet()) {
          final String sequenceNumber = mediaFileUtility.getSequenceNumber();

          UserMemesEntity userMemesEntity = UserMemesEntity.builder()
              .userId(userId)
              .description(description)
              .sequenceNumber(sequenceNumber)
              .twitterMediaUrl(entry.getKey())
              .isGif(entry.getValue())
              .build();

          log.info("Saving meme record for tweetId {} from userId {} with sequence number {}",
              tweetId, userId, sequenceNumber);
          userMemesRepository.save(userMemesEntity);

          String fileSuffix = entry.getKey().substring(entry.getKey().lastIndexOf('.'));
          if (fileSuffix.contains("?")) {
            fileSuffix = fileSuffix.substring(0, fileSuffix.indexOf('?'));
          }
          final String fileName = mediaFileUtility.getFileName(sequenceNumber, fileSuffix);

          log.info("Downloading file from {} to {}", entry.getKey(), fileName);
          downloadFile(entry.getKey(), fileName);
          log.info("Successfully download file from {} to {}", entry.getKey(), fileName);
        }
        log.info(
            "Successfully downloaded all media for tweetId {} from userId {} with description {}",
            tweetId, userId, description);
        log.info("Posting learn request received response to twitter in response to tweetId {}",
            tweetId);

        final Map<String, String> params = new HashMap<>();
        params.put("status", '@' + tweet.getJSONObject("user").getString("screen_name") + "✅️");
        params.put("in_reply_to_status_id", tweet.getString("id_str"));
        params.put("include_entities", "true");

        twitterClient.makeRequest(HttpMethod.POST, "/1.1/statuses/update.json", params);
        log.info(
            "Succefully posted learn request received response to twitter in response to tweetId {}",
            tweetId);
      } else {
        log.info("Received a learn request with an already in use description {} from userId {}",
            description, userId);

        final Map<String, String> params = new HashMap<>();
        params.put("status", "@" + tweet.getJSONObject("user").getString("screen_name")
            + " You already have a meme saved with that description");
        params.put("in_reply_to_status_id", tweet.getString("id_str"));
        params.put("include_entities", "true");

        twitterClient.makeRequest(HttpMethod.POST, "/1.1/statuses/update.json", params);

        log.info(
            "Succefully posted failed learn request received response to twitter in response to tweetId {}",
            tweetId);
      }
    } else {
      log.info("Received a learn request with no media attached with the description ({})",
          description);
    }
  }

  private Map<String, Boolean> getMediaMapFromTweet(final JSONObject tweet) {

    JSONArray medias = new JSONArray();

    // if media is directly attached to tweet
    if (tweet.has("extended_entities")) {
      medias = tweet.getJSONObject("extended_entities").getJSONArray("media");
    }

    // if media is in quoted tweet
    else if (tweet.has("quoted_status")) {
      medias = tweet.getJSONObject("quoted_status").getJSONObject("extended_entities")
          .getJSONArray("media");
    }

    // if inReplyTo tweet exists, try and get media from it
    else if (!tweet.isNull("in_reply_to_status_id_str")) {
      medias = getMediasFromInResponseToTweet(tweet);
    }

    // if no media found, return empty list
    else if (medias.isEmpty()) {
      return Collections.emptyMap();
    }

    return getMediaMapFromMediasArray(medias);
  }

  private JSONArray getMediasFromInResponseToTweet(final JSONObject originalTweet) {
    final String inReplyToTweetId = originalTweet.getString("in_reply_to_status_id_str");

    final JSONObject inReplyToTweet = getInReplyToTweet(inReplyToTweetId);

    if (inReplyToTweet.has("extended_entities")) {
      return inReplyToTweet.getJSONObject("extended_entities").getJSONArray("media");
    }
    return new JSONArray();
  }

  private JSONObject getInReplyToTweet(final String tweetId) {
    final Map<String, String> signatureParams = new HashMap<>();
    signatureParams.put("tweet_mode", "extended");
    signatureParams.put("id", tweetId);

    ResponseEntity<String> responseEntity = twitterClient
        .makeRequest(HttpMethod.GET, SHOW_STATUS_API_PATH, signatureParams);
    return new JSONObject(responseEntity.getBody());
  }

  private Map<String, Boolean> getMediaMapFromMediasArray(final JSONArray medias) {
    final Map<String, Boolean> urls = new HashMap<>();
    for (int i = 0; i < medias.length(); i++) {
      final JSONObject media = medias.getJSONObject(i);
      String twitterMediaUrl = "";
      boolean isGif = false;
      if (media.getString("type").equals("video")) {
        twitterMediaUrl = getOptimalVideoUrl(
            media.getJSONObject("video_info").getJSONArray("variants"));
      } else if (media.getString("type").equals("animated_gif")) {
        twitterMediaUrl = getOptimalVideoUrl(
            media.getJSONObject("video_info").getJSONArray("variants"));
        isGif = true;
      } else {
        twitterMediaUrl = media.getString("media_url_https");
      }
      urls.put(twitterMediaUrl, isGif);
    }
    return urls;
  }

  private String getOptimalVideoUrl(final JSONArray variantsArray) {
    String twitterMediaUrl = "";
    int bitrate = 0;
    for (int j = 0; j < variantsArray.length(); j++) {
      if (((variantsArray.getJSONObject(j).getString("content_type").equals("video/mp4")) && (
          variantsArray.getJSONObject(j).getInt("bitrate") > bitrate))
          || ((j == variantsArray.length() - 1) && (bitrate == 0))) {
        bitrate = (variantsArray.getJSONObject(j).has("bitrate"))
            ? variantsArray.getJSONObject(j).getInt("bitrate")
            : 0;
        twitterMediaUrl = variantsArray.getJSONObject(j).getString("url");
      }
    }
    if (twitterMediaUrl.equals("")) {
      twitterMediaUrl = variantsArray.getJSONObject(0).getString("url");
    }

    return twitterMediaUrl;
  }

  private void downloadFile(final String url, final String fileName) {
    final File file = new File(fileName);
    file.getParentFile().mkdirs();

    ReadableByteChannel readableByteChannel = null;
    try {
      log.info("Opening connection to media url {} ...", url);
      final URL urlStream = new URL(url);
      readableByteChannel = Channels.newChannel(urlStream.openStream());
    } catch (final Exception e) {
      log.error("An exception occurred while connecting to media url {}\n", url, e);
    }
    try {
      log.info("Opening file to save to...");
      final FileOutputStream fileOutputStream = new FileOutputStream(fileName);
      final FileChannel fileChannel = fileOutputStream.getChannel();
      log.info("Writing to file...");
      fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
      fileOutputStream.close();
      fileChannel.close();
      readableByteChannel.close();
    } catch (final IOException e) {
      log.error("Could not save downloaded media to file {}: ", fileName, e);
    }
  }
}