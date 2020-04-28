package org.arlevin.memeDatabaseBot.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.client.TwitterClient;
import org.arlevin.memeDatabaseBot.entity.ProcessedMentionsEntity;
import org.arlevin.memeDatabaseBot.processors.MentionsProcessor;
import org.arlevin.memeDatabaseBot.repositories.ProcessedMentionsRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CheckForNewMentionsService {

  private static final String GET_MENTIONS_API_PATH = "/1.1/statuses/mentions_timeline.json";

  private final ProcessedMentionsRepository processedMentionsRepository;
  private final MentionsProcessor mentionsProcessor;
  private final TwitterClient twitterClient;

  public CheckForNewMentionsService(final ProcessedMentionsRepository processedMentionsRepository,
      final MentionsProcessor mentionsProcessor, final TwitterClient twitterClient) {
    this.processedMentionsRepository = processedMentionsRepository;
    this.mentionsProcessor = mentionsProcessor;
    this.twitterClient = twitterClient;
  }

  @Scheduled(fixedRate = 15000)
  public void checkForNewMentions() {

    final Map<String, String> signatureParams = new HashMap<>();
    signatureParams.put("tweet_mode", "extended");

    final ResponseEntity<String> responseEntity = twitterClient
        .makeRequest(HttpMethod.GET, GET_MENTIONS_API_PATH, signatureParams);
    final JSONArray response = new JSONArray(responseEntity.getBody());

    final List<JSONObject> newMentions = new ArrayList<>();
    for (int i = 0; i < response.length(); i++) {
      final JSONObject tweet = response.getJSONObject(i);

      final boolean isTweetAlreadyProcessed = isTweetAlreadyProcessed(tweet.getString("id_str"));
      if (isTweetAlreadyProcessed && newMentions.isEmpty()) {
        return;
      } else if (isTweetAlreadyProcessed) {
        mentionsProcessor.process(newMentions);
        return;
      }
      newMentions.add(tweet);
    }
    mentionsProcessor.process(newMentions);
  }

  private boolean isTweetAlreadyProcessed(final String id) {
    final Optional<ProcessedMentionsEntity> alreadyProcessedMention = processedMentionsRepository
        .getAllByTweetId(id);
    return alreadyProcessedMention.isPresent();
  }
}
