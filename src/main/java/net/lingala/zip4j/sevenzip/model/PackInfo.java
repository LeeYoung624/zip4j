package net.lingala.zip4j.sevenzip.model;

import java.util.BitSet;

public class PackInfo {
  // Offset from beginning of file + SIGNATURE_HEADER_SIZE to packed streams.
  private long packPos;

  // Size of each packed stream
  private long[] packSizes;

  // crc info digeests
  private Digests digests;

  public long getPackPos() {
    return packPos;
  }

  public void setPackPos(long packPos) {
    this.packPos = packPos;
  }

  public long[] getPackSizes() {
    return packSizes;
  }

  public void setPackSizes(long[] packSizes) {
    this.packSizes = packSizes;
  }

  public Digests getDigests() {
    return digests;
  }

  public void setDigests(Digests digests) {
    this.digests = digests;
  }
}
