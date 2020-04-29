package org.arlevin.memeDatabaseBot.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.client.TwitterClient;
import org.arlevin.memeDatabaseBot.entity.UserMemesEntity;
import org.arlevin.memeDatabaseBot.repositories.UserMemesRepository;
import org.arlevin.memeDatabaseBot.services.TwitterMediaUploadService;
import org.arlevin.memeDatabaseBot.util.GetFilenameFromSequenceNumUtil;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProcessPostMemeMentionsService {

  @Value("${pathPrefix}")
  private String pathPrefix;

  private final UserMemesRepository userMemesRepository;
  private final TwitterMediaUploadService twitterMediaUploadService;
  private final TwitterClient twitterClient;

  public ProcessPostMemeMentionsService(final UserMemesRepository userMemesRepository,
      final TwitterMediaUploadService twitterMediaUploadService,
      final TwitterClient twitterClient) {
    this.userMemesRepository = userMemesRepository;
    this.twitterMediaUploadService = twitterMediaUploadService;
    this.twitterClient = twitterClient;
  }

  public void process(final JSONObject tweet, final String description) {
    final String replyToId = tweet.getString("id_str");
    final String authorId = tweet.getJSONObject("user").getString("id_str");
    final String authorScreenName = tweet.getJSONObject("user").getString("screen_name");

    final Optional<List<UserMemesEntity>> memeData = userMemesRepository
        .findAllByUserIdAndDescription(authorId, description);

    if (memeData.isPresent()) {
      log.info(
          "Received a post meme request mention with tweetId {} from userId {}, with description {}",
          replyToId, authorId, description);
      log.info("{} media files found for meme from userId {} with desciption {}", memeData.get().size(),
          authorId, description);

      final List<String> mediaIds = new ArrayList<>();
      for (final UserMemesEntity media : memeData.get()) {
        String fileSuffix = media.getTwitterMediaUrl()
            .substring(media.getTwitterMediaUrl().lastIndexOf('.'));
        if (fileSuffix.contains("?")) {
          fileSuffix = fileSuffix.substring(0, fileSuffix.indexOf('?'));
        }

        final String fileName =
            pathPrefix + GetFilenameFromSequenceNumUtil.getFileName(media.getSequenceNumber(), fileSuffix);

        log.info("Uploading media file from {} to twitter", fileName);
        final String mediaId = twitterMediaUploadService.uploadMedia(fileName, media.getIsGif());
        log.info("Successfully uploaded media file from {} to twitter and received back mediaId {}",
            fileName, mediaId);
        mediaIds.add(mediaId);
      }
      if (!mediaIds.isEmpty()) {
        log.info(
            "Succesfully uploaded {} media file(s), posting response tweet with media in response to tweetId {} from userId {}",
            mediaIds.size(), replyToId, authorId);

        final Map<String, String> params = new HashMap<>();
        params.put("status", "@" + authorScreenName);
        params.put("in_reply_to_status_id", replyToId);
        params.put("include_entities", "true");
        params.put("media_ids", String.join(",", mediaIds));

        twitterClient.makeRequest(HttpMethod.POST, "/1.1/statuses/update.json", params);

        log.info(
            "Successfully posted meme in response to tweetId {} from userId {} with description {}",
            replyToId, authorId, description);
      }
    } else {
      log.info(
          "Received a post meme request mention for unlearned meme with tweetId {} from userId {} with description {}",
          replyToId, authorId, description);

      final Map<String, String> params = new HashMap<>();
      params.put("status",
          "@" + authorScreenName + " Sorry, I haven't learned that meme from you yet");
      params.put("in_reply_to_status_id", replyToId);
      params.put("include_entities", "true");

      twitterClient.makeRequest(HttpMethod.POST, "/1.1/statuses/update.json", params);

      log.info(
          "Successfully posted failure response to tweetId {} from userId {} with description {}",
          replyToId, authorId, description);
    }
  }
}
