package org.arlevin.memeDatabaseBot.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Formatter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class TwitterClient {

  @Value("${credentials.consumer.key}")
  private String CONSUMER_KEY;

  @Value("${credentials.consumer.secret}")
  private String CONSUMER_SECRET;

  @Value("${credentials.access.key}")
  private String ACCESS_TOKEN;

  @Value("${credentials.access.secret}")
  private String ACCESS_TOKEN_SECRET;

  private static final String BASE_URL = "https://api.twitter.com";

  private final RestTemplate restTemplate = new RestTemplate();

  public ResponseEntity<String> makeRequest(final HttpMethod httpMethod,
      final String apiPath, final Map<String, String> requestParams) {

    final int timestamp = (int) (new Date().getTime() / 1000);
    final String nonce = RandomStringUtils.randomAlphanumeric(42);

    final String oauthSignature = generateSignature(httpMethod, apiPath, nonce, timestamp,
        requestParams);

    final String authorizationHeaderString = "OAuth " +
        "oauth_consumer_key=\"" + encode(CONSUMER_KEY) + "\", " +
        "oauth_nonce=\"" + encode(nonce) + "\", " +
        "oauth_signature=\"" + encode(oauthSignature) + "\", " +
        "oauth_signature_method=\"" + encode("HMAC-SHA1") + "\", " +
        "oauth_timestamp=\"" + encode(Integer.toString(timestamp)) + "\", " +
        "oauth_token=\"" + encode(ACCESS_TOKEN) + "\", " +
        "oauth_version=\"" + encode("1.0") + "\", ";

    final HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", authorizationHeaderString);
    final HttpEntity requestEntity = new HttpEntity(null, headers);

    final String fullRequestUrl = generateFullRequestUrl(apiPath, requestParams);

    return restTemplate.exchange(fullRequestUrl, httpMethod, requestEntity, String.class);
  }

  private String generateSignature(final HttpMethod httpMethod, final String apiPath,
      final String nonce, final int timestamp, final Map<String, String> additionalParams) {

    final String requestUrl = BASE_URL + apiPath;
    final Map<String, String> sortedParamMap = new TreeMap<>();
    sortedParamMap.put(encode("oauth_consumer_key"), encode(CONSUMER_KEY));
    sortedParamMap.put(encode("oauth_nonce"), encode(nonce));
    sortedParamMap.put(encode("oauth_signature_method"), encode("HMAC-SHA1"));
    sortedParamMap.put(encode("oauth_timestamp"), encode(Integer.toString(timestamp)));
    sortedParamMap.put(encode("oauth_token"), encode(ACCESS_TOKEN));
    sortedParamMap.put(encode("oauth_version"), encode("1.0"));
    for (final Entry<String, String> additionalParam : additionalParams.entrySet()) {
      sortedParamMap.put(encode(additionalParam.getKey()), encode(additionalParam.getValue()));
    }

    final String parameterString = generateParameterString(sortedParamMap);

    final String signatureBaseString =
        httpMethod.toString() + '&' + encode(requestUrl) + '&' + encode(parameterString);
    final String signingKey = encode(CONSUMER_SECRET) + '&' + encode(ACCESS_TOKEN_SECRET);

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

  private String generateFullRequestUrl(final String apiPath, final Map<String, String> params) {
    final StringBuilder fullRequestUrlBuilder = new StringBuilder(BASE_URL + apiPath);
    if (!params.isEmpty()) {
      fullRequestUrlBuilder.append('?');
      for (final Entry<String, String> param : params.entrySet()) {
        try {
          fullRequestUrlBuilder
              .append(param.getKey())
              .append('=')
              .append(URLEncoder.encode(param.getValue(), "UTF-8"))
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
        encoded = URLEncoder.encode(value, "UTF-8");
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
    try(final Formatter formatter = new Formatter()) {
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
