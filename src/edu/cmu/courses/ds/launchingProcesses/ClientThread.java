package edu.cmu.courses.ds.launchingProcesses;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import edu.cmu.courses.ds.mprocess.MigratableProcess;

/*
 * This is a Thread to receive object from other machines.
 */
public class ClientThread implements Runnable {

  Socket clientSocket = null;

  MigratableProcess p = null;

  ClientThread(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  /*
   * Receive an object from others and send true back.
   */
  @Override
  public void run() {
    try {
      ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
      DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

      Object obj = in.readObject();

      if (obj instanceof MigratableProcess) {
        p = (MigratableProcess) obj;
        out.writeBoolean(true);
        ProcessManager.getInstance().startProcess(p);
      } else if (obj instanceof String) {
        if (ProcessManager.getInstance().isMaster()) {
          String str = new String((String) obj);
          processString(str);
        }
        out.writeBoolean(true);
      } else {
        out.writeBoolean(false);
      }

      in.close();
      out.close();
      clientSocket.close();

    } catch (IOException e) {
      System.out.println(e.getMessage());
    } catch (ClassNotFoundException e) {
      System.out.println(e.getMessage());
    }
  }

  private void processString(String str) {
    String[] args = str.split("\t");
    ProcessManager.getInstance().processSlaves(args);
  }

}
