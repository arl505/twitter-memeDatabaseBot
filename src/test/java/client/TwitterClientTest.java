package client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;

import java.util.HashMap;
import java.util.Map;
import org.arlevin.memedatabasebot.client.TwitterClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

public class TwitterClientTest {

  private final TwitterClient twitterClient = new TwitterClient();
  private final RestTemplate restTemplate = mock(RestTemplate.class);
  private static final String baseUrl = "https://api.twitter.com/1.1/statuses/mentions_timeline.json";

  public TwitterClientTest() {
    ReflectionTestUtils.setField(twitterClient, "restTemplate", restTemplate);
  }

  @Test
  public void makeRequest_withValidRequest_receive200ResponseWithExpectedBody() {
    when(restTemplate.exchange(eq(baseUrl), eq(GET), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("success"));

    final Map<String, String> params = new HashMap<>();
    params.put("tweet_mode", "extended");
    final ResponseEntity<String> response = twitterClient.makeRequest(GET, baseUrl, params);

    verify(restTemplate)
        .exchange(eq(baseUrl + "?tweet_mode=extended"), eq(GET), any(), eq(String.class));
  }
}
