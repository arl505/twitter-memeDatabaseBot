package org.arlevin.memedatabasebot.service;

import static java.lang.Math.ceil;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.arlevin.memedatabasebot.client.TwitterClient;
import org.arlevin.memedatabasebot.constant.StringConstants;
import org.arlevin.memedatabasebot.enums.MediaUploadCommand;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;


@Slf4j
@Service
public class TwitterMediaUploadService {

  @Value("${credentials.consumer.key}")
  private String consumerApiKey;

  @Value("${credentials.access.key}")
  private String accessToken;

  private final TwitterClient twitterClient;

  public TwitterMediaUploadService(final TwitterClient twitterClient) {
    this.twitterClient = twitterClient;
  }

  public String uploadMedia(final String fileName, final Boolean isGif) {
    final String mediaId = initUpload(fileName, isGif);
    if (mediaId != null) {
      appendProcessor(fileName, mediaId);
      final boolean isMediaUploaded = finalizeUpload(mediaId);
      return (isMediaUploaded)
          ? mediaId
          : null;
    }
    log.error("An issue occurred while making upload INIT request to twitter");
    return null;
  }

  private String initUpload(final String fileName, final boolean isGif) {
    final File file = new File(fileName);
    String fileBytesNum = String.valueOf(file.length());

    String mimeType;
    if (fileName.substring(fileName.indexOf('.')).equals(".mp4")) {
      mimeType = "video/mp4";
    } else {
      mimeType = URLConnection.guessContentTypeFromName(file.getName());
    }

    final String type = mimeType.split("/")[0];
    String mediaCategory;
    if (isGif) {
      mediaCategory = "TweetVideo";
    } else if (type.equals("video")) {
      mediaCategory = "TweetVideo";
    } else {
      mediaCategory = "TweetImage";
    }

    final Map<String, String> params = new HashMap<>();
    params.put(StringConstants.INCLUDE_ENTITIES, "true");
    params.put(StringConstants.COMMAND, MediaUploadCommand.INIT.toString());
    params.put("total_bytes", fileBytesNum);
    params.put("media_type", mimeType);
    params.put("media_category", mediaCategory);

    log.info("Sending twitter upload INIT request...");
    final ResponseEntity<String> responseEntity = twitterClient
        .makeMediaUploadRequest(MediaUploadCommand.INIT, params, null);

    return responseEntity == null
        ? null
        : new JSONObject(responseEntity.getBody()).getString("media_id_string");
  }

  private void appendProcessor(final String fileName, final String mediaId) {
    // send 1 chunk per MB of total fileSize
    final File file = new File(fileName);
    try {
      final byte[] fileBytes = Files.readAllBytes(file.toPath());

      Double fileSizeMB = fileBytes.length / 1000000D;
      fileSizeMB = ceil(fileSizeMB);
      int from;
      int to;

      // upload 1 chunk per MB
      // for a file say 8000424 bytes, this will round up to 9 MB,
      // upload the file 1/9th at a time: nine requests each with a 1/9th chunk of the file
      // if the file is < 1 MB, upload in 2 chunks each 1/2 of the toal file
      int uploadedTotal = 0;
      for (int i = 0; i < fileSizeMB.intValue(); i++) {
        if (fileSizeMB == 1L) {
          from = 0;
          to = fileBytes.length / 2;
          byte[] bytesChunk = Arrays.copyOfRange(fileBytes, from, to);
          log.info(StringConstants.UPLOADING_LOG, from, to);
          sendAppendRequest(mediaId, 0, Base64.getEncoder().encode(bytesChunk));

          from = fileBytes.length / 2;
          to = fileBytes.length;
          log.info(StringConstants.UPLOADING_LOG, from, to);
          bytesChunk = Arrays.copyOfRange(fileBytes, from, to);
          sendAppendRequest(mediaId, 1, Base64.getEncoder().encode(bytesChunk));
          return;
        }
        from = i * (fileBytes.length / fileSizeMB.intValue());
        if (i == fileSizeMB.intValue() - 1) {
          to = fileBytes.length;
        } else {
          to = i * (fileBytes.length / fileSizeMB.intValue()) + (fileBytes.length / fileSizeMB
              .intValue());
        }
        byte[] bytesChunk = Arrays.copyOfRange(fileBytes, from, to);
        uploadedTotal = uploadedTotal + bytesChunk.length;
        log.info(StringConstants.UPLOADING_LOG, from, to);
        sendAppendRequest(mediaId, i, Base64.getEncoder().encode(bytesChunk));
        log.info("Uploaded {} bytes total", uploadedTotal);
      }
    } catch (final IOException e) {
      log.error("Could not read file bytes: {}", e.toString());
    }

  }

  private void sendAppendRequest(final String mediaId, final Integer segmentIndex,
      final byte[] mediaData) {

    final Map<String, String> params = new HashMap<>();
    params.put(StringConstants.INCLUDE_ENTITIES, "true");
    params.put(StringConstants.COMMAND, "APPEND");
    params.put(StringConstants.MEDIA_ID, mediaId);
    params.put("segment_index", segmentIndex.toString());

    log.info("Sending twitter upload APPEND request...");
    twitterClient.makeMediaUploadRequest(MediaUploadCommand.APPEND, params, mediaData);
  }

  private boolean finalizeUpload(final String mediaId) {

    final Map<String, String> params = new HashMap<>();
    params.put(StringConstants.INCLUDE_ENTITIES, "true");
    params.put(StringConstants.COMMAND, "FINALIZE");
    params.put(StringConstants.MEDIA_ID, mediaId);

    log.info("Sending twitter upload FINALIZE request...");
    final ResponseEntity<String> responseEntity = twitterClient
        .makeMediaUploadRequest(MediaUploadCommand.FINALIZE, params, null);
    final JSONObject response = new JSONObject(responseEntity.getBody());

    int checkAfterSecs = 0;
    if (response.has(StringConstants.PROCESSING_INFO)) {
      checkAfterSecs = response.getJSONObject(StringConstants.PROCESSING_INFO).getInt("check_after_secs");
      log.info("Processing info found, will wait {} seconds to check status", checkAfterSecs);
    }
    return checkStatus(mediaId, checkAfterSecs, 3);
  }

  private boolean checkStatus(final String mediaId, final Integer checkAfterSecs,
      final Integer retryTimes) {
    boolean isCompleted = false;
    try {
      log.info("Sleeping for {} seconds", checkAfterSecs);
      TimeUnit.SECONDS.sleep(checkAfterSecs);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Sleeping for {} seconds threw an exception: {}", checkAfterSecs, e.toString());
    }
    final Map<String, String> params = new HashMap<>();
    params.put(StringConstants.COMMAND, "STATUS");
    params.put(StringConstants.MEDIA_ID, mediaId);

    log.info("Sending twitter upload STATUS request...");
    try {
      final ResponseEntity<String> responseEntity = twitterClient
          .makeMediaUploadRequest(MediaUploadCommand.STATUS, params, null);
      final JSONObject response = new JSONObject(responseEntity.getBody());

      if (response.getJSONObject(StringConstants.PROCESSING_INFO).getString("state").equals("succeeded")) {
        log.info("Upload successfully completed, mediaId can now be used to in status update");
        isCompleted = true;
      } else if (retryTimes == 0) {
        log.info("Retried checking the status the maxinum of times, unable to upload media");
        isCompleted = false;
      } else {
        log.info("Upload not completed, will check status retry {} more times with 5 seconds sleep");
        return checkStatus(mediaId, 5, retryTimes - 1);
      }
    } catch (final HttpClientErrorException e) {
      log.info("Could not check status, assuming successful upload: {}", e.toString());
      return true;
    }
    return isCompleted;
  }
}