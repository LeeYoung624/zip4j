package net.lingala.zip4j.sevenzip.model;

public class SignatureHeader {
  private int archiveVersionMajor;

  private int archiveVersionMinor;

  private long startHeaderCRC;

  private StartHeader startHeader;

  public int getArchiveVersionMajor() {
    return archiveVersionMajor;
  }

  public void setArchiveVersionMajor(int archiveVersionMajor) {
    this.archiveVersionMajor = archiveVersionMajor;
  }

  public int getArchiveVersionMinor() {
    return archiveVersionMinor;
  }

  public void setArchiveVersionMinor(int archiveVersionMinor) {
    this.archiveVersionMinor = archiveVersionMinor;
  }

  public long getStartHeaderCRC() {
    return startHeaderCRC;
  }

  public void setStartHeaderCRC(long startHeaderCRC) {
    this.startHeaderCRC = startHeaderCRC;
  }

  public StartHeader getStartHeader() {
    return startHeader;
  }

  public void setStartHeader(StartHeader startHeader) {
    this.startHeader = startHeader;
  }
}
