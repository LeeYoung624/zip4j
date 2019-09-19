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
import net.lingala.zip4j.sevenzip.model.SevenZipModel;
import net.lingala.zip4j.testutils.TestUtils;

public class SevenZipHeaderReaderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testReadAllHeadersThrowsExceptionWhenFileIsNot7z() throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("7z file signature mismatch");

    File zipFile = TestUtils.getTestArchiveFromResources("invalid_extra_data_record.zip");
    RandomAccessFile sevenZipRaf = new RandomAccessFile(zipFile, RandomAccessFileMode.READ.getValue());
    SevenZipHeaderReader sevenZipHeaderReader = new SevenZipHeaderReader();
    sevenZipHeaderReader.readAllHeaders(sevenZipRaf);
  }

  @Test
  public void testReadAllHeaders() throws IOException {
    File zipFile = TestUtils.getTestArchiveFromResources("sample.pdf.7z");
    RandomAccessFile sevenZipRaf = new RandomAccessFile(zipFile, RandomAccessFileMode.READ.getValue());
    SevenZipHeaderReader sevenZipHeaderReader = new SevenZipHeaderReader();
    SevenZipModel sevenZipModel = sevenZipHeaderReader.readAllHeaders(sevenZipRaf);
    assertThat(sevenZipModel).isNotNull();
    assertThat(sevenZipModel.getSignatureHeader()).isNotNull();
  }

}
