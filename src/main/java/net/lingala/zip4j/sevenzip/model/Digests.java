package net.lingala.zip4j.sevenzip.model;

import java.util.BitSet;

public class Digests {
  // Whether each stream has a CRC.
  private BitSet crcDefinedBitSet;

  // CRCs for each packed stream that has a CRC
  private long[] CRCs;

  public BitSet getCrcDefinedBitSet() {
    return crcDefinedBitSet;
  }

  public void setCrcDefinedBitSet(BitSet crcDefinedBitSet) {
    this.crcDefinedBitSet = crcDefinedBitSet;
  }

  public long[] getCRCs() {
    return CRCs;
  }

  public void setCRCs(long[] CRCs) {
    this.CRCs = CRCs;
  }
}
