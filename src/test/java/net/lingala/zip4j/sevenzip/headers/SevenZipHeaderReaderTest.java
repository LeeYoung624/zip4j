package net.lingala.zip4j.sevenzip.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.ZipException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.sevenzip.model.SevenZipFileEntry;
import net.lingala.zip4j.sevenzip.model.SevenZipModel;
import net.lingala.zip4j.testutils.TestUtils;

public class SevenZipHeaderReaderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testReadAllHeadersThrowsExceptionWhenFileIsNot7z() throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("7z file signature mismatch");

    getSevenZipModel("invalid_extra_data_record.zip");
  }

  @Test
  public void testSingleEmptyDirectory() throws IOException {
    SevenZipModel sevenZipModel = getSevenZipModel("single_empty_directory.7z");
    assertThat(sevenZipModel).isNotNull();
    assertThat(sevenZipModel.getFiles().length).isEqualTo(1);
    assertThat(sevenZipModel.getFiles()[0].isDirectory()).isEqualTo(true);
  }

  @Test
  public void testMultipleEmptyDirectory() throws IOException {
    SevenZipModel sevenZipModel = getSevenZipModel("multiple_empty_directory.7z");
    assertThat(sevenZipModel).isNotNull();
    assertThat(sevenZipModel.getFiles().length).isEqualTo(9);
    for (SevenZipFileEntry sevenZipFileEntry : sevenZipModel.getFiles()) {
      assertThat(sevenZipFileEntry.isDirectory()).isEqualTo(true);
    }
  }

  @Test
  public void testSingleFile() throws IOException {
    SevenZipModel sevenZipModel = getSevenZipModel("single_file.7z");
    assertThat(sevenZipModel).isNotNull();
    assertThat(sevenZipModel.getSignatureHeader()).isNotNull();
    assertThat(sevenZipModel.getFiles().length).isEqualTo(1);
    assertThat(sevenZipModel.getFiles()[0].isDirectory()).isEqualTo(false);
  }

  @Test
  public void testMultipleFiles() throws IOException {
    SevenZipModel sevenZipModel = getSevenZipModel("multiple_files.7z");
    assertThat(sevenZipModel).isNotNull();
    assertThat(sevenZipModel.getFiles().length).isEqualTo(3);
    for (SevenZipFileEntry sevenZipFileEntry : sevenZipModel.getFiles()) {
      assertThat(sevenZipFileEntry.isDirectory()).isEqualTo(false);
    }
  }

  @Test
  public void testFilesAndDirectories() throws IOException {
    SevenZipModel sevenZipModel = getSevenZipModel("corrupt_extra_data_record_length.7z");
    assertThat(sevenZipModel).isNotNull();
    // assertThat(sevenZipModel.getSignatureHeader()).isNotNull();
    // files plus directories
    assertThat(sevenZipModel.getFiles().length).isEqualTo(50);
  }

  private SevenZipModel getSevenZipModel(String fileName) throws IOException {
    File zipFile = TestUtils.getTestArchiveFromResources(fileName);
    RandomAccessFile sevenZipRaf = new RandomAccessFile(zipFile, RandomAccessFileMode.READ.getValue());
    SevenZipHeaderReader sevenZipHeaderReader = new SevenZipHeaderReader();
    SevenZipModel sevenZipModel = sevenZipHeaderReader.readAllHeaders(sevenZipRaf);
    return sevenZipModel;
  }

}
