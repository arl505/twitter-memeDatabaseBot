package client;

import static org.arlevin.memedatabasebot.constant.StringConstants.MEDIA_UPLOAD_URL;
import static org.arlevin.memedatabasebot.enums.MediaUploadCommand.APPEND;
import static org.arlevin.memedatabasebot.enums.MediaUploadCommand.FINALIZE;
import static org.arlevin.memedatabasebot.enums.MediaUploadCommand.INIT;
import static org.arlevin.memedatabasebot.enums.MediaUploadCommand.STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.arlevin.memedatabasebot.client.TwitterClient;
import org.arlevin.memedatabasebot.constant.StringConstants;
import org.arlevin.memedatabasebot.enums.MediaUploadCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class TwitterClientTest {

  private final TwitterClient twitterClient = new TwitterClient();
  private final RestTemplate restTemplate = mock(RestTemplate.class);
  private static final String timelineUrl = "https://api.twitter.com/1.1/statuses/mentions_timeline.json";
  private static final String uploadUrl = "https://upload.twitter.com/1.1/media/upload.json?";

  public TwitterClientTest() {
    ReflectionTestUtils.setField(twitterClient, "restTemplate", restTemplate);
  }

  @Test
  public void makeRequest_withValidRequestWithParams_verifyMakingExpectedRequest() {
    final Map<String, String> params = new HashMap<>();
    params.put("tweet_mode", "extended");

    twitterClient.makeRequest(GET, timelineUrl, params);

    final ArgumentCaptor<HttpEntity> httpEntityArgumentCaptor = ArgumentCaptor
        .forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(eq(timelineUrl + "?tweet_mode=extended"), eq(GET),
            httpEntityArgumentCaptor.capture(), eq(String.class));
    assertTrue(httpEntityArgumentCaptor.getValue().getHeaders().containsKey("Authorization"));
  }

  @Test
  public void makeRequest_withValidRequestWithOutParams_verifyMakingExpectedRequest() {
    twitterClient.makeRequest(GET, timelineUrl, Collections.emptyMap());

    final ArgumentCaptor<HttpEntity> httpEntityArgumentCaptor = ArgumentCaptor
        .forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(eq(timelineUrl), eq(GET), httpEntityArgumentCaptor.capture(), eq(String.class));
    assertTrue(httpEntityArgumentCaptor.getValue().getHeaders().containsKey("Authorization"));
  }

  @Test
  public void makeMediaUploadRequest_withValidInitRequest_verifyMakingExpectedRequest() {
    final Map<String, String> params = new HashMap();
    params.put(StringConstants.INCLUDE_ENTITIES, "true");
    params.put(StringConstants.COMMAND, MediaUploadCommand.INIT.toString());
    params.put("total_bytes", "1");
    params.put("media_type", "image/jpeg");
    params.put("media_category", "TweetImage");

    twitterClient.makeMediaUploadRequest(INIT, params, null);

    final ArgumentCaptor<HttpEntity> httpEntityArgumentCaptor = ArgumentCaptor
        .forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(eq(MEDIA_UPLOAD_URL), eq(POST),
            httpEntityArgumentCaptor.capture(), eq(String.class));
    assertTrue(httpEntityArgumentCaptor.getValue().getHeaders().containsKey("Authorization"));
    assertEquals(MediaType.APPLICATION_FORM_URLENCODED,
        httpEntityArgumentCaptor.getValue().getHeaders().getContentType());
    assertEquals("command=INIT&total_bytes=1&media_type=image%2Fjpeg&media_category=TweetImage",
        httpEntityArgumentCaptor.getValue().getBody());
  }

  @Test
  public void makeMediaUploadRequest_withValidAppendRequest_verifyMakingExpectedRequest() {
    final byte[] mediaData = "test".getBytes();
    final String requestUrl =
        MEDIA_UPLOAD_URL + "&command=APPEND&media_id=1&segment_index=1";

    final Map<String, String> params = new HashMap();
    params.put(StringConstants.INCLUDE_ENTITIES, "true");
    params.put(StringConstants.COMMAND, "APPEND");
    params.put(StringConstants.MEDIA_ID, "1");
    params.put("segment_index", "1");

    twitterClient.makeMediaUploadRequest(APPEND, params, mediaData);

    final MultiValueMap<String, Object> expectedBody = new LinkedMultiValueMap<>();
    expectedBody.add("media_data", mediaData);

    final ArgumentCaptor<HttpEntity> httpEntityArgumentCaptor = ArgumentCaptor
        .forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(eq(requestUrl), eq(POST),
            httpEntityArgumentCaptor.capture(), eq(String.class));
    assertTrue(httpEntityArgumentCaptor.getValue().getHeaders().containsKey("Authorization"));
    assertEquals(MediaType.MULTIPART_FORM_DATA,
        httpEntityArgumentCaptor.getValue().getHeaders().getContentType());
    assertEquals(expectedBody, httpEntityArgumentCaptor.getValue().getBody());
  }

  @Test
  public void makeMediaUploadRequest_withValidFinalizeRequest_verifyMakingExpectedRequest() {
    final Map<String, String> params = new HashMap<>();
    params.put(StringConstants.INCLUDE_ENTITIES, "true");
    params.put(StringConstants.COMMAND, "FINALIZE");
    params.put(StringConstants.MEDIA_ID, "1");

    twitterClient.makeMediaUploadRequest(FINALIZE, params, null);

    final ArgumentCaptor<HttpEntity> httpEntityArgumentCaptor = ArgumentCaptor
        .forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(eq(MEDIA_UPLOAD_URL), eq(POST),
            httpEntityArgumentCaptor.capture(), eq(String.class));
    assertTrue(httpEntityArgumentCaptor.getValue().getHeaders().containsKey("Authorization"));
    assertEquals(MediaType.APPLICATION_FORM_URLENCODED,
        httpEntityArgumentCaptor.getValue().getHeaders().getContentType());
    assertEquals("command=FINALIZE&media_id=1", httpEntityArgumentCaptor.getValue().getBody());
  }

  @Test
  public void makeMediaUploadRequest_withValidStatusRequest_verifyMakingExpectedRequest() {
    final String requestUrl = "https://upload.twitter.com/1.1/media/upload.json?command=STATUS&media_id=1";
    final Map<String, String> params = new HashMap<>();
    params.put(StringConstants.COMMAND, "STATUS");
    params.put(StringConstants.MEDIA_ID, "1");

    twitterClient.makeMediaUploadRequest(STATUS, params, null);

    final ArgumentCaptor<HttpEntity> httpEntityArgumentCaptor = ArgumentCaptor
        .forClass(HttpEntity.class);
    verify(restTemplate)
        .exchange(eq(requestUrl), eq(GET),
            httpEntityArgumentCaptor.capture(), eq(String.class));
    assertTrue(httpEntityArgumentCaptor.getValue().getHeaders().containsKey("Authorization"));
    assertEquals(MediaType.APPLICATION_FORM_URLENCODED,
        httpEntityArgumentCaptor.getValue().getHeaders().getContentType());
    assertNull(httpEntityArgumentCaptor.getValue().getBody());
  }

  @Test
  public void makeUpdateStatusRequest_withValidStatusUpdateWithoutMedia_verifyMakingExpectedRequest() {
    final String status = "hello";
    final String replyToId = "1";

    twitterClient.makeUpdateStatusRequest(status, replyToId, null);

    final ArgumentCaptor<HttpEntity> httpEntityArgumentCaptor = ArgumentCaptor
        .forClass(HttpEntity.class);
    verify(restTemplate)
        .postForObject(eq("https://api.twitter.com/1.1/statuses/update.json?include_entities=true"),
            httpEntityArgumentCaptor.capture(), eq(String.class));
    assertTrue(httpEntityArgumentCaptor.getValue().getHeaders().containsKey("Authorization"));
    assertEquals(MediaType.APPLICATION_FORM_URLENCODED,
        httpEntityArgumentCaptor.getValue().getHeaders().getContentType());
    assertEquals("status=hello&in_reply_to_status_id=1",
        httpEntityArgumentCaptor.getValue().getBody());
  }

  @Test
  public void makeUpdateStatusRequest_withValidStatusUpdateWithMedia_verifyMakingExpectedRequest() {
    final String status = "hello";
    final String replyToId = "1";

    twitterClient.makeUpdateStatusRequest(status, replyToId, "1,2");

    final ArgumentCaptor<HttpEntity> httpEntityArgumentCaptor = ArgumentCaptor
        .forClass(HttpEntity.class);
    verify(restTemplate)
        .postForObject(eq("https://api.twitter.com/1.1/statuses/update.json?include_entities=true"),
            httpEntityArgumentCaptor.capture(), eq(String.class));
    assertTrue(httpEntityArgumentCaptor.getValue().getHeaders().containsKey("Authorization"));
    assertEquals(MediaType.APPLICATION_FORM_URLENCODED,
        httpEntityArgumentCaptor.getValue().getHeaders().getContentType());
    assertEquals("status=hello&in_reply_to_status_id=1&media_ids=1,2",
        httpEntityArgumentCaptor.getValue().getBody());
  }
}
