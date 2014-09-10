package edu.cmu.courses.ds.launchingProcesses;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.reflections.Reflections;
import edu.cmu.courses.ds.mprocess.MigratableProcess;

public class ProcessManager {

  private static ProcessManager singleton;

  private Thread receiver = null;

  private ProcessServer runnable = null;

  private AtomicLong idCount;

  private Queue<MigratableProcess> pQueue;

  private boolean master = false;

  private Queue<SlaveManager> sQueue;

  private String masterHost = null;

  private Set<Class<? extends MigratableProcess>> mProcesses = null;

  private ProcessManager() {
    idCount = new AtomicLong(0);
    pQueue = new LinkedList<MigratableProcess>();
    master = true;
    sQueue = new LinkedList<SlaveManager>();
    Reflections reflections = new Reflections("edu.cmu.courses.ds.mprocess");
    mProcesses = reflections.getSubTypesOf(MigratableProcess.class);
  }

  private ProcessManager(String masterHost) {
    idCount = new AtomicLong(0);
    pQueue = new LinkedList<MigratableProcess>();
    master = false;
    this.masterHost = masterHost;
    Reflections reflections = new Reflections("edu.cmu.courses.ds.mprocess");
    mProcesses = reflections.getSubTypesOf(MigratableProcess.class);
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
      case "help":
        doHelp();
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
      // System.out.println("Running processes:");
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

    String[] pArgs = new String[args.length - 2];
    for (int i = 0; i < (args.length - 2); i++) {
      pArgs[i] = args[i + 2];
    }

    /*
     * switch (pName) { case "GrepProcess": if (pArgs.length != 3) {
     * System.out.println("usage: GrepProcess <queryString> <inputFile> <outputFile>"); return; }
     * process = new GrepProcess(pArgs); break; case "WordCountProcess": if (pArgs.length != 2) {
     * System.out.println("usage: WordCountProcess <inputFile> <outputFile>"); return; } process =
     * new WordCountProcess(pArgs); break; case "ReplaceProcess": if (pArgs.length != 4) {
     * System.out
     * .println("usage: ReplaceProcess <regexString> <replacementString> <inputFile> <outputFile>");
     * return; } process = new ReplaceProcess(pArgs); break; default:
     * System.out.println("Please input the right process name."); return; }
     */

    if (startProcess(pName, pArgs) == false) {
      System.out.println("Please input the right process name.");
    }
  }

  private boolean startProcess(String pName, String[] pArgs) throws InstantiationException,
          IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Iterator<Class<? extends MigratableProcess>> iter = mProcesses.iterator();

    while (iter.hasNext()) {
      Class<? extends MigratableProcess> process = iter.next();

      if (process.getSimpleName().equals(pName)) {
        Constructor<?>[] ctors = process.getDeclaredConstructors();
        Constructor<?> ctor = null;

        for (int i = 0; i < ctors.length; i++) {
          ctor = ctors[i];
          if (ctor.getGenericParameterTypes().length != 0)
            break;
        }

        MigratableProcess p = (MigratableProcess) ctor.newInstance((Object) pArgs);
        startProcess(p);

        return true;
      }
    }
    return false;
  }

  private void doQuit() {
    if (isMaster() == false) {
      ProcessManager.getInstance().sendMaster("", "quit");
    }
    if (receiver != null) {
      runnable.terminate();
    }
    System.exit(0);
  }
  
  private void doHelp() {
    System.out.println("help : Print help information. Describe all the commands.");
    System.out.println("des  : Display all the machines with all running processes (ID).");
    System.out.println("ls   : Show all the running processes (ID) in this machine.");
    System.out.println("mig <processID> <hostname> : Migrage the process with <processID> to another machine(hostname).");
    System.out.println("quit : Quit this machine.");
    System.out.println("run <processName> <args> : Run a process in the machine.");
    System.out.println("     e.g. run GrepProcess <queryString> <inputFile> <outputFile>");
    System.out.println("     e.g. run ReplaceProcess <regexString> <replacementString> <inputFile> <outputFile>");
    System.out.println("     e.g. run WordCountProcess <inputFile> <outputFile>");
  }

  public void startTinyShell() throws Exception {
    System.out.println("15640 project-1 ...");

    doHelp();
    
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
