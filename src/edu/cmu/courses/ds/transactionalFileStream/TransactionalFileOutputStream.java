package edu.cmu.courses.ds.transactionalFileStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

public class TransactionalFileOutputStream extends OutputStream implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private String outFilePath;
  
  private File outFile;

  private long off;

  private boolean mFlag;

  private transient RandomAccessFile out = null;

  public TransactionalFileOutputStream(String file, boolean append) throws Exception {
    this.outFilePath = file;
    this.off = 0;
    this.mFlag = false;
    this.outFile = new File(file);
  }

  @Override
  public void write(int b) throws IOException {

    if (mFlag || out == null) {
      out = new RandomAccessFile(outFile, "rw");
      out.seek(off);
      mFlag = false;
    }

    out.write(b);
    this.off++;
  }

  public void setMigrated(boolean mFlag) {
    this.mFlag = mFlag;
  }

  @Override
  public void close() throws IOException {
    out.close();
  }
  
  @Override
  public String toString() {
    return outFilePath;
  }
}
