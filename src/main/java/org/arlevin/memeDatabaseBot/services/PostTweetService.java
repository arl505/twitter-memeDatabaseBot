package org.arlevin.memeDatabaseBot.services;

import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.arlevin.memeDatabaseBot.utilities.SignatureUtility;
import org.arlevin.memeDatabaseBot.utilities.TokenUtility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class PostTweetService {

  @Value("${auth.consumer.apiKey}")
  private String consumerApiKey;

  private SignatureUtility signatureUtility;

  public PostTweetService(SignatureUtility signatureUtility) {
    this.signatureUtility = signatureUtility;
  }

//   @EventListener(ApplicationReadyEvent.class) <------ use this annotation to execute at startup
//    public void doSomethingAfterStartup() {
//
//    }

  public void postTweet(String tweetText) {
    if (TokenUtility.getAccessToken() != null) {
      RestTemplate restTemplate = new RestTemplate();

      String nonce = RandomStringUtils.randomAlphanumeric(42);
      String timestamp = Integer.toString((int) (new Date().getTime() / 1000));

      String url = "https://api.twitter.com/1.1/statuses/update.json?include_entities=true";

      String signature = signatureUtility.calculateStatusUpdateSignature(tweetText, timestamp, nonce);

      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      String authHeaderText = "OAuth oauth_consumer_key=\"" + consumerApiKey + "\", " +
          "oauth_nonce=\"" + nonce + "\", " +
          "oauth_signature=\"" + signature + "\", " +
          "oauth_signature_method=\"HMAC-SHA1\", " +
          "oauth_timestamp=\"" + timestamp + "\", " +
          "oauth_token=\"" + TokenUtility.getAccessToken().getValue() + "\", " +
          "oauth_version=\"1.0\"";
      httpHeaders.add("Authorization", authHeaderText);

      HttpEntity request = new HttpEntity("status=" + signatureUtility.encode(tweetText), httpHeaders);
      restTemplate.postForObject(url, request, String.class);
    }
  }
}