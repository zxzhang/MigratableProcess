package edu.cmu.courses.ds.launchingProcesses;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class processServer implements Runnable {

  public static final int port = 15640;

  private volatile boolean running = false;

  private ServerSocket serverSocket;

  private Socket clientSocket = null;

  Thread clientThread = null;

  @Override
  public void run() {
    running = true;
    try {
      serverSocket = new ServerSocket(port);
    } catch (IOException e) {
      System.out.println(e.getMessage());
      System.exit(1);
    }
    while (running) {
      try {
        clientSocket = serverSocket.accept();
      } catch (SocketException e) {
        if (running) {
          System.out.println(e.getMessage());
          System.exit(1);
        }
      } catch (IOException e) {
        System.out.println(e.getMessage());
        System.exit(1);
      }
      clientThread = new Thread(new clientThread(clientSocket));
      clientThread.start();
    }
  }

  public void terminate() {
    running = false;
  }
}
