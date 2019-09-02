package org.arlevin.memeDatabaseBot.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.utilities.SignatureUtility;
import org.arlevin.memeDatabaseBot.utilities.TokenUtility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class PostTweetService {

  @Value("${auth.consumer.apiKey}")
  private String consumerApiKey;

  @Value("${auth.consumer.apiSecretKey}")
  private String consumerApiSecretKey;

  @Value("${auth.access.token}")
  private String accessToken;

  @Value("${auth.access.tokenSecret}")
  private String accessTokenSecret;

//   @EventListener(ApplicationReadyEvent.class) <------ use this annotation to execute at startup
//    public void doSomethingAfterStartup() {
//
//    }

  public void postTweet(String tweetText) {
    if (TokenUtility.getAccessToken() != null) {
      String nonce = UUID.randomUUID().toString();
      String timestamp = Integer.toString((int) (new Date().getTime() / 1000));
      String params = "OAuth oauth_consumer_key=\"" + consumerApiKey + "\", " +
          "oauth_nonce=\"" + nonce + "\", " +
          "oauth_signature_method=\"HMAC-SHA1\", " +
          "oauth_timestamp=\"" + timestamp + "\", " +
          "oauth_token=\"" + TokenUtility.getAccessToken().getValue() + "\", "+
          "oauth_version=\"1.0\"";
      try {
        String url = "https://api.twitter.com/1.1/statuses/update.json?status=" + tweetText;
        String signatureData = "POST&" + URLEncoder.encode(url, "UTF-8") +
            "&" + URLEncoder.encode(params, "UTF-8");
        try {
          String signature = SignatureUtility
              .calculateRFC2104HMAC(signatureData, accessTokenSecret);

          HttpHeaders httpHeaders = new HttpHeaders();
          String authHeaderText = "OAuth oauth_consumer_key=\"" + consumerApiKey + "\", " +
              "oauth_nonce=\"" + nonce + "\", " +
              "oauth_signature_method=\"HMAC-SHA1\", " +
              "oauth_timestamp=\"" + timestamp + "\", " +
              "oauth_token=\"" + TokenUtility.getAccessToken().getValue() + "\", " +
              "oauth_version=\"1.0\", " +
              "oauth_signature=\"" + signature + "\"";
          httpHeaders.add("authorization", authHeaderText);
          HttpEntity request = new HttpEntity(null, httpHeaders);
          RestTemplate restTemplate = new RestTemplate();
          restTemplate.postForEntity(url, request, String.class);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
          log.error("Could not generate signature: {}", e.toString());

        }
      } catch (UnsupportedEncodingException e) {
        log.error("Could not encode: {}", e.toString());
      }
    }
  }
}