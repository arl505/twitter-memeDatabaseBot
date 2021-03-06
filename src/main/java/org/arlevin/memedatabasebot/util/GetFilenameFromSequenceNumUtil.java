package org.arlevin.memedatabasebot.util;

public class GetFilenameFromSequenceNumUtil {

  private GetFilenameFromSequenceNumUtil() {}

  // store the image using methodology found here:
  // https://serverfault.com/questions/95444/storing-a-million-images-in-the-filesystem:

  /*
  First pad your sequence number with leading zeroes until you have at least 12 digit string.
    This is the name for your file. You may want to add a suffix:
                 12345 -> 000000012345.jpg
  Then split the string to 2 or 3 character blocks where each block denotes a directory level.
    Have a fixed number of directory levels (for example 3):
                  000000012345 -> 000/000/012
  Store the file to under generated directory:
      Thus the full path and file filename for file with sequence id 123 is
                  000/000/012/00000000012345.jpg
      For file with sequence id 12345678901234 the path would be
                  123/456/789/12345678901234.jpg
   */

  public static String getFileName(String sequenceNumber, String fileSuffix) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int j = 0; j < 12 - sequenceNumber.length(); j++) {
      stringBuilder.append("0");
    }
    sequenceNumber = stringBuilder.toString() + sequenceNumber;

    return '/'
        + sequenceNumber.substring(0, 3) + '/'
        + sequenceNumber.substring(3, 6) + '/'
        + sequenceNumber.substring(6, 9) + '/'
        + sequenceNumber.substring(9, 12) + '/'
        + sequenceNumber
        + fileSuffix;
  }
}
