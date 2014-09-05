package edu.cmu.courses.ds.mprocess;

import java.io.Serializable;

public interface MigratableProcess extends Runnable, Serializable {
  /*
  List<String> arguments;
  boolean suspending;
  long id;
  */
  //public abstract MigratableProcess();
  //public abstract MigratableProcess(String[] args);
  public void suspend() throws InterruptedException;
  public String toString();
  public void run();
  public void migrated();
  
  //public void resume();
  //public long getId();
  
  //public void processing() throws IOException;
}
