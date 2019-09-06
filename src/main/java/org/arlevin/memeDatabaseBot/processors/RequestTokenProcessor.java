package org.arlevin.memeDatabaseBot.processors;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.utilities.TokenUtility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.social.oauth1.AuthorizedRequestToken;
import org.springframework.social.oauth1.OAuth1Template;
import org.springframework.social.oauth1.OAuthToken;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RequestTokenProcessor {

  @Value("${auth.consumer.apiKey}")
  private String consumerApiKey;

  @Value("${auth.consumer.apiSecretKey}")
  private String consumerApiSecretKey;

  public void processRequest(String callback) {
    OAuth1Template oAuth1Template = new OAuth1Template(consumerApiKey, consumerApiSecretKey,
        "https://api.twitter.com/oauth/request_token", "https://api.twitter.com/oauth/authenticate",
        "https://api.twitter.com/oauth/access_token");

    OAuthToken requestToken = oAuth1Template.fetchRequestToken(callback, null);
    TokenUtility.setRequestToken(requestToken);

    String authorizeUrl = oAuth1Template
        .buildAuthorizeUrl(TokenUtility.getRequestToken().getValue(), null);
    Runtime runtime = Runtime.getRuntime();
    try {
      runtime.exec("open " + authorizeUrl);
    } catch (IOException e) {
      log.error("Could not open url: {}", e.toString());
    }
  }

  public void processCallback(String oauth_token, String oauth_verifier) {
    log.info("Received token ({}) and verifier ({})", oauth_token, oauth_verifier);
    if (TokenUtility.getRequestToken() != null) {
      log.info("Requesting access token");

      OAuth1Template oAuth1Template = new OAuth1Template(consumerApiKey, consumerApiSecretKey,
          "https://api.twitter.com/oauth/request_token", "https://api.twitter.com/oauth/authorize",
          "https://api.twitter.com/oauth/access_token");

      AuthorizedRequestToken authorizedRequestToken = new AuthorizedRequestToken(
          TokenUtility.getRequestToken(), oauth_verifier);

      OAuthToken accessToken = oAuth1Template.exchangeForAccessToken(authorizedRequestToken, null);
      TokenUtility.setAccessToken(accessToken);
      log.info("Received accessToken ({}) and accessTokenSecret ({})",
          TokenUtility.getAccessToken().getValue(), TokenUtility.getAccessToken().getSecret());
    }
  }
}