package socketclient;

import java.util.*;
import java.lang.reflect.*;
import java.net.*;
import java.io.*;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import com.google.gson.*;
import com.google.gson.reflect.*;

public class Client implements Runnable
{
	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;
	private Timer timer;
	private OnMessage receiver;
	private Thread readerThread;
	private final int TIMEOUTDELAY = 30000;
	private final boolean enableTimeout = false;
	private volatile boolean runReader;
	private ActionListener timerListener = new ActionListener()
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			System.out.println("Timed out");
			closeSocket();
		}
	};

	public Client(OnMessage client)
	{
		receiver = client;
	}

	public boolean openSocket(String host, int port)
	{
		try {
			InetAddress ip = InetAddress.getByName(host);
			socket = new Socket(ip, port);
			runReader = true;
			timer = new Timer(TIMEOUTDELAY, timerListener);
			writer = new PrintWriter(socket.getOutputStream(), true);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			readerThread = new Thread(this);
			readerThread.start();
			if(enableTimeout)
			{
				timer.start();
			}
			return true;
		} catch (IOException e) {
			System.out.println("Socket Connection Error");
			e.printStackTrace();
			return false;
		}
	}

	public synchronized boolean closeSocket()
	{
		if(enableTimeout)
		{
			timer.stop();
		}

		runReader = false;
		String payload = encodeJson("EXIT", 3);
		sendMessage(payload);

		try {
			readerThread.join();
		} catch (InterruptedException e) {
			System.out.println("Thread join error");
		}

		writer.flush();
		writer.close();

		try {
			reader.close();
		} catch (IOException e) {
			System.out.println("Reader error");
		}

		try {
			socket.close();
			return true;
		} catch (IOException e) {
			System.out.println("Connection close error");
		}

		return false;
	}

	@Override
	public void run()
	{
		int errorCount = 0;
		String received = "";
		while (runReader)
		{
			try {
				received = receiveMessage();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Read error");
			}
			if (received == null)
			{
				if (socket.isConnected())
				{
					System.out.println("Received NULL");
					errorCount++;
					if (errorCount < 4)
					{
						continue;
					}
					else
					{
						System.out.println("Error count exceeded");
						break;
					}
				}
				else
				{
					System.out.println("Connection error");
					break;
				}
			}
			else if (received.trim().equals("EXIT"))
			{
				break;
			}
			else if (socket.isConnected() == false)
			{
				System.out.println("Connection lost");
				closeSocket();
				break;
			}
			else if (runReader)
			{
				if(enableTimeout)
				{
					timer.restart();
				}

				errorCount = 0;

				//code 0 = received broadcasted message
				//code 1 = set username
				//code 2 = received list of connected clients
				//code 3 = exit

				Map<String, String> load = decodeJson(received);
				if (load.get("code").equals("0"))
				{
					receiver.onMessage(load.get("username"), load.get("message"));
				}
				else if (load.get("code").equals("2"))
				{
					String[] payload = load.get("message").split(",");
					receiver.updateConnected(payload);
				}
			}
		}
	}

	public void sendMessage(String message)
	{
		message += "\n";
		writer.write(message, 0, message.length());
		writer.flush();
	}

	public String receiveMessage() throws IOException
	{
		try {
			return reader.readLine();
		} catch (IOException e) {
			throw e;
		}
	}

	public String encodeJson(String message, int code)
	{
		Map<String, String> map = new HashMap<>();
		map.put("message", message);
		map.put("code", Integer.toString(code));
		Gson gson = new GsonBuilder().create();
		return gson.toJson(map);
	}

	public Map<String, String> decodeJson(String received)
	{
		Gson gson = new Gson();
		Type type = new TypeToken<Map<String, String>>(){}.getType();
		return gson.fromJson(received, type);
	}
}
