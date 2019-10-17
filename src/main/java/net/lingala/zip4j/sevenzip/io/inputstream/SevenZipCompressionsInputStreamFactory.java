package net.lingala.zip4j.sevenzip.io.inputstream;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.sevenzip.headers.SevenZipHeaderUtil;
import net.lingala.zip4j.sevenzip.model.Coder;
import net.lingala.zip4j.sevenzip.model.Folder;
import net.lingala.zip4j.sevenzip.model.enums.SevenZipCompressionMethod;
import net.lingala.zip4j.util.RawIO;
import org.tukaani.xz.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class SevenZipCompressionsInputStreamFactory {
  private static RawIO rawIO = new RawIO();

  public static InputStream generateInputStream(InputStream inputStream, Coder coder, Long unpackSize) throws IOException {
    byte[] compressionMethodCode = coder.getCodecId();

    if(Arrays.equals(SevenZipCompressionMethod.LZMA.getMethodCode(), compressionMethodCode)) {
      final byte lzmaPropertyByte = coder.getProperties()[0];
      final int lzmaDictSize = rawIO.readIntLittleEndian(coder.getProperties(), 1);
      // currently not used
//    final int memoryUsageInKb = LZMAInputStream.getMemoryUsage(lzmaDictSize, lzmaPropertyByte);
      return new LZMAInputStream(inputStream, unpackSize, lzmaPropertyByte, lzmaDictSize);
    }

    if(Arrays.equals(SevenZipCompressionMethod.LZMA2.getMethodCode(), compressionMethodCode)) {
      final int lzma2DictSize = getLZMA2DictionarySize(coder.getProperties()[0] & 0xFF);
      return new LZMA2InputStream(inputStream, lzma2DictSize);
    }

    if(Arrays.equals(SevenZipCompressionMethod.BCJ_X86_FILTER.getMethodCode(), compressionMethodCode)) {
      return new X86Options().getInputStream(inputStream);
    }

    if(Arrays.equals(SevenZipCompressionMethod.BCJ_PPC_FILTER.getMethodCode(), compressionMethodCode)) {
      return new PowerPCOptions().getInputStream(inputStream);
    }

    if(Arrays.equals(SevenZipCompressionMethod.BCJ_IA64_FILTER.getMethodCode(), compressionMethodCode)) {
      return new IA64Options().getInputStream(inputStream);
    }

    if(Arrays.equals(SevenZipCompressionMethod.BCJ_ARM_FILTER.getMethodCode(), compressionMethodCode)) {
      return new ARMOptions().getInputStream(inputStream);
    }

    if(Arrays.equals(SevenZipCompressionMethod.BCJ_ARM_THUMB_FILTER.getMethodCode(), compressionMethodCode)) {
      return new ARMThumbOptions().getInputStream(inputStream);
    }

    if(Arrays.equals(SevenZipCompressionMethod.BCJ_SPARC_FILTER.getMethodCode(), compressionMethodCode)) {
      return new SPARCOptions().getInputStream(inputStream);
    }

    throw new ZipException("the compression method with code of " + compressionMethodCode + " is not supported");
  }

  /**
   * 40 indicates a 4GB - 1 dictionary size
   * Even values less than 40 indicate a 2^(v/2 + 12) bytes dictionary size
   * Odd values less than 40 indicate a 3*2^((v - 1)/2 + 11) bytes dictionary size
   * Values higher than 40 are invalid
   *
   * @param lzma2DictionarySizeByte
   * @return
   */
  private static int getLZMA2DictionarySize(int lzma2DictionarySizeByte) throws ZipException {
    if(lzma2DictionarySizeByte > 40) {
      throw new ZipException("LZMA2 dictionary size is too large");
    }

    if(lzma2DictionarySizeByte == 40) {
      return 0xFFFFFFFF;
    }

    if((lzma2DictionarySizeByte & 0x01) == 0) {
      return 1 << (lzma2DictionarySizeByte/2 + 12);
    }

    return 3 << ((lzma2DictionarySizeByte - 1)/2 + 11);
  }

  public static InputStream generateFolderInputStream(InputStream originalInputStream, Folder folder) throws IOException {
    InputStream inputStream = new BoundedInputStream(originalInputStream, folder.getPackedSize());
    List<Coder> orderedCoders = SevenZipHeaderUtil.getOrderedCodersInFolder(folder);
    long unpackSize;

    for(Coder coder : orderedCoders) {
      // todo: read options of each method
      unpackSize = SevenZipHeaderUtil.getUncompressedSizeForCoderInFolder(folder, coder);
      inputStream = generateInputStream(inputStream, coder, unpackSize);
    }

    return inputStream;
  }
}
