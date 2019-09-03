package net.lingala.zip4j.sevenzip.headers;

import net.lingala.zip4j.sevenzip.InternalSevenZipConstants;
import net.lingala.zip4j.sevenzip.model.*;
import net.lingala.zip4j.util.RawIO;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
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
    sevenZipRaf.readFully(intBuff);
    signatureHeader.setStartHeaderCRC(rawIO.readLongLittleEndian(intBuff, 0));

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
    startHeader.setNextHeaderCRC(rawIO.readLongLittleEndian(intBuff, 0));

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
      readUncompressedHeader(sevenZipRaf);
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

    // read archive properties if it exists
    if(tempByte == InternalSevenZipConstants.kArchiveProperties) {
      readArchiveProperties(sevenZipRaf);
      tempByte = rawIO.readByte(sevenZipRaf);
    }

    // read additional streams info if it exists
    if(tempByte == InternalSevenZipConstants.kAdditionalStreamsInfo) {
      // todo:read additional streams
      tempByte = rawIO.readByte(sevenZipRaf);
    }

    // read main streams info if it exists
    if(tempByte == InternalSevenZipConstants.kMainStreamsInfo) {
      readStreamsInfo(sevenZipRaf);
      tempByte = rawIO.readByte(sevenZipRaf);
    }
  }

  private void readArchiveProperties(RandomAccessFile sevenZipRaf) throws IOException {
    long propertySize;
    byte[] property;

    int propertyType = rawIO.readByte(sevenZipRaf);
    while(propertyType != InternalSevenZipConstants.kEnd) {
      propertySize = SevenZipHeaderUtil.readSevenZipUint64(rawIO, sevenZipRaf);
      // fixme:long is converted to int here,need to add some assert detection
      property = new byte[(int)propertySize];
      // todo: is property stored as little endian or not? where are these properties used?
      // todo: store properties in sevenZipModel
      sevenZipRaf.readFully(property);
    }
  }

  /**
   * Streams Info structure
   * ~~~~~~~~~~~~
   *
   *   []
   *   PackInfo
   *   []
   *
   *
   *   []
   *   CodersInfo
   *   []
   *
   *
   *   []
   *   SubStreamsInfo
   *   []
   *
   *   BYTE NID::kEnd
   * @param sevenZipRaf
   * @throws IOException
   */
  private void readStreamsInfo(RandomAccessFile sevenZipRaf) throws IOException {
    int tempByte = rawIO.readByte(sevenZipRaf);

    // read pack info if it exists
    if(tempByte == InternalSevenZipConstants.kPackInfo) {
      readPackInfo(sevenZipRaf);
      tempByte = rawIO.readByte(sevenZipRaf);
    }
  }

  /**
   * PackInfo
   * ~~~~~~~~~~~~
   *   BYTE NID::kPackInfo  (0x06)
   *   UINT64 PackPos
   *   UINT64 NumPackStreams
   *
   *   []
   *   BYTE NID::kSize    (0x09)
   *   UINT64 PackSizes[NumPackStreams]
   *   []
   *
   *   []
   *   BYTE NID::kCRC      (0x0A)
   *   PackStreamDigests[NumPackStreams]
   *   []
   *
   *   BYTE NID::kEnd
   * @param sevenZipRaf
   * @throws IOException
   */
  private void readPackInfo(RandomAccessFile sevenZipRaf) throws IOException {
    PackInfo packInfo = new PackInfo();
    sevenZipModel.setPackInfo(packInfo);

    // read pack position
    packInfo.setPackPos(SevenZipHeaderUtil.readSevenZipUint64(rawIO, sevenZipRaf));

    final long numPackStreamsLong = SevenZipHeaderUtil.readSevenZipUint64(rawIO, sevenZipRaf);
    // fixme: convert long to int here
    final int numPackStreamsInt = (int) numPackStreamsLong;

    int tempByte = rawIO.readByte(sevenZipRaf);
    // read pack sizes if it exists
    if(tempByte == InternalSevenZipConstants.kSize) {
      long[] packSizes = new long[numPackStreamsInt];
      for(int i = 0; i < numPackStreamsInt;i++) {
        packSizes[i] = SevenZipHeaderUtil.readSevenZipUint64(rawIO, sevenZipRaf);
      }
      packInfo.setPackSizes(packSizes);

      tempByte = rawIO.readByte(sevenZipRaf);
    }

    // read pack stream digests if it exists
    if(tempByte == InternalSevenZipConstants.kCRC) {
      Digests packStreamDigests = readDigests(sevenZipRaf, numPackStreamsInt);
      packInfo.setDigests(packStreamDigests);

      tempByte = rawIO.readByte(sevenZipRaf);
    }

    if(tempByte != InternalSevenZipConstants.kEnd) {
      // todo: throw new excepiton
    }
  }

  private Digests readDigests(RandomAccessFile sevenZipRaf, int numStreams) throws IOException {
    Digests digests = new Digests();
    BitSet bitSet = new BitSet();
    digests.setCrcDefinedBitSet(bitSet);

    final int allAreDefined = rawIO.readByte(sevenZipRaf);
    if(allAreDefined == 0) {
      int numStreamsDoneRead = 0;
      int mask;
      int numStreamsToRead;
      int tempByte;
      while(numStreamsDoneRead < numStreams) {
        mask = 0x80;
        numStreamsToRead = numStreams - numStreamsDoneRead > 8 ? 8:(numStreams - numStreamsDoneRead);
        tempByte = rawIO.readByte(sevenZipRaf);
        for(int i = 0;i < numStreamsToRead;i++) {
          bitSet.set(numStreamsDoneRead + i, (tempByte & mask) != 0);
          mask >>>= 1;
        }

        numStreamsDoneRead += numStreamsToRead;
      }
    } else {
      // all crcs are defined, set all bits to true
      bitSet = new BitSet();
      for(int i = 0;i < numStreams;i++) {
        bitSet.set(i);
      }
    }

    long[] CRCs = new long[numStreams];
    digests.setCRCs(CRCs);
    // read CRCs of streams that has CRC defined
    for(int i = 0;i < numStreams;i++) {
      if(bitSet.get(i)) {
        CRCs[i] = rawIO.readIntLittleEndian(sevenZipRaf);
      }
    }

    return digests;
  }
}
