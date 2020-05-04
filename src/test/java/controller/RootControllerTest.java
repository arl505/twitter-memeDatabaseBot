package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.arlevin.memedatabasebot.controller.RootController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

public class RootControllerTest {

  private final RootController rootController = new RootController();

  @Test
  public void endpoint_returnHelloWorld() {
    final ResponseEntity<String> expectedResponse = ResponseEntity.ok("Hello world!");
    final ResponseEntity<String> actualResponse = rootController.endpoint();
    assertEquals(expectedResponse, actualResponse);
  }
}
