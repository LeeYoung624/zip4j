package net.lingala.zip4j.sevenzip.model;

public class Coder {
  private boolean isComplexCoder;

  private byte[] codecId;

  private long numInStreams;

  private long numOutStreams;

  private byte[] properties;

  public boolean isComplexCoder() {
    return isComplexCoder;
  }

  public void setComplexCoder(boolean complexCoder) {
    isComplexCoder = complexCoder;
  }

  public byte[] getCodecId() {
    return codecId;
  }

  public void setCodecId(byte[] codecId) {
    this.codecId = codecId;
  }

  public long getNumInStreams() {
    return numInStreams;
  }

  public void setNumInStreams(long numInStreams) {
    this.numInStreams = numInStreams;
  }

  public long getNumOutStreams() {
    return numOutStreams;
  }

  public void setNumOutStreams(long numOutStreams) {
    this.numOutStreams = numOutStreams;
  }

  public byte[] getProperties() {
    return properties;
  }

  public void setProperties(byte[] properties) {
    this.properties = properties;
  }
}
