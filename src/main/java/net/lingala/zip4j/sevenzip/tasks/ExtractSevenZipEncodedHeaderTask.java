package net.lingala.zip4j.sevenzip.tasks;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.sevenzip.coders.SevenZipCoder;
import net.lingala.zip4j.sevenzip.headers.SevenZipHeaderUtil;
import net.lingala.zip4j.sevenzip.model.Coder;
import net.lingala.zip4j.sevenzip.model.Folder;
import net.lingala.zip4j.sevenzip.model.enums.SevenZipCompressionMethod;
import net.lingala.zip4j.tasks.AsyncZipTask;
import net.lingala.zip4j.sevenzip.tasks.ExtractSevenZipEncodedHeaderTask.ExtractSevenZipEncodedHeaderTaskParameters;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.CRC32;

public class ExtractSevenZipEncodedHeaderTask extends AsyncZipTask<ExtractSevenZipEncodedHeaderTaskParameters> {
  private char[] password;

  private byte[] result;

  public ExtractSevenZipEncodedHeaderTask(ProgressMonitor progressMonitor, boolean runInThread) {
    super(progressMonitor, runInThread);
  }

  @Override
  protected void executeTask(ExtractSevenZipEncodedHeaderTaskParameters taskParameters, ProgressMonitor progressMonitor) throws IOException {
    // for compressed header, there should be only one packed stream, which is compressedHeaderFolder.getPackedStreams()[0]
    List<Coder> orderedCoders = SevenZipHeaderUtil.getOrderedCodersInFolder(taskParameters.compressedHeaderFolder, taskParameters.compressedHeaderFolder.getPackedStreams()[0]);
    SevenZipCompressionMethod compressionMethod;
    long unpackSize = 0L;
    InputStream inputStream = taskParameters.inputStream;
    SevenZipCoder sevenZipCoder;
    for(Coder coder : orderedCoders) {
      // todo : coder should have only 1 input stream and 1 output stream
      unpackSize = SevenZipHeaderUtil.getUnpackSizeForCoderInFolder(taskParameters.compressedHeaderFolder, coder);
      sevenZipCoder = SevenZipCompressionMethod.getCompressionMethodFromCode(coder).getSevenZipCoder();
      inputStream = sevenZipCoder.decode(coder, inputStream, unpackSize);
    }
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

    public ExtractSevenZipEncodedHeaderTaskParameters(InputStream inputStream, Folder compressedHeaderFolder) {
      this.inputStream = inputStream;
      this.compressedHeaderFolder = compressedHeaderFolder;
    }
  }
}
