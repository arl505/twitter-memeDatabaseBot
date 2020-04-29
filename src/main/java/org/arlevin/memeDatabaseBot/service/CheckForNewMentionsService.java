package org.arlevin.memeDatabaseBot.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.client.TwitterClient;
import org.arlevin.memeDatabaseBot.entity.ProcessedMentionsEntity;
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
  private final SortMentionsService sortMentionsService;
  private final TwitterClient twitterClient;

  public CheckForNewMentionsService(final ProcessedMentionsRepository processedMentionsRepository,
      final SortMentionsService sortMentionsService, final TwitterClient twitterClient) {
    this.processedMentionsRepository = processedMentionsRepository;
    this.sortMentionsService = sortMentionsService;
    this.twitterClient = twitterClient;
  }

  @Scheduled(fixedRate = 15000)
  public void checkForNewMentions() {
    log.info("Checking for new mentions...");

    final Map<String, String> signatureParams = new HashMap<>();
    signatureParams.put("tweet_mode", "extended");

    log.info("Calling twitter to get mentions");
    final ResponseEntity<String> responseEntity = twitterClient
        .makeRequest(HttpMethod.GET, GET_MENTIONS_API_PATH, signatureParams);
    final JSONArray response = new JSONArray(responseEntity.getBody());

    final List<JSONObject> newMentions = new ArrayList<>();
    for (int i = 0; i < response.length(); i++) {
      final JSONObject tweet = response.getJSONObject(i);
      final String tweetId = tweet.getString("id_str");
      final boolean isTweetAlreadyProcessed = isTweetAlreadyProcessed(tweetId);

      if (isTweetAlreadyProcessed && newMentions.isEmpty()) {
        log.info(
            "First mention (id {}) was already processed, thus found no new mentions, successfully finished",
            tweetId);
        return;
      } else if (isTweetAlreadyProcessed) {
        log.info(
            "Found already processed mention with id {}, calling sort mentions service with {} tweets",
            tweetId, newMentions.size());
        sortMentionsService.process(newMentions);
        log.info("Successfully finished processing new mentions");
        return;
      }
      newMentions.add(tweet);
    }
    log.info(
        "Found no already processed mention, calling sort mentions service with all {} tweets",
        newMentions.size());
    sortMentionsService.process(newMentions);
    log.info("Successfully finished processing new mentions");
  }

  private boolean isTweetAlreadyProcessed(final String id) {
    log.info("Calling database to check if tweetId {} has already been processed", id);
    final Optional<ProcessedMentionsEntity> alreadyProcessedMention = processedMentionsRepository
        .getAllByTweetId(id);
    return alreadyProcessedMention.isPresent();
  }
}
