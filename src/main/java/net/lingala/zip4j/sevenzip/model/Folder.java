package net.lingala.zip4j.sevenzip.model;

public class Folder {
  /// List of coders used in this folder, eg. one for compression, one for encryption.
  private Coder[] coders;

  /// Total number of input streams across all coders.
  /// this field is currently unused but technically part of the 7z API
  private long numInputStreamsTotal;

  /// Total number of output streams across all coders.
  private long numOutputStreamsTotal;

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
  /// folder.
  private int numUnpackSubStreams;
}
