package org.arlevin.memeDatabaseBot.listeners;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.arlevin.memeDatabaseBot.domain.ProcessedMentionsEntity;
import org.arlevin.memeDatabaseBot.processors.MentionsProcessor;
import org.arlevin.memeDatabaseBot.repositories.ProcessedMentionsRepository;
import org.arlevin.memeDatabaseBot.utilities.SignatureUtility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class MentionsListener {

  @Value("${auth.consumer.apiKey}")
  private String consumerApiKey;

  @Value("${auth.access.token}")
  private String accessToken;

  private final ProcessedMentionsRepository processedMentionsRepository;
  private final MentionsProcessor mentionsProcessor;
  private final SignatureUtility signatureUtility;

  public MentionsListener(ProcessedMentionsRepository processedMentionsRepository,
      MentionsProcessor mentionsProcessor, SignatureUtility signatureUtility) {
    this.processedMentionsRepository = processedMentionsRepository;
    this.mentionsProcessor = mentionsProcessor;
    this.signatureUtility = signatureUtility;
  }

  @Scheduled(fixedRate = 30000)
  public void checkForNewMentions() {
    RestTemplate restTemplate = new RestTemplate();
    String url = "https://api.twitter.com/1.1/statuses/mentions_timeline.json";

    String nonce = RandomStringUtils.randomAlphanumeric(42);
    String timestamp = Integer.toString((int) (new Date().getTime() / 1000));

    String signature = signatureUtility
        .calculateStatusUpdateSignature(url, "GET", null, timestamp, nonce);

    HttpHeaders httpHeaders = new HttpHeaders();
    String authHeaderText = "OAuth oauth_consumer_key=\"" + consumerApiKey + "\", " +
        "oauth_nonce=\"" + nonce + "\", " +
        "oauth_signature=\"" + signature + "\", " +
        "oauth_signature_method=\"HMAC-SHA1\", " +
        "oauth_timestamp=\"" + timestamp + "\", " +
        "oauth_token=\"" + accessToken + "\", " +
        "oauth_version=\"1.0\"";
    httpHeaders.add("Authorization", authHeaderText);
    HttpEntity entity = new HttpEntity(httpHeaders);

    ResponseEntity<String> responseEntity = restTemplate
        .exchange(url, HttpMethod.GET, entity, String.class);
    JSONArray response = new JSONArray(responseEntity.getBody());

    List<JSONObject> newMentions = new ArrayList<>();
    for (int i = 0; i < response.length(); i++) {
      JSONObject tweet = response.getJSONObject(i);

      if (isTweetAlreadyProcessed(tweet)) {
        if (newMentions.isEmpty()) {
          return;
        } else {
          mentionsProcessor.process(newMentions);
          return;
        }
      }
      newMentions.add(tweet);
    }
    mentionsProcessor.process(newMentions);
  }

  private boolean isTweetAlreadyProcessed(JSONObject tweet) {
    Optional<ProcessedMentionsEntity> alreadyProcessedMention = processedMentionsRepository
        .getAllByTweetId(tweet.getString("id_str"));
    return alreadyProcessedMention.isPresent();
  }
}