package edu.cmu.courses.ds.launchingProcesses;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class SlaveManager {
  private String host;

  private Queue<String> pQueue;

  SlaveManager(String host) {
    this.host = new String(host);
    this.pQueue = new LinkedList<String>();
  }

  public boolean isHost(String host) {
    return (this.host.equals(host));
  }

  public void addProcess(String process) {
    pQueue.add(process);
  }

  public boolean removeProcess(String process) {
    return pQueue.remove(process);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("--- ").append(host).append(" ---").append("\n");
    Iterator<String> iter = pQueue.iterator();
    while (iter.hasNext()) {
      String p = iter.next();
      sb.append(p).append("\n");
    }
    return sb.toString();
  }
}
