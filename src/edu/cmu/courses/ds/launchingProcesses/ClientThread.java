package edu.cmu.courses.ds.launchingProcesses;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import edu.cmu.courses.ds.mprocess.MigratableProcess;

public class ClientThread implements Runnable {

  Socket clientSocket = null;

  MigratableProcess p = null;

  ClientThread(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    try {
      ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
      DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

      Object obj = in.readObject();

      if (obj instanceof MigratableProcess) {
        p = (MigratableProcess) obj;
        p.migrated();
        out.writeBoolean(true);
        ProcessManager.getInstance().startProcess(p);
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

}
