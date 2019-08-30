package org.arlevin.memeDatabaseBot.controllers;

import org.arlevin.memeDatabaseBot.services.PostTweetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("v1/testingService")
@RestController
public class PostTweetController {

  private final PostTweetService postTweetService;

  public PostTweetController(PostTweetService postTweetService) {
    this.postTweetService = postTweetService;
  }

  @PostMapping("/postTweet")
  public ResponseEntity postTweetEndpoint(@RequestBody String tweetText){
    postTweetService.postTweet(tweetText);
    return ResponseEntity.ok(null);
  }
}