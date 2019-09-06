package org.arlevin.memeDatabaseBot.controllers;

import org.arlevin.memeDatabaseBot.services.PostTweetService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("v1/testingService")
public class PostTweetController {

  private final PostTweetService postTweetService;

  public PostTweetController(PostTweetService postTweetService) {
    this.postTweetService = postTweetService;
  }

  @PostMapping("/postTweet")
  public ResponseEntity<String> postTweetEndpoint(@RequestBody String tweetText){
    return ResponseEntity.ok(postTweetService.postTweet(tweetText));
  }
}