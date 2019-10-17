package net.lingala.zip4j.sevenzip.headers;

import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.sevenzip.model.BindPair;
import net.lingala.zip4j.sevenzip.model.Coder;
import net.lingala.zip4j.sevenzip.model.Folder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SevenZipHeaderUtilTest {
  protected File testFile;

  protected RandomAccessFile testFileRaf;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void before() throws IOException {
    deleteTempFiles();
    testFile = temporaryFolder.newFile("seven_zip_header_test_file");
    testFileRaf = new RandomAccessFile(testFile, RandomAccessFileMode.READ.getValue());
  }

  @After
  public void after() {
    deleteTempFiles();
    temporaryFolder.delete();
  }

  protected void deleteTempFiles() {
    File[] allTempFiles = temporaryFolder.getRoot().listFiles();
    Arrays.stream(allTempFiles).forEach(File::delete);
  }

  protected void cleanUpTestFile() throws IOException {
    new PrintWriter(testFile).close();
  }

  @Test
  public void testSevenZipReadUint64() throws IOException {
    byte[] testData;

    testData = new byte[] {(byte)0x7f, (byte)0xff , (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
    cleanUpTestFileAndWriteData(testData);
    testFileRaf.seek(0);
    assertThat(SevenZipHeaderUtil.readSevenZipUint64(testFileRaf)).isEqualTo(0x7fL);

    testData[0] = (byte)0xbf;
    cleanUpTestFileAndWriteData(testData);
    testFileRaf.seek(0);
    assertThat(SevenZipHeaderUtil.readSevenZipUint64(testFileRaf)).isEqualTo(0x3fffL);

    testData[0] = (byte)0xdf;
    cleanUpTestFileAndWriteData(testData);
    testFileRaf.seek(0);
    assertThat(SevenZipHeaderUtil.readSevenZipUint64(testFileRaf)).isEqualTo(0x1fffffL);

    testData[0] = (byte)0xef;
    cleanUpTestFileAndWriteData(testData);
    testFileRaf.seek(0);
    assertThat(SevenZipHeaderUtil.readSevenZipUint64(testFileRaf)).isEqualTo(0x0fffffffL);

    testData[0] = (byte)0xf7;
    cleanUpTestFileAndWriteData(testData);
    testFileRaf.seek(0);
    assertThat(SevenZipHeaderUtil.readSevenZipUint64(testFileRaf)).isEqualTo(0x07ffffffffL);

    testData[0] = (byte)0xfb;
    cleanUpTestFileAndWriteData(testData);
    testFileRaf.seek(0);
    assertThat(SevenZipHeaderUtil.readSevenZipUint64(testFileRaf)).isEqualTo(0x03ffffffffffL);

    testData[0] = (byte)0xfd;
    cleanUpTestFileAndWriteData(testData);
    testFileRaf.seek(0);
    assertThat(SevenZipHeaderUtil.readSevenZipUint64(testFileRaf)).isEqualTo(0x01ffffffffffffL);

    testData[0] = (byte)0xfe;
    cleanUpTestFileAndWriteData(testData);
    testFileRaf.seek(0);
    assertThat(SevenZipHeaderUtil.readSevenZipUint64(testFileRaf)).isEqualTo(0x00ffffffffffffffL);

    testData[0] = (byte)0xff;
    cleanUpTestFileAndWriteData(testData);
    testFileRaf.seek(0);
    assertThat(SevenZipHeaderUtil.readSevenZipUint64(testFileRaf)).isEqualTo(0x00ffffffffffffffffL);
  }

  private void cleanUpTestFileAndWriteData(byte[] dataToWrite) throws IOException {
    cleanUpTestFile();
    FileOutputStream outputStream = new FileOutputStream(testFile);
    outputStream.write(dataToWrite);
    outputStream.close();
  }
  
  @Test
  public void testGetFolderUnpackSize() {
    Folder folder = new Folder();
    assertThat(SevenZipHeaderUtil.getFolderUnpackSize(folder)).isEqualTo(0);

    folder.setNumOutStreamsTotal(2);

    BindPair[] bindPairs = new BindPair[2];
    folder.setBindPairs(bindPairs);

    BindPair bindPair0 = new BindPair();
    bindPair0.setOutIndex(0);
    bindPairs[0] = bindPair0;

    BindPair bindPair1 = new BindPair();
    bindPair1.setOutIndex(1);
    bindPairs[1] = bindPair1;

    assertThat(SevenZipHeaderUtil.getFolderUnpackSize(folder)).isEqualTo(0);

    folder.setNumOutStreamsTotal(3);
    long[] unpackSizes = { 1, 2, 3 };
    folder.setUnpackSizes(unpackSizes);
    assertThat(SevenZipHeaderUtil.getFolderUnpackSize(folder)).isEqualTo(3);
  }

  @Test
  public void testReadBitsWithAllAreDefined() throws IOException {
    // 0xCF binary is 11001111, 0xBF binary is 10111111
    byte[] testData = new byte[] { 1, (byte) 0xCF, (byte) 0xBF };
    cleanUpTestFileAndWriteData(testData);
    testFileRaf.seek(0);
    int maxBits = 10;
    BitSet bitSet = SevenZipHeaderUtil.readBitsWithAllAreDefined(testFileRaf, maxBits);
    for (int i = 0; i < maxBits; i++) {
      assertThat(bitSet.get(i)).isEqualTo(true);
    }

    testData[0] = 0;
    cleanUpTestFileAndWriteData(testData);
    testFileRaf.seek(0);
    bitSet = SevenZipHeaderUtil.readBitsWithAllAreDefined(testFileRaf, maxBits);
    assertThat(bitSet.get(0)).isEqualTo(true);
    assertThat(bitSet.get(1)).isEqualTo(true);
    assertThat(bitSet.get(2)).isEqualTo(false);
    assertThat(bitSet.get(3)).isEqualTo(false);
    assertThat(bitSet.get(4)).isEqualTo(true);
    assertThat(bitSet.get(5)).isEqualTo(true);
    assertThat(bitSet.get(6)).isEqualTo(true);
    assertThat(bitSet.get(7)).isEqualTo(true);
    assertThat(bitSet.get(8)).isEqualTo(true);
    assertThat(bitSet.get(9)).isEqualTo(false);
    assertThat(bitSet.get(10)).isEqualTo(false);
    assertThat(bitSet.get(11)).isEqualTo(false);
  }
  
  @Test
  public void testGetOrderedCodersInFolder() {
    Folder folder = new Folder();

    long[] packedStreams = { 0 };
    folder.setPackedStreams(packedStreams);

    Coder coder = new Coder();
    Coder[] coders = new Coder[1];
    coders[0] = coder;
    folder.setCoders(coders);

    BindPair bindPair = new BindPair();
    // outIndex inIndex
    bindPair.setOutIndex(0);
    bindPair.setInIndex(1);
    BindPair[] bindPairs = new BindPair[1];
    bindPairs[0] = bindPair;
    folder.setBindPairs(bindPairs);

    long packedStreamIndex = 0;
    List<Coder> coderList = SevenZipHeaderUtil.getOrderedCodersInFolder(folder);
    assertThat(coderList).isNotNull();
    assertThat(coderList.size()).isEqualTo(1);
    assertThat(coderList.get(0)).isEqualTo(coder);
  }

  @Test
  public void testGetUnpackSizeForCoderInFolder() {
    Folder folder = new Folder();
    long[] unpackSizes = { 1024 };
    folder.setUnpackSizes(unpackSizes);
    Coder[] coders = new Coder[1];
    folder.setCoders(coders);
    Coder coder = new Coder();
    assertThat(SevenZipHeaderUtil.getUncompressedSizeForCoderInFolder(folder, coder)).isEqualTo(-1L);

    coders[0] = coder;
    assertThat(SevenZipHeaderUtil.getUncompressedSizeForCoderInFolder(folder, coder)).isEqualTo(1024);
  }
}
