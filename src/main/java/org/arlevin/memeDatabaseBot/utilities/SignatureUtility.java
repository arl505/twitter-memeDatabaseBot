package org.arlevin.memeDatabaseBot.utilities;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignatureUtility {
  private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

  private static String toHexString(byte[] bytes) {
    Formatter formatter = new Formatter();

    for (byte b : bytes) {
      formatter.format("%02x", b);
    }

    return formatter.toString();
  }

  public static String calculateRFC2104HMAC(String data, String key)
      throws NoSuchAlgorithmException, InvalidKeyException
  {
    SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
    Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
    mac.init(signingKey);
    return toHexString(mac.doFinal(data.getBytes()));
  }

  public static void main(String[] args) throws Exception {
    String hmac = calculateRFC2104HMAC("data", "key");

    log.info(hmac);
    assert hmac.equals("104152c5bfdca07bc633eebd46199f0255c9f49d");
  }
}