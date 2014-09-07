package edu.cmu.courses.ds.transactionalFileStream;

import java.io.File;
import java.io.FileInputStream;
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

  private String outFile;

  private long off;

  private boolean mFlag;

  private transient RandomAccessFile out = null;

  public TransactionalFileOutputStream(String file, boolean append) throws Exception {
    this.outFile = file;
    this.off = 0;
    this.mFlag = false;
    //this.out = new FileOutputStream(outFile, append);
  }

  @Override
  public void write(int b) throws IOException {

    if (mFlag || out == null) {
      out = new RandomAccessFile(new File(outFile), "wr");
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
    return outFile;
  }
}
