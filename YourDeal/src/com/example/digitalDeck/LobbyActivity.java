package com.example.digitalDeck;

import com.example.yourdeal.R;

import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.support.v4.app.NavUtils;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import android.view.ViewGroup.LayoutParams;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jmdns.*;

public class LobbyActivity extends Activity {

    private ArrayList<String> gamePlayers;
    private boolean isHost;
    android.net.wifi.WifiManager.MulticastLock lock;
    JmDNS jmdns;
    Socket[] outputs;
    ServerSocket[] inputs;
    Handler updateConversationHandler;
    String gameTitle;
    Thread serverThread = null;
    ServiceInfo info;
    int openInConnection;
    int openOutConnection;
    String hostName;
    Server server;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lobby);
		
		//TODO start connection to server/client depending

        gamePlayers = new ArrayList<String>();
        outputs = new Socket[4]; //This should change when adding more games
        inputs = new ServerSocket[4]; //Also this
        updateConversationHandler = new Handler();

        gameTitle = getIntent().getStringExtra("gameTitle");
        String caller = getIntent().getStringExtra("caller");
        isHost = false;
        if (caller.equals("CreateGameActivity")) isHost = true;
        
        //Get the display name of the user
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        hostName = prefs.getString("display_name", "Unknown");

        if (isHost) {
        	gamePlayers.add(hostName);
            drawPlayers();
            String gameType = getIntent().getStringExtra("gameType");
            String title = getIntent().getStringExtra("title");
            EuchreGame game = new EuchreGame(hostName, title);
            server = new Server(game);
            int numPlayers = 0;
            //Gather the correct list of properties and broadcast over zeroconf
            final HashMap<String, String> props = new HashMap<String, String>();
            props.put("gameType", gameType);
            props.put("playerCount", Integer.toString(numPlayers));
            props.put("gameTitle", title);
            new Thread(){ public void run() { try{
                jmdns = JmDNS.create();
                createService(props);
            } catch (IOException e) {
                e.printStackTrace();
            }}}.start();
            //Listen for incoming clients and send them the clientlist
            openInConnection = 0;
            openOutConnection = 0;
            while (info == null) {} //This is horrible. I'll fix it later
            int serverPort = info.getPort();
            System.out.println("Starting server thread");
            serverThread = new Thread(new ServerThread(serverPort));
            serverThread.start();
            //Make the start button visible to the host
            Button start = (Button)findViewById(R.id.start);
            start.setVisibility(View.VISIBLE);
        } else {
        	//Start a connection to the server
        	String[] ips = getIntent().getStringArrayExtra("ips");
        	String port = getIntent().getStringExtra("port");
        	System.out.println("Trying to connect");
        	while (outputs[0] == null) {
        		for (int i = 0; i < ips.length; i++) {
        			System.out.println("Trying to connect to ip " + ips[i]);
        			new Thread(new ClientThread(hostName, ips[i].substring(1, ips[i].length()), Integer.parseInt(port))).start();
        		}
        	}
        	sendMessage(outputs[0], hostName);
        	String read = "";
        	new Thread(new ClientListenThread()).start();
        }
        
        drawPlayers();

		// Show the Up button in the action bar.
		setupActionBar();
	}

    public void createService(HashMap<String, String> props) throws IOException {
        android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager)getSystemService(android.content.Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("DefinitelyCats");
        lock.setReferenceCounted(true);
        lock.acquire();
        info = ServiceInfo.create("_DigitalDeck._tcp.local.", props.get("gameTitle"), 36241, 0, 0, props);
        jmdns.registerService(info);
    }
    
    @Override
	protected void onDestroy() {
    	super.onDestroy();
    }
    
    @Override
	protected void onStop() {
    	if (jmdns != null) {
    	}
    	lock.release();
    	super.onStop();
    }

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.lobby, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
        case R.id.action_settings:
            Intent toSettings = new Intent(this, SettingsActivity.class);
            startActivity(toSettings);
            return true;
		}
		return super.onOptionsItemSelected(item);
	}

    public void drawPlayers() {
        TableLayout table = (TableLayout)findViewById(R.id.currentPlayers);
        table.removeAllViews(); //Clear all previous views upon refresh
        LayoutParams tableParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

        //Create and add new rows with player info
        for (int i = 0; i < gamePlayers.size(); i++) {
            if (gamePlayers.get(i) == null) break; //Break when lobby ends

            //Draw the specified player
            TableRow row = new TableRow(this);
            row.setLayoutParams(tableParams);
            TextView name = new TextView(this);
            name.setText(gamePlayers.get(i));
            name.setTextAppearance(this, android.R.style.TextAppearance_Large);
            name.setPadding(0,0,0,10);
            row.addView(name);
            row.setPadding(0,20,0,0);
            table.addView(row);

            //Draw a line beneath the text
            View line = new View(this);
            line.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, 1));
            line.setBackgroundColor(Color.parseColor("#808080"));
        }
        setTitle(gameTitle);
    }

    public void startGame(View view) {
        for (int i = 0; i < gamePlayers.size(); i++) {
            if (gamePlayers.get(i) == null) return;
        }
        //TODO start the game
    }
    
    public void sendMessage(Socket output, String message) {
    	try { //Send display name to host
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(output.getOutputStream())),
					true);
			out.println(message);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

/*==============================================THREAD LINE! DO NOT CROSS=================================================================================*/
    /*Threads meant for socket communication
     * Special thanks to javacodegeeks*/
    class ServerThread implements Runnable {
    	
    	int port;
    	
    	public ServerThread(int serverPort) {
    		port = serverPort;
    	}

		public void run() {
			Socket socket = null;
			int index = 0;
			if (isHost) index = openInConnection;
			try {
				inputs[index] = new ServerSocket(port);
				System.out.println("Made ServerSocket on port " + port);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Listening for connections on port " + port);
			while (!Thread.currentThread().isInterrupted()) {

				try {

					socket = inputs[index].accept();
					
					if (socket != null) {
						socket.setKeepAlive(true);
						System.out.println("Got a connection");
					}

					CommunicationThread commThread = new CommunicationThread(socket);
					new Thread(commThread).start();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (isHost) { //Scan for the next open connection
				for (int i = 0; i < inputs.length; i++) {
					if (inputs[i] == null) {
						openInConnection = i;
						break;
					}
				}
			}
		}
	}

	class CommunicationThread implements Runnable {

		private Socket clientSocket;

		private BufferedReader input;

		public CommunicationThread(Socket clientSocket) {

			this.clientSocket = clientSocket;

			try {

				this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {

			while (!Thread.currentThread().isInterrupted()) {

				try {

					String read = input.readLine();
					if (read != null) System.out.println("Recieved message " + read);

					updateConversationHandler.post(new updateUIThread(read, clientSocket));

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	class updateUIThread implements Runnable {
		private String msg;
		private Socket clientSocket;
		
		public updateUIThread(String str, Socket socket) {
			this.msg = str;
			clientSocket = socket;
		}

		@Override
		public void run() {

			if (gamePlayers.contains(msg)) {
				gamePlayers.remove(msg);
			} else {
				gamePlayers.add(msg);
				if (isHost) {
					String ip = clientSocket.getInetAddress().toString();
					int port = clientSocket.getPort();
					int open = openOutConnection;
					for (int i = 0; i < gamePlayers.size(); i++) {
						final String player = gamePlayers.get(i);
						try { //Send display names to client
							runOnUiThread(new Runnable() {
				                public void run() {
				                	sendMessage(clientSocket, player);					                }
				            });
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				
			}
			runOnUiThread(new Runnable() {
                public void run() {
                   drawPlayers();
                }
	        });
			if (isHost) {
				//Send out update to clients
				for (int i = 0; i < outputs.length; i++) {
					try {
						if (outputs[i] != null) {
							PrintWriter out = new PrintWriter(new BufferedWriter(
									new OutputStreamWriter(outputs[i].getOutputStream())),
									true);
							out.println(msg);
						}
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	class ClientThread implements Runnable {
		
		String serverIP;
		int serverPort;
		String userName;
		
		public ClientThread(String name, String ip, int port) {
			serverIP = ip;
			serverPort = port;
			userName = name;
		}
		@Override
		public void run() {

			try {

				if (isHost) {
					InetAddress serverAddr = InetAddress.getByName(serverIP);
					//What to do when host connects output to client
					Socket out = new Socket(serverAddr, serverPort);
					if (out != null) {
						outputs[openOutConnection] = out;
						//Change openOutConnection to the next open slot
						for (int i = 0; i < outputs.length; i++) {
							if (outputs[i] == null) {
								openOutConnection = i;
								break;
							}
						}
					}
				}
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (!isHost) {
				try {
					InetAddress serverAddr = InetAddress.getByName(serverIP);
					Socket out = new Socket(serverAddr, serverPort);
					if (out != null) outputs[0] = out;
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}

	}
	
	class ClientListenThread implements Runnable {
		
		private BufferedReader input;

		public ClientListenThread() {
			try {
				input = new BufferedReader(new InputStreamReader(outputs[0].getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					String read = input.readLine();
					if (read != null) System.out.println("Recieved message " + read);
					updateConversationHandler.post(new updateUIThread(read, outputs[0]));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
