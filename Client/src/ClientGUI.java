package socketclient;

import java.util.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.sound.sampled.*;

public class ClientGUI implements KeyListener, OnMessage
{
	private Client client;
	private JTextArea chatBox;
	private JTextField inputBox;
	private JFrame frame;
	private JTextField usernameEntry;
	private String message;
	private JButton btConnectbutton;
	private JButton btDisconnectButton;
	private JButton sendButton;
	private JButton clearButton;
	private JButton setNameButton;
	private boolean connectState;
	private String userName;
	private JList lsList0;
	private JPanel pnPanel0;
	private Panel p;
	private JTextArea userBox;
	private ArrayList<Integer> keyCodes = new ArrayList<Integer>();
	private boolean colorShiftActive = false;
	private final String HOST;
	private final int PORT;

	public ClientGUI(String title, String host, int port)
	{
		client = new Client(this);

		this.HOST = host;
		this.PORT = port;

		frame = new JFrame(title);
		frame.setSize(907,600);
		Point pointerLocation = MouseInfo.getPointerInfo().getLocation();
		frame.setLocation((int) pointerLocation.getLocation().getX() - 907/2, (int) pointerLocation.getLocation().getY() - 300);
		frame.setResizable(false);
		frame.setBackground(new Color(232,230,235));
		frame.setForeground(new Color(200,200,200));
		frame.setLayout(new BorderLayout());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		chatBox = new JTextArea();
		chatBox.setEditable(false);
		chatBox.setLineWrap(true);
		chatBox.setWrapStyleWord(true);
		chatBox.setFont(new Font("Jokewood", Font.PLAIN, 16));

		Color txar = new Color(224,224,224);
		userBox = new JTextArea();
		userBox.setEditable(false);
		userBox.setLineWrap(true);
		userBox.setBackground(txar);
		userBox.setFont(new Font("Times New Roman", Font.PLAIN, 16));

		JScrollPane scroller2 = new JScrollPane(userBox);
		frame.add(scroller2, "East");

		JScrollPane scroller = new JScrollPane(chatBox);
		frame.add(scroller, "Center");

		SmartScroller smartScroller = new SmartScroller(scroller);
		SmartScroller smartScroller2 = new SmartScroller(scroller2);

		p = new Panel();
		p.setLayout(new FlowLayout());
		p.setBackground(new Color(221,221,211));

		pnPanel0 = new JPanel();
		GridBagLayout gbPanel0 = new GridBagLayout();
		GridBagConstraints gbcPanel0 = new GridBagConstraints();
		pnPanel0.setLayout(gbPanel0);

		usernameEntry = new JTextField(20);
		usernameEntry.setToolTipText("Enter username here");
		usernameEntry.setFont(new Font("Times New Roman", Font.ITALIC, 12));
		p.add(usernameEntry);
		usernameEntry.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				setUsername();
			}
		});

		setNameButton = new JButton("Register Username");
		setNameButton.setEnabled(false);
		setNameButton.setToolTipText("Set the current user name (Cannot be changed until disconnected)");
		setNameButton.setBackground(new Color( 179,224,155));
		setNameButton.setForeground(new Color( 187,230,170));
		p.add(setNameButton);
		setNameButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				setUsername();
			}
		});

		inputBox = new JTextField(30);
		inputBox.setToolTipText("Enter messages here");
		p.add(inputBox);
		inputBox.setFont(new Font("Courier New", Font.ITALIC, 12));
		inputBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if(inputBox.getText().length() > 0)
				{
					sendMessage(inputBox.getText(), 0);
					inputBox.setText("");
				}
			}
		});

		sendButton = new JButton("Send");
		sendButton.setToolTipText("Send current message");
		sendButton.setForeground(new Color(164,215,247));
		sendButton.setBackground(new Color(193,241,255));
		sendButton.setEnabled(false);
		p.add(sendButton);
		sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if(inputBox.getText().length() > 0)
				{
					sendMessage(inputBox.getText(), 0);
					inputBox.setText("");
				}
			}
		});

		clearButton = new JButton("Clear");
		clearButton.setToolTipText("Clear the chat area");
		clearButton.setBackground(new Color(147,221,255));
		clearButton.setForeground(new Color(162,255,245));
		clearButton.setEnabled(false);
		p.add(clearButton);
		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				chatBox.setText("");
			}
		});

		btConnectbutton = new JButton("Connect");
		btConnectbutton.setToolTipText("Attempt connection");
		btConnectbutton.setActionCommand("");
		btConnectbutton.setBackground(new Color(96,241,96));
		btConnectbutton.setForeground(new Color(0,190,0));
		p.add(btConnectbutton);
		btConnectbutton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				btConnectbutton.setEnabled(false);
				connect();
				if(connectState)
				{
					btDisconnectButton.setEnabled(true);
					setNameButton.setEnabled(true);
					sendButton.setEnabled(false);
					usernameEntry.setEnabled(true);
					usernameEntry.requestFocus();
					chatBox.append("\n" + "[Connection Established]");
				}
				else
				{
					btConnectbutton.setEnabled(true);
					chatBox.append("\n" + "[Connection Failed]");
				}
			}
		});

		btDisconnectButton = new JButton("Disconnect");
		btDisconnectButton.setToolTipText("Disconnect from the server");
		btDisconnectButton.setEnabled(false);
		btDisconnectButton.setBackground(new Color(235,54,60));
		btDisconnectButton.setForeground(new Color(207,0,0));
		p.add(btDisconnectButton);
		btDisconnectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				btDisconnectButton.setEnabled(false);
				disconnect();
				if(connectState == false)
				{
					btConnectbutton.setEnabled(true);
					setNameButton.setEnabled(false);
					inputBox.setEnabled(false);
					usernameEntry.setEnabled(false);
					sendButton.setEnabled(false);
					userBox.setText("");
					chatBox.append("\n" + "[Disconnected]");
				}
				else
				{
					btDisconnectButton.setEnabled(true);
				}
			}
		});

		inputBox.setEnabled(false);
		usernameEntry.setEnabled(false);
		chatBox.addKeyListener(this);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e)
			{
				if (connectState)
					disconnect();
			}
		});
		frame.add(p, "South");
		frame.setVisible(true);
		frame.requestFocus();
	}

	public void setUsername()
	{
		setNameButton.setEnabled(false);
		usernameEntry.setEnabled(false);

		if(usernameEntry.getText().length() > 0)
		{
			userName = usernameEntry.getText();
			chatBox.append("\n[" + userName + " has signed into the channel]");
			usernameEntry.setText(userName);
			sendMessage(userName, 1);
			sendButton.setEnabled(true);
			clearButton.setEnabled(true);
			inputBox.setEnabled(true);
			inputBox.requestFocusInWindow();
			inputBox.requestFocus();
		}
		else
		{
			chatBox.append("\n" + "[Username must contain characters]");
			setNameButton.setEnabled(true);
			usernameEntry.setEnabled(true);
		}
	}

	private void connect()
	{
		connectState = client.openSocket(this.HOST, this.PORT);
	}

	private void disconnect()
	{
		boolean state = client.closeSocket();
		if(state)
			connectState = false;
	}

	private void sendMessage(String message, int code)
	{
		client.sendMessage(client.encodeJson(message, code));
	}

	@Override
	public void onMessage(String usernameEntry, String message)
	{
		chatBox.append('\n' + usernameEntry + ": " + message);
	}

	@Override
	public void updateConnected(String[] list)
	{
		userBox.setText("");
		for (String username : list)
		{
			if (username.length() >= 12)
			{
				username = username.substring(0, 11) + "...";
			}
			userBox.append(username + "\n");
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (colorShiftActive == false)
		{
			keyCodes.add(e.getKeyCode());
			if (keyCodes.size() == 4 && keyCodes.get(0) == 75 && keyCodes.get(1) == 69 && keyCodes.get(2) == 75 && keyCodes.get(3) == 59)
			{
				colorShiftActive = true;
				(new Thread() {
					public void run()
					{
						float hue = 0.9f;
						float saturation = 1.0f;
						float brightness = 0.8f;
						Color color = Color.getHSBColor(hue, saturation, brightness);
						try {
							AudioInputStream kek = AudioSystem.getAudioInputStream(this.getClass().getResource("kek.wav"));
							Clip clip = AudioSystem.getClip();
							clip.open(kek);
							clip.start();
						} catch (Exception e) {
							e.printStackTrace();
							System.exit(-1);
						}

						while (true)
						{
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {

							}
							if (hue > 0.99f)
								hue = 0.0f;
							else
								hue += 0.01f;
							color = Color.getHSBColor(hue, saturation, brightness);
							chatBox.setBackground(color);
							userBox.setBackground(color);
						}
					}
				}).start();
			}
			else if (keyCodes.size() == 4 || e.getKeyCode() == 67)
			{
				keyCodes.clear();
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}

	public static void main(String[] args)
	{
		ClientGUI client = new ClientGUI("Chat", "localhost", 9999);
	}
}
