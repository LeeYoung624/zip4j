package net.lingala.zip4j.sevenzip.model;

import java.util.BitSet;

public class SevenZipModel {
  private SignatureHeader signatureHeader;

  /// pack info in seven zip header
  private PackInfo packInfo;

  private CodersInfo codersInfo;
  /// Temporary properties for non-empty files (subsumed into the files array later).
  SubStreamsInfo subStreamsInfo;
  /// The files and directories in the archive.
  SevenZipFileEntry[] files;
  /// Mapping between folders, files and streams.
//  StreamMap streamMap;


  public SignatureHeader getSignatureHeader() {
    return signatureHeader;
  }

  public void setSignatureHeader(SignatureHeader signatureHeader) {
    this.signatureHeader = signatureHeader;
  }

  public PackInfo getPackInfo() {
    return packInfo;
  }

  public void setPackInfo(PackInfo packInfo) {
    this.packInfo = packInfo;
  }

  public CodersInfo getCodersInfo() {
    return codersInfo;
  }

  public void setCodersInfo(CodersInfo codersInfo) {
    this.codersInfo = codersInfo;
  }

  public SubStreamsInfo getSubStreamsInfo() {
    return subStreamsInfo;
  }

  public void setSubStreamsInfo(SubStreamsInfo subStreamsInfo) {
    this.subStreamsInfo = subStreamsInfo;
  }

  public SevenZipFileEntry[] getFiles() {
    return files;
  }

  public void setFiles(SevenZipFileEntry[] files) {
    this.files = files;
  }
}
