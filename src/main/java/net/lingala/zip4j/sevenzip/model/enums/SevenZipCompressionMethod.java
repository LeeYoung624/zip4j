package net.lingala.zip4j.sevenzip.model.enums;

import net.lingala.zip4j.sevenzip.coders.LZMACoder;
import net.lingala.zip4j.sevenzip.coders.SevenZipCoder;
import net.lingala.zip4j.sevenzip.model.Coder;

import java.util.Arrays;

public enum SevenZipCompressionMethod {
  /** no compression at all */
  LZMA(new LZMACoder());
//  COPY(new byte[] { (byte)0x00 }),
//  /** LZMA - only supported when reading */
//  LZMA(new byte[] { (byte)0x03, (byte)0x01, (byte)0x01 }),
//  /** LZMA2 */
//  LZMA2(new byte[] { (byte)0x21 }),
//  /** Deflate */
//  DEFLATE(new byte[] { (byte)0x04, (byte)0x01, (byte)0x08 }),
//  /**
//   * Deflate64
//   * @since 1.16
//   */
//  DEFLATE64(new byte[] { (byte)0x04, (byte)0x01, (byte)0x09 }),
//  /** BZIP2 */
//  BZIP2(new byte[] { (byte)0x04, (byte)0x02, (byte)0x02 }),
//  /**
//   * AES encryption with a key length of 256 bit using SHA256 for
//   * hashes - only supported when reading
//   */
//  AES256SHA256(new byte[] { (byte)0x06, (byte)0xf1, (byte)0x07, (byte)0x01 }),
//  /**
//   * BCJ x86 platform version 1.
//   * @since 1.8
//   */
//  BCJ_X86_FILTER(new byte[] { 0x03, 0x03, 0x01, 0x03 }),
//  /**
//   * BCJ PowerPC platform.
//   * @since 1.8
//   */
//  BCJ_PPC_FILTER(new byte[] { 0x03, 0x03, 0x02, 0x05 }),
//  /**
//   * BCJ I64 platform.
//   * @since 1.8
//   */
//  BCJ_IA64_FILTER(new byte[] { 0x03, 0x03, 0x04, 0x01 }),
//  /**
//   * BCJ ARM platform.
//   * @since 1.8
//   */
//  BCJ_ARM_FILTER(new byte[] { 0x03, 0x03, 0x05, 0x01 }),
//  /**
//   * BCJ ARM Thumb platform.
//   * @since 1.8
//   */
//  BCJ_ARM_THUMB_FILTER(new byte[] { 0x03, 0x03, 0x07, 0x01 }),
//  /**
//   * BCJ Sparc platform.
//   * @since 1.8
//   */
//  BCJ_SPARC_FILTER(new byte[] { 0x03, 0x03, 0x08, 0x05 }),
//  /**
//   * Delta filter.
//   * @since 1.8
//   */
//  DELTA_FILTER(new byte[] { 0x03 });

  private byte[] methodCode;

  private SevenZipCoder sevenZipCoder;

  <T extends SevenZipCoder> SevenZipCompressionMethod (T t) {
    this.methodCode = t.getCoderID();
    this.sevenZipCoder = t;
  }

  public byte[] getMethodCode() {
    return methodCode;
  }

  public static SevenZipCompressionMethod getCompressionMethodFromCode(Coder coder) {
    return getCompressionMethodFromCode(coder.getCodecId());
  }

  public static SevenZipCompressionMethod getCompressionMethodFromCode(byte[] code) {
    for(SevenZipCompressionMethod compressionMethod : values()) {
      if(Arrays.equals(compressionMethod.getMethodCode(), code)) {
        return compressionMethod;
      }
    }

    return null;
  }

  public SevenZipCoder getSevenZipCoder() {
    return sevenZipCoder;
  }
}
