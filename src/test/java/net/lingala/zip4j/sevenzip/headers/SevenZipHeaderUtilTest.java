package net.lingala.zip4j.sevenzip.headers;

import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.util.RawIO;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.Arrays;

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
    FileOutputStream outputStream = new FileOutputStream(testFile);

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
}
