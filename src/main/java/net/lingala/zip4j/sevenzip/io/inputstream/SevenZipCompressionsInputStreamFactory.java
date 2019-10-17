package net.lingala.zip4j.sevenzip.io.inputstream;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.sevenzip.headers.SevenZipHeaderUtil;
import net.lingala.zip4j.sevenzip.model.Coder;
import net.lingala.zip4j.sevenzip.model.Folder;
import net.lingala.zip4j.sevenzip.model.enums.SevenZipCompressionMethod;
import net.lingala.zip4j.util.RawIO;
import org.tukaani.xz.LZMAInputStream;
import org.tukaani.xz.X86Options;

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

    if(Arrays.equals(SevenZipCompressionMethod.BCJ_X86_FILTER.getMethodCode(), compressionMethodCode)) {
      return new X86Options().getInputStream(inputStream);
    }

    throw new ZipException("the compression method with code of " + compressionMethodCode + " is not supported");
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
