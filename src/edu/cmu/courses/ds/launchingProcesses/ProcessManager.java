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

/*
 * This is a process manager class. In this class, there are two options: master or slave.
 * Slave need master's hostname to be launched. 
 */
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

  /*
   * Receive the command and go to each methods...
   */
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
      case "test":
        if (isMaster()) {
          doTest();
        } else {
          System.out.println("Not master machine...");
        }
        break;
      default:
        System.out.println("Unknown command! Please input again...");
        break;
    }
  }

  /*
   * test command method.
   */
  private void doTest() throws Exception {
    String[] command = { "run GrepProcess 5 ./testCase/grepTest ./testCase/grepTest.out",
        "run ReplaceProcess 0 32123 ./testCase/replaceTest ./testCase/replaceTest.out",
        "run WordCountProcess ./testCase/wordTest ./testCase/wordTest.out" };

    for (int i = 0; i < 3; i++) {
      Thread.sleep(100);
      System.out.print("tsh> ");
      System.out.println(command[i]);
      processCommand(command[i]);
    }

    String h = null;
    Iterator<SlaveManager> siter = sQueue.iterator();
    if (siter.hasNext()) {
      SlaveManager s = siter.next();
      h = s.getHost();
    } else {
      System.out.println("Plean launch a slave...");
      return;
    }

    Thread.sleep(3000);
    while (!pQueue.isEmpty()) {
      MigratableProcess p = pQueue.peek();
      long id = p.getId();
      StringBuffer mig = new StringBuffer();
      mig.append("mig ").append(id).append(" ").append(h);
      System.out.print("tsh> ");
      System.out.println(mig.toString());
      processCommand(mig.toString());
      Thread.sleep(2000);
    }

    System.out.println("tsh> des");
    processCommand("des");
  }

  /*
   * describe command method...
   */
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

  /*
   * list command method.
   */
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

  /*
   * migrate a process.
   */
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

    // suspend a process and then set flag. Finally, send it to another machine.
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

  /*
   * send a process to another machine.
   */
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

  /*
   * input the process ID and return the process.
   */
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

  /*
   * run command method.
   */
  private void doRun(String[] args) throws Exception {
    if (args.length < 2) {
      return;
    }

    String pName = args[1];

    String[] pArgs = new String[args.length - 2];
    for (int i = 0; i < (args.length - 2); i++) {
      pArgs[i] = args[i + 2];
    }

    if (startProcess(pName, pArgs) == false) {
      System.out.println("Please input the right process name.");
    }
  }

  /*
   * find a process class and launch a process.
   */
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

  /*
   * quit command method.
   */
  private void doQuit() {
    if (isMaster() == false) {
      ProcessManager.getInstance().sendMaster("", "quit");
    }
    if (receiver != null) {
      runnable.terminate();
    }
    System.exit(0);
  }

  /*
   * help command method.
   */
  private void doHelp() {
    System.out
            .println("Run master node: java -classpath ./src:./jar/reflections-0.9.9-RC1-uberjar.jar:./jar/com.google.common_1.0.0.201004262004.jar:./jar/javassist-3.8.0.GA.jar edu.cmu.courses.ds.launchingProcesses.ProcessManager master");
    System.out
            .println("Run slave node: java -classpath ./src:./jar/reflections-0.9.9-RC1-uberjar.jar:./jar/com.google.common_1.0.0.201004262004.jar:./jar/javassist-3.8.0.GA.jar edu.cmu.courses.ds.launchingProcesses.ProcessManager slave <master's hostname>\n");
    System.out
            .println("help                       : Print help information. Describe all the commands.");
    System.out
            .println("des                        : Display all the machines with all running processes (ID).");
    System.out
            .println("ls                         : Show all the running processes (ID) in this machine.");
    System.out
            .println("mig <processID> <hostname> : Migrage the process with <processID> to another machine(hostname).");
    System.out.println("quit                       : Quit this machine.");
    System.out.println("run <processName> <args>   : Run a process in the machine.");
    System.out.println("         run GrepProcess <queryString> <inputFile> <outputFile>");
    System.out
            .println("         e.g. run GrepProcess 5 ./testCase/grepTest ./testCase/grepTest.out");
    System.out
            .println("         run ReplaceProcess <regexString> <replacementString> <inputFile> <outputFile>");
    System.out
            .println("         e.g. run ReplaceProcess 0 32123 ./testCase/replaceTest ./testCase/replaceTest.out");
    System.out.println("         run WordCountProcess <inputFile> <outputFile>");
    System.out
            .println("         e.g. run WordCountProcess ./testCase/wordTest ./testCase/wordTest.out");
    System.out.println("test                       : Run the test case.");
  }

  /*
   * open the tiny shell to input command.
   */
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

  /*
   * start a server to receive object.
   */
  public void startServer() {
    runnable = new ProcessServer();
    receiver = new Thread(runnable);
    receiver.start();
  }

  /*
   * singleton
   */
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

  /*
   * input a process, add it into the process queue and start it.
   */
  public void startProcess(MigratableProcess p) {
    Thread thread = new Thread(p);
    thread.start();
    pQueue.add(p);
    if (master == false) {
      sendMaster(p.toString(), "start");
    }
  }

  /*
   * remove a process from the process queue.
   */
  public void finishProcess(MigratableProcess p) {
    pQueue.remove(p);
    if (master == false) {
      sendMaster(p.toString(), "remove");
    }
  }

  /*
   * send master a message.
   */
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

  /*
   * send messages...
   */
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

  /*
   * generate a processID.
   */
  public long generateID() {
    return idCount.incrementAndGet();
  }

  /*
   * is master machine?
   */
  public boolean isMaster() {
    return this.master;
  }

  /*
   * receive a message from slaves, this method parse the message.
   */
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

  /*
   * main method. master or slave...
   */
  public static void main(String args[]) throws Exception {
    if (args.length < 1) {
      System.out.println("Please input the argument...");
    }
    switch (args[0]) {
      case "master":
        if (args.length != 1) {
          System.out.println("master");
          break;
        }
        ProcessManager.getInstance().startServer();
        ProcessManager.getInstance().startTinyShell();
        break;
      case "slave":
        if (args.length != 2) {
          System.out.println("slave + <hostname>");
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
