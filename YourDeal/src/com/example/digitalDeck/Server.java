package com.example.digitalDeck;

/**Server
 * @author Bradley Johns
 * The server-end portion of the app. After recieving
 * socket connections from the lobby activity class, 
 * the server stores the connection in order to use them
 * throughout the duration of the game in order to update
 * other devices on the status of the game
 */

import java.util.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.io.*;

import org.json.*;

import android.app.Application;

public class Server implements Delegate{

    private Hashtable<RemotePlayer, Socket> sockets;
    private Hashtable<RemotePlayer, ServerSocket> serverSockets;
    private Game game;
    private Socket listening;
    private EuchreUIActivity UI;
	
    /**Server constructor
     * @param newGame the game that the server is hosting
     * initializes the outputs array that contains all remote
     * player connections (starting empty) and sets the game
     * object to the game that will be played
     */
    public Server(Game newGame) {
        sockets = new Hashtable<RemotePlayer, Socket>();
        serverSockets = new Hashtable<RemotePlayer, ServerSocket>();
        game = newGame;
    }
    
    public void start(EuchreUIActivity euchre) {
    	UI = euchre;
    	Player[] players = game.getPlayers();
    	for (int i = 0; i < players.length; i++) {
    		if (players[i] == null) break; //This shouldn't happen, but just in case
    		if (players[i] instanceof RemotePlayer) {
    			RemotePlayer remote = (RemotePlayer)players[i];
    			//new Thread(new CommunicationThread(sockets.get(remote))).start();
    		}
    	}
    	JSONObject empty = new JSONObject();
    	game.setUI(euchre);
    	game.process(empty);
    }

    /**addPlayer
     * @param name
     * @param connection
     * @return whether the add was successful
     * Assuming there is room left in the game, the method adds
     * them to the dictionary of player/socket values and returns
     * whether or not they were added successfully.
     */
    public boolean addPlayer(String name, Socket sock, ServerSocket serv) {
        if (game.isFull()) return false;
        RemotePlayer newPlayer = new RemotePlayer(name);
        newPlayer.setDelegate(this);
        sockets.put(newPlayer, sock);
        serverSockets.put(newPlayer, serv);
        game.addPlayer(name);
        return true;
    }

    /**updateProperties
     * @param updates a dictionary of player object or remote player object keys to an update dictionary value
     * iterates through each player who needs to recieve updates and forwards their information in the form of
     * a JSON dictionary
     */
    public void updateProperties(Hashtable<Object, Hashtable<String, Object>> updates) {
        Enumeration<Object> keys = updates.keys();
        System.out.println("Updating properties");
        while (keys.hasMoreElements()) {
            Object obj = keys.nextElement();
            Hashtable<String, Object> table = updates.get(obj);
            JSONObject JSON = new JSONObject(table);

            if (obj instanceof RemotePlayer) { //If the key is a remoteplayer forward it to the correct client
                /*try {
                    JSON.put("target", "player");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RemotePlayer player = (RemotePlayer)obj;
                Socket connection = sockets.get(player);
                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(connection.getOutputStream())),
                            true);
                    out.println(JSON.toString());
                } catch(IOException e) {
                    e.printStackTrace();
                }*/
            } else if (obj instanceof Player) { //If the key is for the local player call Player.updateProperties with the JSON
                Player player = (Player)obj;
                System.out.println("updating local player properties");
                player.updateProperties(table);
            }
        }
    }

    /**updateGame
     * @param updates
     * Sends an update of general game information to all players currently
     * connected
     */
    public void updateGame(Hashtable<String, Object> updates) {
        JSONObject JSON = new JSONObject(updates);
        try {
            JSON.put("target", "game");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Enumeration<RemotePlayer> keys = sockets.keys();
        while (keys.hasMoreElements()) {
            Socket connection = sockets.get(keys.nextElement());
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(connection.getOutputStream())),
                        true);
                out.println(JSON.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        UI.updateUI();
        JSONObject empty = new JSONObject();
        game.process(empty);
    }
    
    public void forwardInfo(JSONObject info) {
    	game.process(info);
    }
    
    public void listen(Socket toListen) {
    	if (listening == null) listening = toListen;
    }
    
    class CommunicationThread implements Runnable {
		private Socket clientSocket;
		private DataInputStream input;

		/**CommunicationThread constructor
		 * @param clientSocket the socket the communication takes place over
		 * Initializes the variables to be used in the run() method of the thread
		 */
		public CommunicationThread(Socket clientSocket) {
			this.clientSocket = clientSocket;
			try {
				this.input = new DataInputStream(this.clientSocket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

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
					JSONObject JSON = new JSONObject(read);
					forwardInfo(JSON);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}

	}
}
