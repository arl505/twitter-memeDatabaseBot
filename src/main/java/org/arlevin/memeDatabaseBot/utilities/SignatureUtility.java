package org.arlevin.memeDatabaseBot.utilities;

import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SignatureUtility {

  @Value("${auth.consumer.apiKey}")
  private String consumerApiKey;

  @Value("${auth.consumer.apiSecretKey}")
  private String consumerApiSecretKey;

  @Value("${auth.access.token}")
  private String accessToken;

  @Value("${auth.access.tokenSecret}")
  private String accessTokenSecret;

  public String calculateStatusUpdateSignature(String status, String timestamp, String nonce) {
    String parameterString = generateParameterString(status, timestamp, nonce);

    String signatureBaseString =
        "POST&"
        + encode("https://api.twitter.com/1.1/statuses/update.json")
        + "&"
        + encode(parameterString);

    String signingKey = consumerApiSecretKey + '&' + accessTokenSecret;

    String signature;
    try {
      signature = calculateRFC2104HMAC(signatureBaseString, signingKey);
      signature = encode(signature);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      signature = "Could not calculate hash";
    }
    return signature;
  }

  private String generateParameterString(String status, String timestamp, String nonce) {
    String _includeEntitiesKey = encode("include_entities");
    String _statusKey = encode("status");
    String _consumerKeyKey = encode("oauth_consumer_key");
    String _nonceKey = encode("oauth_nonce");
    String _signatureMethodKey = encode("oauth_signature_method");
    String _timestampKey = encode("oauth_timestamp");
    String _tokenKey = encode("oauth_token");
    String _oauthVersionKey = encode("oauth_version");

    String _includeEntities = encode("true");
    String _status = encode(status);
    String _consumerKey = encode(consumerApiKey);
    String _nonce = encode(nonce);
    String _signatureMethod = encode("HMAC-SHA1");
    String _timestamp = encode(timestamp);
    String _token = encode(accessToken);
    String _oauthVersion = encode("1.0");

    Map<String, String> keyValuePairs = new HashMap<>();
    keyValuePairs.put(_includeEntitiesKey, _includeEntities);
    keyValuePairs.put(_statusKey, _status);
    keyValuePairs.put(_consumerKeyKey, _consumerKey);
    keyValuePairs.put(_nonceKey, _nonce);
    keyValuePairs.put(_signatureMethodKey, _signatureMethod);
    keyValuePairs.put(_timestampKey, _timestamp);
    keyValuePairs.put(_tokenKey, _token);
    keyValuePairs.put(_oauthVersionKey, _oauthVersion);

    List<String> encodedKeys = new ArrayList<>();
    encodedKeys.add(_includeEntitiesKey);
    encodedKeys.add(_statusKey);
    encodedKeys.add(_consumerKeyKey);
    encodedKeys.add(_nonceKey);
    encodedKeys.add(_signatureMethodKey);
    encodedKeys.add(_timestampKey);
    encodedKeys.add(_tokenKey);
    encodedKeys.add(_oauthVersionKey);

    Collections.sort(encodedKeys);

    String parameterString = "";

    for(int i = 0; i < encodedKeys.size(); i++) {
      String key = encodedKeys.get(i);

      parameterString = parameterString.concat(key)
          .concat("=")
          .concat(keyValuePairs.get(key));
      if(i != encodedKeys.size()-1) {
        parameterString = parameterString.concat("&");
      }
    }

    return parameterString;
  }

  public String encode(String value) {
    String encoded = "";
    try {
      encoded = URLEncoder.encode(value, "UTF-8");
    } catch (Exception e) {
      log.error("Unable to encode text ({}): {}", value, e.toString());
    }
    String sb = "";
    char focus;
    for (int i = 0; i < encoded.length(); i++) {
      focus = encoded.charAt(i);
      if (focus == '*') {
        sb += "%2A";
      } else if (focus == '+') {
        sb += "%20";
      } else if (focus == '%' && i + 1 < encoded.length()
          && encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E') {
        sb += '~';
        i += 2;
      } else {
        sb += focus;
      }
    }
    return sb.toString();
  }

  private static String toHexString(byte[] bytes) {
    Formatter formatter = new Formatter();

    for (byte b : bytes) {
      formatter.format("%02x", b);
    }

    try {
      byte[] decodedHex = Hex.decodeHex(formatter.toString());
      return Base64.getEncoder().encodeToString(decodedHex);
    } catch (DecoderException e) {
      return "Could not convert base 64 signature to string: " + e.toString();
    }
  }

  private static String calculateRFC2104HMAC(String data, String key)
      throws NoSuchAlgorithmException, InvalidKeyException
  {
    SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(signingKey);
    byte[] byteArray = mac.doFinal(data.getBytes());
    return toHexString(byteArray);
  }
}