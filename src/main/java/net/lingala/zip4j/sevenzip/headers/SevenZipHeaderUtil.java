package net.lingala.zip4j.sevenzip.headers;

import net.lingala.zip4j.sevenzip.model.BindPair;
import net.lingala.zip4j.sevenzip.model.Folder;
import net.lingala.zip4j.util.RawIO;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

public class SevenZipHeaderUtil {
  private static RawIO rawIO = new RawIO();

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
   * @param sevenZipRaf
   * @return
   * @throws IOException
   */
  public static long readSevenZipUint64(RandomAccessFile sevenZipRaf) throws IOException {
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

  /**
   * Calculate unpack size of a folder. The unpack size should be the final output stream size
   * To find the final output stream, you need to find the only one output stream that is not
   * in bind pairs.
   *
   * @param folder
   * @return
   */
  public static long getFolderUnpackSize(Folder folder) {
    if(folder.getNumOutStreamsTotal() == 0) {
      return 0;
    }
    Set<Long> outIndexInBindPairsSet = new HashSet<Long>();
    for(BindPair bindPair : folder.getBindPairs()) {
      outIndexInBindPairsSet.add(bindPair.getOutIndex());
    }

    for(int i = 0; i < folder.getNumOutStreamsTotal();i++) {
      if(!outIndexInBindPairsSet.contains(i)) {
        return folder.getUnpackSizes()[i];
      }
    }

    return 0;
  }

  /**
   * read bits in Byte size
   *
   * @param sevenZipRaf
   * @param maxBits
   * @return
   * @throws IOException
   */
  public static BitSet readBitsAsBitSet(RandomAccessFile sevenZipRaf, int maxBits) throws IOException {
    BitSet bitSet = new BitSet();
    int numBitsDoneRead = 0;
    int mask;
    int numBitsToRead;
    int tempByte;
    while(numBitsDoneRead < maxBits) {
      mask = 0x80;
      numBitsToRead = maxBits - numBitsDoneRead > 8 ? 8:(maxBits - numBitsDoneRead);
      tempByte = rawIO.readByte(sevenZipRaf);
      for(int i = 0;i < numBitsToRead;i++) {
        bitSet.set(numBitsDoneRead + i, (tempByte & mask) != 0);
        mask >>>= 1;
      }

      numBitsDoneRead += numBitsToRead;
    }

    return bitSet;
  }

  /**
   * read bits with all are defined flag
   *
   * @param sevenZipRaf
   * @param maxBits
   * @return
   * @throws IOException
   */
  public static BitSet readBitsWithAllAreDefined(RandomAccessFile sevenZipRaf, int maxBits) throws IOException {
    BitSet bitSet = new BitSet();
    final int allAreDefined = rawIO.readByte(sevenZipRaf);
    if(allAreDefined == 0) {
      bitSet = SevenZipHeaderUtil.readBitsAsBitSet(sevenZipRaf, maxBits);
    } else {
      // all crcs are defined, set all bits to true
      for(int i = 0;i < maxBits;i++) {
        bitSet.set(i);
      }
    }

    return bitSet;
  }
}
