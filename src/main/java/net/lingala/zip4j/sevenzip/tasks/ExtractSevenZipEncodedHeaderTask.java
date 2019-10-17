package net.lingala.zip4j.sevenzip.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.sevenzip.headers.SevenZipHeaderUtil;
import net.lingala.zip4j.sevenzip.io.inputstream.SevenZipCompressionsInputStreamFactory;
import net.lingala.zip4j.sevenzip.model.Coder;
import net.lingala.zip4j.sevenzip.model.Folder;
import net.lingala.zip4j.tasks.AsyncZipTask;
import net.lingala.zip4j.sevenzip.tasks.ExtractSevenZipEncodedHeaderTask.ExtractSevenZipEncodedHeaderTaskParameters;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.CRC32;

public class ExtractSevenZipEncodedHeaderTask extends AsyncZipTask<ExtractSevenZipEncodedHeaderTaskParameters> {
  private char[] password;

  private byte[] result;

  private byte[] buffer = new byte[4096];

  public ExtractSevenZipEncodedHeaderTask(ProgressMonitor progressMonitor, boolean runInThread) {
    super(progressMonitor, runInThread);
  }

  @Override
  protected void executeTask(ExtractSevenZipEncodedHeaderTaskParameters taskParameters, ProgressMonitor progressMonitor) throws IOException {
    // for compressed header, there should be only one packed stream, which is compressedHeaderFolder.getPackedStreams()[0]
    List<Coder> orderedCoders = SevenZipHeaderUtil.getOrderedCodersInFolder(taskParameters.compressedHeaderFolder);
    long unpackSize = 0L;
//    InputStream inputStream = taskParameters.inputStream;
//    for(Coder coder : orderedCoders) {
//      // todo : coder should have only 1 input stream and 1 output stream
//      unpackSize = SevenZipHeaderUtil.getUncompressedSizeForCoderInFolder(taskParameters.compressedHeaderFolder, coder);
//      inputStream = SevenZipCompressionsInputStreamFactory.generateInputStream(inputStream, coder, unpackSize);
//    }
    InputStream inputStream = SevenZipCompressionsInputStreamFactory.generateFolderInputStream(taskParameters.inputStream, taskParameters.compressedHeaderFolder);
    result = new byte[(int)unpackSize];
    while(inputStream.read(result) > 0);

    // verify CRC32 if it exists
    if(taskParameters.compressedHeaderFolder.isHasCrc()) {
      CRC32 crc32 = new CRC32();
      crc32.reset();
      crc32.update(result, 0, result.length);
      if(crc32.getValue() != taskParameters.compressedHeaderFolder.getCrc()) {
        throw new java.util.zip.ZipException("compressed header CRC32 verify failed");
      }
    }
  }

  @Override
  protected long calculateTotalWork(ExtractSevenZipEncodedHeaderTaskParameters taskParameters) throws ZipException {
    return 0L;
  }

  @Override
  protected ProgressMonitor.Task getTask() {
    return null;
  }

  public byte[] getResult() {
    return result;
  }

  public static class ExtractSevenZipEncodedHeaderTaskParameters {
    private InputStream inputStream;
    private Folder compressedHeaderFolder;
    private long compressedHeaderSize;

    public ExtractSevenZipEncodedHeaderTaskParameters(InputStream inputStream, Folder compressedHeaderFolder, long compressedHeaderSize) {
      this.inputStream = inputStream;
      this.compressedHeaderFolder = compressedHeaderFolder;
      this.compressedHeaderSize = compressedHeaderSize;
    }
  }
}
