package edu.cmu.courses.ds.mprocess;

import java.io.Serializable;

public interface MigratableProcess extends Runnable, Serializable {

  public void suspend() throws InterruptedException;
  
  public void closeIO() throws InterruptedException;

  public String toString();

  public void run();

  public void migrated();

  public void resume();
  
  public long getId();
}
