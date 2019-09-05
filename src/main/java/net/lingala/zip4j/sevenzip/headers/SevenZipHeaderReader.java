package net.lingala.zip4j.sevenzip.headers;

import net.lingala.zip4j.sevenzip.InternalSevenZipConstants;
import net.lingala.zip4j.sevenzip.model.*;
import net.lingala.zip4j.util.RawIO;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.zip.CRC32;
import java.util.zip.ZipException;

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
      throw new ZipException("7z file signature mismatch");
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
      throw new ZipException("7z signature header crc32 validation failed");
    }

    sevenZipModel.setSignatureHeader(signatureHeader);
  }

  private void readHeader(RandomAccessFile sevenZipRaf) throws IOException {
    // verify crc
    if(!verifyHeaderCrc(sevenZipRaf)) {
      throw new ZipException("7z next header crc32 validation failed");
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
      throw new ZipException("7z read next header failed, property id is expected to be kEncodedHeader or kHeader, but got " + headerFlag);
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
      propertySize = SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf);
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

    // read coders info if it exists
    if(tempByte == InternalSevenZipConstants.kUnpackInfo) {
      readCodersInfo(sevenZipRaf);
      tempByte = rawIO.readByte(sevenZipRaf);
      // todo: what if no coders info ?
    }

    // read substreams info if it exists
    if(tempByte == InternalSevenZipConstants.kSubStreamsInfo) {
      readSubStreamsInfo(sevenZipRaf);
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
    packInfo.setPackPos(SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf));

    final long numPackStreamsLong = SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf);
    // fixme: convert long to int here
    final int numPackStreamsInt = (int) numPackStreamsLong;

    int tempByte = rawIO.readByte(sevenZipRaf);
    // read pack sizes if it exists
    if(tempByte == InternalSevenZipConstants.kSize) {
      long[] packSizes = new long[numPackStreamsInt];
      for(int i = 0; i < numPackStreamsInt;i++) {
        packSizes[i] = SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf);
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
      throw new ZipException("7z read pack info error, end byte should be kEnd but got " + tempByte);
    }
  }

  /**
   * Coders Info
   * ~~~~~~~~~~~
   *
   *   BYTE NID::kUnPackInfo  (0x07)
   *
   *
   *   BYTE NID::kFolder  (0x0B)
   *   UINT64 NumFolders
   *   BYTE External
   *   switch(External)
   *   {
   *     case 0:
   *       Folders[NumFolders]
   *     case 1:
   *       UINT64 DataStreamIndex
   *   }
   *
   *
   *   BYTE ID::kCodersUnPackSize  (0x0C)
   *   for(Folders)
   *     for(Folder.NumOutStreams)
   *      UINT64 UnPackSize;
   *
   *
   *   []
   *   BYTE NID::kCRC   (0x0A)
   *   UnPackDigests[NumFolders]
   *   []
   *
   *
   *
   *   BYTE NID::kEnd
   *
   * @param sevenZipRaf
   * @throws IOException
   */
  private void readCodersInfo(RandomAccessFile sevenZipRaf) throws IOException {
    CodersInfo codersInfo = new CodersInfo();
    sevenZipModel.setCodersInfo(codersInfo);

    int tempByte = rawIO.readByte(sevenZipRaf);
    if(tempByte != InternalSevenZipConstants.kFolder) {
      throw new ZipException("7z read coders info failed, kFolder is expected but got " + tempByte);
    }

    final long numFolders = SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf);
    // fixme:numFolders transform to int
    final int numFoldersInt = (int)numFolders;
    final int external = rawIO.readByte(sevenZipRaf);

    Folder[] folders = new Folder[numFoldersInt];
    codersInfo.setFolders(folders);
    if(external == 0) {
      for(int i = 0; i < numFolders;i++) {
        folders[i] = readFolder(sevenZipRaf);
      }
    } else {
      //todo: datastreamindex?
    }

    // read unpack size
    tempByte = rawIO.readByte(sevenZipRaf);
    if(tempByte != InternalSevenZipConstants.kCodersUnpackSize) {
      throw new ZipException("7z read coders info failed, kCodersUnpackSize is expected but got " + tempByte);
    }

    for(Folder folder : folders) {
      // todo:convert from long to int here
      final int numOutStreamsTotalInt = (int) folder.getNumOutStreamsTotal();
      long[] unpackSizes = new long[numOutStreamsTotalInt];
      for(int i = 0; i < numOutStreamsTotalInt;i++) {
        unpackSizes[i] = SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf);
      }
      folder.setUnpackSizes(unpackSizes);
    }

    tempByte = rawIO.readByte(sevenZipRaf);
    // read crc info if it exists
    if(tempByte == InternalSevenZipConstants.kCRC) {
      // put crc info in each Folder object
      Digests unpackDigests = readDigests(sevenZipRaf, numFoldersInt);
      for(int i = 0;i < numFoldersInt;i++) {
        if(unpackDigests.getCrcDefinedBitSet().get(i)) {
          folders[i].setHasCrc(true);
          folders[i].setCrc(unpackDigests.getCRCs()[i]);
        } else {
          folders[i].setHasCrc(false);
        }
      }
      tempByte = rawIO.readByte(sevenZipRaf);
    }

    if(tempByte != InternalSevenZipConstants.kEnd) {
      throw new ZipException("7z read coders info failed, kEnd is expected but got " + tempByte);
    }

  }

  /**
   * Folder
   * ~~~~~~
   *   UINT64 NumCoders;
   *   for (NumCoders)
   *   {
   *     BYTE
   *     {
   *       0:3 CodecIdSize
   *       4:  Is Complex Coder
   *       5:  There Are Attributes
   *       6:  Reserved
   *       7:  There are more alternative methods. (Not used anymore, must be 0).
   *     }
   *     BYTE CodecId[CodecIdSize]
   *     if (Is Complex Coder)
   *     {
   *       UINT64 NumInStreams;
   *       UINT64 NumOutStreams;
   *     }
   *     if (There Are Attributes)
   *     {
   *       UINT64 PropertiesSize
   *       BYTE Properties[PropertiesSize]
   *     }
   *   }
   *
   *   NumBindPairs = NumOutStreamsTotal - 1;
   *
   *   for (NumBindPairs)
   *   {
   *     UINT64 InIndex;
   *     UINT64 OutIndex;
   *   }
   *
   *   NumPackedStreams = NumInStreamsTotal - NumBindPairs;
   *   if (NumPackedStreams > 1)
   *     for(NumPackedStreams)
   *     {
   *       UINT64 Index;
   *     };
   *
   * @param sevenZipRaf
   * @return
   */
  private Folder readFolder(RandomAccessFile sevenZipRaf) throws IOException {
    Folder folder = new Folder();
    final long numCoders = SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf);
    // fixme: numCoders transform to int
    final int numCodersInt = (int) numCoders;

    Coder[] coders = new Coder[numCodersInt];
    for(int i = 0; i < numCodersInt;i++) {
      coders[i] = readCoder(sevenZipRaf);
    }
    folder.setCoders(coders);
    long numInStreamsTotal = 0;
    long numOutStreamsTotal = 0;
    for (Coder coder: coders) {
      numInStreamsTotal += coder.getNumInStreams();
      numOutStreamsTotal += coder.getNumOutStreams();
    }
    folder.setNumInStreamsTotal(numInStreamsTotal);
    folder.setNumOutStreamsTotal(numOutStreamsTotal);

    // read bind pairs
    // todo:assert numOutStreamsTotal > 0 ?
    final long numBindPairs = numOutStreamsTotal - 1;
    // todo:convert from long to int here
    final int numBindPairsInt = (int) numBindPairs;
    BindPair[] bindPairs = new BindPair[numBindPairsInt];
    for(int i = 0; i < numBindPairsInt;i++) {
      bindPairs[i].setInIndex(SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf));
      bindPairs[i].setOutIndex(SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf));
    }
    folder.setBindPairs(bindPairs);

    // read packed streams
    if(numInStreamsTotal <= numBindPairs) {
      throw new ZipException("7z read header failed, numInStreamsTotal should be greater than numBindPairs");
    }
    final long numPackedStreams = numInStreamsTotal - numBindPairs;
    // todo:convert from long to int here
    final int numPackedStreamsInt = (int) numPackedStreams;
    long[] packedStreams = new long[numPackedStreamsInt];

    if(numPackedStreamsInt == 1) {
      // need to find out the packedStreams that is not in bind pairs
      Set<Long> inIndexInBindPairsSet = new HashSet<Long>();
      for(BindPair bindPair : bindPairs) {
        inIndexInBindPairsSet.add(bindPair.getInIndex());
      }

      // todo: assert if more than 1 packed stream index is found, or can't find packed stream index
      for(long i = 0;i < numInStreamsTotal;i++) {
        if(!inIndexInBindPairsSet.contains(i)) {
          packedStreams[0] = i;
          break;
        }
      }
    } else {
      for(int i = 0; i < numPackedStreamsInt;i++) {
        packedStreams[i] = SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf);
      }
    }

    return folder;
  }

  /**
   *     BYTE
   *     {
   *       0:3 CodecIdSize
   *       4:  Is Complex Coder
   *       5:  There Are Attributes
   *       6:  Reserved
   *       7:  There are more alternative methods. (Not used anymore, must be 0).
   *     }
   *     BYTE CodecId[CodecIdSize]
   *     if (Is Complex Coder)
   *     {
   *       UINT64 NumInStreams;
   *       UINT64 NumOutStreams;
   *     }
   *     if (There Are Attributes)
   *     {
   *       UINT64 PropertiesSize
   *       BYTE Properties[PropertiesSize]
   *     }
   *
   * @param sevenZipRaf
   * @return
   * @throws IOException
   */
  private Coder readCoder(RandomAccessFile sevenZipRaf) throws IOException {
    Coder coder = new Coder();

    final int coderInfoByte = rawIO.readByte(sevenZipRaf);
    final int codercIdSize = coderInfoByte & 0x0f;
    final boolean isComplexCoder = (coderInfoByte & 0x10) != 0;
    coder.setComplexCoder(isComplexCoder);
    final boolean isHasAttributes = (coderInfoByte & 0x20) != 0;
    final boolean isHasAlternativeMethods = (coderInfoByte & 0x80) != 0;

    if(isHasAlternativeMethods) {
      throw new IOException("alternative methods bit is not used yet, must be 0");
    }

    // read codecid
    byte[] codecId = new byte[codercIdSize];
    sevenZipRaf.readFully(codecId);
    coder.setCodecId(codecId);

    // read numInStreams and numOutStreams
    if(coder.isComplexCoder()) {
      coder.setNumInStreams(SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf));
      coder.setNumOutStreams(SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf));
    } else {
      coder.setNumInStreams(1L);
      coder.setNumOutStreams(1L);
    }

    // read attributes if it exists
    if(isHasAttributes) {
      final long propertiesSize = SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf);
      // fixme: transform long to int
      final int propertiesSizeInt = (int)propertiesSize;
      byte[] properties = new byte[propertiesSizeInt];
      sevenZipRaf.readFully(properties);

      coder.setProperties(properties);
    }

    return coder;
  }

  /**
   * SubStreams Info
   * ~~~~~~~~~~~~~~
   *   BYTE NID::kSubStreamsInfo; (0x08)
   *
   *   []
   *   BYTE NID::kNumUnPackStream; (0x0D)
   *   UINT64 NumUnPackStreamsInFolders[NumFolders];
   *   []
   *
   *
   *   []
   *   BYTE NID::kSize  (0x09)
   *   UINT64 UnPackSizes[]
   *   []
   *
   *
   *   []
   *   BYTE NID::kCRC  (0x0A)
   *   Digests[Number of streams with unknown CRC]
   *   []
   *
   *
   *   BYTE NID::kEnd
   *
   * @param sevenZipRaf
   * @throws IOException
   */
  private void readSubStreamsInfo(RandomAccessFile sevenZipRaf) throws IOException {
    int tempByte = rawIO.readByte(sevenZipRaf);

    long numUnpackStreamsInFolder;
    int numUnpackStreamsInFolderInt;
    // read num unpack stream info if it exists
    if(tempByte == InternalSevenZipConstants.kNumUnpackStream) {
      for(Folder folder : sevenZipModel.getCodersInfo().getFolders()) {
        numUnpackStreamsInFolder = SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf);
        // fixme: transform long to int here
        numUnpackStreamsInFolderInt = (int) numUnpackStreamsInFolder;
        folder.setNumUnpackStreams(numUnpackStreamsInFolderInt);
      }

      tempByte = rawIO.readByte(sevenZipRaf);
    }

    int numUnpackStreamsInFoldersTotal = 0;
    // calculate total number of unpack streams
    for(Folder folder : sevenZipModel.getCodersInfo().getFolders()) {
      numUnpackStreamsInFoldersTotal += folder.getNumUnpackStreams();
    }

    // init unpack sizez
    long[] unpackSizes = new long[numUnpackStreamsInFoldersTotal];
    SubStreamsInfo subStreamsInfo = new SubStreamsInfo();
    subStreamsInfo.setUnpackSizes(unpackSizes);

    // read unpack sizes info if it exists
    Folder[] folders = sevenZipModel.getCodersInfo().getFolders();
    if(tempByte == InternalSevenZipConstants.kSize) {
      readSubStreamsSizesInfo(sevenZipRaf, unpackSizes, folders);
      tempByte = rawIO.readByte(sevenZipRaf);
    } else {
      // put all size info from Folder level to SubStreamsInfo level
      // if kSize is not presented, it means each Folder only has 1 unpack stream,
      // then the unpack size equals to Folder.unpackSize
      for(int i = 0; i < folders.length;i++) {
        unpackSizes[i] = SevenZipHeaderUtil.getFolderUnpackSize(folders[i]);
      }
    }

    // init sub streams crc digests info
    Digests subStreamsDigests = new Digests();
    BitSet subStreamsCrcDefinedBitSet = new BitSet(numUnpackStreamsInFoldersTotal);
    long[] subStreamsCRCs = new long[numUnpackStreamsInFoldersTotal];
    subStreamsDigests.setCrcDefinedBitSet(subStreamsCrcDefinedBitSet);
    subStreamsDigests.setCRCs(subStreamsCRCs);
    subStreamsInfo.setSubStreamsDigests(subStreamsDigests);

    // read sub streams crc digests if it exists
    if(tempByte == InternalSevenZipConstants.kCRC) {
      readSubStreamsDigests(sevenZipRaf, folders, subStreamsCrcDefinedBitSet, subStreamsCRCs);
      tempByte = rawIO.readByte(sevenZipRaf);
    }

    if(tempByte != InternalSevenZipConstants.kEnd) {
      throw new ZipException("7z read sub streams info failed, kEnd is expected but got " + tempByte);
    }

    sevenZipModel.setSubStreamsInfo(subStreamsInfo);
  }

  private void readSubStreamsSizesInfo(RandomAccessFile sevenZipRaf, long[] unpackSizes, Folder[] folders) throws IOException {
    // read each unpack size
    // notice that if a Folder has N unpack streams, only (N-1) unpack sizes will be listed here,
    // the Nth unpack size can be calculated by Folder.unpackSize - SUM(unpack sizes of (N-1) streams)
    long unpackSize;
    int unpackSizesIndex = 0;
    long sumUnpackSize;
    for(Folder folder : folders) {
      // if a folder does not contain unpack streams, then it should be skiped
      if(folder.getNumUnpackStreams() == 0) {
        continue;
      }

      sumUnpackSize = 0;
      // calculate sum of N-1 unpack sizes of unpack streams, and put unpack sizes into SubStreamsInfo
      for(int i = 0; i < (folder.getNumUnpackStreams() - 1);i++) {
        unpackSize = SevenZipHeaderUtil.readSevenZipUint64(sevenZipRaf);
        unpackSizes[unpackSizesIndex] = unpackSize;
        sumUnpackSize += unpackSize;
        unpackSizesIndex++;
      }

      unpackSizes[unpackSizesIndex] = SevenZipHeaderUtil.getFolderUnpackSize(folder) - sumUnpackSize;
      unpackSizesIndex++;
    }
  }

  /**
   * only those unpack streams have CRC:
   * unpack streams in folders with only 1 unpack streams and has crc in folder
   *
   * @param sevenZipRaf
   * @param folders
   * @param subStreamsCrcDefinedBitSet
   * @param subStreamsCRCs
   * @throws IOException
   */
  private void readSubStreamsDigests(RandomAccessFile sevenZipRaf, Folder[] folders, BitSet subStreamsCrcDefinedBitSet, long[] subStreamsCRCs) throws IOException {
    int numOfStreamsWithUnknownCRC = 0;
    for(Folder folder : folders) {
      if(folder.getNumUnpackStreams() != 1 || !folder.isHasCrc() ) {
        numOfStreamsWithUnknownCRC += folder.getNumUnpackStreams();
      }
    }

    // read digests info of sub streams with unknown CRC
    Digests subStreamsWithoutCRCDigests = readDigests(sevenZipRaf, numOfStreamsWithUnknownCRC);
    int crcInSubStreamsIndex = 0;
    int unknownCRCIndex = 0;
    // put crc digests of streams with unknown CRC,
    // and streams that already has CRC,
    // to sub streams CRC digests
    for(Folder folder : folders) {
      if(folder.getNumUnpackStreams() == 1 && folder.isHasCrc()) {
        // put CRC info into sub streams digests if it already has CRC
        subStreamsCrcDefinedBitSet.set(crcInSubStreamsIndex);
        subStreamsCRCs[crcInSubStreamsIndex] = folder.getCrc();
        crcInSubStreamsIndex++;
      } else {
        // put other CRC info to sub streams digests
        for(int i = 0; i < folder.getNumUnpackStreams();i++) {
          subStreamsCrcDefinedBitSet.set(crcInSubStreamsIndex,
                  subStreamsWithoutCRCDigests.getCrcDefinedBitSet().get(unknownCRCIndex));
          subStreamsCRCs[crcInSubStreamsIndex] = subStreamsWithoutCRCDigests.getCRCs()[unknownCRCIndex];
          crcInSubStreamsIndex++;
          unknownCRCIndex++;
        }
      }
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
