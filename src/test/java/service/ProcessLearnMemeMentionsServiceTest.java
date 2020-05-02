package service;

import static org.arlevin.memedatabasebot.constant.StringConstants.BITRATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.arlevin.memedatabasebot.client.TwitterClient;
import org.arlevin.memedatabasebot.entity.SequenceNumberEntity;
import org.arlevin.memedatabasebot.entity.UserMemesEntity;
import org.arlevin.memedatabasebot.repository.SequenceNumberRepository;
import org.arlevin.memedatabasebot.repository.UserMemesRepository;
import org.arlevin.memedatabasebot.service.ProcessLearnMemeMentionsService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

public class ProcessLearnMemeMentionsServiceTest {

  private final UserMemesRepository userMemesRepository;
  private final SequenceNumberRepository sequenceNumberRepository;
  private final TwitterClient twitterClient;
  private final ProcessLearnMemeMentionsService processLearnMemeMentionsService;

  public ProcessLearnMemeMentionsServiceTest() {
    this.userMemesRepository = mock(UserMemesRepository.class);
    this.sequenceNumberRepository = mock(SequenceNumberRepository.class);
    this.twitterClient = mock(TwitterClient.class);
    this.processLearnMemeMentionsService = new ProcessLearnMemeMentionsService(userMemesRepository,
        sequenceNumberRepository, twitterClient);

    String path = "src/test/resources/writeToFolder";
    File file = new File(path);
    String absolutePath = file.getAbsolutePath();
    ReflectionTestUtils.setField(processLearnMemeMentionsService, "pathPrefix", absolutePath);
  }

  @Test
  public void process_withNewLearnRequest_withExtendedEntitiesWithVideoWithMP4_saveToDbAndUpdateStatus() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "tweet1")
        .put("user", new JSONObject()
            .put("id_str", "user1")
            .put("screen_name", "username"))
        .put("extended_entities", new JSONObject()
            .put("media", new JSONArray()
                .put(new JSONObject()
                    .put("type", "video")
                    .put("video_info", new JSONObject()
                        .put("variants", new JSONArray()
                            .put(new JSONObject()
                                .put("content_type", "video/mp4")
                                .put(BITRATE, 1)
                                .put("url",
                                    "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4?hello=true")))))));

    when(sequenceNumberRepository.save(eq(new SequenceNumberEntity())))
        .thenReturn(new SequenceNumberEntity(2L));

    processLearnMemeMentionsService.process(tweet, "test");

    final ArgumentCaptor<UserMemesEntity> userMemesEntityArgumentCaptor = ArgumentCaptor
        .forClass(UserMemesEntity.class);
    verify(userMemesRepository).save(userMemesEntityArgumentCaptor.capture());
    assertEquals("test", userMemesEntityArgumentCaptor.getValue().getDescription());
    assertEquals(false, userMemesEntityArgumentCaptor.getValue().getIsGif());
    assertEquals("2", userMemesEntityArgumentCaptor.getValue().getSequenceNumber());
    assertEquals("user1", userMemesEntityArgumentCaptor.getValue().getUserId());
    assertEquals("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4?hello=true",
        userMemesEntityArgumentCaptor.getValue().getTwitterMediaUrl());

    verify(twitterClient).makeUpdateStatusRequest("@username✅️", "tweet1", null);
  }

  @Test
  public void process_withNewLearnRequest_withQuotedStatusWithExtendedEntitiesWithAnimatedGifWithNonMP4WithBitrate_saveToDbAndUpdateStatus() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "tweet1")
        .put("user", new JSONObject()
            .put("id_str", "user1")
            .put("screen_name", "username"))
        .put("quoted_status", new JSONObject()
            .put("extended_entities", new JSONObject()
                .put("media", new JSONArray()
                    .put(new JSONObject()
                        .put("type", "animated_gif")
                        .put("video_info", new JSONObject()
                            .put("variants", new JSONArray()
                                .put(new JSONObject()
                                    .put("content_type", "other/mp4")
                                    .put(BITRATE, 1)
                                    .put("url",
                                        "https://upload.wikimedia.org/wikipedia/commons/2/2c/Rotating_earth_%28large%29.gif"))))))));

    when(sequenceNumberRepository.save(eq(new SequenceNumberEntity())))
        .thenReturn(new SequenceNumberEntity(3L));

    processLearnMemeMentionsService.process(tweet, "test");

    final ArgumentCaptor<UserMemesEntity> userMemesEntityArgumentCaptor = ArgumentCaptor
        .forClass(UserMemesEntity.class);
    verify(userMemesRepository).save(userMemesEntityArgumentCaptor.capture());
    assertEquals("test", userMemesEntityArgumentCaptor.getValue().getDescription());
    assertEquals(true, userMemesEntityArgumentCaptor.getValue().getIsGif());
    assertEquals("3", userMemesEntityArgumentCaptor.getValue().getSequenceNumber());
    assertEquals("user1", userMemesEntityArgumentCaptor.getValue().getUserId());
    assertEquals("https://upload.wikimedia.org/wikipedia/commons/2/2c/Rotating_earth_%28large%29.gif",
        userMemesEntityArgumentCaptor.getValue().getTwitterMediaUrl());

    verify(twitterClient).makeUpdateStatusRequest("@username✅️", "tweet1", null);
  }

  @Test
  public void process_withNewLearnRequest_withQuotedStatusWithExtendedEntitiesWithAnimatedGifWithNonMP4WithOutBitrate_saveToDbAndUpdateStatus() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "tweet1")
        .put("user", new JSONObject()
            .put("id_str", "user1")
            .put("screen_name", "username"))
        .put("quoted_status", new JSONObject()
            .put("extended_entities", new JSONObject()
                .put("media", new JSONArray()
                    .put(new JSONObject()
                        .put("type", "animated_gif")
                        .put("video_info", new JSONObject()
                            .put("variants", new JSONArray()
                                .put(new JSONObject()
                                    .put("content_type", "other/mp4")
                                    .put("url",
                                        "https://upload.wikimedia.org/wikipedia/commons/2/2c/Rotating_earth_%28large%29.gif"))))))));

    when(sequenceNumberRepository.save(eq(new SequenceNumberEntity())))
        .thenReturn(new SequenceNumberEntity(3L));

    processLearnMemeMentionsService.process(tweet, "test");

    final ArgumentCaptor<UserMemesEntity> userMemesEntityArgumentCaptor = ArgumentCaptor
        .forClass(UserMemesEntity.class);
    verify(userMemesRepository).save(userMemesEntityArgumentCaptor.capture());
    assertEquals("test", userMemesEntityArgumentCaptor.getValue().getDescription());
    assertEquals(true, userMemesEntityArgumentCaptor.getValue().getIsGif());
    assertEquals("3", userMemesEntityArgumentCaptor.getValue().getSequenceNumber());
    assertEquals("user1", userMemesEntityArgumentCaptor.getValue().getUserId());
    assertEquals("https://upload.wikimedia.org/wikipedia/commons/2/2c/Rotating_earth_%28large%29.gif",
        userMemesEntityArgumentCaptor.getValue().getTwitterMediaUrl());

    verify(twitterClient).makeUpdateStatusRequest("@username✅️", "tweet1", null);
  }

  @Test
  public void process_withNewLearnRequest_withQuotedStatusWithExtendedEntitiesWithOtherType_saveToDbAndUpdateStatus() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "tweet1")
        .put("user", new JSONObject()
            .put("id_str", "user1")
            .put("screen_name", "username"))
        .put("quoted_status", new JSONObject()
            .put("extended_entities", new JSONObject()
                .put("media", new JSONArray()
                    .put(new JSONObject()
                        .put("type", "other")
                        .put("media_url_https",
                            "https://arlevin.org/static/media/react.97893603.jpg")))));

    when(sequenceNumberRepository.save(eq(new SequenceNumberEntity())))
        .thenReturn(new SequenceNumberEntity(1L));

    processLearnMemeMentionsService.process(tweet, "test");

    final ArgumentCaptor<UserMemesEntity> userMemesEntityArgumentCaptor = ArgumentCaptor
        .forClass(UserMemesEntity.class);
    verify(userMemesRepository).save(userMemesEntityArgumentCaptor.capture());
    assertEquals("test", userMemesEntityArgumentCaptor.getValue().getDescription());
    assertEquals(false, userMemesEntityArgumentCaptor.getValue().getIsGif());
    assertEquals("1", userMemesEntityArgumentCaptor.getValue().getSequenceNumber());
    assertEquals("user1", userMemesEntityArgumentCaptor.getValue().getUserId());
    assertEquals("https://arlevin.org/static/media/react.97893603.jpg",
        userMemesEntityArgumentCaptor.getValue().getTwitterMediaUrl());

    verify(twitterClient).makeUpdateStatusRequest("@username✅️", "tweet1", null);
  }

  @Test
  public void process_withNewLearnRequest_withInReplyToStatusWithExtendedEntitiesWithOtherType_saveToDbAndUpdateStatus() {
    final JSONObject tweet = new JSONObject()
        .put("in_reply_to_status_id_str", "1")
        .put("id_str", "tweet1")
        .put("user", new JSONObject()
            .put("id_str", "user1")
            .put("screen_name", "username"));

    final Map<String, String> params = new HashMap<>();
    params.put("tweet_mode", "extended");
    params.put("id", "1");

    when(twitterClient.makeRequest(GET, "https://api.twitter.com/1.1/statuses/show.json", params))
        .thenReturn(ResponseEntity.ok(new JSONObject()
            .put("extended_entities", new JSONObject()
                .put("media", new JSONArray()
                    .put(new JSONObject()
                        .put("type", "other")
                        .put("media_url_https",
                            "https://arlevin.org/static/media/react.97893603.jpg"))))
            .toString()));

    when(sequenceNumberRepository.save(eq(new SequenceNumberEntity())))
        .thenReturn(new SequenceNumberEntity(1L));

    processLearnMemeMentionsService.process(tweet, "test");

    final ArgumentCaptor<UserMemesEntity> userMemesEntityArgumentCaptor = ArgumentCaptor
        .forClass(UserMemesEntity.class);
    verify(userMemesRepository).save(userMemesEntityArgumentCaptor.capture());
    assertEquals("test", userMemesEntityArgumentCaptor.getValue().getDescription());
    assertEquals(false, userMemesEntityArgumentCaptor.getValue().getIsGif());
    assertEquals("1", userMemesEntityArgumentCaptor.getValue().getSequenceNumber());
    assertEquals("user1", userMemesEntityArgumentCaptor.getValue().getUserId());
    assertEquals("https://arlevin.org/static/media/react.97893603.jpg",
        userMemesEntityArgumentCaptor.getValue().getTwitterMediaUrl());

    verify(twitterClient).makeUpdateStatusRequest("@username✅️", "tweet1", null);
  }

  @Test
  public void process_withNewLearnRequest_withInReplyToStatusWithOutExtendedEntities_doNotSaveToDbDoNotUpdateStatus() {
    final JSONObject tweet = new JSONObject()
        .put("in_reply_to_status_id_str", "1")
        .put("id_str", "tweet1")
        .put("user", new JSONObject()
            .put("id_str", "user1")
            .put("screen_name", "username"));

    final Map<String, String> params = new HashMap<>();
    params.put("tweet_mode", "extended");
    params.put("id", "1");

    when(twitterClient.makeRequest(GET, "https://api.twitter.com/1.1/statuses/show.json", params))
        .thenReturn(ResponseEntity.ok("{}"));

    when(sequenceNumberRepository.save(eq(new SequenceNumberEntity())))
        .thenReturn(new SequenceNumberEntity(1L));

    processLearnMemeMentionsService.process(tweet, "test");

    verifyZeroInteractions(userMemesRepository);
    verify(twitterClient)
        .makeRequest(GET, "https://api.twitter.com/1.1/statuses/show.json", params);
    verify(twitterClient, never()).makeUpdateStatusRequest(any(), any(), any());
  }

  @Test
  public void process_withNewLearnRequest_withExtendedEntitiesWithEmptyMedia_doNotSaveToDbDoNotUpdateStatus() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "tweet1")
        .put("user", new JSONObject()
            .put("id_str", "user1")
            .put("screen_name", "username"));

    when(sequenceNumberRepository.save(eq(new SequenceNumberEntity())))
        .thenReturn(new SequenceNumberEntity(1L));

    processLearnMemeMentionsService.process(tweet, "test");

    verifyZeroInteractions(userMemesRepository);
    verifyZeroInteractions(twitterClient);
  }

  @Test
  public void process_withNewLearnRequestWithAlreadyUsedDescription_doNotSaveToDbAndUpateStatus() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "tweet1")
        .put("user", new JSONObject()
            .put("id_str", "user1")
            .put("screen_name", "username"))
        .put("extended_entities", new JSONObject()
            .put("media", new JSONArray()
                .put(new JSONObject()
                    .put("type", "video")
                    .put("video_info", new JSONObject()
                        .put("variants", new JSONArray()
                            .put(new JSONObject()
                                .put("content_type", "video/mp4")
                                .put(BITRATE, 1)
                                .put("url",
                                    "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4?hello=true")))))));

    when(sequenceNumberRepository.save(eq(new SequenceNumberEntity())))
        .thenReturn(new SequenceNumberEntity(1L));

    when(userMemesRepository.findAllByUserIdAndDescription("user1", "test"))
        .thenReturn(Optional.of(Collections.singletonList(
            new UserMemesEntity("1", "test", "1",
                "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4?hello=true",
                false))));

    processLearnMemeMentionsService.process(tweet, "test");

    verify(userMemesRepository, never()).save(any());
    verify(twitterClient)
        .makeUpdateStatusRequest("@username You already have a meme saved with that description",
            "tweet1", null);
  }
}
