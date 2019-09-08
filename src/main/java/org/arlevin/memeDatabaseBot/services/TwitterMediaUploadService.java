package org.arlevin.memeDatabaseBot.services;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.arlevin.memeDatabaseBot.utilities.SignatureUtility;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
      SignatureUtility signatureUtility){
    this.signatureUtility = signatureUtility;
  }

  public void uploadMedia(String fileName) {
    initUpload(fileName);
  }

  private void initUpload(String fileName) {
    String url = "https://upload.twitter.com/1.1/media/upload.json";
    String nonce = RandomStringUtils.randomAlphanumeric(42);
    String timestamp = Integer.toString((int) (new Date().getTime() / 1000));

    File file = new File(fileName);
    Long fileBytesNum = file.length();

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

    Map<String, String> params = new HashMap<>();
    params.put("command", "INIT");
    params.put("total_bytes", fileBytesNum.toString());
    params.put("media_type", mimeType);
    params.put("media_category", media_category);

    String signature = signatureUtility
        .calculateStatusUpdateSignature(url, "POST", null, timestamp, nonce, false, new HashMap<>());

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

    HttpEntity request = new HttpEntity(new JSONObject(params), headers);

    RestTemplate restTemplate = new RestTemplate();
      List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
      MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
      converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED));
      messageConverters.add(converter);
      restTemplate.setMessageConverters(messageConverters);

    restTemplate.postForEntity(url, request, JSONObject.class).getBody();
  }
}