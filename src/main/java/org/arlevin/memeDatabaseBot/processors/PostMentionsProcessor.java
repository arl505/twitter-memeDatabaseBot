package org.arlevin.memeDatabaseBot.processors;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.domain.UserMemesEntity;
import org.arlevin.memeDatabaseBot.repositories.UserMemesRepository;
import org.arlevin.memeDatabaseBot.utilities.MediaFileUtility;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PostMentionsProcessor {

  private final UserMemesRepository userMemesRepository;
  private final MediaFileUtility mediaFileUtility;

  public PostMentionsProcessor(UserMemesRepository userMemesRepository,
      MediaFileUtility mediaFileUtility) {
    this.userMemesRepository = userMemesRepository;
    this.mediaFileUtility = mediaFileUtility;
  }

  void process(JSONObject tweet, String description) {
    String authorId = tweet.getJSONObject("user").getString("id_str");
    Optional<List<UserMemesEntity>> memeData = userMemesRepository
        .findAllByUserIdAndDescription(authorId, description);
    if (memeData.isPresent()) {
      for (UserMemesEntity media : memeData.get()) {
        String fileSuffix = media.getTwitterMediaUrl()
            .substring(media.getTwitterMediaUrl().lastIndexOf('.'));
        String fileName = mediaFileUtility.getFileName(media.getSequenceNumber(), fileSuffix);


      }
    }
  }
}
