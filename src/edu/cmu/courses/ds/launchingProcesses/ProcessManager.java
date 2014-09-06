package edu.cmu.courses.ds.launchingProcesses;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import edu.cmu.courses.ds.mprocess.GrepProcess;
import edu.cmu.courses.ds.mprocess.MigratableProcess;

public class ProcessManager {

  private static ProcessManager singleton;
  
  private Thread receiver = null;
  private processServer runnable = null;
  
  private AtomicLong idCount;
  private Queue<MigratableProcess> pQueue;

  private ProcessManager() {
    idCount = new AtomicLong(0);
    pQueue = new LinkedList<MigratableProcess>();
  }
  
  private void processCommand(String command) throws Exception {
    if (command == null) {
      return;
    }

    String[] args = command.split("\\s+");
    if (args.length == 0) {
      return;
    }

    switch (args[0]) {
      case "quit":
        doQuit();
        break;
      case "run":
        doRun(args);
        break;
      case "mig":
        doMig(args);
        break;
      case "ls":
        doLs(args);
        break;
      default:
        System.out.println("unknown command! Please input again...");
        break;
    }
  }

  private void doLs(String[] args) {
    if (pQueue.isEmpty()) {
      System.out.println("None...");
    } else {
      System.out.println("running processes...");
      Iterator<MigratableProcess> iter = pQueue.iterator();
      while (iter.hasNext()) {
        MigratableProcess p = iter.next();
        System.out.println(p.toString());
      }
    }
  }

  private void doMig(String[] args) {
    if (args.length < 3) {
      return;
    }
    
    long id = Long.parseLong(args[1]);
    String host = args[2];
    MigratableProcess p = getProcessbyID(id);
    
    if (p == null) {
      System.out.println("wrong process id...");
      return;
    }
    
    Socket socket = null;
    try {
      socket = new Socket(host, processServer.port);
      p.suspend();
      migrate(p, socket);
      socket.close();
    } catch (InterruptedException e) {
      System.out.println(e.getMessage());
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private void migrate(MigratableProcess p, Socket socket) {
    try {
      ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
      DataInputStream in = new DataInputStream(socket.getInputStream());
      
      out.writeObject(p);
      
      boolean mFlag = in.readBoolean();
      if (!mFlag) {
        p.resume();
        p.migrated();
        startProcess(p);
      }
      
      in.close();
      out.close();
      
      socket.close();
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private MigratableProcess getProcessbyID(long id) {
    Iterator<MigratableProcess> iter = pQueue.iterator();
    while (iter.hasNext()) {
      MigratableProcess p = iter.next();
      if (p.getId() == id) {
        return p;
      }
    }
    return null;
  }

  private void doRun(String[] args) throws Exception {
    if (args.length < 4) {
      return;
    }
    
    String pName = args[1];
    MigratableProcess process = null;
        
    String[] pArgs = new String[args.length - 2];
    for (int i = 0; i < (args.length - 2); i++) {
      pArgs[i] = args[i + 2];
    }
    
    switch (pName) {
      case "GrepProcess":
        process = new GrepProcess(pArgs);
        break;
      default:
        return;
    }
    
    startProcess(process);
  }

  private void doQuit() {
    if (receiver != null) {
      runnable.terminate();
      try {
        receiver.join();
      } catch (InterruptedException e) {
        System.out.println(e.getMessage());
      }
    }
    System.exit(0);
  }

  public void startTinyShell() throws Exception {
    System.out.println("15640 project1! ...");

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    while (true) {
      System.out.print("tsh> ");
      String command = null;

      try {
        command = br.readLine();
      } catch (IOException e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }

      processCommand(command);
    }
  }
  
  public void startServer() {
    runnable = new processServer();
    receiver = new Thread(runnable);
    receiver.start();
  }

  synchronized static public ProcessManager getInstance() {
    if (singleton == null) {
      singleton = new ProcessManager();
    }
    return singleton;
  }

  public static void main(String args[]) throws Exception {
    ProcessManager.getInstance().startServer();
    ProcessManager.getInstance().startTinyShell();
  }

  public void startProcess(MigratableProcess p) {
    Thread thread = new Thread(p);
    thread.start();
    pQueue.add(p);
  }
  
  public void finishProcess(MigratableProcess p) {
    pQueue.remove(p);
  }
  
  public long generateID() {
    return idCount.incrementAndGet();
  }
}