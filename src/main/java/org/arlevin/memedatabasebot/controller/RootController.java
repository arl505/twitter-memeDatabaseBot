package org.arlevin.memedatabasebot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

  @GetMapping("/")
  public ResponseEntity<String> endpoint() {
    return ResponseEntity.ok("Hello world!");
  }
}
