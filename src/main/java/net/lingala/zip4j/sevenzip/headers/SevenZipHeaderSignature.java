package net.lingala.zip4j.sevenzip.headers;

public enum SevenZipHeaderSignature {
  SEVEN_ZIP_DIGTIAL_SIGNATURE(0x1c27afbc7a37L); // "7, Z, 0xBC, 0xAF, 0x27, 0x1C"
  private long value;

  SevenZipHeaderSignature(long value) {
    this.value = value;
  }

  public long getValue() {
    return value;
  }
}
