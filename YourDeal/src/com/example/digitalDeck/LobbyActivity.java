package com.example.digitalDeck;

import com.example.yourdeal.R;

import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import android.view.ViewGroup.LayoutParams;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import org.json.JSONException;
import org.json.JSONObject;

import javax.jmdns.*;


/**LobbyActivty
 * @author Bradley Johns
 * Joins the user to the list of players in the game and starts
 * the server or client depending on whether or not the user is 
 * hosting the game or joining. Also initiates all socket connections
 * and initiates the game variable. Also does everything else. This
 * class is magic. I love it no matter how broken it is. This documentation
 * is starting to becoming rambling, you have my fullest apologies possible
 * employers who may be reading this.
 */
public class LobbyActivity extends Activity {

    private ArrayList<String> gamePlayers;
    private boolean isHost;
    android.net.wifi.WifiManager.MulticastLock lock;
    JmDNS jmdns;
    ArrayList<Socket> outputs;
    ArrayList<ServerSocket> inputs;
    Handler updateConversationHandler;
    String gameTitle;
    Thread serverThread = null;
    ServiceInfo info;
    String hostName;
    EuchreGame game;
    public static Server server;

    /**onCreate
     * @param savedInstanceState used for super.onCreate()
     * Initializes the screen as well as initializes the client
     * or server depending on whether or not the given user hosted
     * the lobby as well as initializes basic variables
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lobby);
		
        gamePlayers = new ArrayList<String>();
        outputs = new ArrayList<Socket>(); //This should change when adding more games
        inputs = new ArrayList<ServerSocket>(); //Also this
        updateConversationHandler = new Handler();

        gameTitle = getIntent().getStringExtra("title");
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
            game = new EuchreGame(hostName, title);
            server = new Server(game);
            int numPlayers = 1;
            //Gather the correct list of properties and broadcast over zeroconf
            final HashMap<String, String> props = new HashMap<String, String>();
            props.put("gameType", gameType);
            props.put("playerCount", Integer.toString(numPlayers));
            props.put("hostUser", hostName);
            new Thread(){ public void run() { try{
                jmdns = JmDNS.create("GlaDOS");
                createService(props);
            } catch (IOException e) {
                e.printStackTrace();
            }}}.start();
            //Listen for incoming clients and send them the clientlist
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
        	while (outputs.size() == 0) {
        		for (int i = 0; i < ips.length; i++) {
        			System.out.println("Trying to connect to ip " + ips[i]);
        			new Thread(new ClientThread(hostName, ips[i].substring(1, ips[i].length()), Integer.parseInt(port))).start();
        		}
        	}
        	//Send the player information to the server over JSON
        	Hashtable<String, String> table = new Hashtable<String, String>();
        	table.put("target", "game");
        	table.put("addPlayer", hostName);
        	JSONObject JSON = new JSONObject(table);
        	sendMessage(outputs.get(0), JSON.toString());
        	String read = "";
        	new Thread(new ClientListenThread()).start();
        }
        
        drawPlayers();

		// Show the Up button in the action bar.
		setupActionBar();
	}

	/**createService
	 * @param props the dictionary of properties to be broadcast
	 * @throws IOException
	 * starts a JmDNS service so that other users can detect the lobby
	 */
    public void createService(HashMap<String, String> props) throws IOException {
    	gameTitle = getIntent().getStringExtra("title");
    	System.out.println("Advertising service with name " + gameTitle);
        android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager)getSystemService(android.content.Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("DefinitelyCats");
        lock.setReferenceCounted(true);
        lock.acquire();
        info = ServiceInfo.create("_DigitalDeck._tcp.local.", gameTitle, 36241, 0, 0, props);
        jmdns.registerService(info);
    }
    
    /**onDestroy
     * Stops JmDNS service when window is closed
     */
    @Override
	protected void onDestroy() {
    	super.onDestroy();
    	//TODO shut down JmDNS
    }
    
    /**onStop
     * destroys JmDNS as well as closes sockets
     * when the application is stopped
     */
    @Override
	protected void onStop() {
    	if (jmdns != null) {
    	}
    	lock.release();
    	super.onStop();
    	//TODO shut down JmDNS
    	//TODO close sockets
    }

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	/**onCreateOptionsMenu
	 * @param menu the menu to be created
	 * starts and inflates the action bar
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.lobby, menu);
		return true;
	}

	/**onOptionsItemSelected
	 * @param item the item that was selected
	 * sends the user to the specified page for the
	 * item that was selected in the app
	 */
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

	/**drawPlayers
	 * iterates through the gamePlayers string arraylist
	 * and draws the display name of each user currently
	 * in the lobby to the screen
	 */
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

    /**startGame
     * @param view the view that was activated to start the game
     * Begins the game itself if the game has the required number
     * of players
     */
    public void startGame(View view) {
        if (gamePlayers.size() != 4) return;
        YourDealApplication.delegate = server;
        YourDealApplication.game = game;
        YourDealApplication.local = game.players[0];
        Intent toUI = new Intent(this, EuchreUIActivity.class);
        //TODO send out a start game message
        startActivity(toUI);
    }
    
    /**sendMessage
     * @param output the socket to send the string over
     * @param message the string to be sent over the socket
     * writes the given string to the given socket
     */
    public void sendMessage(Socket output, String message) {
    	try { //Send display name to host
			/*PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(output.getOutputStream())),
					true);*/
			DataOutputStream out = new DataOutputStream(output.getOutputStream());
			out.writeInt(0x444C4447); //Write to charlie
			out.write(0x0F);
			out.writeInt(message.getBytes().length);
			System.out.println("Sending JSON: " + message);
			out.writeBytes(message);
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
    	/**ServerThread
    	 * The thread that is created when the server starts listening
    	 * for clients to be added. When  the listen method attached
    	 * to the ServerSocket is resolved it forwards the information
    	 * to a communication thread
    	 */
    	int port;
    	
    	/**ServerThread constructor
    	 * @param serverPort the port to start a connection over
    	 * Initializes the port variable to be used by the server
    	 */
    	public ServerThread(int serverPort) {
    		port = serverPort;
    	}

    	/**run
    	 * creates a ServerSocket object and then listens for the socket
    	 * to be initialized, when the socket is initialized under the listen()
    	 * method it turns on the socket's keepAlive flag and starts a new
    	 * communication Thread and changes openInConnection to the next available connection
    	 */
		public void run() {
			Socket socket = null;
			try {
				if (inputs.size() < 4) inputs.add(new ServerSocket(port));
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (!Thread.currentThread().isInterrupted()) {
				try {
					if (inputs.size() < 4) socket = inputs.get(inputs.size() - 1).accept();
					
					if (socket != null) {
						socket.setKeepAlive(true);
						outputs.add(socket);
						System.out.println("Connection established!");
					}

					CommunicationThread commThread = new CommunicationThread(socket, inputs.get(inputs.size() - 1));
					new Thread(commThread).start();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//Scan for the next open connection and set openInConnection to it
			/*for (int i = 0; i < inputs.length; i++) {
				if (inputs[i] == null) {
					openInConnection = i;
					break;
				} else if (i == inputs.length - 1) {
					//openInConnection = -1; //No more open connection slots
				}
			}*/
		}
	}

	class CommunicationThread implements Runnable {
		/**CommuncationThread
		 * Reads from input over the specified socket and if it recieves
		 * new information sends it to the UpdateUIThread
		 */
		private Socket clientSocket;
		private ServerSocket serverSocket;
		private DataInputStream input;

		/**CommunicationThread constructor
		 * @param clientSocket the socket the communication takes place over
		 * Initializes the variables to be used in the run() method of the thread
		 */
		public CommunicationThread(Socket clientSocket, ServerSocket server) {
			this.clientSocket = clientSocket;
			serverSocket = server;
			try {
				this.input = new DataInputStream(this.clientSocket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**run
		 * Continuously reads from the input buffer of the socket and if the input changes
		 * forwards the information to the updateUIThread
		 */
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					ByteBuffer buf = ByteBuffer.allocate(9);
					input.readFully(buf.array(), 0, 9);
					int charlie = buf.getInt(0); //Charlie is my magic buddy
					if (charlie != 0x444C4447) { //Check "DGLD" notifier
						System.out.println("What was that, charlie? " + charlie);
						continue;
					}
					byte type = buf.get(4); //get the packet type
					if (type != 0x0F) {
						System.out.println("Charlie, you're silly");
						System.out.println("Charlie said " + type);
						continue;
					}
					int size = buf.getInt(5);
					ByteBuffer payloadBuf = ByteBuffer.allocate(size);
					input.readFully(payloadBuf.array(), 0, size);
					String read = new String(payloadBuf.array());
					System.out.println(read);
					
					String name = null;
					try {
						JSONObject JSON = new JSONObject(read);
						name = JSON.get("addPlayer").toString();
					} catch (JSONException e) {
						e.printStackTrace();
					}
					server.addPlayer(name, clientSocket, serverSocket);
					updateConversationHandler.post(new updateUIThread(read, clientSocket));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class updateUIThread implements Runnable {
		/**updateUIThread
		 * called on a change in input, specifies a string and a
		 * socket that the string came from. If the string containing
		 * the information must be removed from the players list the
		 * thread removes it, otherwise it adds the player to the list
		 * and updates the UI of the server and clients accordingly
		 */
		private String msg;
		private Socket clientSocket;
		
		/**updateUIThread constructor
		 * @param str the string representing the packet received
		 * @param socket the socket the packet was received from
		 * initializes the variables to be used by the run() method
		 */
		public updateUIThread(String str, Socket socket) {
			this.msg = str;
			clientSocket = socket;
		}

		/**run
		 * adds or removes the player from the lobby accordingly
		 * If the user is the server it sends out the change in
		 * lobby information to the rest of the users
		 */
		@Override
		public void run() {
			String newPlayer = null;
			String toRemove = null;
			try {
				JSONObject recieved = new JSONObject(msg);
				newPlayer = recieved.get("addPlayer").toString();
				toRemove = recieved.get("removePlayer").toString();
			} catch(JSONException e) {
				e.printStackTrace();
			}
			if (newPlayer != null) { //Add the player to the list
				gamePlayers.add(newPlayer);
				Hashtable<String, String> newPHash = new Hashtable<String, String>();
				newPHash.put("target", "game");
				newPHash.put("addPlayer", newPlayer);
				JSONObject toSend = new JSONObject(newPHash);
				if (isHost) { //In the case of a new client send it existing information
					for (Socket s : outputs) {
						if (s.equals(clientSocket)) continue;
						sendMessage(s, toSend.toString());
					}
					for (int i = 0; i < gamePlayers.size(); i++) {
						Hashtable<String, String> table = new Hashtable<String, String>();
						table.put("target", "game");
						table.put("addPlayer", gamePlayers.get(i));
						final JSONObject JSON = new JSONObject(table);
						try { //Send display names to client
							runOnUiThread(new Runnable() {
				                public void run() {
				                	sendMessage(clientSocket, JSON.toString());					                }
				            });
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				
			} 
			if (toRemove != null) {
				gamePlayers.remove(toRemove);
			}
			runOnUiThread(new Runnable() { //Update the UI on the main thread
                public void run() {
                   drawPlayers();
                }
	        });
			if (isHost) {
				//Send out update to clients
				Hashtable<String, Object> toSend = new Hashtable<String, Object>();
				toSend.put("target", "game");
				JSONObject JSON = new JSONObject(toSend);
				if (newPlayer != null) {
					System.out.println("added player: " + newPlayer);
				}
				for (int i = 0; i < outputs.size(); i++) {
					Socket s = outputs.get(i);
					System.out.println("Sending message: " + JSON.toString());
					sendMessage(s, JSON.toString());
				}
			}
		}
	}
	
	class ClientThread implements Runnable {
		/**ClientThread
		 * The thread for the client to use to send information to the server
		 */
		String serverIP;
		int serverPort;
		String userName;
		
		/**ClientThread constructor
		 * @param name the displayName of the client
		 * @param ip the ip of the server the client wishes to connect to
		 * @param port the port that the client would like to connect over
		 * instantiates variables to be used by the run method
		 */
		public ClientThread(String name, String ip, int port) {
			serverIP = ip;
			serverPort = port;
			userName = name;
		}
		
		/**run
		 * Attempts to connect to the server over the specified address and port
		 */
		@Override
		public void run() {

			/*try {
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
			}*/
			if (!isHost) {
				try {
					InetAddress serverAddr = InetAddress.getByName(serverIP);
					Socket out = new Socket(serverAddr, serverPort);
					if (out != null) outputs.add(out);
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
		/**ClientListenThread
		 * The equivalent to a communication thread on the client end
		 * Listens for changes in the input from the server and runs
		 * the updateUItThread accordingly
		 */
		private DataInputStream input;

		/**ClientListenThread constructor
		 * initializes the input bufferedReader
		 */
		public ClientListenThread() {
			try {
				this.input = new DataInputStream(outputs.get(0).getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**run
		 * Continuously scans for changes in input from the server and adjusts the UI
		 * accordingly
		 */
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					ByteBuffer buf = ByteBuffer.allocate(9);
					input.readFully(buf.array(), 0, 9);
					int charlie = buf.getInt(0); //Charlie is my magic buddy
					if (charlie != 0x444C4447) { //Check "DGLD" notifier
						System.out.println("What was that, charlie? " + charlie);
						continue;
					}
					byte type = buf.get(4); //get the packet type
					if (type != 0x0F) {
						System.out.println("Charlie, you're silly");
						System.out.println("Charlie said " + type);
						continue;
					}
					int size = buf.getInt(5);
					ByteBuffer payloadBuf = ByteBuffer.allocate(size);
					input.readFully(payloadBuf.array(), 0, size);
					String read = new String(payloadBuf.array());
					System.out.println(read);
					updateConversationHandler.post(new updateUIThread(read, outputs.get(0)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
