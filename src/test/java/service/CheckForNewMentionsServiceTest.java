package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.arlevin.memedatabasebot.client.TwitterClient;
import org.arlevin.memedatabasebot.entity.ProcessedMentionsEntity;
import org.arlevin.memedatabasebot.repository.ProcessedMentionsRepository;
import org.arlevin.memedatabasebot.service.CheckForNewMentionsService;
import org.arlevin.memedatabasebot.service.SortMentionsService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

public class CheckForNewMentionsServiceTest {

  private final ProcessedMentionsRepository processedMentionsRepository;
  private final SortMentionsService sortMentionsService;
  private final TwitterClient twitterClient;
  private final CheckForNewMentionsService checkForNewMentionsService;
  private final static String GET_MENTIONS_BASE_URL = "https://api.twitter.com/1.1/statuses/mentions_timeline.json";
  final Map<String, String> expectedParams;

  public CheckForNewMentionsServiceTest() {
    this.processedMentionsRepository = mock(ProcessedMentionsRepository.class);
    this.sortMentionsService = mock(SortMentionsService.class);
    this.twitterClient = mock(TwitterClient.class);
    this.checkForNewMentionsService = new CheckForNewMentionsService(processedMentionsRepository,
        sortMentionsService, twitterClient);
    expectedParams = new HashMap<>();
    expectedParams.put("tweet_mode", "extended");
  }

  @Test
  public void checkForNewMentions_withNoMentionsReturned_callSortServiceWithEmptyList() {

    when(twitterClient.makeRequest(GET, GET_MENTIONS_BASE_URL, expectedParams)).thenReturn(
        ResponseEntity.ok("[]"));

    checkForNewMentionsService.checkForNewMentions();

    verify(twitterClient)
        .makeRequest(GET, "https://api.twitter.com/1.1/statuses/mentions_timeline.json",
            expectedParams);
    verifyZeroInteractions(sortMentionsService);
  }

  @Test
  public void checkForNewMentions_with1NewMentionReturned_callSortServiceWithSingletonList() {

    final JSONObject tweet = new JSONObject().put("id_str", "1");
    final List<JSONObject> tweetsList = Collections.singletonList(tweet);
    final String responseString = new JSONArray(tweetsList).toString();

    when(twitterClient.makeRequest(GET, GET_MENTIONS_BASE_URL, expectedParams)).thenReturn(
        ResponseEntity.ok(responseString));
    when(processedMentionsRepository.getAllByTweetId("1")).thenReturn(Optional.empty());

    checkForNewMentionsService.checkForNewMentions();

    verify(twitterClient)
        .makeRequest(GET, "https://api.twitter.com/1.1/statuses/mentions_timeline.json",
            expectedParams);

    final ArgumentCaptor<ArrayList<JSONObject>> processArgumentCaptor = ArgumentCaptor
        .forClass(ArrayList.class);
    verify(sortMentionsService).process(processArgumentCaptor.capture());
    assertEquals(1, processArgumentCaptor.getValue().size());
    assertEquals(tweetsList.toString(), processArgumentCaptor.getValue().toString());
  }

  @Test
  public void checkForNewMentions_with1NewAnd1OldMentionReturned_callSortServiceWithSingletonList() {

    final JSONObject tweet1 = new JSONObject().put("id_str", "1");
    final JSONObject tweet2 = new JSONObject().put("id_str", "2");
    final List<JSONObject> tweetsList = Arrays.asList(tweet1, tweet2);
    final String responseString = new JSONArray(tweetsList).toString();

    when(twitterClient.makeRequest(GET, GET_MENTIONS_BASE_URL, expectedParams)).thenReturn(
        ResponseEntity.ok(responseString));
    when(processedMentionsRepository.getAllByTweetId("1")).thenReturn(Optional.empty());
    when(processedMentionsRepository.getAllByTweetId("2"))
        .thenReturn(Optional.of(new ProcessedMentionsEntity()));

    checkForNewMentionsService.checkForNewMentions();

    verify(twitterClient)
        .makeRequest(GET, "https://api.twitter.com/1.1/statuses/mentions_timeline.json",
            expectedParams);

    final ArgumentCaptor<ArrayList<JSONObject>> processArgumentCaptor = ArgumentCaptor
        .forClass(ArrayList.class);
    verify(sortMentionsService).process(processArgumentCaptor.capture());
    assertEquals(1, processArgumentCaptor.getValue().size());
    assertEquals(Collections.singletonList(tweet1).toString(),
        processArgumentCaptor.getValue().toString());
  }

  @Test
  public void checkForNewMentions_with1OldMentionReturned_dontCallSortMentions() {

    final JSONObject tweet1 = new JSONObject().put("id_str", "1");
    final List<JSONObject> tweetsList = Collections.singletonList(tweet1);
    final String responseString = new JSONArray(tweetsList).toString();

    when(twitterClient.makeRequest(GET, GET_MENTIONS_BASE_URL, expectedParams)).thenReturn(
        ResponseEntity.ok(responseString));
    when(processedMentionsRepository.getAllByTweetId("1"))
        .thenReturn(Optional.of(new ProcessedMentionsEntity()));

    checkForNewMentionsService.checkForNewMentions();

    verify(twitterClient)
        .makeRequest(GET, "https://api.twitter.com/1.1/statuses/mentions_timeline.json",
            expectedParams);

    verifyZeroInteractions(sortMentionsService);
  }
}
