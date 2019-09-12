package org.arlevin.memeDatabaseBot.services;

import static java.lang.Math.ceil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.arlevin.memeDatabaseBot.utilities.SignatureUtility;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class TwitterMediaUploadService {

  @Value("${auth.consumer.apiKey}")
  private String consumerApiKey;

  @Value("${auth.access.token}")
  private String accessToken;

  private final SignatureUtility signatureUtility;

  @Autowired
  public TwitterMediaUploadService(
      SignatureUtility signatureUtility) {
    this.signatureUtility = signatureUtility;
  }

  public void uploadMedia(String fileName) {
    String mediaId = initUpload(fileName);
    appendProcessor(fileName, mediaId);
  }

  private String initUpload(String fileName) {
    String nonce = RandomStringUtils.randomAlphanumeric(42);
    String timestamp = Integer.toString((int) (new Date().getTime() / 1000));

    File file = new File(fileName);
    long fileBytesNum = file.length();
    String mimeType;
    if(fileName.substring(fileName.indexOf('.')).equals(".mp4")) {
      mimeType = "video/mp4";
    } else {
      mimeType = URLConnection.guessContentTypeFromName(file.getName());
    }
    String type = mimeType.split("/")[0];
    String media_category;
    if (type.equals("video")) {
      media_category = "TweetVideo";
    } else {
      if (mimeType.equals("image/gif")) {
        media_category = "TweetGif";
      } else {
        media_category = "TweetImage";
      }
    }

    Map<String, String> requestParamsMap = new HashMap();

    requestParamsMap.put("include_entities", "true");
    requestParamsMap.put("command", "INIT");
    requestParamsMap.put("total_bytes", String.valueOf(fileBytesNum));
    requestParamsMap.put("media_type", mimeType);
    requestParamsMap.put("media_category", media_category);
    String signature = signatureUtility
        .calculateStatusUpdateSignature("https://upload.twitter.com/1.1/media/upload.json", "POST",
            timestamp, nonce, requestParamsMap);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    String authHeaderText = "OAuth oauth_consumer_key=\"" + consumerApiKey + "\", " +
        "oauth_nonce=\"" + nonce + "\", " +
        "oauth_signature=\"" + signature + "\", " +
        "oauth_signature_method=\"HMAC-SHA1\", " +
        "oauth_timestamp=\"" + timestamp + "\", " +
        "oauth_token=\"" + accessToken + "\", " +
        "oauth_version=\"1.0\"";
    headers.add("Authorization", authHeaderText);

    try {
      String requestBodyString = "command=INIT&total_bytes=" + fileBytesNum + "&media_type="
          + URLEncoder.encode(mimeType, "UTF-8") + "&media_category=" + media_category;
      HttpEntity request = new HttpEntity(requestBodyString, headers);

      RestTemplate restTemplate = new RestTemplate();
      ResponseEntity<String> responseEntity = restTemplate
          .exchange("https://upload.twitter.com/1.1/media/upload.json?include_entities=true",
              HttpMethod.POST, request, String.class);
      String responseString = responseEntity.getBody();
      JSONObject response = new JSONObject(responseString);
      return response.getString("media_id_string");
    } catch (UnsupportedEncodingException e) {
      log.error("Could not encode url: {}", e.toString());
      return "Could not encode url";
    }
  }

  private void appendProcessor(String fileName, String mediaId) {
    // send 1 chunk per MB of total fileSize
    File file = new File(fileName);
    try {
      byte[] fileBytes = Files.readAllBytes(file.toPath());

      Double fileSizeMB = fileBytes.length / 1000000D;
      fileSizeMB = ceil(fileSizeMB);
      int from;
      int to;

      // upload 1 chunk per MB
      // for a file say 8000424 bytes, this will round up to 9 MB,
      // upload the file 1/9th at a time: nine requests each with a 1/9th chunk of the file
      // if the file is < 1 MB, upload in 2 chunks each 1/2 of the toal file
      Integer uploadedTotal = 0;
      for (int i = 0; i < fileSizeMB.intValue(); i++) {
        if (fileSizeMB == 1L) {
          from = 0;
          to = fileBytes.length / 2;
          byte[] bytesChunk = Arrays.copyOfRange(fileBytes, from, to);
          log.info("uploading bytes from (inclusive): {} \nto (exclusive): {}", from, to);
          sendAppendRequest(mediaId, 0, Base64.getEncoder().encode(bytesChunk));

          from = fileBytes.length / 2;
          to = fileBytes.length;
          log.info("uploading bytes from (inclusive): {} \nto (exclusive): {}", from, to);
          bytesChunk = Arrays.copyOfRange(fileBytes,from, to);
          sendAppendRequest(mediaId, 1, Base64.getEncoder().encode(bytesChunk));
          return;
        }
        from = i * (fileBytes.length / fileSizeMB.intValue());
        if(i == fileSizeMB.intValue() - 1) {
          to = fileBytes.length;
        } else {
          to = i * (fileBytes.length / fileSizeMB.intValue()) + (fileBytes.length / fileSizeMB
              .intValue());
        }
        byte[] bytesChunk = Arrays.copyOfRange(fileBytes, from, to);
        uploadedTotal = uploadedTotal + bytesChunk.length;
        log.info("uploading bytes from (inclusive): {} \nto (exclusive): {}", from, to);
        sendAppendRequest(mediaId, i, Base64.getEncoder().encode(bytesChunk));
        log.info("Uploaded {} bytes total", uploadedTotal);
      }
      log.info("Finished APPEND portion of media upload");
    } catch (IOException e) {
      log.error("Could not read file bytes: {}", e.toString());
    }
  }

  private void sendAppendRequest(String mediaId, Integer segmentIndex, byte[] mediaData) {
    String nonce = RandomStringUtils.randomAlphanumeric(42);
    String timestamp = Integer.toString((int) (new Date().getTime() / 1000));

    Map<String, String> requestParamsMap = new HashMap();
    requestParamsMap.put("include_entities", "true");
    requestParamsMap.put("command", "APPEND");
    requestParamsMap.put("media_id", mediaId);
    requestParamsMap.put("segment_index", segmentIndex.toString());

    String signature = signatureUtility
        .calculateStatusUpdateSignature("https://upload.twitter.com/1.1/media/upload.json", "POST",
            timestamp, nonce, requestParamsMap);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    String authHeaderText = "OAuth oauth_consumer_key=\"" + consumerApiKey + "\", " +
        "oauth_nonce=\"" + nonce + "\", " +
        "oauth_signature=\"" + signature + "\", " +
        "oauth_signature_method=\"HMAC-SHA1\", " +
        "oauth_timestamp=\"" + timestamp + "\", " +
        "oauth_token=\"" + accessToken + "\", " +
        "oauth_version=\"1.0\"";
    headers.add("Authorization", authHeaderText);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("media_data", mediaData);

    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
    String url = "https://upload.twitter.com/1.1/media/upload.json?include_entities=true"
        + "&command=APPEND&media_id=" + mediaId + "&segment_index=" + segmentIndex.toString();

    RestTemplate restTemplate = new RestTemplate();
    restTemplate.exchange(url, HttpMethod.POST, request, String.class);
  }
}