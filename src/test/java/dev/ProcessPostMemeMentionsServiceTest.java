package dev;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.arlevin.memedatabasebot.client.TwitterClient;
import org.arlevin.memedatabasebot.entity.UserMemesEntity;
import org.arlevin.memedatabasebot.repository.UserMemesRepository;
import org.arlevin.memedatabasebot.service.ProcessPostMemeMentionsService;
import org.arlevin.memedatabasebot.service.TwitterMediaUploadService;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class ProcessPostMemeMentionsServiceTest {

  private final UserMemesRepository userMemesRepository;
  private final TwitterMediaUploadService twitterMediaUploadService;
  private final TwitterClient twitterClient;
  private final ProcessPostMemeMentionsService processPostMemeMentionsService;

  public ProcessPostMemeMentionsServiceTest() {
    this.userMemesRepository = mock(UserMemesRepository.class);
    this.twitterMediaUploadService = mock(TwitterMediaUploadService.class);
    this.twitterClient = mock(TwitterClient.class);
    this.processPostMemeMentionsService = new ProcessPostMemeMentionsService(userMemesRepository,
        twitterMediaUploadService, twitterClient);
  }

  @Test
  public void process_withValidUnLearnedRequest_updateStatus() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "tweet1")
        .put("user", new JSONObject()
            .put("id_str", "user1")
            .put("screen_name", "username"));

    when(userMemesRepository.findAllByUserIdAndDescription("user1", "test"))
        .thenReturn(Optional.empty());

    processPostMemeMentionsService.process(tweet, "test");

    verify(twitterClient)
        .makeUpdateStatusRequest("@username Sorry, I haven't learned that meme from you yet",
            "tweet1", null);
  }

  @Test
  public void process_withValidLearnedRequest_retrieveSequenceNumberFromDatabaseAndPostMeme() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "tweet1")
        .put("user", new JSONObject()
            .put("id_str", "user1")
            .put("screen_name", "username"));

    final Optional<List<UserMemesEntity>> userMemesEntityListOptional = Optional
        .of(Collections.singletonList(UserMemesEntity.builder()
            .description("test")
            .isGif(false)
            .twitterMediaUrl("https://www.test.com/img.jpeg?metadata=true")
            .sequenceNumber("1")
            .userId("1")
            .build()));

    when(twitterMediaUploadService.uploadMedia(anyString(), eq(false)))
        .thenReturn("1");

    when(userMemesRepository.findAllByUserIdAndDescription("user1", "test"))
        .thenReturn(userMemesEntityListOptional);

    processPostMemeMentionsService.process(tweet, "test");

    verify(twitterClient)
        .makeUpdateStatusRequest("@username", "tweet1", "1");
  }

  @Test
  public void process_withValidLearnedRequest_unableToUploadMedia_doNotUpdateStatus() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "tweet1")
        .put("user", new JSONObject()
            .put("id_str", "user1")
            .put("screen_name", "username"));

    final Optional<List<UserMemesEntity>> userMemesEntityListOptional = Optional
        .of(Collections.singletonList(UserMemesEntity.builder()
            .description("test")
            .isGif(false)
            .twitterMediaUrl("https://www.test.com/img.jpeg?metadata=true")
            .sequenceNumber("1")
            .userId("1")
            .build()));

    when(userMemesRepository.findAllByUserIdAndDescription("user1", "test"))
        .thenReturn(userMemesEntityListOptional);

    processPostMemeMentionsService.process(tweet, "test");

    verifyZeroInteractions(twitterClient);
  }
}
