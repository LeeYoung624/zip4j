package net.lingala.zip4j.sevenzip.model;

public class StartHeader {
  private long nextHeaderOffset;

  private long nextHeaderSize;

  private long nextHeaderCRC;

  public long getNextHeaderOffset() {
    return nextHeaderOffset;
  }

  public void setNextHeaderOffset(long nextHeaderOffset) {
    this.nextHeaderOffset = nextHeaderOffset;
  }

  public long getNextHeaderSize() {
    return nextHeaderSize;
  }

  public void setNextHeaderSize(long nextHeaderSize) {
    this.nextHeaderSize = nextHeaderSize;
  }

  public long getNextHeaderCRC() {
    return nextHeaderCRC;
  }

  public void setNextHeaderCRC(long nextHeaderCRC) {
    this.nextHeaderCRC = nextHeaderCRC;
  }
}
