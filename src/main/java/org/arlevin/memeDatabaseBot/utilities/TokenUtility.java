package org.arlevin.memeDatabaseBot.utilities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.social.oauth1.OAuthToken;

public class TokenUtility {

  @Getter
  @Setter
  private static OAuthToken requestToken;

  @Getter
  @Setter
  private static OAuthToken accessToken;
}
