package org.arlevin.memeDatabaseBot.controllers;

import lombok.extern.slf4j.Slf4j;
import org.arlevin.memeDatabaseBot.entities.OauthToken;
import org.arlevin.memeDatabaseBot.processors.RequestTokenProcessor;
import org.arlevin.memeDatabaseBot.utilities.TokenUtility;
import org.springframework.http.ResponseEntity;
import org.springframework.social.oauth1.OAuthToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("v1/oauth")
@RestController
@Slf4j
public class OauthController {

  private final RequestTokenProcessor requestTokenProcessor;

  public OauthController(RequestTokenProcessor requestTokenProcessor) {
    this.requestTokenProcessor = requestTokenProcessor;
  }

  @GetMapping("/requestToken")
  public ResponseEntity requestTokenHandler(@RequestParam("callback") String callback) {
    requestTokenProcessor.processRequest(callback);
    return ResponseEntity.ok(null);
  }

  @GetMapping("/requestTokenCallback")
  public ResponseEntity requestTokenCallbackHandler(@RequestParam("oauth_token") String oauth_token,
      @RequestParam("oauth_verifier") String oauth_verifier) {
    requestTokenProcessor.processCallback(oauth_token, oauth_verifier);
    return ResponseEntity.ok(null);
  }

  @PostMapping("/loadToken")
  public ResponseEntity loadToken(@RequestBody OauthToken oauthToken) {
    TokenUtility.setAccessToken(new OAuthToken(oauthToken.getToken(), oauthToken.getSecret()));
    return ResponseEntity.ok(null);
  }
}