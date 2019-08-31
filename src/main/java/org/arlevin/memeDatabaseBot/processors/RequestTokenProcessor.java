package org.arlevin.memeDatabaseBot.processors;

import java.io.IOException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.social.oauth1.OAuth1Template;
import org.springframework.social.oauth1.OAuthToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class RequestTokenProcessor {

  @Value("${auth.consumer.apiKey}")
  private String consumerApiKey;

  @Value("${auth.consumer.apiSecretKey}")
  private String consumerApiSecretKey;

  @Value("${auth.access.token}")
  private String accessToken;

  @Value("${auth.access.tokenSecret}")
  private String accessTokenSecret;

  public void processRequest() {
    RestTemplate restTemplate = new RestTemplate();
    OAuth1Template oAuth1Template = new OAuth1Template(consumerApiKey, consumerApiSecretKey,
        "https://api.twitter.com/oauth/request_token", "https://api.twitter.com/oauth/authorize",
        "https://api.twitter.com/oauth/access_token");
    OAuthToken token = oAuth1Template
        .fetchRequestToken("https://arlevin.org:3003/api/memebot/v1/oauth/requestTokenCallback", null);

    String authorizeUrl = oAuth1Template.buildAuthorizeUrl(token.getValue(), null);

    ClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
    try {
      ClientHttpRequest request = clientHttpRequestFactory.createRequest(URI.create(authorizeUrl), HttpMethod.GET);
      request.execute();
    } catch (IOException e) {
      log.error("Could not create authorize request: {}", e.toString());
    }
    restTemplate.getForEntity(authorizeUrl, String.class);
  }

  public void processCallback(String oauth_token, String oauth_verifier) {
    log.info("Received token ({}) and verifier ({})", oauth_token, oauth_verifier);
  }
}