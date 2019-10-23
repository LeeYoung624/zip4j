package net.lingala.zip4j.sevenzip.headers;

import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.sevenzip.io.inputstream.SevenZipCompressionsInputStreamFactory;
import net.lingala.zip4j.sevenzip.tasks.ExtractSevenZipEncodedHeaderTask;
import net.lingala.zip4j.sevenzip.tasks.ExtractSevenZipEncodedHeaderTask.ExtractSevenZipEncodedHeaderTaskParameters;
import net.lingala.zip4j.sevenzip.util.InternalSevenZipConstants;
import net.lingala.zip4j.sevenzip.model.*;
import net.lingala.zip4j.util.RawIO;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
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

  private ProgressMonitor progressMonitor = new ProgressMonitor();

  private boolean runInThread = false;

  public SevenZipModel readAllHeaders(RandomAccessFile sevenZipRaf) throws IOException {
    sevenZipModel = new SevenZipModel();

    // read signature header
    readSignatureHeader(sevenZipRaf);

    // read header
    readHeader(sevenZipRaf);

    return sevenZipModel;
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

    InputStream sevenZipInputStream;
    int tempByte = rawIO.readByte(sevenZipRaf);
    if(tempByte == InternalSevenZipConstants.kEncodedHeader) {
      // compressed header read
      sevenZipInputStream = readEncodedHeader(sevenZipRaf, null);

      // sevenZipModel is written in readCompressedHeader
      sevenZipModel = new SevenZipModel();
      tempByte = rawIO.readByte(sevenZipInputStream);
    } else {
      sevenZipInputStream = Channels.newInputStream(sevenZipRaf.getChannel());
    }

    if (tempByte == InternalSevenZipConstants.kHeader) {
      // uncompressed header read
      readUncompressedHeader(sevenZipInputStream);
    } else {
      throw new ZipException("7z read next header failed, property id is expected to be kEncodedHeader or kHeader, but got " + tempByte);
    }
  }

  /**
   * HeaderInfo
   * ~~~~~~~~~~
   *   []
   *   BYTE NID::kEncodedHeader; (0x17)
   *   StreamsInfo for Encoded Header
   *   []
   * @param sevenZipRaf
   * @param password
   * @throws IOException
   */
  private InputStream readEncodedHeader(RandomAccessFile sevenZipRaf, String password) throws IOException {
    // compressed header is started with streams info
    readStreamsInfo(Channels.newInputStream(sevenZipRaf.getChannel()));

    // there should be only one folder
    Folder compressedHeaderFolder = sevenZipModel.getCodersInfo().getFolders()[0];
    compressedHeaderFolder.setPackedSize(sevenZipModel.getPackInfo().getPackSizes()[0]);
    long packOffset = InternalSevenZipConstants.SIGNATURE_HEADER_SIZE + sevenZipModel.getPackInfo().getPackPos();
    sevenZipRaf.seek(packOffset);
    InputStream sevenZipIS = Channels.newInputStream(sevenZipRaf.getChannel());

    InputStream headerInputStream = SevenZipCompressionsInputStreamFactory.generateFolderInputStream(sevenZipIS, compressedHeaderFolder);

    // verify CRC32 if it exists
    if(compressedHeaderFolder.isHasCrc()) {
      byte[] buffer = new byte[4096];
      int readLen;
      crc32.reset();
      while((readLen = headerInputStream.read(buffer)) > 0) {
        if(compressedHeaderFolder.isHasCrc()) {
          crc32.update(buffer, 0, readLen);
        }
      }
      if(crc32.getValue() != compressedHeaderFolder.getCrc()) {
        throw new ZipException("compressed header CRC32 verify failed");
      }

      sevenZipRaf.seek(packOffset);
      sevenZipIS = Channels.newInputStream(sevenZipRaf.getChannel());
      return SevenZipCompressionsInputStreamFactory.generateFolderInputStream(sevenZipIS, compressedHeaderFolder);
    }

    return headerInputStream;
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

  private void readUncompressedHeader(InputStream inputStream) throws IOException {
    int tempByte = rawIO.readByte(inputStream);

    // read archive properties if it exists
    if(tempByte == InternalSevenZipConstants.kArchiveProperties) {
      readArchiveProperties(inputStream);
      tempByte = rawIO.readByte(inputStream);
    }

    // read additional streams info if it exists
    if(tempByte == InternalSevenZipConstants.kAdditionalStreamsInfo) {
      // todo:read additional streams
      tempByte = rawIO.readByte(inputStream);
    }

    // read main streams info if it exists
    if(tempByte == InternalSevenZipConstants.kMainStreamsInfo) {
      readStreamsInfo(inputStream);
      tempByte = rawIO.readByte(inputStream);
    }

    // read files info if it exists
    if(tempByte == InternalSevenZipConstants.kFilesInfo) {
      readFilesInfo(inputStream);
      tempByte = rawIO.readByte(inputStream);
    }

    if (sevenZipModel.getFiles().length > 0 && sevenZipModel.getCodersInfo() != null
        && sevenZipModel.getCodersInfo().getFolders().length > 0) {
      calcRelationInFilesAndFolders();
    }

    if(tempByte != InternalSevenZipConstants.kEnd) {
      throw new ZipException("7z read uncompressed header error, end byte should be kEnd but got " + tempByte);
    }
  }

  private void readArchiveProperties(InputStream inputStream) throws IOException {
    long propertySize;
    byte[] property;

    int propertyType = rawIO.readByte(inputStream);
    while(propertyType != InternalSevenZipConstants.kEnd) {
      propertySize = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
      // fixme:long is converted to int here,need to add some assert detection
      property = new byte[(int)propertySize];
      // todo: is property stored as little endian or not? where are these properties used?
      // todo: store properties in sevenZipModel
      inputStream.read(property);
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
   * @param inputStream
   * @throws IOException
   */
  private void readStreamsInfo(InputStream inputStream) throws IOException {
    int tempByte = rawIO.readByte(inputStream);

    // read pack info if it exists
    if(tempByte == InternalSevenZipConstants.kPackInfo) {
      readPackInfo(inputStream);
      tempByte = rawIO.readByte(inputStream);
    }

    // read coders info if it exists
    if(tempByte == InternalSevenZipConstants.kUnpackInfo) {
      readCodersInfo(inputStream);
      tempByte = rawIO.readByte(inputStream);
      // todo: what if no coders info ?
    }

    // read substreams info if it exists
    if(tempByte == InternalSevenZipConstants.kSubStreamsInfo) {
      readSubStreamsInfo(inputStream);
      tempByte = rawIO.readByte(inputStream);
    }

    if(tempByte != InternalSevenZipConstants.kEnd) {
      throw new ZipException("7z read streams info error, end byte should be kEnd but got " + tempByte);
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
   * @param inputStream
   * @throws IOException
   */
  private void readPackInfo(InputStream inputStream) throws IOException {
    PackInfo packInfo = new PackInfo();
    sevenZipModel.setPackInfo(packInfo);

    // read pack position
    packInfo.setPackPos(SevenZipHeaderUtil.readSevenZipUint64(inputStream));

    final long numPackStreamsLong = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
    // fixme: convert long to int here
    final int numPackStreamsInt = (int) numPackStreamsLong;

    int tempByte = rawIO.readByte(inputStream);
    // read pack sizes if it exists
    if(tempByte == InternalSevenZipConstants.kSize) {
      long[] packSizes = new long[numPackStreamsInt];
      for(int i = 0; i < numPackStreamsInt;i++) {
        packSizes[i] = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
      }
      packInfo.setPackSizes(packSizes);

      tempByte = rawIO.readByte(inputStream);
    }

    // read pack stream digests if it exists
    if(tempByte == InternalSevenZipConstants.kCRC) {
      Digests packStreamDigests = readDigests(inputStream, numPackStreamsInt);
      packInfo.setDigests(packStreamDigests);

      tempByte = rawIO.readByte(inputStream);
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
   * @param inputStream
   * @throws IOException
   */
  private void readCodersInfo(InputStream inputStream) throws IOException {
    CodersInfo codersInfo = new CodersInfo();
    sevenZipModel.setCodersInfo(codersInfo);

    int tempByte = rawIO.readByte(inputStream);
    if(tempByte != InternalSevenZipConstants.kFolder) {
      throw new ZipException("7z read coders info failed, kFolder is expected but got " + tempByte);
    }

    final long numFolders = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
    // fixme:numFolders transform to int
    final int numFoldersInt = (int)numFolders;
    final int external = rawIO.readByte(inputStream);

    Folder[] folders = new Folder[numFoldersInt];
    codersInfo.setFolders(folders);
    if(external == 0) {
      for(int i = 0; i < numFolders;i++) {
        folders[i] = readFolder(inputStream);
      }
    } else {
      //todo: datastreamindex?
    }

    // read unpack size
    tempByte = rawIO.readByte(inputStream);
    if(tempByte != InternalSevenZipConstants.kCodersUnpackSize) {
      throw new ZipException("7z read coders info failed, kCodersUnpackSize is expected but got " + tempByte);
    }

    for(Folder folder : folders) {
      // todo:convert from long to int here
      final int numOutStreamsTotalInt = (int) folder.getNumOutStreamsTotal();
      long[] unpackSizes = new long[numOutStreamsTotalInt];
      for(int i = 0; i < numOutStreamsTotalInt;i++) {
        unpackSizes[i] = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
      }
      folder.setUnpackSizes(unpackSizes);
    }

    tempByte = rawIO.readByte(inputStream);
    // read crc info if it exists
    if(tempByte == InternalSevenZipConstants.kCRC) {
      // put crc info in each Folder object
      Digests unpackDigests = readDigests(inputStream, numFoldersInt);
      for(int i = 0;i < numFoldersInt;i++) {
        if(unpackDigests.getCrcDefinedBitSet().get(i)) {
          folders[i].setHasCrc(true);
          // ignore other bits by & 0xFFFFFFFFL
          folders[i].setCrc(unpackDigests.getCRCs()[i] & 0xFFFFFFFFL);
        } else {
          folders[i].setHasCrc(false);
        }
      }
      tempByte = rawIO.readByte(inputStream);
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
   * @param inputStream
   * @return
   */
  private Folder readFolder(InputStream inputStream) throws IOException {
    Folder folder = new Folder();
    final long numCoders = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
    // fixme: numCoders transform to int
    final int numCodersInt = (int) numCoders;

    Coder[] coders = new Coder[numCodersInt];
    for(int i = 0; i < numCodersInt;i++) {
      coders[i] = readCoder(inputStream);
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
      bindPairs[i] = new BindPair();
      bindPairs[i].setInIndex(SevenZipHeaderUtil.readSevenZipUint64(inputStream));
      bindPairs[i].setOutIndex(SevenZipHeaderUtil.readSevenZipUint64(inputStream));
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
    folder.setPackedStreams(packedStreams);

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
        packedStreams[i] = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
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
   * @param inputStream
   * @return
   * @throws IOException
   */
  private Coder readCoder(InputStream inputStream) throws IOException {
    Coder coder = new Coder();

    final int coderInfoByte = rawIO.readByte(inputStream);
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
    inputStream.read(codecId);
    coder.setCodecId(codecId);

    // read numInStreams and numOutStreams
    if(coder.isComplexCoder()) {
      coder.setNumInStreams(SevenZipHeaderUtil.readSevenZipUint64(inputStream));
      coder.setNumOutStreams(SevenZipHeaderUtil.readSevenZipUint64(inputStream));
    } else {
      coder.setNumInStreams(1L);
      coder.setNumOutStreams(1L);
    }

    // read attributes if it exists
    if(isHasAttributes) {
      final long propertiesSize = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
      // fixme: transform long to int
      final int propertiesSizeInt = (int)propertiesSize;
      byte[] properties = new byte[propertiesSizeInt];
      inputStream.read(properties);

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
   * @param inputStream
   * @throws IOException
   */
  private void readSubStreamsInfo(InputStream inputStream) throws IOException {
    int tempByte = rawIO.readByte(inputStream);

    long numUnpackStreamsInFolder;
    int numUnpackStreamsInFolderInt;
    // read num unpack stream info if it exists
    if(tempByte == InternalSevenZipConstants.kNumUnpackStream) {
      for(Folder folder : sevenZipModel.getCodersInfo().getFolders()) {
        numUnpackStreamsInFolder = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
        // fixme: transform long to int here
        numUnpackStreamsInFolderInt = (int) numUnpackStreamsInFolder;
        folder.setNumUnpackStreams(numUnpackStreamsInFolderInt);
      }

      tempByte = rawIO.readByte(inputStream);
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
      readSubStreamsSizesInfo(inputStream, unpackSizes, folders);
      tempByte = rawIO.readByte(inputStream);
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
      readSubStreamsDigests(inputStream, folders, subStreamsCrcDefinedBitSet, subStreamsCRCs);
      tempByte = rawIO.readByte(inputStream);
    }

    if(tempByte != InternalSevenZipConstants.kEnd) {
      throw new ZipException("7z read sub streams info failed, kEnd is expected but got " + tempByte);
    }

    sevenZipModel.setSubStreamsInfo(subStreamsInfo);
  }

  private void readSubStreamsSizesInfo(InputStream inputStream, long[] unpackSizes, Folder[] folders) throws IOException {
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
        unpackSize = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
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
   * @param inputStream
   * @param folders
   * @param subStreamsCrcDefinedBitSet
   * @param subStreamsCRCs
   * @throws IOException
   */
  private void readSubStreamsDigests(InputStream inputStream, Folder[] folders, BitSet subStreamsCrcDefinedBitSet, long[] subStreamsCRCs) throws IOException {
    int numOfStreamsWithUnknownCRC = 0;
    for(Folder folder : folders) {
      if(folder.getNumUnpackStreams() != 1 || !folder.isHasCrc() ) {
        numOfStreamsWithUnknownCRC += folder.getNumUnpackStreams();
      }
    }

    // read digests info of sub streams with unknown CRC
    Digests subStreamsWithoutCRCDigests = readDigests(inputStream, numOfStreamsWithUnknownCRC);
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

  /**
   * FilesInfo
   * ~~~~~~~~~
   *   BYTE NID::kFilesInfo;  (0x05)
   *   UINT64 NumFiles
   *
   *   for (;;)
   *   {
   *     BYTE PropertyType;
   *     if (aType == 0)
   *       break;
   *
   *     UINT64 Size;
   *
   *     switch(PropertyType)
   *     {
   *       kEmptyStream:   (0x0E)
   *         for(NumFiles)
   *           BIT IsEmptyStream
   *
   *       kEmptyFile:     (0x0F)
   *         for(EmptyStreams)
   *           BIT IsEmptyFile
   *
   *       kAnti:          (0x10)
   *         for(EmptyStreams)
   *           BIT IsAntiFile
   *
   *       case kCTime: (0x12)
   *       case kATime: (0x13)
   *       case kMTime: (0x14)
   *         BYTE AllAreDefined
   *         if (AllAreDefined == 0)
   *         {
   *           for(NumFiles)
   *             BIT TimeDefined
   *         }
   *         BYTE External;
   *         if(External != 0)
   *           UINT64 DataIndex
   *         []
   *         for(Definded Items)
   *           REAL_UINT64 Time
   *         []
   *
   *       kNames:     (0x11)
   *         BYTE External;
   *         if(External != 0)
   *           UINT64 DataIndex
   *         []
   *         for(Files)
   *         {
   *           wchar_t Names[NameSize];
   *           wchar_t 0;
   *         }
   *         []
   *
   *       kAttributes:  (0x15)
   *         BYTE AllAreDefined
   *         if (AllAreDefined == 0)
   *         {
   *           for(NumFiles)
   *             BIT AttributesAreDefined
   *         }
   *         BYTE External;
   *         if(External != 0)
   *           UINT64 DataIndex
   *         []
   *         for(Definded Attributes)
   *           UINT32 Attributes
   *         []
   *     }
   *   }
   *
   * @param inputStream
   * @throws IOException
   */
  private void readFilesInfo(InputStream inputStream) throws IOException {
    final long numFiles = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
    // fixme : cast from long to int here
    final int numFilesInt = (int) numFiles;

    // init seven zip file entries
    SevenZipFileEntry[] fileEntries = new SevenZipFileEntry[numFilesInt];
    for(int i = 0; i < numFilesInt;i++) {
      fileEntries[i] = new SevenZipFileEntry();
    }

    int propertyType;
    long size;
    BitSet isEmptyStream = null;
    BitSet isEmptyFile = null;
    BitSet isAnti = null;
    int external;
    while(true) {
      propertyType = rawIO.readByte(inputStream);
      if(propertyType == 0) {
        break;
      }

      size = SevenZipHeaderUtil.readSevenZipUint64(inputStream);
      switch (propertyType) {
        case InternalSevenZipConstants.kEmptyStream:
          isEmptyStream = SevenZipHeaderUtil.readBitsAsBitSet(inputStream, numFilesInt);
          break;
        case InternalSevenZipConstants.kEmptyFile:
          if(isEmptyStream == null) {
            throw new ZipException("error occur when reading files info : kEmptyStream must appear before kEmptyFile");
          }

          isEmptyFile = SevenZipHeaderUtil.readBitsAsBitSet(inputStream, isEmptyStream.cardinality());
          break;
        case InternalSevenZipConstants.kAnti:
          if(isEmptyStream == null) {
            throw new ZipException("error occur when reading files info : kEmptyStream must appear before kAnti");
          }

          isAnti = SevenZipHeaderUtil.readBitsAsBitSet(inputStream, isEmptyStream.cardinality());
          break;
        case InternalSevenZipConstants.kCTime:
        case InternalSevenZipConstants.kATime:
        case InternalSevenZipConstants.kMTime:
          boolean hasDate;
          long date = 0;
          BitSet timeDefined = SevenZipHeaderUtil.readBitsWithAllAreDefined(inputStream, numFilesInt);
          external = rawIO.readByte(inputStream);
          if(external != 0) {
            // todo : read data index
          }

          for(int i = 0; i < numFilesInt;i++) {
            hasDate = timeDefined.get(i);
            if(hasDate) {
              date = rawIO.readLongLittleEndian(inputStream);
            }
            setFileTimeInfo(fileEntries[i], hasDate, date, propertyType);
          }
          break;
        case InternalSevenZipConstants.kName:
          external = rawIO.readByte(inputStream);
          if(external != 0) {
            // todo : read data index
          }

          // fixme : why?
          if (((size - 1) & 1) != 0) {
            throw new IOException("File names length invalid");
          }

          // todo : transform long to int
          // fixme : why?
          int numFileNamesInt = (int)size - 1;
          byte[] names = new byte[numFileNamesInt];
          inputStream.read(names);
          int fileIndex = 0;
          int nameStartByteIndex = 0;

          // todo:why i += 2?
          for(int i = 0; i < numFileNamesInt;i += 2) {
            if (names[i] == 0 && names[i+1] == 0) {
              fileEntries[fileIndex].setName(new String(names, nameStartByteIndex, i - nameStartByteIndex, StandardCharsets.UTF_16LE));
              nameStartByteIndex = i + 2;
              fileIndex++;
            }
          }

          // todo : validate fileIndex == numFilesInt and nameStartByteIndex == numFileNamesInt
          break;
        case InternalSevenZipConstants.kAttributes:
          BitSet attributeAreDefined = SevenZipHeaderUtil.readBitsWithAllAreDefined(inputStream, numFilesInt);
          external = rawIO.readByte(inputStream);
          if(external != 0) {
            // todo : read data index
          }

          for(int i = 0; i < numFilesInt;i++) {
            fileEntries[i].setHasAttributes(attributeAreDefined.get(i));
            if(fileEntries[i].isHasAttributes()) {
              fileEntries[i].setAttributes(rawIO.readIntLittleEndian(inputStream));
            }
          }
          break;
        default:
          // todo : skip [size] bytes
      }
    }

    int emptyFileIndex = 0;
    int notEmptyFileIndex = 0;
    for(int i = 0; i < numFilesInt;i++) {
      if(isEmptyStream == null || !isEmptyStream.get(i)) {
        fileEntries[i].setHasStream(true);
        fileEntries[i].setDirectory(false);
        fileEntries[i].setAntiItem(false);
        fileEntries[i].setHasCrc(sevenZipModel.getSubStreamsInfo().getSubStreamsDigests().getCrcDefinedBitSet().get(notEmptyFileIndex));
        fileEntries[i].setCrc(sevenZipModel.getSubStreamsInfo().getSubStreamsDigests().getCRCs()[notEmptyFileIndex]);
        fileEntries[i].setSize(sevenZipModel.getSubStreamsInfo().getUnpackSizes()[notEmptyFileIndex]);
        notEmptyFileIndex++;
      } else {
        fileEntries[i].setHasStream(false);
        fileEntries[i].setDirectory(isEmptyFile == null || !isEmptyFile.get(emptyFileIndex));
        fileEntries[i].setAntiItem(isAnti != null && isAnti.get(emptyFileIndex));
        fileEntries[i].setHasCrc(false);
        fileEntries[i].setSize(0L);
        emptyFileIndex++;
      }
    }
    sevenZipModel.setFiles(fileEntries);
//    calculateStreamMap(archive);
  }

  private void setFileTimeInfo(SevenZipFileEntry file, boolean hasDate, long date, int propertyType) {
    switch(propertyType) {
      case InternalSevenZipConstants.kCTime:
        file.setHasCreationDate(hasDate);
        if(hasDate) {
          file.setCreationDate(date);
        }
        break;
      case InternalSevenZipConstants.kATime:
        file.setHasAccessDate(hasDate);
        if(hasDate) {
          file.setAccessDate(date);
        }
        break;
      case InternalSevenZipConstants.kMTime:
        file.setHasLastModifiedDate(hasDate);
        if(hasDate) {
          file.setLastModifiedDate(date);
        }
        break;
      default:
        return;
    }
  }

  /**
   * calculate the relation between files and folders : a file should know its corresponding folder,
   * and a folder should know its offset(which is the offset of the first file in this folder)
   */
  private void calcRelationInFilesAndFolders() {
    int fileIndex;
    int folderIndex = 0;
    int fileInFolderCount = 1;
    SevenZipFileEntry[] files = sevenZipModel.getFiles();
    Folder[] folders = sevenZipModel.getCodersInfo().getFolders();
    long[] packSizes = sevenZipModel.getPackInfo().getPackSizes();
    long[] packOffsets = new long[packSizes.length];
    long packedStreamOffsetTotal = 0;
    int currentFolderPackSizeIndex = 0;
    SevenZipFileEntry fileInFolder;
    Folder tempFolder;

    // calc pack offsets based on pack sizes
    for(int i = 0; i < packSizes.length; i++) {
      packOffsets[i] = packedStreamOffsetTotal;
      packedStreamOffsetTotal += packSizes[i];
    }

    // calc pack stream offset of each folder, each folder may have many packed streams,
    // only the offset of the fist packed stream need to be stored
    // the pack size of each folder equals to the pack size of its first pack stream
    for(Folder folder : folders) {
      folder.setFolderPackStreamOffset(packOffsets[currentFolderPackSizeIndex]);
      folder.setPackedSize(packSizes[currentFolderPackSizeIndex]);
      currentFolderPackSizeIndex += folder.getPackedStreams().length;
    }


    for(fileIndex = 0;fileIndex < files.length; fileIndex++) {
      fileInFolder = files[fileIndex];
      // some files don't have any streams, like some directories or empty files, they should be skipped
      if(!fileInFolder.isHasStream() && fileInFolderCount == 1) {
        fileInFolder.setCorrespondingFolder(null);
        continue;
      }
      tempFolder = folders[folderIndex];

      // the folders with no unpack streams should be skipped
      while(tempFolder.getNumUnpackStreams() <= 0) {
        folderIndex++;
      }

      fileInFolder.setCorrespondingFolder(tempFolder);
      if(tempFolder.getFiles() == null) {
        tempFolder.setFiles(new ArrayList<>());
      }
      tempFolder.getFiles().add(fileInFolder);

      fileInFolderCount++;
      // if a folder is finished, the folder index should ++
      if(fileInFolderCount > tempFolder.getNumUnpackStreams()) {
        fileInFolderCount = 1;
        folderIndex++;
      }
    }
  }

  private Digests readDigests(InputStream inputStream, int numStreams) throws IOException {
    Digests digests = new Digests();
    BitSet bitSet = SevenZipHeaderUtil.readBitsWithAllAreDefined(inputStream, numStreams);
    digests.setCrcDefinedBitSet(bitSet);

    long[] CRCs = new long[numStreams];
    digests.setCRCs(CRCs);
    // read CRCs of streams that has CRC defined
    for(int i = 0;i < numStreams;i++) {
      if(bitSet.get(i)) {
        CRCs[i] = rawIO.readIntLittleEndian(inputStream);
      }
    }

    return digests;
  }
}
