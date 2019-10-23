package net.lingala.zip4j.sevenzip.io.inputstream;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.sevenzip.model.SevenZipFileEntry;
import net.lingala.zip4j.testutils.TestUtils;

public class SevenZipInputStreamTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testGetNextEntry() throws IOException {
    SevenZipInputStream sevenZipInputStream = getSevenZipInputStream("corrupt_extra_data_record_length.7z");
    assertThat(sevenZipInputStream.getNextEntry()).isNotNull();
  }

  @Test
  public void testReadThrowsException() throws IOException {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("getNextEntry should be called first");

    SevenZipInputStream sevenZipInputStream = getSevenZipInputStream("corrupt_extra_data_record_length.7z");
    sevenZipInputStream.read();
  }

  @Test
  public void testRead() throws IOException {
    SevenZipInputStream sevenZipInputStream = getSevenZipInputStream("corrupt_extra_data_record_length.7z");
    sevenZipInputStream.getNextEntry();
    sevenZipInputStream.read();
  }

  @Test
  public void testReadBytes() throws IOException {
    SevenZipInputStream sevenZipInputStream = getSevenZipInputStream("corrupt_extra_data_record_length.7z");
    sevenZipInputStream.getNextEntry();

    byte[] bytes = new byte[3];
    assertThat(sevenZipInputStream.read(bytes)).isEqualTo(bytes.length);
  }

  @Test
  public void testReadBytesWithLen() throws IOException {
    SevenZipInputStream sevenZipInputStream = getSevenZipInputStream("corrupt_extra_data_record_length.7z");
    sevenZipInputStream.getNextEntry();
    byte[] bytes = new byte[3];
    assertThat(sevenZipInputStream.read(bytes, 0, 2)).isEqualTo(2);
    assertThat(bytes[2]).isEqualTo((byte) 0);
  }

  @Test
  public void testSkipThrowsException() throws IOException {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("getNextEntry should be called first");

    SevenZipInputStream sevenZipInputStream = getSevenZipInputStream("corrupt_extra_data_record_length.7z");
    sevenZipInputStream.skip(1);
  }

  @Test
  public void testSkip() throws IOException {
    SevenZipInputStream sevenZipInputStream = getSevenZipInputStream("corrupt_extra_data_record_length.7z");
    SevenZipFileEntry sevenZipFileEntry = sevenZipInputStream.getNextEntry();

    int n = new Random().nextInt((int) sevenZipFileEntry.getSize());
    assertThat(sevenZipInputStream.skip(n)).isEqualTo(n);

    sevenZipFileEntry = sevenZipInputStream.getNextEntry();

    assertThat(sevenZipInputStream.skip(Long.MAX_VALUE)).isEqualTo(sevenZipFileEntry.getSize());
  }

  @Test
  public void testUn7z() throws IOException {
    File targetDir = temporaryFolder.newFolder("targetDir");
    SevenZipInputStream sevenZipInputStream = getSevenZipInputStream("corrupt_extra_data_record_length.7z");
    SevenZipFileEntry sevenZipFileEntry = sevenZipInputStream.getNextEntry();
    File file = null;
    File parentFile = null;
    OutputStream os = null;
    byte[] content = new byte[1024];
    int readLen = 0;
    while (sevenZipFileEntry != null) {
      if (sevenZipFileEntry.isDirectory()) {
        new File(targetDir, sevenZipFileEntry.getName()).mkdirs();
        sevenZipFileEntry = sevenZipInputStream.getNextEntry();
        continue;
      }
      file = new File(targetDir, sevenZipFileEntry.getName());
      parentFile = file.getParentFile();
      if (!parentFile.exists()) {
        parentFile.mkdirs();
      }
      os = new FileOutputStream(file);
      while ((readLen = sevenZipInputStream.read(content)) > 0) {
        os.write(content, 0, readLen);
      }
      os.close();
      sevenZipFileEntry = sevenZipInputStream.getNextEntry();
    }
    sevenZipInputStream.close();

  }

  private SevenZipInputStream getSevenZipInputStream(String fileName) throws IOException {
    File file = TestUtils.getTestArchiveFromResources(fileName);
    RandomAccessFile sevenZipFile = new RandomAccessFile(file, RandomAccessFileMode.READ.getValue());
    SevenZipInputStream sevenZipInputStream = new SevenZipInputStream(sevenZipFile);
    return sevenZipInputStream;
  }

}
