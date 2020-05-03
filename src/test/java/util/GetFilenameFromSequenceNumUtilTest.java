package util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.arlevin.memedatabasebot.util.GetFilenameFromSequenceNumUtil;
import org.junit.jupiter.api.Test;

public class GetFilenameFromSequenceNumUtilTest {

  @Test
  public void getFilename_with1DigitSequenceNumber_returnExpectedFilename() {
    final String expectedFilename = "/000/000/000/001/000000000001.mp4";

    final String actualFilename = GetFilenameFromSequenceNumUtil.getFileName("1", ".mp4");

    assertEquals(expectedFilename, actualFilename);
  }

  @Test
  public void getFilename_with12DigitSequenceNumber_returnExpectedFilename() {
    final String expectedFilename = "/123/456/789/012/123456789012.mp4";

    final String actualFilename = GetFilenameFromSequenceNumUtil.getFileName("123456789012", ".mp4");

    assertEquals(expectedFilename, actualFilename);
  }
}
