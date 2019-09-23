package org.arlevin.memeDatabaseBot.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.domain.UserMemesEntity;
import org.arlevin.memeDatabaseBot.repositories.UserMemesRepository;
import org.arlevin.memeDatabaseBot.services.PostTweetService;
import org.arlevin.memeDatabaseBot.services.TwitterMediaUploadService;
import org.arlevin.memeDatabaseBot.utilities.MediaFileUtility;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PostMentionsProcessor {

  private final UserMemesRepository userMemesRepository;
  private final MediaFileUtility mediaFileUtility;
  private final TwitterMediaUploadService twitterMediaUploadService;
  private final PostTweetService postTweetService;

  public PostMentionsProcessor(UserMemesRepository userMemesRepository,
      MediaFileUtility mediaFileUtility, TwitterMediaUploadService twitterMediaUploadService,
      PostTweetService postTweetService) {
    this.userMemesRepository = userMemesRepository;
    this.mediaFileUtility = mediaFileUtility;
    this.twitterMediaUploadService = twitterMediaUploadService;
    this.postTweetService = postTweetService;
  }

  void process(JSONObject tweet, String description) {
    String replyToId = tweet.getString("id_str");
    String authorId = tweet.getJSONObject("user").getString("id_str");
    String authorScreenName = tweet.getJSONObject("user").getString("screen_name");
    Optional<List<UserMemesEntity>> memeData = userMemesRepository
        .findAllByUserIdAndDescription(authorId, description);
    if (memeData.isPresent()) {
      List<String> mediaIds = new ArrayList<>();
      for (UserMemesEntity media : memeData.get()) {
        String fileSuffix = media.getTwitterMediaUrl()
            .substring(media.getTwitterMediaUrl().lastIndexOf('.'));
        if (fileSuffix.contains("?")) {
          fileSuffix = fileSuffix.substring(0, fileSuffix.indexOf('?'));
        }

        String fileName = mediaFileUtility.getFileName(media.getSequenceNumber(), fileSuffix);

        String mediaId = twitterMediaUploadService.uploadMedia(fileName, media.getIsGif());
        mediaIds.add(mediaId);
      }
      if (!mediaIds.isEmpty()) {
        postTweetService.postTweet("@" + authorScreenName, replyToId, mediaIds);
      }
    } else {
      postTweetService.postTweet("@" + authorScreenName + " Sorry, I haven't learned that meme from you yet", replyToId, null);
    }
  }
}
