package net.lingala.zip4j.sevenzip.coders;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.sevenzip.model.Coder;
import net.lingala.zip4j.util.RawIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public abstract class SevenZipCoder {
  protected RawIO rawIO = new RawIO();

  protected char[] password;

  protected void validateCoderID(Coder coder) throws ZipException {
    if(!Arrays.equals(getCoderID(), coder.getCodecId())) {
      throw new ZipException("Coder ID mismatch, " + getCoderName() + " is expacted but got " + coder.getCodecId());
    }
  }

  public void setPassword(char[] password) {
    this.password = password;
  }

  public abstract byte[] getCoderID();

  public abstract String getCoderName();

  public abstract InputStream decode(Coder coder, InputStream inputStream, final long unpackSize) throws IOException;

  public abstract OutputStream encode(OutputStream outputStream);
}
