package net.lingala.zip4j.sevenzip.model;

import java.util.List;

public class Folder {
  /// List of coders used in this folder, eg. one for compression, one for encryption.
  private Coder[] coders;

  /// Total number of input streams across all coders.
  /// this field is currently unused but technically part of the 7z API
  private long numInStreamsTotal;

  /// Total number of output streams across all coders.
  private long numOutStreamsTotal;

  /// Mapping between input and output streams.
  private BindPair[] bindPairs;

  /// Indeces of input streams, one per input stream not listed in bindPairs.
  private long[] packedStreams;

  /// Unpack sizes, per each output stream.
  private long[] unpackSizes;

  /// Whether the folder has a CRC.
  private boolean hasCrc;

  /// The CRC, if present.
  private long crc;

  /// The number of unpack substreams, product of the number of
  /// output streams and the number of non-empty files in this
  /// folder. Default to be 1 : at least 1 output stream
  private int numUnpackStreams = 1;

  private List<SevenZipFileEntry> files;

  // the offset of the folder, it equals to the offset of the first file belonging to this folder
  private long folderPackStreamOffset;

  // the packed size of this folder, it equals to the pack size of ther first input stream
  private long packedSize;

  public Coder[] getCoders() {
    return coders;
  }

  public void setCoders(Coder[] coders) {
    this.coders = coders;
  }

  public long getNumInStreamsTotal() {
    return numInStreamsTotal;
  }

  public void setNumInStreamsTotal(long numInStreamsTotal) {
    this.numInStreamsTotal = numInStreamsTotal;
  }

  public long getNumOutStreamsTotal() {
    return numOutStreamsTotal;
  }

  public void setNumOutStreamsTotal(long numOutStreamsTotal) {
    this.numOutStreamsTotal = numOutStreamsTotal;
  }

  public BindPair[] getBindPairs() {
    return bindPairs;
  }

  public void setBindPairs(BindPair[] bindPairs) {
    this.bindPairs = bindPairs;
  }

  public long[] getPackedStreams() {
    return packedStreams;
  }

  public void setPackedStreams(long[] packedStreams) {
    this.packedStreams = packedStreams;
  }

  public long[] getUnpackSizes() {
    return unpackSizes;
  }

  public void setUnpackSizes(long[] unpackSizes) {
    this.unpackSizes = unpackSizes;
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

  public int getNumUnpackStreams() {
    return numUnpackStreams;
  }

  public void setNumUnpackStreams(int numUnpackStreams) {
    this.numUnpackStreams = numUnpackStreams;
  }

  public List<SevenZipFileEntry> getFiles() {
    return files;
  }

  public void setFiles(List<SevenZipFileEntry> files) {
    this.files = files;
  }

  public long getFolderPackStreamOffset() {
    return folderPackStreamOffset;
  }

  public void setFolderPackStreamOffset(long folderPackStreamOffset) {
    this.folderPackStreamOffset = folderPackStreamOffset;
  }

  public long getPackedSize() {
    return packedSize;
  }

  public void setPackedSize(long packedSize) {
    this.packedSize = packedSize;
  }
}
