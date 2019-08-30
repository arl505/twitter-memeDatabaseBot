package org.arlevin.memeDatabaseBot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.stereotype.Component;

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

  // @EventListener(ApplicationReadyEvent.class) <------ use this annotation to execute at startup
  //  public void doSomethingAfterStartup() {
  //
  //  }

  public void postTweet(String tweetText) {
    TwitterTemplate twitterTemplate = new TwitterTemplate(consumerApiKey, consumerApiSecretKey,
        accessToken, accessTokenSecret);

    try {
      twitterTemplate.timelineOperations().updateStatus(tweetText);
    } catch(RuntimeException e) {
      log.info("Unable to post tweet: {}", e.toString());
    }
  }
}