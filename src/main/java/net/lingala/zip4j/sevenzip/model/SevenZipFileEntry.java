package net.lingala.zip4j.sevenzip.model;

public class SevenZipFileEntry {
  private String name;
  private boolean hasStream;
  private boolean isDirectory;
  private boolean isAntiItem;
  private boolean hasCreationDate;
  private boolean hasLastModifiedDate;
  private boolean hasAccessDate;
  private long creationDate;
  private long lastModifiedDate;
  private long accessDate;
  private boolean hasAttributes;
  private int attributes;
  private boolean hasCrc;
  private long crc;
  private long compressedCrc;
  private long size;
  private long compressedSize;
//  private Iterable<? extends SevenZMethodConfiguration> contentMethods;


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isHasStream() {
    return hasStream;
  }

  public void setHasStream(boolean hasStream) {
    this.hasStream = hasStream;
  }

  public boolean isDirectory() {
    return isDirectory;
  }

  public void setDirectory(boolean directory) {
    isDirectory = directory;
  }

  public boolean isAntiItem() {
    return isAntiItem;
  }

  public void setAntiItem(boolean antiItem) {
    isAntiItem = antiItem;
  }

  public boolean isHasCreationDate() {
    return hasCreationDate;
  }

  public void setHasCreationDate(boolean hasCreationDate) {
    this.hasCreationDate = hasCreationDate;
  }

  public boolean isHasLastModifiedDate() {
    return hasLastModifiedDate;
  }

  public void setHasLastModifiedDate(boolean hasLastModifiedDate) {
    this.hasLastModifiedDate = hasLastModifiedDate;
  }

  public boolean isHasAccessDate() {
    return hasAccessDate;
  }

  public void setHasAccessDate(boolean hasAccessDate) {
    this.hasAccessDate = hasAccessDate;
  }

  public long getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(long creationDate) {
    this.creationDate = creationDate;
  }

  public long getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(long lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public long getAccessDate() {
    return accessDate;
  }

  public void setAccessDate(long accessDate) {
    this.accessDate = accessDate;
  }

  public boolean isHasAttributes() {
    return hasAttributes;
  }

  public void setHasAttributes(boolean hasAttributes) {
    this.hasAttributes = hasAttributes;
  }

  public int getAttributes() {
    return attributes;
  }

  public void setAttributes(int attributes) {
    this.attributes = attributes;
  }

  public boolean isHasCrc() {
    return hasCrc;
  }

  public void setHasCrc(boolean hasCrc) {
    this.hasCrc = hasCrc;
  }

  public long getCrc() {
    return crc;
  }

  public void setCrc(long crc) {
    this.crc = crc;
  }

  public long getCompressedCrc() {
    return compressedCrc;
  }

  public void setCompressedCrc(long compressedCrc) {
    this.compressedCrc = compressedCrc;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public long getCompressedSize() {
    return compressedSize;
  }

  public void setCompressedSize(long compressedSize) {
    this.compressedSize = compressedSize;
  }
}
