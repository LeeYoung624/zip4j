package net.lingala.zip4j.sevenzip.headers;

import net.lingala.zip4j.sevenzip.InternalSevenZipConstants;
import net.lingala.zip4j.sevenzip.model.SevenZipModel;
import net.lingala.zip4j.sevenzip.model.SignatureHeader;
import net.lingala.zip4j.sevenzip.model.StartHeader;
import net.lingala.zip4j.util.RawIO;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.CRC32;

/**
 * Helper class for reading 7zip headers, including signature header and header
 */
public class SevenZipHeaderReader {
  private RawIO rawIO = new RawIO();

  private SevenZipModel sevenZipModel;

  private CRC32 crc32 = new CRC32();

  private byte[] intBuff = new byte[4];

  private byte[] longBuff = new byte[8];

  public SevenZipModel readAllHeaders(RandomAccessFile sevenZipRaf) throws IOException {
    sevenZipModel = new SevenZipModel();

    // read signature header
    readSignatureHeader(sevenZipRaf);

    // read header
    readHeader(sevenZipRaf);

    return null;
  }

  private void readSignatureHeader(RandomAccessFile sevenZipRaf) throws IOException {
    SignatureHeader signatureHeader = new SignatureHeader();
    crc32.reset();

    // read digital signature
    long digitalSignature = rawIO.readLongLittleEndian(sevenZipRaf, InternalSevenZipConstants.DIGITAL_SIGNATURE_LENGTH);
    if(SevenZipHeaderSignature.SEVEN_ZIP_DIGTIAL_SIGNATURE.getValue() != digitalSignature) {
      // todo: throw exception
    }

    // read version
    signatureHeader.setArchiveVersionMajor(rawIO.readByte(sevenZipRaf));
    signatureHeader.setArchiveVersionMinor(rawIO.readByte(sevenZipRaf));

    // read signature header crc
    signatureHeader.setStartHeaderCRC(rawIO.readIntLittleEndian(sevenZipRaf));

    // read start header and validate crc32
    StartHeader startHeader = new StartHeader();
    signatureHeader.setStartHeader(startHeader);

    sevenZipRaf.readFully(longBuff);
    crc32.update(longBuff, 0, longBuff.length);
    startHeader.setNextHeaderOffset(rawIO.readLongLittleEndian(longBuff, 0));

    sevenZipRaf.readFully(longBuff);
    crc32.update(longBuff, 0, longBuff.length);
    startHeader.setNextHeaderSize(rawIO.readLongLittleEndian(longBuff, 0));

    sevenZipRaf.readFully(intBuff);
    crc32.update(intBuff, 0, intBuff.length);
    startHeader.setNextHeaderCRC(rawIO.readIntLittleEndian(intBuff));

    // verify crc
    if(crc32.getValue() != signatureHeader.getStartHeaderCRC()) {
      // todo: throw exception
    }

    sevenZipModel.setSignatureHeader(signatureHeader);
  }

  private void readHeader(RandomAccessFile sevenZipRaf) throws IOException {
    // verify crc
    if(!verifyHeaderCrc(sevenZipRaf)) {
      // todo:throw exception
    }

    // header start position = signature_header_size + nextHeaderOffset
    sevenZipRaf.seek(InternalSevenZipConstants.SIGNATURE_HEADER_SIZE + sevenZipModel.getSignatureHeader().getStartHeader().getNextHeaderOffset());

    int headerFlag = rawIO.readByte(sevenZipRaf);
    if(headerFlag == InternalSevenZipConstants.kEncodedHeader) {
      // compressed header read
    } else if (headerFlag == InternalSevenZipConstants.kHeader) {
      // uncompressed header read
    } else {
      // todo: throw exception
    }
  }

  /**
   * allocate nextHeaderBuffForCrc in method so that the allocated memory can be GC after CRC32 verify is finished
   * @param sevenZipRaf
   * @return
   * @throws IOException
   */
  private boolean verifyHeaderCrc(RandomAccessFile sevenZipRaf) throws IOException {
    // header start position = signature_header_size + nextHeaderOffset
    sevenZipRaf.seek(InternalSevenZipConstants.SIGNATURE_HEADER_SIZE + sevenZipModel.getSignatureHeader().getStartHeader().getNextHeaderOffset());
    crc32.reset();

    int nextHeaderSizeInt = (int)sevenZipModel.getSignatureHeader().getStartHeader().getNextHeaderSize();
    byte[] nextHeaderBuffForCrc = new byte[nextHeaderSizeInt];

    sevenZipRaf.readFully(nextHeaderBuffForCrc);
    crc32.update(nextHeaderBuffForCrc, 0, nextHeaderBuffForCrc.length);

    if(crc32.getValue() != sevenZipModel.getSignatureHeader().getStartHeader().getNextHeaderCRC()) {
      return false;
    }

    return true;
  }

  private void readUncompressedHeader(RandomAccessFile sevenZipRaf) throws IOException {
    int tempByte = rawIO.readByte(sevenZipRaf);

    // read archive properties is it exists
    if(tempByte == InternalSevenZipConstants.kArchiveProperties) {
      tempByte = rawIO.readByte(sevenZipRaf);
    }
  }

  private void readArchiveProperties(RandomAccessFile sevenZipRaf) throws IOException {
    long propertySize;
    byte[] property;

    int propertyType = rawIO.readByte(sevenZipRaf);
    while(propertyType != InternalSevenZipConstants.kEnd) {
      propertySize = rawIO.readLongLittleEndian(sevenZipRaf);
    }
  }
}
