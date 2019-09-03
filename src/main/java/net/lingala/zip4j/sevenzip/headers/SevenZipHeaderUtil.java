package net.lingala.zip4j.sevenzip.headers;

import net.lingala.zip4j.util.RawIO;

import java.io.IOException;
import java.io.RandomAccessFile;

public class SevenZipHeaderUtil {
  /**
   * UINT64 means real UINT64 encoded with the following scheme:
   *
   *   Size of encoding sequence depends from first byte:
   *   First_Byte  Extra_Bytes        Value
   *   (binary)
   *   0xxxxxxx               : ( xxxxxxx           )
   *   10xxxxxx    BYTE y[1]  : (  xxxxxx << (8 * 1)) + y
   *   110xxxxx    BYTE y[2]  : (   xxxxx << (8 * 2)) + y
   *   ...
   *   1111110x    BYTE y[6]  : (       x << (8 * 6)) + y
   *   11111110    BYTE y[7]  :                         y
   *   11111111    BYTE y[8]  :                         y
   * @param rawIO
   * @param sevenZipRaf
   * @return
   * @throws IOException
   */
  public static long readSevenZipUint64(RawIO rawIO, RandomAccessFile sevenZipRaf) throws IOException {
    int uint64FirstByteMask = 0x80;
    final long firestByte = rawIO.readByte(sevenZipRaf);

    int extraByteLength = 0;
    for(int i = 0; i < 8;i++,extraByteLength++) {
      if((firestByte & uint64FirstByteMask) == 0) {
        break;
      }

      uint64FirstByteMask >>>= 1;
    }

    int uint64ValueMask = uint64FirstByteMask - 1;
    long result = (firestByte & uint64ValueMask) << (8 * extraByteLength);
    long extraByte;
    for(int i = 0;i < extraByteLength;i++) {
      // convert int(rawIO.readByte) to long
      extraByte = rawIO.readByte(sevenZipRaf);
      result |= extraByte << (8 * i);
    }

    return result;
  }
}
