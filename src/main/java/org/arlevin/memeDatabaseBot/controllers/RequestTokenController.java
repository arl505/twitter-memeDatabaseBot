package org.arlevin.memeDatabaseBot.controllers;

import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.processors.RequestTokenProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("v1/oauth")
@RestController
@Slf4j
public class RequestTokenController {

  private final RequestTokenProcessor requestTokenProcessor;

  public RequestTokenController(RequestTokenProcessor requestTokenProcessor) {
    this.requestTokenProcessor = requestTokenProcessor;
  }

  @GetMapping("/requestToken")
  public ResponseEntity requestTokenHandler() {
    requestTokenProcessor.processRequest();
    return ResponseEntity.ok(null);
  }

  @GetMapping("/requestTokenCallback")
  public ResponseEntity requestTokenCallbackHandler(@RequestParam("oauth_token") String oauth_token,
      @RequestParam("oauth_verifier") String oauth_verifier) {
    log.info("invoked");
    requestTokenProcessor.processCallback(oauth_token, oauth_verifier);
    return ResponseEntity.ok(null);
  }
}