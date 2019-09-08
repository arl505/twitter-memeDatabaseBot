package org.arlevin.memeDatabaseBot.processors;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.domain.UserMemesEntity;
import org.arlevin.memeDatabaseBot.repositories.UserMemesRepository;
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

  public PostMentionsProcessor(UserMemesRepository userMemesRepository,
      MediaFileUtility mediaFileUtility, TwitterMediaUploadService twitterMediaUploadService) {
    this.userMemesRepository = userMemesRepository;
    this.mediaFileUtility = mediaFileUtility;
    this.twitterMediaUploadService = twitterMediaUploadService;
  }

  void process(JSONObject tweet, String description) {
    String authorId = tweet.getJSONObject("user").getString("id_str");
    Optional<List<UserMemesEntity>> memeData = userMemesRepository
        .findAllByUserIdAndDescription(authorId, description);
    if (memeData.isPresent()) {
      for (UserMemesEntity media : memeData.get()) {
        String fileSuffix = media.getTwitterMediaUrl()
            .substring(media.getTwitterMediaUrl().lastIndexOf('.'));
        if (fileSuffix.contains("?")) {
          fileSuffix = fileSuffix.substring(0, fileSuffix.indexOf('?'));
        }

        String fileName = mediaFileUtility.getFileName(media.getSequenceNumber(), fileSuffix);

        twitterMediaUploadService.uploadMedia(fileName);
      }
    }
    else {
      log.info("Received a post request for an unlearned meme");
    }
  }
}
