package edu.cmu.courses.ds.transactionalFileStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

public class TransactionalFileOutputStream extends OutputStream implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private String outFile;

  private long off;

  private boolean mFlag;

  private FileOutputStream out = null;

  private FileInputStream in = null;

  public TransactionalFileOutputStream(String file, boolean append) throws Exception {
    this.outFile = file;
    this.off = 0;
    this.mFlag = false;
    this.out = new FileOutputStream(outFile, append);
  }

  @Override
  public void write(int b) throws IOException {

    if (mFlag) {
      out = new FileOutputStream(outFile);

      in = new FileInputStream(outFile);
      int c;

      for (int i = 0; i < this.off; i++) {
        c = in.read();
        out.write(c);
      }

      in.close();
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
}
