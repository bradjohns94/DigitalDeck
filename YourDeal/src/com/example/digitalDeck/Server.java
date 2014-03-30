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

public class Server implements NetworkingDelegate, StreamDelegate {
    private HashMap<RemotePlayer, Stream> streamsByPlayer;
    private ServerListener serverListener;
    private Thread serverThread;
    private Game game;
    
    private android.net.wifi.WifiManager.MulticastLock lock;
    private JmDNS jmdns;
    private ServiceInfo broadcastService;
	
    /**Server constructor
     * @param newGame the game that the server is hosting
     * initializes the outputs array that contains all remote
     * player connections (starting empty) and sets the game
     * object to the game that will be played
     */
    public Server(Game newGame) {
        streamsByPlayer = new HashMap<RemotePlayer, Stream>();
        serverListener = new ServerListener(0);
        game = newGame;
    }
    
    public void start() {
    	serverThread = new Thread(serverListener);
    	serverThread.start();
    	this.createService();
    }
    
    public void stop() {
        serverListener.stop();
    }
    
    /**createService
	 * @param props the dictionary of properties to be broadcast
	 * @throws IOException
	 * Starts a JmDNS service so that other users can detect the lobby
	 */
    public void createService() {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put("gameType", game.getType());
        props.put("gameSize", Integer.toString(game.getGameSize()));
        props.put("playerCount", Integer.toString(game.getNumPlayers()));
        props.put("hostUser", game.getHost());
        
        if (lock == null) {
        	android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager)YourDealApplication.getInstance().getSystemService(android.content.Context.WIFI_SERVICE);
        	lock = wifi.createMulticastLock("DefinitelyCats");
        	lock.setReferenceCounted(true);
        }
        lock.acquire(); // Lock multicast open
        
        if (broadcastService == null) {
        	broadcastService = ServiceInfo.create("_DigitalDeck._tcp.local.", game.getTitle(), serverListener.getPort(), 0, 0, props);
        }
        
        try {
            jmdns.registerService(broadcastService); // Broadcast!
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void stopBroadcasting() {
    	jmdns.unregisterService(broadcastService);
    	lock.release();
    }

    public void addedPlayer(Player aPlayer) {
        JSONObject updates = new JSONObject();
        try {
            updates.put("target", "game");
            updates.put("addedPlayer", aPlayer.get("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        for (Stream stream : streamsByPlayer.values()) {
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
        try {
            if (data.has("addPlayer")) {
                String name = data.get("addPlayer").toString();
                for (RemotePlayer player : streamsByPlayer.keySet()) {
                    // TODO: Add playersByStream
                    if (streamsByPlayer.get(player).equals(aStream)) {
                        player.put("name", name);
                        game.addPlayer(player);
                        break;
                    }
                }
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
			while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
				try {
					Socket newSocket = serverSocket.accept();
					
					newSocket.setKeepAlive(true);
					System.out.println("Connection established!");
					
					streamsByPlayer.put(new RemotePlayer(null, Server.this), new Stream(newSocket));

				} catch (IOException e) {
				    System.out.println("closed serversocket");
				}
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
    
    private class ClientStream {
        ClientReader reader;
        ClientWriter writer;
        Socket socket;
        
        public ClientStream(Socket aSocket) {
            socket = aSocket;
            reader = new ClientReader(aSocket);
            writer = new ClientWriter(aSocket);
            new Thread(reader).start();
            new Thread(writer).start();
        }
        
        public void queueWrite(JSONObject data) {
            writer.queueWrite(data);
        }
        
        public ClientReader getReader() {
            return reader;
        }
        
        public ClientWriter getWriter() {
            return writer;
        }
        
        public void stop() {
            try {
                writer.stop();
                reader.stop();
                if (!socket.isClosed()) {
                    socket.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class ClientReader implements Runnable {
		/**CommuncationThread
		 * Reads from input over the specified socket and if it receives
		 * new information sends it to the UpdateUIThread
		 */
		private Socket socket;
		private DataInputStream input;

		/**CommunicationThread constructor
		 * @param clientSocket the socket the communication takes place over
		 * Initializes the variables to be used in the run() method of the thread
		 */
		public ClientReader(Socket clientSocket) {
			socket = clientSocket;
			try {
				input = new DataInputStream(this.socket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**run
		 * Continuously reads from the input buffer of the socket and if the input changes
		 * forwards the information to the updateUIThread
		 */
		public void run() {
			while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
				try {
					ByteBuffer buf = ByteBuffer.allocate(9);
					input.readFully(buf.array(), 0, 9);
					
					int magic = buf.getInt(0); 
					if (magic != 0x444c4447) {
						System.out.println("Magic: " + magic);
						continue;
					}
					
					byte type = buf.get(4); //get the packet type
					if (type != 0x0f) {
						System.out.println("Non-JSON packet, skipping reading payload");
						continue;
					}
					
					int payloadSize = buf.getInt(5);
					ByteBuffer payloadBuf = ByteBuffer.allocate(payloadSize);
					
					// Read the payload
					input.readFully(payloadBuf.array(), 0, payloadSize);
					String read = new String(payloadBuf.array(), Charset.forName("UTF-8"));
					System.out.println(read);
/*					
					JSONObject payload = new JSONObject(read);

					if (payload.has("addPlayer")) {
						String name = payload.get("addPlayer").toString();
						for (RemotePlayer player : streamsByPlayer.keySet()) {
						    if (streamsByPlayer.get(player).getReader().equals(this)) {
						        player.put("name", name);
						        game.addPlayer(player);
						        break;
						    }
						}
					}
					
					if (payload.has("removePlayer")) {
						// TODO
					}
					
					if (payload.has("action")) {
					    game.process(payload);
					}

				} catch (JSONException e) {
					e.printStackTrace();
*/				} catch (IOException e) {
				    System.out.println("reader socket closed");
				}
			}
		}
		
		public void stop() {
		    try {
                socket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
		}
	}
    
    private static class ClientWriter implements Runnable {
    	private Socket socket;
    	private DataOutputStream output;
    	private LinkedBlockingQueue<JSONObject> queue;
    	private JSONObject DOTA;
    	
    	public ClientWriter(Socket aSocket) {
    		socket = aSocket;
    		queue = new LinkedBlockingQueue<JSONObject>();
    		
    		try {
    			output = new DataOutputStream(socket.getOutputStream());
				DOTA = new JSONObject("[\"DOTA\"]");
			} 
    		catch (JSONException e) {
				System.out.println("DOTA is wrong");
				e.printStackTrace();
			} 
    		catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	
		@Override
		public void run() {
			try {
				JSONObject payload = queue.take(); // Blocks until there is something to take
				if (payload.equals(DOTA)) return;
				
				String payloadString = payload.toString();
				
				// Build the header:
				ByteBuffer buf = ByteBuffer.allocate(9);
				buf.putInt(0x444c4447);
				buf.put((byte)0x0f);
				buf.putInt(payloadString.length());
				
				// Now write them to the socket in order:
				output.write(buf.array(), 0, 9);
				output.writeUTF(payloadString);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				System.out.println("writer socket closed");
			}
		}
		
		public void queueWrite(JSONObject payload) {
			try {
				queue.put(payload);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		public void stop() {
		    try {
                queue.put(DOTA);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
		}
    }
}
