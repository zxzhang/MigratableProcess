package edu.cmu.courses.ds.mprocess;

import edu.cmu.courses.ds.transactionalFileStream.TransactionalFileInputStream;
import edu.cmu.courses.ds.transactionalFileStream.TransactionalFileOutputStream;

public abstract class BaseMigratableProcess implements MigratableProcess {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private TransactionalFileInputStream inFile;

  private TransactionalFileOutputStream outFile;

  private volatile boolean suspending;

  private long id;

  @Override
  public void suspend() throws InterruptedException {
    suspending = true;
    while (suspending)
      ;
  }

  @Override
  public void migrated() {
    inFile.setMigrated();
    outFile.setMigrated();
  }

  @Override
  public long getId() {
    return this.id;
  }

}
