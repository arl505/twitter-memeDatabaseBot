package org.arlevin.memeDatabaseBot.services;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
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
    appendProcessor(mediaId);
  }

  private String initUpload(String fileName) {
    String nonce = RandomStringUtils.randomAlphanumeric(42);
    String timestamp = Integer.toString((int) (new Date().getTime() / 1000));

    File file = new File(fileName);
    long fileBytesNum = file.length();
    String mimeType = URLConnection.guessContentTypeFromName(file.getName());

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
        .calculateStatusUpdateSignature("https://upload.twitter.com/1.1/media/upload.json", "POST", null, timestamp, nonce, false,
            requestParamsMap);

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
          .exchange("https://upload.twitter.com/1.1/media/upload.json?include_entities=true", HttpMethod.POST, request, String.class);
      String responseString = responseEntity.getBody();
      JSONObject response = new JSONObject(responseString);
      return response.getString("media_id");
    } catch (UnsupportedEncodingException e) {
      log.error("Could not encode url: {}", e.toString());
      return "Could not encode url";
    }
  }

  private void appendProcessor(String mediaId) {

  }
}