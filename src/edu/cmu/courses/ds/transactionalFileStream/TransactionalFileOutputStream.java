package edu.cmu.courses.ds.transactionalFileStream;

import java.io.File;
import java.io.FileOutputStream;
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

  private long off;

  private boolean mFlag;

  private transient FileOutputStream out = null;

  public TransactionalFileOutputStream(String file, boolean append) throws Exception {
    this.outFilePath = file;
    this.off = 0;
    this.mFlag = false;
    this.out = new FileOutputStream(outFilePath);
  }

  @Override
  public void write(int b) throws IOException {

    if (mFlag) {
      out = new FileOutputStream(outFilePath);
      out.write(b);
      mFlag = false;
    }

    out.write(b);
    this.off++;
  }

  public void setMigrated() {
    this.mFlag = true;
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
