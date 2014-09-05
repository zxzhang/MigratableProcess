package edu.cmu.courses.ds.mprocess;

import java.io.PrintStream;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.Thread;
import java.lang.InterruptedException;

import edu.cmu.courses.ds.transactionalFileStream.TransactionalFileInputStream;
import edu.cmu.courses.ds.transactionalFileStream.TransactionalFileOutputStream;

public class GrepProcess implements MigratableProcess {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private TransactionalFileInputStream inFile;

  private TransactionalFileOutputStream outFile;

  private String query;

  private volatile boolean suspending;

  public GrepProcess(String args[]) throws Exception {
    if (args.length != 3) {
      System.out.println("usage: GrepProcess <queryString> <inputFile> <outputFile>");
      throw new Exception("Invalid Arguments");
    }

    query = args[0];
    inFile = new TransactionalFileInputStream(args[1]);
    outFile = new TransactionalFileOutputStream(args[2], false);
  }

  @Override
  public void suspend() throws InterruptedException {
    suspending = true;
    while (suspending)
      ;
  }

  @SuppressWarnings("deprecation")
  @Override
  public void run() {
    PrintStream out = new PrintStream(outFile);
    DataInputStream in = new DataInputStream(inFile);

    try {
      while (!suspending) {
        String line = in.readLine();

        if (line == null)
          break;

        if (line.contains(query)) {
          out.println(line);
        }

        // Make grep take longer so that we don't require extremely large files for interesting
        // results
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          // ignore it
        }
      }
    } catch (EOFException e) {
      // End of File
    } catch (IOException e) {
      System.out.println("GrepProcess: Error: " + e);
    }

    suspending = false;
  }

  @Override
  public void migrated() {
    inFile.setMigrated(true);
    outFile.setMigrated(true);
  }

  /*
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(this.getClass().getSimpleName());
    sb.append("[" + id + "]: ");

    return sb.toString();
  }
  */

}