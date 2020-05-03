package service;

import static org.arlevin.memedatabasebot.constant.StringConstants.COMMAND;
import static org.arlevin.memedatabasebot.constant.StringConstants.INCLUDE_ENTITIES;
import static org.arlevin.memedatabasebot.constant.StringConstants.MEDIA_ID;
import static org.arlevin.memedatabasebot.enums.MediaUploadCommand.APPEND;
import static org.arlevin.memedatabasebot.enums.MediaUploadCommand.FINALIZE;
import static org.arlevin.memedatabasebot.enums.MediaUploadCommand.INIT;
import static org.arlevin.memedatabasebot.enums.MediaUploadCommand.STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.arlevin.memedatabasebot.client.TwitterClient;
import org.arlevin.memedatabasebot.service.TwitterMediaUploadService;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings({"unchecked"})
public class TwitterMediaUploadServiceTest {

  private final TwitterClient twitterClient;
  private final TwitterMediaUploadService twitterMediaUploadService;

  public TwitterMediaUploadServiceTest() {
    this.twitterClient = mock(TwitterClient.class);
    this.twitterMediaUploadService = new TwitterMediaUploadService(twitterClient);
    ReflectionTestUtils.setField(twitterMediaUploadService, "defaultCheckAfterSecs", 0);
  }

  @Test
  public void uploadMedia_withValidImageFileNameNotGif_withNoResponseFromTwitterInit() {
    String path = "src/test/resources/writeToFolder/000/000/000/001/000000000001.jpg";
    File file = new File(path);
    String absolutePath = file.getAbsolutePath();
    String fileBytesNum = String.valueOf(file.length());

    final String mediaId = twitterMediaUploadService.uploadMedia(absolutePath, false);

    final Map<String, String> params = new HashMap<>();
    params.put(INCLUDE_ENTITIES, "true");
    params.put(COMMAND, INIT.toString());
    params.put("total_bytes", fileBytesNum);
    params.put("media_type", "image/jpeg");
    params.put("media_category", "TweetImage");

    verify(twitterClient).makeMediaUploadRequest(INIT, params, null);
    verify(twitterClient, never()).makeMediaUploadRequest(eq(APPEND), any(), any());
    verify(twitterClient, never()).makeMediaUploadRequest(eq(FINALIZE), any(), any());
    verify(twitterClient, never()).makeMediaUploadRequest(eq(STATUS), any(), any());

    assertNull(mediaId);
  }

  @Test
  public void uploadMedia_withValidVideoFileNameNotGif_with6Chunks_successfullyUpload() {
    String path = "src/test/resources/writeToFolder/000/000/000/002/000000000002.mp4";
    File file = new File(path);
    String absolutePath = file.getAbsolutePath();
    String fileBytesNum = String.valueOf(file.length());

    final Map<String, String> expectedInitParams = new HashMap<>();
    expectedInitParams.put(INCLUDE_ENTITIES, "true");
    expectedInitParams.put(COMMAND, INIT.toString());
    expectedInitParams.put("total_bytes", fileBytesNum);
    expectedInitParams.put("media_type", "video/mp4");
    expectedInitParams.put("media_category", "TweetVideo");

    when(twitterClient.makeMediaUploadRequest(INIT, expectedInitParams, null)).thenReturn(
        ResponseEntity.ok("{\"media_id_string\":\"1\"}"));

    final String finalizeResponse = new JSONObject()
        .put("processing_info", new JSONObject()
            .put("check_after_secs", 0)).toString();

    final Map<String, String> expectedFinalizeParams = new HashMap<>();
    expectedFinalizeParams.put(INCLUDE_ENTITIES, "true");
    expectedFinalizeParams.put(COMMAND, "FINALIZE");
    expectedFinalizeParams.put(MEDIA_ID, "1");

    when(twitterClient.makeMediaUploadRequest(FINALIZE, expectedFinalizeParams, null))
        .thenReturn(ResponseEntity.ok(finalizeResponse));

    final Map<String, String> statusParams = new HashMap<>();
    statusParams.put(COMMAND, "STATUS");
    statusParams.put(MEDIA_ID, "1");

    final String statusResponse = new JSONObject()
        .put("processing_info", new JSONObject()
            .put("state", "succeeded")).toString();

    when(twitterClient.makeMediaUploadRequest(STATUS, statusParams, null))
        .thenReturn(ResponseEntity.ok(statusResponse));

    final String mediaId = twitterMediaUploadService.uploadMedia(absolutePath, false);

    final ArgumentCaptor<Map<String, String>> paramsArgumentCaptor = ArgumentCaptor
        .forClass(HashMap.class);
    final ArgumentCaptor<byte[]> byteArrayArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(twitterClient).makeMediaUploadRequest(INIT, expectedInitParams, null);
    verify(twitterClient, times(6))
        .makeMediaUploadRequest(eq(APPEND), paramsArgumentCaptor.capture(),
            byteArrayArgumentCaptor.capture());
    verify(twitterClient).makeMediaUploadRequest(FINALIZE, expectedFinalizeParams, null);
    verify(twitterClient).makeMediaUploadRequest(STATUS, statusParams, null);
    assertEquals("1", mediaId);
  }

  @Test
  public void uploadMedia_withValidVideoFileNameNotGif_with2Chunks_successfullyUpload() {
    String path = "src/test/resources/writeToFolder/000/000/000/001/000000000001.jpg";
    File file = new File(path);
    String absolutePath = file.getAbsolutePath();
    String fileBytesNum = String.valueOf(file.length());

    final Map<String, String> expectedInitParams = new HashMap<>();
    expectedInitParams.put(INCLUDE_ENTITIES, "true");
    expectedInitParams.put(COMMAND, INIT.toString());
    expectedInitParams.put("total_bytes", fileBytesNum);
    expectedInitParams.put("media_type", "image/jpeg");
    expectedInitParams.put("media_category", "TweetImage");

    when(twitterClient.makeMediaUploadRequest(INIT, expectedInitParams, null)).thenReturn(
        ResponseEntity.ok("{\"media_id_string\":\"1\"}"));

    final String finalizeResponse = new JSONObject()
        .put("processing_info", new JSONObject()
            .put("check_after_secs", 0)).toString();

    final Map<String, String> expectedFinalizeParams = new HashMap<>();
    expectedFinalizeParams.put(INCLUDE_ENTITIES, "true");
    expectedFinalizeParams.put(COMMAND, "FINALIZE");
    expectedFinalizeParams.put(MEDIA_ID, "1");

    when(twitterClient.makeMediaUploadRequest(FINALIZE, expectedFinalizeParams, null))
        .thenReturn(ResponseEntity.ok(finalizeResponse));

    final Map<String, String> statusParams = new HashMap<>();
    statusParams.put(COMMAND, "STATUS");
    statusParams.put(MEDIA_ID, "1");

    final String statusResponse = new JSONObject()
        .put("processing_info", new JSONObject()
            .put("state", "succeeded")).toString();

    when(twitterClient.makeMediaUploadRequest(STATUS, statusParams, null))
        .thenReturn(ResponseEntity.ok(statusResponse));

    final String mediaId = twitterMediaUploadService.uploadMedia(absolutePath, false);

    final ArgumentCaptor<Map<String, String>> paramsArgumentCaptor = ArgumentCaptor
        .forClass(HashMap.class);
    final ArgumentCaptor<byte[]> byteArrayArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(twitterClient).makeMediaUploadRequest(INIT, expectedInitParams, null);
    verify(twitterClient, times(2))
        .makeMediaUploadRequest(eq(APPEND), paramsArgumentCaptor.capture(),
            byteArrayArgumentCaptor.capture());
    verify(twitterClient).makeMediaUploadRequest(FINALIZE, expectedFinalizeParams, null);
    verify(twitterClient).makeMediaUploadRequest(STATUS, statusParams, null);
    assertEquals("1", mediaId);
  }

  @Test
  public void uploadMedia_withValidVideoFileNameNotGif_unableToCheckStatusRetry3MoreTimesThenReturnNull() {
    String path = "src/test/resources/writeToFolder/000/000/000/002/000000000002.mp4";
    File file = new File(path);
    String absolutePath = file.getAbsolutePath();
    String fileBytesNum = String.valueOf(file.length());

    final Map<String, String> expectedInitParams = new HashMap<>();
    expectedInitParams.put(INCLUDE_ENTITIES, "true");
    expectedInitParams.put(COMMAND, INIT.toString());
    expectedInitParams.put("total_bytes", fileBytesNum);
    expectedInitParams.put("media_type", "video/mp4");
    expectedInitParams.put("media_category", "TweetVideo");

    when(twitterClient.makeMediaUploadRequest(INIT, expectedInitParams, null)).thenReturn(
        ResponseEntity.ok("{\"media_id_string\":\"1\"}"));

    final String finalizeResponse = new JSONObject()
        .put("processing_info", new JSONObject()
            .put("check_after_secs", 0)).toString();

    final Map<String, String> expectedFinalizeParams = new HashMap<>();
    expectedFinalizeParams.put(INCLUDE_ENTITIES, "true");
    expectedFinalizeParams.put(COMMAND, "FINALIZE");
    expectedFinalizeParams.put(MEDIA_ID, "1");

    when(twitterClient.makeMediaUploadRequest(FINALIZE, expectedFinalizeParams, null))
        .thenReturn(ResponseEntity.ok(finalizeResponse));

    final Map<String, String> statusParams = new HashMap<>();
    statusParams.put(COMMAND, "STATUS");
    statusParams.put(MEDIA_ID, "1");

    when(twitterClient.makeMediaUploadRequest(STATUS, statusParams, null))
        .thenReturn(ResponseEntity.ok("{\"processing_info\": {\"state\": \"invalid\"}}"));

    final String mediaId = twitterMediaUploadService.uploadMedia(absolutePath, false);

    final ArgumentCaptor<Map<String, String>> paramsArgumentCaptor = ArgumentCaptor
        .forClass(HashMap.class);
    final ArgumentCaptor<byte[]> byteArrayArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(twitterClient).makeMediaUploadRequest(INIT, expectedInitParams, null);
    verify(twitterClient, times(6))
        .makeMediaUploadRequest(eq(APPEND), paramsArgumentCaptor.capture(),
            byteArrayArgumentCaptor.capture());
    verify(twitterClient).makeMediaUploadRequest(FINALIZE, expectedFinalizeParams, null);
    verify(twitterClient, times(4)).makeMediaUploadRequest(STATUS, statusParams, null);
    assertNull(mediaId);
  }
}
