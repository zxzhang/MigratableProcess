package edu.cmu.courses.ds.launchingProcesses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessManager {

  private static ProcessManager singleton;

  private void processCommand(String command) {
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
      default:
        System.out.println("unknown command! Please input again...");
        break;
    }
  }

  private void doMig(String[] args) {
    // TODO Auto-generated method stub
    
  }

  private void doRun(String[] args) {
    // TODO Auto-generated method stub
    
  }

  private void doQuit() {
    System.exit(0);
  }

  public void startTinyShell() {
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

  synchronized static public ProcessManager getInstance() {
    if (singleton == null) {
      singleton = new ProcessManager();
    }
    return singleton;
  }

  public static void main(String args[]) {
    ProcessManager.getInstance().startTinyShell();
  }
}
