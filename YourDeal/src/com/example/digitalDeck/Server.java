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
import java.util.concurrent.LinkedBlockingQueue;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.io.*;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.json.*;

import android.os.Looper;

public class Server implements NetworkingDelegate, StreamDelegate {
    private HashMap<RemotePlayer, Stream> streamsByPlayer;
    private ArrayList<Stream> pendingStreams;
    private ServerListener serverListener;
    private Thread serverThread;
    private Game game;
    private Looper serverLooper;
    
    private android.net.wifi.WifiManager.MulticastLock lock;
    private JmDNS jmdns;
    private ServiceInfo broadcastService;
    private HashMap<String, String> broadcastProperties;
    private boolean broadcasting;
	
    /**Server constructor
     * @param newGame the game that the server is hosting
     * initializes the outputs array that contains all remote
     * player connections (starting empty) and sets the game
     * object to the game that will be played
     */
    public Server(Game newGame) {
        streamsByPlayer = new HashMap<RemotePlayer, Stream>();
        pendingStreams = new ArrayList<Stream>();
        serverListener = new ServerListener(0);
        game = newGame;
    }
    
    public void start() {
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                serverLooper = Looper.myLooper();
                
                serverThread = new Thread(serverListener);
                serverThread.start();
                Server.this.createService();
                
                Looper.loop();
            }
        }).start();
    }
    
    public void stop() {
        if (broadcasting) {
            stopBroadcasting();
        }
        serverListener.stop();
    }
    
    /**createService
	 * @param props the dictionary of properties to be broadcast
	 * @throws IOException
	 * Starts a JmDNS service so that other users can detect the lobby
	 */
    public void createService() {
        try {
            jmdns = JmDNS.create();
            updateProperties();
            
            if (broadcastService == null) {
                broadcastService = ServiceInfo.create("_DigitalDeck._tcp.local.", 
                                                      game.getTitle(), 
                                                      serverListener.getPort(), 
                                                      0, 
                                                      0, 
                                                      broadcastProperties);
            }
            
            jmdns.registerService(broadcastService); // Broadcast!
            broadcasting = true;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**updateProperties
     * Regenerates the DNSText object and updates the broadcast if it already exists.
     */
    private void updateProperties() {
        System.out.println("updating properties");
        if (broadcastProperties == null) {
            broadcastProperties = new HashMap<String, String>();
        }
        
        broadcastProperties.put("gameType", game.getType());
        broadcastProperties.put("gameSize", Integer.toString(game.getGameSize()));
        broadcastProperties.put("playerCount", Integer.toString(game.getNumPlayers()));
        
        if (broadcastService != null) { // This is a hack
            broadcastService.setText(broadcastProperties);
        }
    }
    
    public void stopBroadcasting() {
        System.out.println("unregistering service " + broadcastService);
    	jmdns.unregisterService(broadcastService);
    	broadcasting = false;
    	//lock.release();
    }

    public void addedPlayer(Player aPlayer) {
        JSONObject updates = new JSONObject();
        try {
            updates.put("target", "game");
            updates.put("addedPlayer", aPlayer.get("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        if (broadcasting) {
            System.out.println("updating properties after adding player");
            updateProperties();
        }
        
        for (Player player : streamsByPlayer.keySet()) {
            Stream stream = streamsByPlayer.get(player);
            System.out.println("server is queuing write to " + player.get("name"));
            stream.queueWrite(updates);
        }
    }
    
    public void removedPlayer(Player aPlayer) {
        JSONObject updates = new JSONObject();
        try {
            updates.put("target", "game");
            updates.put("removedPlayer", aPlayer.get("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        if (broadcasting) {
            updateProperties();
        }
        
        for (Stream stream : streamsByPlayer.values()) {
            stream.queueWrite(updates);
        }
    }

    /**updateProperties
     * @param updates a dictionary of player object or remote player object keys to an update dictionary value
     * iterates through each player who needs to receive updates and forwards their information in the form of
     * a JSON dictionary
     */
    public void updatedPlayer(RemotePlayer player, JSONObject updates) {
    	try {
            updates.put("target", "player");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    	
    	streamsByPlayer.get(player).queueWrite(updates);
    }

    /**updateGame
     * @param updates
     * Sends an update of general game information to all players currently
     * connected
     */
    public void updatedGame(JSONObject updates) {
        try {
            updates.put("target", "game");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        for (Stream stream : streamsByPlayer.values()) {
            stream.queueWrite(updates);
        }
        JSONObject empty = new JSONObject();
        game.process(empty);
    }
    
    @Override
	public boolean isHostingGame() {
		return true;
	}
    
    @Override
    public Game getGame() {
    	return game;
    }
    
    public void streamReceivedData(Stream aStream, JSONObject data) {
        System.out.println("streamReceivedData: " + data);
        try {
            if (data.has("request")) {
                String request = (String)data.get("request");
                System.out.println("request get: " + request);
                
                if (request.equals("playerList")) {
                    System.out.println("getPlayers: " + game.getPlayers());
                    for (Player player : game.getPlayers()) {
                        JSONObject playerJSON = new JSONObject();
                        playerJSON.put("target", "game");
                        playerJSON.put("addPlayer", player.get("name"));
                        aStream.queueWrite(playerJSON);
                    }
                }
            }
            
            if (data.has("addPlayer")) {
                String name = (String)data.get("addPlayer");
                RemotePlayer newPlayer = new RemotePlayer(name, this);
                streamsByPlayer.put(newPlayer, aStream);
                pendingStreams.remove(aStream);
                game.addPlayer(newPlayer);
            }
            
            if (data.has("removePlayer")) {
                Player player = game.getPlayerNamed(data.getString("removePlayer"));
                game.removePlayer(player);
            }
            
            if (data.has("action")) {
                game.process(data);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        
        if (YourDealApplication.currentUI != null) {
            YourDealApplication.currentUI.updateUI(); // Will run on the correct Thread.
        }
    }
    
    @Override
    public void lobbyIsClosing() {
        stop();
    }
    
    /******************************************************************************************************************/
    
    private class ServerListener implements Runnable {
    	/**ServerListener
    	 * The thread that is created when the server starts listening
    	 * for clients to be added. When  the listen method attached
    	 * to the ServerSocket is resolved it forwards the information
    	 * to a communication thread
    	 */
    	int port;
    	ServerSocket serverSocket;
    	
    	/**ServerThread constructor
    	 * @param serverPort the port to start a connection over
    	 * Initializes the port variable to be used by the server
    	 */
    	public ServerListener(int serverPort) {
    		try {
                serverSocket = new ServerSocket(serverPort);
                port = serverSocket.getLocalPort();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
    	}

        public int getPort() {
            return port;
        }

        /**run
    	 * creates a ServerSocket object and then listens for the socket
    	 * to be initialized, when the socket is initialized under the listen()
    	 * method it turns on the socket's keepAlive flag and starts a new
    	 * communication Thread and changes openInConnection to the next available connection
    	 */
		public void run() {
			try {
			    while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
					Socket newSocket = serverSocket.accept();
					
					newSocket.setKeepAlive(true);
					System.out.println("Connection established!");
					
					Stream newStream = new Stream(newSocket, Server.this, serverLooper);
					pendingStreams.add(newStream);
				}
			}
			catch (IOException e) {
			    System.out.println("closed serversocket");
			}
		}
		
		public void stop() {
		    try {
                serverSocket.close(); // Kills the blocked accept()
                for (Stream stream : streamsByPlayer.values()) {
                    stream.stop();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
		}
	}
}
