package edu.cmu.courses.ds.mprocess;

import java.io.PrintStream;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.Thread;
import java.lang.InterruptedException;

import edu.cmu.courses.ds.launchingProcesses.ProcessManager;
import edu.cmu.courses.ds.transactionalFileStream.TransactionalFileInputStream;
import edu.cmu.courses.ds.transactionalFileStream.TransactionalFileOutputStream;

public class WordCountProcess extends BaseMigratableProcess {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private TransactionalFileInputStream inFile;

  private TransactionalFileOutputStream outFile;

  private volatile boolean suspending;

  private long id;

  public WordCountProcess(String args[]) throws Exception {
    if (args.length != 2) {
      System.out.println("usage: WordCountProcess <inputFile> <outputFile>");
      throw new Exception("Invalid Arguments");
    }

    this.id = ProcessManager.getInstance().generateID();

    inFile = new TransactionalFileInputStream(args[1]);
    outFile = new TransactionalFileOutputStream(args[2], false);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void run() {
    PrintStream out = new PrintStream(outFile);
    DataInputStream in = new DataInputStream(inFile);

    try {
      while (!suspending) {
        String line = in.readLine().trim();

        if (line == null)
          break;

        String[] words = line.split("\\s+");
        out.print(words.length + '\t' + line);

        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          // ignore it
        }

      }
    } catch (EOFException e) {
      // End of File
    } catch (IOException e) {
      System.out.println("WordCountProcess: Error: " + e);
    } finally {
      ProcessManager.getInstance().finishProcess(this);
      suspending = false;
    }

    try {
      in.close();
      out.close();
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }

  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[id: ").append(id).append("] ").append(this.getClass().getSimpleName()).append(" ");
    sb.append("<").append(inFile.toString()).append("> <").append(outFile.toString()).append(">");
    return sb.toString();
  }
}
