package net.lingala.zip4j.sevenzip.model;

import java.util.BitSet;

public class PackInfo {
  // Offset from beginning of file + SIGNATURE_HEADER_SIZE to packed streams.
  private long packPos;

  // Size of each packed stream
  private long[] packSizes;

  // Whether each particular packed streams has a CRC.
  private BitSet packCrcsDefined;

  // CRCs for each packed stream that has a CRC
  private int[] CRCs;
}
