package net.lingala.zip4j.headers;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.AesVersion;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.util.InternalZipConstants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.Charset;

import static net.lingala.zip4j.util.BitUtils.isBitSet;
import static net.lingala.zip4j.util.Zip4jUtil.javaToDosTime;
import static org.assertj.core.api.Assertions.assertThat;

public class FileHeaderFactoryTest {

  private static final String FILE_NAME_IN_ZIP = "filename.txt";
  private static final long ENTRY_CRC = 2323L;

  private FileHeaderFactory fileHeaderFactory = new FileHeaderFactory();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testGenerateFileHeaderWithoutFileNameThrowsException() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("fileNameInZip is null or empty");

    fileHeaderFactory.generateFileHeader(new ZipParameters(), false, 0, InternalZipConstants.CHARSET_UTF_8);
  }

  @Test
  public void testGenerateFileHeaderDefaults() throws ZipException {
    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(generateZipParameters(), false, 0, InternalZipConstants.CHARSET_UTF_8);

    assertThat(fileHeader).isNotNull();
    assertThat(fileHeader.getCompressionMethod()).isEqualTo(CompressionMethod.DEFLATE);
    verifyCompressionLevelGridForDeflate(CompressionLevel.NORMAL,
        fileHeader.getGeneralPurposeFlag()[0]);
    assertThat(fileHeader.isEncrypted()).isFalse();
    assertThat(fileHeader.getEncryptionMethod()).isEqualTo(EncryptionMethod.NONE);
    assertThat(fileHeader.getAesExtraDataRecord()).isNull();
    assertThat(fileHeader.getLastModifiedTime()).isNotZero();
    assertThat(fileHeader.getCompressedSize()).isEqualTo(0);
    assertThat(fileHeader.getUncompressedSize()).isEqualTo(-1);
  }

  @Test
  public void testGenerateFileHeaderForStoreWithoutEncryption() throws ZipException {
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);
    verifyFileHeader(fileHeader, zipParameters, false, 0, false);
  }

  @Test
  public void testGenerateFileHeaderWhenEncryptingWithoutMethodThrowsException() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("Encryption method has to be set when encryptFiles flag is set in zip parameters");

    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setEncryptFiles(true);

    fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);
  }

  @Test
  public void testGenerateFileHeaderWithStandardEncryption() throws ZipException {
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);
    verifyFileHeader(fileHeader, zipParameters, false, 0, false);
  }

  @Test
  public void testGenerateFileHeaderWithAesEncryptionWithNullKeyStrengthThrowsException() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("invalid AES key strength");

    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(null);

    fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);
  }

  @Test
  public void testGenerateFileHeaderWithAesEncryptionWithoutKeyStrengthUsesDefault() throws ZipException {
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);
    verifyFileHeader(fileHeader, zipParameters, false, 0, true);
    verifyAesExtraDataRecord(fileHeader.getAesExtraDataRecord(), AesKeyStrength.KEY_STRENGTH_256,
        CompressionMethod.DEFLATE, AesVersion.TWO);
  }

  @Test
  public void testGenerateFileHeaderWithAesEncryptionWithKeyStrength128() throws ZipException {
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_128);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);
    verifyFileHeader(fileHeader, zipParameters, false, 0, true);
    verifyAesExtraDataRecord(fileHeader.getAesExtraDataRecord(), AesKeyStrength.KEY_STRENGTH_128,
        CompressionMethod.DEFLATE, AesVersion.TWO);
  }

  @Test
  public void testGenerateFileHeaderWithAesEncryptionWithKeyStrength192() throws ZipException {
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_192);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);
    verifyFileHeader(fileHeader, zipParameters, false, 0, true);
    verifyAesExtraDataRecord(fileHeader.getAesExtraDataRecord(), AesKeyStrength.KEY_STRENGTH_192,
        CompressionMethod.DEFLATE, AesVersion.TWO);
  }

  @Test
  public void testGenerateFileHeaderWithAesEncryptionWithKeyStrength256() throws ZipException {
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);
    verifyFileHeader(fileHeader, zipParameters, false, 0, true);
    verifyAesExtraDataRecord(fileHeader.getAesExtraDataRecord(), AesKeyStrength.KEY_STRENGTH_256,
        CompressionMethod.DEFLATE, AesVersion.TWO);
  }

  @Test
  public void testGenerateFileHeaderWithAesEncryptionVersionV1() throws ZipException {
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setAesVersion(AesVersion.ONE);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);
    verifyFileHeader(fileHeader, zipParameters, false, 0, true);
    verifyAesExtraDataRecord(fileHeader.getAesExtraDataRecord(), AesKeyStrength.KEY_STRENGTH_256,
        CompressionMethod.DEFLATE, AesVersion.ONE);
  }

  @Test
  public void testGenerateFileHeaderWithAesEncryptionWithNullVersionUsesV2() throws ZipException {
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setAesVersion(null);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);
    verifyFileHeader(fileHeader, zipParameters, false, 0, true);
    verifyAesExtraDataRecord(fileHeader.getAesExtraDataRecord(), AesKeyStrength.KEY_STRENGTH_256,
        CompressionMethod.DEFLATE, AesVersion.TWO);
  }

  @Test
  public void testGenerateFileHeaderWithLastModifiedFileTime() throws ZipException {
    long lastModifiedFileTime = System.currentTimeMillis();
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setLastModifiedFileTime(lastModifiedFileTime);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);

    assertThat(fileHeader.getLastModifiedTime()).isEqualTo(javaToDosTime(zipParameters.getLastModifiedFileTime()));
  }

  @Test
  public void testGenerateFileHeaderWithCompressionLevelMaximum() throws ZipException {
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setCompressionLevel(CompressionLevel.MAXIMUM);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);

    verifyCompressionLevelGridForDeflate(CompressionLevel.MAXIMUM, fileHeader.getGeneralPurposeFlag()[0]);
  }

  @Test
  public void testGenerateFileHeaderWithCompressionLevelFast() throws ZipException {
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setCompressionLevel(CompressionLevel.FAST);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);

    verifyCompressionLevelGridForDeflate(CompressionLevel.FAST, fileHeader.getGeneralPurposeFlag()[0]);
  }

  @Test
  public void testGenerateFileHeaderWithCompressionLevelFastest() throws ZipException {
    ZipParameters zipParameters = generateZipParameters();
    zipParameters.setCompressionLevel(CompressionLevel.FASTEST);

    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8);

    verifyCompressionLevelGridForDeflate(CompressionLevel.FASTEST, fileHeader.getGeneralPurposeFlag()[0]);
  }

  @Test
  public void testGenerateFileHeaderWithCorrectCharset() throws ZipException {
    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(generateZipParameters(), false, 0, Charset.forName("Cp949"));
    assertThat(isBitSet(fileHeader.getGeneralPurposeFlag()[1], 3)).isFalse();
  }

  @Test
  public void testGenerateFileHeaderWithUTF8Charset() throws ZipException {
    FileHeader fileHeader = fileHeaderFactory.generateFileHeader(generateZipParameters(), false, 0, InternalZipConstants.CHARSET_UTF_8);
    assertThat(isBitSet(fileHeader.getGeneralPurposeFlag()[1], 3)).isTrue();
  }

  @Test
  public void testGenerateLocalFileHeader() {
    long lastModifiedFileTime = javaToDosTime(System.currentTimeMillis());
    FileHeader  fileHeader = generateFileHeader(lastModifiedFileTime);

    LocalFileHeader localFileHeader = fileHeaderFactory.generateLocalFileHeader(fileHeader);

    verifyLocalFileHeader(localFileHeader, lastModifiedFileTime);
  }

  private ZipParameters generateZipParameters() {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip(FILE_NAME_IN_ZIP);
    zipParameters.setEntryCRC(ENTRY_CRC);
    return zipParameters;
  }

  private FileHeader generateFileHeader(long lastModifiedFileTime) {
    FileHeader fileHeader = new FileHeader();
    fileHeader.setVersionNeededToExtract(20);
    fileHeader.setCompressionMethod(CompressionMethod.STORE);
    fileHeader.setLastModifiedTime(lastModifiedFileTime);
    fileHeader.setUncompressedSize(1000L);
    fileHeader.setFileName(FILE_NAME_IN_ZIP);
    fileHeader.setFileNameLength(FILE_NAME_IN_ZIP.getBytes(InternalZipConstants.CHARSET_UTF_8).length);
    fileHeader.setEncrypted(true);
    fileHeader.setEncryptionMethod(EncryptionMethod.AES);
    fileHeader.setCrc(1231231L);
    fileHeader.setCompressedSize(23523L);
    fileHeader.setGeneralPurposeFlag(new byte[] {2, 28});
    fileHeader.setDataDescriptorExists(true);
    return fileHeader;
  }

  private void verifyFileHeader(FileHeader fileHeader, ZipParameters zipParameters, boolean isSplitZip,
                                int diskNumberStart, boolean aesExtraDataRecordPresent) {
    assertThat(fileHeader).isNotNull();
    assertThat(fileHeader.getSignature()).isEqualTo(HeaderSignature.CENTRAL_DIRECTORY);
    assertThat(fileHeader.getVersionMadeBy()).isEqualTo(20);
    assertThat(fileHeader.getVersionNeededToExtract()).isEqualTo(20);
    verifyCompressionMethod(fileHeader, zipParameters);
    assertThat(fileHeader.isEncrypted()).isEqualTo(zipParameters.isEncryptFiles());
    assertThat(fileHeader.getEncryptionMethod()).isEqualTo(zipParameters.isEncryptFiles()
        ? zipParameters.getEncryptionMethod() : EncryptionMethod.NONE);
    assertThat(fileHeader.getFileName()).isEqualTo(FILE_NAME_IN_ZIP);
    assertThat(fileHeader.getFileNameLength()).isEqualTo(FILE_NAME_IN_ZIP.length());
    verifyGeneralPurposeBytes(fileHeader.getGeneralPurposeFlag(), zipParameters);
    assertThat(fileHeader.getDiskNumberStart()).isEqualTo(isSplitZip ? diskNumberStart : 0);
    verifyLastModifiedFileTime(fileHeader, zipParameters);
    assertThat(fileHeader.getExternalFileAttributes()).isEqualTo(new byte[4]);
    assertThat(fileHeader.isDirectory()).isEqualTo(false);
    assertThat(fileHeader.getUncompressedSize()).isEqualTo(zipParameters.getEntrySize());
    verifyCrc(fileHeader);
    assertThat(fileHeader.isDataDescriptorExists()).isEqualTo(zipParameters.isWriteExtendedLocalFileHeader());
    assertThat(fileHeader.getAesExtraDataRecord() != null).isEqualTo(aesExtraDataRecordPresent);
  }

  private void verifyLocalFileHeader(LocalFileHeader localFileHeader, long lastModifiedFileTime) {
    assertThat(localFileHeader).isNotNull();
    assertThat(localFileHeader.getSignature()).isEqualTo(HeaderSignature.LOCAL_FILE_HEADER);
    assertThat(localFileHeader.getVersionNeededToExtract()).isEqualTo(20);
    assertThat(localFileHeader.getCompressionMethod()).isEqualTo(CompressionMethod.STORE);
    assertThat(localFileHeader.getLastModifiedTime()).isEqualTo(lastModifiedFileTime);
    assertThat(localFileHeader.getUncompressedSize()).isEqualTo(1000L);
    assertThat(localFileHeader.getFileName()).isEqualTo(FILE_NAME_IN_ZIP);
    assertThat(localFileHeader.getFileNameLength()).isEqualTo(FILE_NAME_IN_ZIP.length());
    assertThat(localFileHeader.isEncrypted()).isEqualTo(true);
    assertThat(localFileHeader.getEncryptionMethod()).isEqualTo(EncryptionMethod.AES);
    assertThat(localFileHeader.getCrc()).isEqualTo(1231231L);
    assertThat(localFileHeader.getCompressedSize()).isEqualTo(23523L);
    assertThat(localFileHeader.getGeneralPurposeFlag()).containsExactly(2, 28);
    assertThat(localFileHeader.isDataDescriptorExists()).isTrue();
  }

  private void verifyCompressionMethod(FileHeader fileHeader, ZipParameters zipParameters) {
    if (fileHeader.isEncrypted() && fileHeader.getEncryptionMethod().equals(EncryptionMethod.AES)) {
      assertThat(fileHeader.getCompressionMethod()).isEqualTo(CompressionMethod.AES_INTERNAL_ONLY);
    } else {
      assertThat(fileHeader.getCompressionMethod()).isEqualTo(zipParameters.getCompressionMethod());
    }
  }

  private void verifyLastModifiedFileTime(FileHeader fileHeader, ZipParameters zipParameters) {
    if (zipParameters.getLastModifiedFileTime() > 0) {
      assertThat(fileHeader.getLastModifiedTime()).isEqualTo(javaToDosTime(
          zipParameters.getLastModifiedFileTime()));
    } else {
      assertThat(fileHeader.getLastModifiedTime()).isGreaterThan(0);
    }
  }

  private void verifyGeneralPurposeBytes(byte[] generalPurposeBytes, ZipParameters zipParameters) {
    assertThat(generalPurposeBytes).isNotNull();
    assertThat(generalPurposeBytes.length).isEqualTo(2);
    assertThat(isBitSet(generalPurposeBytes[0], 0)).isEqualTo(zipParameters.isEncryptFiles());
    if (zipParameters.getCompressionMethod() == CompressionMethod.DEFLATE) {
      verifyCompressionLevelGridForDeflate(zipParameters.getCompressionLevel(),
          generalPurposeBytes[0]);
    } else {
      assertThat(isBitSet(generalPurposeBytes[0], 1)).isFalse();
      assertThat(isBitSet(generalPurposeBytes[0], 2)).isFalse();
    }
    assertThat(isBitSet(generalPurposeBytes[0], 3)).isEqualTo(zipParameters.isWriteExtendedLocalFileHeader());
    assertThat(isBitSet(generalPurposeBytes[1], 3)).isTrue();
  }

  private void verifyCompressionLevelGridForDeflate(CompressionLevel compressionLevel,
                                                    byte firstByteOfGeneralPurposeBytes) {
    if (compressionLevel == CompressionLevel.NORMAL) {
      assertThat(isBitSet(firstByteOfGeneralPurposeBytes, 1)).isFalse();
      assertThat(isBitSet(firstByteOfGeneralPurposeBytes, 2)).isFalse();
    } else if (compressionLevel == CompressionLevel.MAXIMUM) {
      assertThat(isBitSet(firstByteOfGeneralPurposeBytes, 1)).isTrue();
      assertThat(isBitSet(firstByteOfGeneralPurposeBytes, 2)).isFalse();
    } else if (compressionLevel == CompressionLevel.FAST) {
      assertThat(isBitSet(firstByteOfGeneralPurposeBytes, 1)).isFalse();
      assertThat(isBitSet(firstByteOfGeneralPurposeBytes, 2)).isTrue();
    } else if (compressionLevel == CompressionLevel.FASTEST) {
      assertThat(isBitSet(firstByteOfGeneralPurposeBytes, 1)).isTrue();
      assertThat(isBitSet(firstByteOfGeneralPurposeBytes, 2)).isTrue();
    }
  }

  private void verifyCrc(FileHeader fileHeader) {
    if (fileHeader.isEncrypted() && fileHeader.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
      assertThat(fileHeader.getCrc()).isEqualTo(ENTRY_CRC);
    } else {
      assertThat(fileHeader.getCrc()).isEqualTo(0);
    }
  }

  private void verifyAesExtraDataRecord(AESExtraDataRecord aesExtraDataRecord, AesKeyStrength aesKeyStrength,
                                        CompressionMethod compressionMethod, AesVersion aesVersion) {
    assertThat(aesExtraDataRecord).isNotNull();
    assertThat(aesExtraDataRecord.getSignature()).isEqualTo(HeaderSignature.AES_EXTRA_DATA_RECORD);
    assertThat(aesExtraDataRecord.getDataSize()).isEqualTo(7);
    assertThat(aesExtraDataRecord.getVendorID()).isEqualTo("AE");
    assertThat(aesExtraDataRecord.getCompressionMethod()).isEqualTo(compressionMethod);
    assertThat(aesExtraDataRecord.getAesVersion()).isEqualTo(aesVersion);
    assertThat(aesExtraDataRecord.getAesKeyStrength()).isEqualTo(aesKeyStrength);
  }

}