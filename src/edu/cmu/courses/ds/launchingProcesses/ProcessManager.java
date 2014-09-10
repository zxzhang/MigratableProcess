package edu.cmu.courses.ds.launchingProcesses;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import edu.cmu.courses.ds.mprocess.GrepProcess;
import edu.cmu.courses.ds.mprocess.MigratableProcess;
import edu.cmu.courses.ds.mprocess.ReplaceProcess;
import edu.cmu.courses.ds.mprocess.WordCountProcess;

public class ProcessManager {

  private static ProcessManager singleton;

  private Thread receiver = null;

  private ProcessServer runnable = null;

  private AtomicLong idCount;

  private Queue<MigratableProcess> pQueue;

  private boolean master = false;

  private Queue<SlaveManager> sQueue;

  private String masterHost = null;

  private ProcessManager() {
    idCount = new AtomicLong(0);
    pQueue = new LinkedList<MigratableProcess>();
    master = true;
    sQueue = new LinkedList<SlaveManager>();
  }

  private ProcessManager(String masterHost) {
    idCount = new AtomicLong(0);
    pQueue = new LinkedList<MigratableProcess>();
    master = false;
    this.masterHost = masterHost;
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
        doLs();
        break;
      case "des":
        if (isMaster()) {
          doDes(args);
        } else {
          System.out.println("Not master machine...");
        }
        break;
      default:
        System.out.println("Unknown command! Please input again...");
        break;
    }
  }

  private void doDes(String[] args) throws UnknownHostException {
    System.out.println("--- Master ---");
    System.out.println("--- " + InetAddress.getLocalHost().getHostName() + " ---");
    doLs();

    System.out.println("--- Slaves ---");
    Iterator<SlaveManager> iter = sQueue.iterator();
    while (iter.hasNext()) {
      SlaveManager s = iter.next();
      System.out.print(s.toString());
    }
  }

  private void doLs() {
    if (!pQueue.isEmpty()) {
      System.out.println("Running processes:");
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
      System.out.println("Wrong process id. Please input the right process id.");
      return;
    }

    Socket socket = null;
    try {
      socket = new Socket(host, ProcessServer.port);
      p.suspend();
      p.migrated();
      migrate(p, socket);
      socket.close();
    } catch (IOException | InterruptedException e) {
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
    if (args.length < 2) {
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
        if (pArgs.length != 3) {
          System.out.println("usage: GrepProcess <queryString> <inputFile> <outputFile>");
          return;
        }
        process = new GrepProcess(pArgs);
        break;
      case "WordCountProcess":
        if (pArgs.length != 2) {
          System.out.println("usage: WordCountProcess <inputFile> <outputFile>");
          return;
        }
        process = new WordCountProcess(pArgs);
        break;
      case "ReplaceProcess":
        if (pArgs.length != 4) {
          System.out
                  .println("usage: ReplaceProcess <regexString> <replacementString> <inputFile> <outputFile>");
          return;
        }
        process = new ReplaceProcess(pArgs);
        break;
      default:
        System.out.println("Please input the right process name.");
        return;
    }

    startProcess(process);
  }

  private void doQuit() {
    if (receiver != null) {
      runnable.terminate();
    }
    ProcessManager.getInstance().sendMaster("", "quit");
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
        System.out.println(e.getMessage());
        System.exit(1);
      }

      processCommand(command);
    }
  }

  public void startServer() {
    runnable = new ProcessServer();
    receiver = new Thread(runnable);
    receiver.start();
  }

  synchronized static public ProcessManager getInstance() {
    if (singleton == null) {
      singleton = new ProcessManager();
    }
    return singleton;
  }

  synchronized static public ProcessManager getInstance(String masterHost) {
    if (singleton == null) {
      singleton = new ProcessManager(masterHost);
    }
    return singleton;
  }

  public void startProcess(MigratableProcess p) {
    Thread thread = new Thread(p);
    thread.start();
    pQueue.add(p);
    if (master == false) {
      sendMaster(p.toString(), "start");
    }
  }

  public void finishProcess(MigratableProcess p) {
    pQueue.remove(p);
    if (master == false) {
      sendMaster(p.toString(), "remove");
    }
  }

  private void sendMaster(String p, String sr) {
    Socket socket = null;
    try {
      socket = new Socket(masterHost, ProcessServer.port);
      sendMess(p, socket, sr);
      socket.close();
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private void sendMess(String p, Socket socket, String sr) {
    try {
      ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
      DataInputStream in = new DataInputStream(socket.getInputStream());

      String myHost = InetAddress.getLocalHost().getHostName();

      out.writeObject(new String(myHost + '\t' + sr + '\t' + p));

      boolean mFlag = in.readBoolean();
      if (!mFlag) {
        System.out.println("error message!");
      }

      in.close();
      out.close();

      socket.close();
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  public long generateID() {
    return idCount.incrementAndGet();
  }

  public boolean isMaster() {
    return this.master;
  }

  public void processSlaves(String[] args) {
    if (args[1].equals("launch")) {
      SlaveManager slave = new SlaveManager(args[0]);
      sQueue.add(slave);
      return;
    }

    Iterator<SlaveManager> iter = sQueue.iterator();
    while (iter.hasNext()) {
      SlaveManager s = iter.next();
      if (s.isHost(args[0])) {
        if (args[1].equals("start")) {
          s.addProcess(args[2]);
          return;
        } else if (args[1].equals("remove")) {
          s.removeProcess(args[2]);
          return;
        } else if (args[1].equals("quit")) {
          sQueue.remove(s);
          return;
        } else {
          System.out.println("Wrong mess for master...");
          return;
        }
      }
    }

    SlaveManager slave = new SlaveManager(args[0]);
    if (args[1].equals("start")) {
      slave.addProcess(args[2]);
    } else if (args[1].equals("remove")) {
      slave.removeProcess(args[2]);
    }
    sQueue.add(slave);

  }

  public static void main(String args[]) throws Exception {
    if (args.length < 1) {
      System.out.println("Please input the argument...");
    }
    switch (args[0]) {
      case "master":
        if (args.length != 1) {
          System.out.println("wrong argument...");
          break;
        }
        ProcessManager.getInstance().startServer();
        ProcessManager.getInstance().startTinyShell();
        break;
      case "slave":
        if (args.length != 2) {
          System.out.println("wrong argument...");
          break;
        }
        ProcessManager.getInstance(args[1]).startServer();
        ProcessManager.getInstance(args[1]).sendMaster("", "launch");
        ProcessManager.getInstance(args[1]).startTinyShell();
        break;
      default:
        System.out.println("Unknown command! Please input again...");
        break;
    }
  }

}
