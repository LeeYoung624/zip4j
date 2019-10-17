package net.lingala.zip4j.sevenzip.io.inputstream;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.sevenzip.headers.SevenZipHeaderReader;
import net.lingala.zip4j.sevenzip.model.Folder;
import net.lingala.zip4j.sevenzip.model.SevenZipFileEntry;
import net.lingala.zip4j.sevenzip.model.SevenZipModel;
import net.lingala.zip4j.sevenzip.util.InternalSevenZipConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.zip.CRC32;

public class SevenZipInputStream extends InputStream {
  private SevenZipHeaderReader headerReader = new SevenZipHeaderReader();
  private SevenZipModel sevenZipModel;
  private int currentEntryIndex = -1;
  private Folder currentFolder;
  private SevenZipFileEntry currentEntry;
  private RandomAccessFile sevenZipFile;
  private InputStream currentFolderInputStream;
  private InputStream currentEntryInputStream;
  private CRC32 crc32 = new CRC32();
  private InputStream emptyInputStream = new ByteArrayInputStream(new byte[0]);

  public SevenZipInputStream(RandomAccessFile sevenZipFile) throws IOException {
//    InputStream sevenZipIS = Channels.newInputStream(sevenZipFile.getChannel());
    this.sevenZipFile = sevenZipFile;
    sevenZipModel = headerReader.readAllHeaders(sevenZipFile);
  }

  public SevenZipFileEntry getNextEntry() throws IOException {
    if(sevenZipModel == null) {
      throw new ZipException("seven zip model is null");
    }

    if(currentEntryIndex >= sevenZipModel.getFiles().length - 1) {
      return null;
    }

    currentEntryIndex++;
    currentEntry = sevenZipModel.getFiles()[currentEntryIndex];

    if(currentEntryInputStream != null && currentEntry.getCorrespondingFolder() == currentFolder) {
      // skip all the data of the previous entry
      // only skip the data when the folder of current entry is same with previous folder, because if the folder is a new one,
      // then a new inputstream would be created from this new folder, and the previous entry's data don't need to be skipped
      currentEntryInputStream.skip(Long.MAX_VALUE);
    }
    currentEntryInputStream = initializeSevenZipEntryInputStream();
    crc32.reset();

    return currentEntry;
  }

  private InputStream initializeSevenZipEntryInputStream() throws IOException {
    if(currentEntry.getCorrespondingFolder() == null) {
      return emptyInputStream;
    }

    // todo: if the folder equals to the current folder, the mothods should be set for this folder
    if(currentEntry.getCorrespondingFolder() != currentFolder) {
      currentFolder = currentEntry.getCorrespondingFolder();

      if(currentFolderInputStream != null) {
        currentFolderInputStream.close();
      }

      final long currentFolderOffset = InternalSevenZipConstants.SIGNATURE_HEADER_SIZE + sevenZipModel.getPackInfo().getPackPos() + currentFolder.getFolderPackStreamOffset();
      // reset the position to the start of the folder
//      inputStream.reset();
//      inputStream.skip(currentFolderOffset);
      sevenZipFile.seek(currentFolderOffset);
      InputStream inputStream = Channels.newInputStream(sevenZipFile.getChannel());

      currentFolderInputStream = SevenZipCompressionsInputStreamFactory.generateFolderInputStream(inputStream, currentFolder);
    }

    // the current entry inputstream can only read the size of current entry
    return new BoundedInputStream(currentFolderInputStream, currentEntry.getSize());

    // todo:verify folder CRC
  }

  @Override
  public int read() throws IOException {
    byte[] b = new byte[1];
    int readLen = read(b);

    if (readLen == -1) {
      return -1;
    }

    return b[0];
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (len < 0) {
      throw new IllegalArgumentException("Negative read length");
    }

    if(currentEntryInputStream == null) {
      throw new IllegalStateException("getNextEntry should be called first");
    }

    if (len == 0) {
      return 0;
    }

    int readLen = currentEntryInputStream.read(b, off, len);
    if(readLen < 0) {
      // end of current entry
      endOfEntryReached();
    } else {
      crc32.update(b, off, readLen);
    }
    return readLen;
  }

  @Override
  public long skip(long n) throws IOException {
    if(currentEntryInputStream == null) {
      throw new IllegalStateException("getNextEntry should be called first");
    }
    return currentEntryInputStream.skip(n);
  }

  private void endOfEntryReached() throws IOException {
    verifyCrc();
  }

  private void verifyCrc() throws IOException {
    if(crc32.getValue() != currentEntry.getCrc()) {
      throw new ZipException("Reached end of entry, but crc verification failed for " + currentEntry.getName());
    }
  }
}
