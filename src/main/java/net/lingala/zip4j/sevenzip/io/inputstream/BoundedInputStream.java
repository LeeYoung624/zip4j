package net.lingala.zip4j.sevenzip.io.inputstream;

import java.io.IOException;
import java.io.InputStream;

public class BoundedInputStream extends InputStream {
  private InputStream inputStream;
  private long numberOfBytesRead = 0;
  private byte[] singleByteArray = new byte[1];
  private long bytesToRead;

  public BoundedInputStream(InputStream inputStream, long size) {
    this.inputStream = inputStream;
    this.bytesToRead = size;
  }

  @Override
  public int read() throws IOException {
    int readLen = read(singleByteArray);
    if (readLen == -1) {
      return -1;
    }

    return singleByteArray[0] & 0xff;
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {

    if (bytesToRead != -1) {
      if (numberOfBytesRead >= bytesToRead) {
        return -1;
      }

      if (len > bytesToRead - numberOfBytesRead) {
        len = (int) (bytesToRead - numberOfBytesRead);
      }
    }

    int readLen = inputStream.read(b, off, len);

    if (readLen > 0) {
      numberOfBytesRead += readLen;
    }

    return readLen;
  }

  @Override
  public void close() throws IOException {
//    inputStream.close();
    // input stream need to be closed mannually
  }
}
