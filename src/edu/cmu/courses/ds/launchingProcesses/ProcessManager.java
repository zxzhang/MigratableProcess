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

	private ProcessServer runnable = null;

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
			System.out.println("Unknown command! Please input again...");
			break;
		}
	}

	private void doLs(String[] args) {
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
			ObjectOutputStream out = new ObjectOutputStream(
					socket.getOutputStream());
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

	public static void main(String args[]) throws Exception {
		ProcessManager.getInstance().startServer();
		ProcessManager.getInstance().startTinyShell();
	}
}
