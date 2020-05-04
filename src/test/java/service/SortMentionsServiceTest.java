package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Collections;
import java.util.List;
import org.arlevin.memedatabasebot.entity.ProcessedMentionsEntity;
import org.arlevin.memedatabasebot.repository.ProcessedMentionsRepository;
import org.arlevin.memedatabasebot.service.ProcessLearnMemeMentionsService;
import org.arlevin.memedatabasebot.service.ProcessPostMemeMentionsService;
import org.arlevin.memedatabasebot.service.SortMentionsService;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SortMentionsServiceTest {

  private final ProcessedMentionsRepository processedMentionsRepository;
  private final ProcessLearnMemeMentionsService processLearnMemeMentionsService;
  private final ProcessPostMemeMentionsService processPostMemeMentionsService;
  private final SortMentionsService sortMentionsService;

  public SortMentionsServiceTest() {
    this.processedMentionsRepository = mock(ProcessedMentionsRepository.class);
    this.processLearnMemeMentionsService = mock(ProcessLearnMemeMentionsService.class);
    this.processPostMemeMentionsService = mock(ProcessPostMemeMentionsService.class);
    this.sortMentionsService = new SortMentionsService(processedMentionsRepository,
        processLearnMemeMentionsService, processPostMemeMentionsService);
  }

  @Test
  public void process_withValidLearnMention_saveAsProcessedAndCallLearnService() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "1")
        .put("full_text", "@memestorebot learn test")
        .put("is_quote_status", false);
    final List<JSONObject> tweetList = Collections.singletonList(tweet);

    sortMentionsService.process(tweetList);

    final ProcessedMentionsEntity expectedProcessedMentionEntity = ProcessedMentionsEntity.builder()
        .tweetId("1").build();

    final ArgumentCaptor<ProcessedMentionsEntity> processedMentionsEntityArgumentCaptor = ArgumentCaptor
        .forClass(ProcessedMentionsEntity.class);
    final ArgumentCaptor<JSONObject> tweetArgumentCaptor = ArgumentCaptor
        .forClass(JSONObject.class);
    final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    verify(processedMentionsRepository).save(processedMentionsEntityArgumentCaptor.capture());
    assertEquals(expectedProcessedMentionEntity.getTweetId(),
        processedMentionsEntityArgumentCaptor.getValue().getTweetId());
    assertEquals(expectedProcessedMentionEntity.getProcessedTimestamp(),
        processedMentionsEntityArgumentCaptor.getValue().getProcessedTimestamp());

    verify(processLearnMemeMentionsService)
        .process(tweetArgumentCaptor.capture(), stringArgumentCaptor.capture());
    assertEquals(tweet.get("id_str"), tweetArgumentCaptor.getValue().get("id_str"));
    assertEquals(tweet.get("full_text"), tweetArgumentCaptor.getValue().get("full_text"));
    assertEquals(tweet.get("is_quote_status"),
        tweetArgumentCaptor.getValue().get("is_quote_status"));
    assertEquals("test", stringArgumentCaptor.getValue());

    verifyZeroInteractions(processPostMemeMentionsService);
  }

  @Test
  public void process_withValidPostMention_isNotQuoteStatus_saveAsProcessedAndCallPostService() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "1")
        .put("full_text", "@memestorebot post test")
        .put("is_quote_status", false);
    final List<JSONObject> tweetList = Collections.singletonList(tweet);

    sortMentionsService.process(tweetList);

    final ProcessedMentionsEntity expectedProcessedMentionEntity = ProcessedMentionsEntity.builder()
        .tweetId("1").build();

    final ArgumentCaptor<ProcessedMentionsEntity> processedMentionsEntityArgumentCaptor = ArgumentCaptor
        .forClass(ProcessedMentionsEntity.class);
    final ArgumentCaptor<JSONObject> tweetArgumentCaptor = ArgumentCaptor
        .forClass(JSONObject.class);
    final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    verify(processedMentionsRepository).save(processedMentionsEntityArgumentCaptor.capture());
    assertEquals(expectedProcessedMentionEntity.getTweetId(),
        processedMentionsEntityArgumentCaptor.getValue().getTweetId());
    assertEquals(expectedProcessedMentionEntity.getProcessedTimestamp(),
        processedMentionsEntityArgumentCaptor.getValue().getProcessedTimestamp());

    verify(processPostMemeMentionsService)
        .process(tweetArgumentCaptor.capture(), stringArgumentCaptor.capture());
    assertEquals(tweet.get("id_str"), tweetArgumentCaptor.getValue().get("id_str"));
    assertEquals(tweet.get("full_text"), tweetArgumentCaptor.getValue().get("full_text"));
    assertEquals(tweet.get("is_quote_status"),
        tweetArgumentCaptor.getValue().get("is_quote_status"));
    assertEquals("test", stringArgumentCaptor.getValue());

    verifyZeroInteractions(processLearnMemeMentionsService);
  }

  @Test
  public void process_withValidPostMention_isQuoteStatus_saveAsProcessedAndCallPostService() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "1")
        .put("full_text", "@memestorebot post test https://t.co/52341")
        .put("is_quote_status", true);
    final List<JSONObject> tweetList = Collections.singletonList(tweet);

    sortMentionsService.process(tweetList);

    final ProcessedMentionsEntity expectedProcessedMentionEntity = ProcessedMentionsEntity.builder()
        .tweetId("1").build();

    final ArgumentCaptor<ProcessedMentionsEntity> processedMentionsEntityArgumentCaptor = ArgumentCaptor
        .forClass(ProcessedMentionsEntity.class);
    final ArgumentCaptor<JSONObject> tweetArgumentCaptor = ArgumentCaptor
        .forClass(JSONObject.class);
    final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    verify(processedMentionsRepository).save(processedMentionsEntityArgumentCaptor.capture());
    assertEquals(expectedProcessedMentionEntity.getTweetId(),
        processedMentionsEntityArgumentCaptor.getValue().getTweetId());
    assertEquals(expectedProcessedMentionEntity.getProcessedTimestamp(),
        processedMentionsEntityArgumentCaptor.getValue().getProcessedTimestamp());

    verify(processPostMemeMentionsService)
        .process(tweetArgumentCaptor.capture(), stringArgumentCaptor.capture());
    assertEquals(tweet.get("id_str"), tweetArgumentCaptor.getValue().get("id_str"));
    assertEquals(tweet.get("full_text"), tweetArgumentCaptor.getValue().get("full_text"));
    assertEquals(tweet.get("is_quote_status"),
        tweetArgumentCaptor.getValue().get("is_quote_status"));
    assertEquals("test", stringArgumentCaptor.getValue());

    verifyZeroInteractions(processLearnMemeMentionsService);
  }

  @Test
  public void process_withInvalidRequest_saveAsProcessed() {
    final JSONObject tweet = new JSONObject()
        .put("id_str", "1")
        .put("full_text", "@memestorebot invalid request")
        .put("is_quote_status", false);
    final List<JSONObject> tweetList = Collections.singletonList(tweet);

    sortMentionsService.process(tweetList);

    final ProcessedMentionsEntity expectedProcessedMentionEntity = ProcessedMentionsEntity.builder()
        .tweetId("1").build();

    final ArgumentCaptor<ProcessedMentionsEntity> processedMentionsEntityArgumentCaptor = ArgumentCaptor
        .forClass(ProcessedMentionsEntity.class);
    verify(processedMentionsRepository).save(processedMentionsEntityArgumentCaptor.capture());
    assertEquals(expectedProcessedMentionEntity.getTweetId(),
        processedMentionsEntityArgumentCaptor.getValue().getTweetId());
    assertEquals(expectedProcessedMentionEntity.getProcessedTimestamp(),
        processedMentionsEntityArgumentCaptor.getValue().getProcessedTimestamp());

    verifyZeroInteractions(processLearnMemeMentionsService);
    verifyZeroInteractions(processPostMemeMentionsService);
  }
}
