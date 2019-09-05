package net.lingala.zip4j.sevenzip.model;

/// Properties for non-empty files.
public class SubStreamsInfo {
  /// Unpacked size of each unpacked stream.
  private long[] unpackSizes;

  // CRC info for sub streams, including whether or not a sub stream has a CRC,
  // and the CRC value of sub stream if it has CRC
  private Digests subStreamsDigests;

  public long[] getUnpackSizes() {
    return unpackSizes;
  }

  public void setUnpackSizes(long[] unpackSizes) {
    this.unpackSizes = unpackSizes;
  }

  public Digests getSubStreamsDigests() {
    return subStreamsDigests;
  }

  public void setSubStreamsDigests(Digests subStreamsDigests) {
    this.subStreamsDigests = subStreamsDigests;
  }
}