package org.arlevin.memeDatabaseBot.processors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.arlevin.memeDatabaseBot.domain.UserMemesEntity;
import org.arlevin.memeDatabaseBot.repositories.UserMemesRepository;
import org.arlevin.memeDatabaseBot.services.PostTweetService;
import org.arlevin.memeDatabaseBot.utilities.MediaFileUtility;
import org.arlevin.memeDatabaseBot.utilities.SignatureUtility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class LearnMentionsProcessor {

  @Value("${auth.consumer.apiKey}")
  private String consumerApiKey;

  @Value("${auth.access.token}")
  private String accessToken;

  private final UserMemesRepository userMemesRepository;
  private final MediaFileUtility mediaFileUtility;
  private final PostTweetService postTweetService;
  private final SignatureUtility signatureUtility;

  public LearnMentionsProcessor(UserMemesRepository userMemesRepository,
      MediaFileUtility mediaFileUtility,
      PostTweetService postTweetService,
      SignatureUtility signatureUtility) {
    this.userMemesRepository = userMemesRepository;
    this.mediaFileUtility = mediaFileUtility;
    this.postTweetService = postTweetService;
    this.signatureUtility = signatureUtility;
  }

  void process(JSONObject tweet, String description) {
    List<String> urls = getUrls(tweet);
    if (!urls.isEmpty()) {
      String userId = tweet.getJSONObject("user").getString("id_str");
      if (!userMemesRepository.findAllByUserIdAndDescription(userId, description).isPresent()) {
        for (String url : urls) {
          String sequenceNumber = mediaFileUtility.getSequenceNumber();

          UserMemesEntity userMemesEntity = UserMemesEntity.builder()
              .userId(userId)
              .description(description)
              .sequenceNumber(sequenceNumber)
              .twitterMediaUrl(url)
              .build();

          userMemesRepository.save(userMemesEntity);

          String fileSuffix = url.substring(url.lastIndexOf('.'));
          if (fileSuffix.contains("?")) {
            fileSuffix = fileSuffix.substring(0, fileSuffix.indexOf('?'));
          }
          downloadFile(url, mediaFileUtility.getFileName(sequenceNumber, fileSuffix));
        }
        postTweetService
            .postTweet('@' + tweet.getJSONObject("user").getString("screen_name") + "✅️",
                tweet.getString("id_str"), null);
      } else {
        log.info("Received a learn request with an already in use description {} from userId {}",
            description, userId);
        postTweetService.postTweet("@" + tweet.getJSONObject("user").getString("screen_name")
                + " You already have a meme saved with that description", tweet.getString("id_str"),
            null);
      }
    } else {
      log.info("Received a learn request with no media attached");
    }
  }

  private List<String> getUrls(JSONObject tweet) {

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
      medias = getReplyToMedias(tweet);
    }

    // if no media found, return empty list
    else if (medias.isEmpty()) {
      return Collections.emptyList();
    }

    return getMediaUrl(medias);
  }

  private JSONArray getReplyToMedias(JSONObject originalTweet) {
    String inReplyToTweetId = originalTweet.getString("in_reply_to_status_id_str");

    JSONObject inReplyToTweet = getInReplyToTweet(inReplyToTweetId);

    if (inReplyToTweet.has("extended_entities")) {
      return inReplyToTweet.getJSONObject("extended_entities").getJSONArray("media");
    }
    return new JSONArray();
  }

  private JSONObject getInReplyToTweet(String tweetId) {
    RestTemplate restTemplate = new RestTemplate();
    String url = "https://api.twitter.com/1.1/statuses/show.json";

    String nonce = RandomStringUtils.randomAlphanumeric(42);
    String timestamp = Integer.toString((int) (new Date().getTime() / 1000));

    Map<String, String> signatureParams = new HashMap<>();
    signatureParams.put("tweet_mode", "extended");
    signatureParams.put("id", tweetId);

    String signature = signatureUtility
        .calculateStatusUpdateSignature(url, "GET", timestamp, nonce, signatureParams);

    HttpHeaders httpHeaders = new HttpHeaders();
    String authHeaderText = "OAuth oauth_consumer_key=\"" + consumerApiKey + "\", " +
        "oauth_nonce=\"" + nonce + "\", " +
        "oauth_signature=\"" + signature + "\", " +
        "oauth_signature_method=\"HMAC-SHA1\", " +
        "oauth_timestamp=\"" + timestamp + "\", " +
        "oauth_token=\"" + accessToken + "\", " +
        "oauth_version=\"1.0\"";
    httpHeaders.add("Authorization", authHeaderText);
    HttpEntity entity = new HttpEntity(httpHeaders);

    ResponseEntity<String> responseEntity = restTemplate
        .exchange(url + "?tweet_mode=extended&id=" + tweetId, HttpMethod.GET, entity, String.class);
    return new JSONObject(responseEntity.getBody());
  }

  private List<String> getMediaUrl(JSONArray medias) {
    List<String> urls = new ArrayList<>();
    for (int i = 0; i < medias.length(); i++) {
      JSONObject media = medias.getJSONObject(i);
      String twitterMediaUrl = "";
      if (media.getString("type").equals("video") || media.getString("type")
          .equals("animated_gif")) {
        twitterMediaUrl = getVideoUrl(media);
      } else {
        twitterMediaUrl = media.getString("media_url_https");
      }
      urls.add(twitterMediaUrl);
    }
    return urls;
  }

  private String getVideoUrl(JSONObject media) {
    JSONArray variantsArray = media
        .getJSONObject("video_info")
        .getJSONArray("variants");

    String twitterMediaUrl = "";
    int bitrate = 0;
    for (int j = 0; j < variantsArray.length(); j++) {
      if (((variantsArray.getJSONObject(j).getString("content_type").equals("video/mp4")) && (
          variantsArray.getJSONObject(j).getInt("bitrate") > bitrate))
          || j == variantsArray.length() - 1) {
        bitrate = variantsArray.getJSONObject(j).getInt("bitrate");
        twitterMediaUrl = variantsArray.getJSONObject(j).getString("url");
      }
    }
    if (twitterMediaUrl.equals("")) {
      twitterMediaUrl = variantsArray.getJSONObject(0).getString("url");
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