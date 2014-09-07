package edu.cmu.courses.ds.transactionalFileStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

public class TransactionalFileInputStream extends InputStream implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private String inFile;

  private long off;

  private boolean mFlag;

  private transient RandomAccessFile in = null;

  public TransactionalFileInputStream(String file) throws Exception {
    this.inFile = file;
    this.off = 0;
    this.mFlag = false;
    //this.in = new FileInputStream(file);
  }

  @Override
  public int read() throws IOException {
    int c = -1;

    if (mFlag || in == null) {
      in = new RandomAccessFile(new File(inFile), "r");
      in.seek(off);
      mFlag = false;
    }

    c = in.read();
    this.off++;
    return c;
  }

  public void setMigrated(boolean mFlag) {
    this.mFlag = mFlag;
  }

  @Override
  public void close() throws IOException {
    in.close();
  }
  
  @Override
  public String toString() {
    return inFile;
  }
}
