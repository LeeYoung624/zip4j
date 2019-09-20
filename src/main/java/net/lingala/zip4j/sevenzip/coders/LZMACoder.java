package net.lingala.zip4j.sevenzip.coders;

import net.lingala.zip4j.sevenzip.model.Coder;
import org.tukaani.xz.LZMAInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LZMACoder extends SevenZipCoder {
  public byte[] getCoderID() {
    return new byte[] { (byte)0x03, (byte)0x01, (byte)0x01 };
  }

  public String getCoderName() {
    return "LZMA";
  }

  @Override
  public InputStream decode(Coder coder, InputStream inputStream, final long unpackSize) throws IOException {
    validateCoderID(coder);

    final byte lzmaPropertyByte = coder.getProperties()[0];
    final int lzmaDictSize = rawIO.readIntLittleEndian(coder.getProperties(), 1);
    // currently not used
//    final int memoryUsageInKb = LZMAInputStream.getMemoryUsage(lzmaDictSize, lzmaPropertyByte);
    return new LZMAInputStream(inputStream, unpackSize, lzmaPropertyByte, lzmaDictSize);
  }

  @Override
  public OutputStream encode(OutputStream outputStream) {
    return null;
  }
}
