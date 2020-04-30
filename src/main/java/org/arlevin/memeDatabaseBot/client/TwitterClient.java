package org.arlevin.memedatabasebot.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomStringUtils;
import org.arlevin.memedatabasebot.constant.StringConstants;
import org.arlevin.memedatabasebot.enums.MediaUploadCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class TwitterClient {

  @Value("${credentials.consumer.key}")
  private String consumerKey;

  @Value("${credentials.consumer.secret}")
  private String consumerSecret;

  @Value("${credentials.access.key}")
  private String accessKey;

  @Value("${credentials.access.secret}")
  private String accessSecret;

  private final RestTemplate restTemplate = new RestTemplate();

  // for simple, generic requests, like getting statuses
  public ResponseEntity<String> makeRequest(final HttpMethod httpMethod,
      final String baseUrl, final Map<String, String> requestParams) {

    final int timestamp = (int) (new Date().getTime() / 1000);
    final String nonce = RandomStringUtils.randomAlphanumeric(42);

    final String oauthSignature = generateSignature(httpMethod, baseUrl, nonce, timestamp,
        requestParams);

    final String authorizationHeaderString = "OAuth " +
        "oauth_consumer_key=\"" + encode(consumerKey) + "\", " +
        StringConstants.NONCE_AUTH_HEADER_PART + encode(nonce) + "\", " +
        StringConstants.SIGNATURE_AUTH_HEADER_PART + encode(oauthSignature) + "\", " +
        "oauth_signature_method=\"" + encode("HMAC-SHA1") + "\", " +
        StringConstants.TIMESTAMP_AUTH_HEADER_PART + encode(Integer.toString(timestamp)) + "\", " +
        StringConstants.TOKEN_AUTH_HEADER_PART + encode(accessKey) + "\", " +
        "oauth_version=\"" + encode("1.0") + "\"";

    final HttpHeaders headers = new HttpHeaders();
    headers.add(StringConstants.AUTHORIZATION, authorizationHeaderString);
    final HttpEntity requestEntity = new HttpEntity(null, headers);

    final String fullRequestUrl = generateFullRequestUrl(baseUrl, requestParams);

    return restTemplate.exchange(fullRequestUrl, httpMethod, requestEntity, String.class);
  }

  // for making the various media upload requests to perform chunked media upload
  // takes required params for each request type to form appropriate request
  public ResponseEntity<String> makeMediaUploadRequest(final MediaUploadCommand mediaUploadCommand,
      final Map<String, String> params, final byte[] mediaData) {

    final int timestamp = (int) (new Date().getTime() / 1000);
    final String nonce = RandomStringUtils.randomAlphanumeric(42);

    final String signature = generateSignature(HttpMethod.POST,
        "https://upload.twitter.com/1.1/media/upload.json", nonce, timestamp, params);

    final String authHeaderText = "OAuth oauth_consumer_key=\"" + consumerKey + "\", " +
        StringConstants.NONCE_AUTH_HEADER_PART + nonce + "\", " +
        StringConstants.SIGNATURE_AUTH_HEADER_PART + encode(signature) + "\", " +
        "oauth_signature_method=\"HMAC-SHA1\", " +
        StringConstants.TIMESTAMP_AUTH_HEADER_PART + timestamp + "\", " +
        StringConstants.TOKEN_AUTH_HEADER_PART + accessKey + "\", " +
        "oauth_version=\"1.0\"";

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.add(StringConstants.AUTHORIZATION, authHeaderText);

    String requestBodyString = null;
    HttpEntity request = null;
    String requestUrl = null;
    HttpMethod httpMethod = HttpMethod.POST;

    switch (mediaUploadCommand) {
      case INIT:

        try {
          requestBodyString =
              "command=INIT&total_bytes=" + params.get("total_bytes") + "&media_type="
                  + URLEncoder.encode(params.get("media_type"), StringConstants.UTF8)
                  + "&media_category=" +
                  params.get("media_category");
        } catch (Exception e) {
          log.error("Could not encode media_type {}: ", params.get("media_type"), e);
          return null;
        }
        request = new HttpEntity(requestBodyString, headers);
        requestUrl = StringConstants.MEDIA_UPLOAD_URL;
        break;

      case APPEND:

        final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("media_data", mediaData);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        request = new HttpEntity(body, headers);
        requestUrl = StringConstants.MEDIA_UPLOAD_URL + "&command=APPEND&media_id=" + params
            .get(StringConstants.MEDIA_ID) + "&segment_index=" + params.get("segment_index");
        break;

      case FINALIZE:

        requestBodyString = "command=FINALIZE&media_id=" + params.get(StringConstants.MEDIA_ID);
        request = new HttpEntity(requestBodyString, headers);
        requestUrl = "https://upload.twitter.com/1.1/media/upload.json?include_entities=true";
        break;

      case STATUS:

        requestUrl =
            "https://upload.twitter.com/1.1/media/upload.json?command=STATUS&media_id=" + params
                .get(StringConstants.MEDIA_ID);
        request = new HttpEntity(headers);
        httpMethod = HttpMethod.GET;
        break;

      default:
        return null;
    }

    return restTemplate.exchange(requestUrl, httpMethod, request, String.class);
  }

  // for making update status requests in response to mentions
  public void makeUpdateStatusRequest(final String status, final String replyToId,
      final String mediaIds) {
    final int timestamp = (int) (new Date().getTime() / 1000);
    final String nonce = RandomStringUtils.randomAlphanumeric(42);

    String requestBody = "status" + "=" + encode(status) + "&in_reply_to_status_id=" + replyToId;

    final Map<String, String> params = new HashMap<>();
    params.put("include_entities", "true");
    params.put("status", status);
    params.put("in_reply_to_status_id", replyToId);

    if (mediaIds != null) {
      requestBody = requestBody + "&media_ids=" + mediaIds;
      params.put("media_ids", mediaIds);
    }

    final String signature = generateSignature(HttpMethod.POST,
        "https://api.twitter.com/1.1/statuses/update.json", nonce, timestamp, params);

    final String authHeaderText = "OAuth oauth_consumer_key=\"" + consumerKey + "\", " +
        StringConstants.NONCE_AUTH_HEADER_PART + nonce + "\", " +
        StringConstants.SIGNATURE_AUTH_HEADER_PART + encode(signature) + "\", " +
        "oauth_signature_method=\"HMAC-SHA1\", " +
        StringConstants.TIMESTAMP_AUTH_HEADER_PART + timestamp + "\", " +
        StringConstants.TOKEN_AUTH_HEADER_PART + accessKey + "\", " +
        "oauth_version=\"1.0\"";

    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    httpHeaders.add(StringConstants.AUTHORIZATION, authHeaderText);

    HttpEntity request = new HttpEntity(requestBody, httpHeaders);
    restTemplate
        .postForObject(StringConstants.MEDIA_UPLOAD_URL, request, String.class);
  }

  private String generateSignature(final HttpMethod httpMethod, final String baseUrl,
      final String nonce, final int timestamp, final Map<String, String> additionalParams) {

    final Map<String, String> sortedParamMap = new TreeMap<>();
    sortedParamMap.put(encode("oauth_consumer_key"), encode(consumerKey));
    sortedParamMap.put(encode("oauth_nonce"), encode(nonce));
    sortedParamMap.put(encode("oauth_signature_method"), encode("HMAC-SHA1"));
    sortedParamMap.put(encode("oauth_timestamp"), encode(Integer.toString(timestamp)));
    sortedParamMap.put(encode("oauth_token"), encode(accessKey));
    sortedParamMap.put(encode("oauth_version"), encode("1.0"));
    for (final Entry<String, String> additionalParam : additionalParams.entrySet()) {
      sortedParamMap.put(encode(additionalParam.getKey()), encode(additionalParam.getValue()));
    }

    final String parameterString = generateParameterString(sortedParamMap);

    final String signatureBaseString =
        httpMethod.toString() + '&' + encode(baseUrl) + '&' + encode(parameterString);
    final String signingKey = encode(consumerSecret) + '&' + encode(accessSecret);

    return calculateSignatureWithKey(signatureBaseString, signingKey);
  }

  private String generateParameterString(final Map<String, String> params) {
    final StringBuilder parameterString = new StringBuilder();
    for (final Entry<String, String> param : params.entrySet()) {
      parameterString
          .append(param.getKey())
          .append('=')
          .append(param.getValue())
          .append('&');
    }
    return parameterString.substring(0, parameterString.length() - 1);
  }

  private String generateFullRequestUrl(final String baseUrl, final Map<String, String> params) {
    final StringBuilder fullRequestUrlBuilder = new StringBuilder(baseUrl);
    if (!params.isEmpty()) {
      fullRequestUrlBuilder.append('?');
      for (final Entry<String, String> param : params.entrySet()) {
        try {
          fullRequestUrlBuilder
              .append(param.getKey())
              .append('=')
              .append(URLEncoder.encode(param.getValue(), StringConstants.UTF8))
              .append('&');
        } catch (UnsupportedEncodingException e) {
          log.error("Could not encode param value for key {}:", param.getKey(), e);
        }
      }
      final String fullRequestUrl = fullRequestUrlBuilder.toString();
      return fullRequestUrl.substring(0, fullRequestUrl.length() - 1);
    }
    return fullRequestUrlBuilder.toString();
  }

  private String encode(final String value) {
    if (value != null) {
      String encoded = "";
      try {
        encoded = URLEncoder.encode(value, StringConstants.UTF8);
      } catch (final Exception e) {
        log.error("Unable to encode text ({}): ", value, e);
      }
      final StringBuilder sb = new StringBuilder();
      char focus;
      for (int i = 0; i < encoded.length(); i++) {
        focus = encoded.charAt(i);
        if (focus == '*') {
          sb.append("%2A");
        } else if (focus == '+') {
          sb.append("%20");
        } else if (focus == '%' && i + 1 < encoded.length()
            && encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E') {
          sb.append('~');
          i += 2;
        } else {
          sb.append(focus);
        }
      }
      return sb.toString();
    }
    return "";
  }

  private String toHexString(final byte[] bytes) {
    try (final Formatter formatter = new Formatter()) {
      for (final byte b : bytes) {
        formatter.format("%02x", b);
      }
      final byte[] decodedHex = Hex.decodeHex(formatter.toString());
      return Base64.getEncoder().encodeToString(decodedHex);
    } catch (final DecoderException e) {
      return "Could not convert base 64 signature to string: " + e.toString();
    }
  }

  private String calculateSignatureWithKey(final String data, final String key) {
    try {
      final SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
      final Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(signingKey);
      final byte[] byteArray = mac.doFinal(data.getBytes());
      return toHexString(byteArray);
    } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
      log.error("An exception occurred: ", e);
      return null;
    }
  }
}
