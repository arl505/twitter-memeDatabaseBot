package org.arlevin.memeDatabaseBot.services;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.arlevin.memeDatabaseBot.utilities.SignatureUtility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class PostTweetService {

  @Value("${auth.consumer.apiKey}")
  private String consumerApiKey;

  @Value("${auth.access.token}")
  private String accessToken;

  private SignatureUtility signatureUtility;

  public PostTweetService(SignatureUtility signatureUtility) {
    this.signatureUtility = signatureUtility;
  }

  public void postTweet(String tweetText, String replyToId, List<String> mediaIds) {
    RestTemplate restTemplate = new RestTemplate();

    String nonce = RandomStringUtils.randomAlphanumeric(42);
    String timestamp = Integer.toString((int) (new Date().getTime() / 1000));

    String url = "https://api.twitter.com/1.1/statuses/update.json";

    String requestBody = "status" + "=" + signatureUtility.encode(tweetText) + "&in_reply_to_status_id=" + replyToId;

    Map<String, String> params = new HashMap<>();
    params.put("include_entities", "true");
    params.put("status", tweetText);
    params.put("in_reply_to_status_id", replyToId);
    if (mediaIds != null) {
      String mediaIdsString = String.join(",", mediaIds);
      requestBody = requestBody + "&media_ids=" + mediaIdsString;
      params.put("media_ids", mediaIdsString);
    }

    String signature = signatureUtility
        .calculateStatusUpdateSignature(url, "POST", timestamp, nonce, params);

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    String authHeaderText = "OAuth oauth_consumer_key=\"" + consumerApiKey + "\", " +
        "oauth_nonce=\"" + nonce + "\", " +
        "oauth_signature=\"" + signature + "\", " +
        "oauth_signature_method=\"HMAC-SHA1\", " +
        "oauth_timestamp=\"" + timestamp + "\", " +
        "oauth_token=\"" + accessToken + "\", " +
        "oauth_version=\"1.0\"";
    httpHeaders.add("Authorization", authHeaderText);

    HttpEntity request = new HttpEntity(requestBody, httpHeaders);
    restTemplate.postForObject(url + "?include_entities=true", request, String.class);
  }
}