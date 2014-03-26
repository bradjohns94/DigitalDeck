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
import java.io.*;

public class Server {

    private Hashtable<RemotePlayer, Socket> outputs;
    private Hashtable<RemotePlayer, Socket> inputs;
    private Game game;
	
    /**Server constructor
     * @param newGame the game that the server is hosting
     * initializes the outputs array that contains all remote
     * player connections (starting empty) and sets the game
     * object to the game that will be played
     */
    public Server(Game newGame) {
        outputs = new Hashtable<RemotePlayer, Socket>();
        inputs = new Hashtable<RemotePlayer, Socket>();
        game = newGame;
    }

    /**addPlayer
     * @param name
     * @param connection
     * @return whether the add was successful
     * Assuming there is room left in the game, the method adds
     * them to the dictionary of player/socket values and returns
     * whether or not they were added successfully.
     */
    public boolean addPlayer(String name, Socket out, Socket in) {
        if (game.isFull()) return false;
        RemotePlayer newPlayer = new RemotePlayer(name);
        newPlayer.setDelegate(this);
        outputs.put(newPlayer, out);
        inputs.put(newPlayer, in);
        game.addPlayer(name);
        return true;
    }

    /**updateProperties
     * @param toUpdate the player whos information on the remote device needs an update
     * @param udates a string/object dictionary of the required updates
     * connects to the pre-specified socket connection to the remote player and forwards
     * the required updates to them
     */
    public void updateProperties(RemotePlayer toUpdate, Dictionary<String, Object> updates) {
        Socket connection = outputs.get(toUpdate);
        String JSON = "";
        //TODO convert the dictionary to JSON
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(connection.getOutputStream())),
                    true);
            out.println(JSON);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**updateGame
     * @param updates
     * Sends an update of general game information to all players currently
     * connected
     */
    public void updateGame(Dictionary<String, Object> updates) {
        Enumeration<RemotePlayer> keys = outputs.keys();
        while (keys.hasMoreElements()) {
            Socket connection = outputs.get(keys.nextElement());
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(connection.getOutputStream())),
                        true);
                String JSON = "";
                //TODO fill JSON string with formatting of updates dictionary
                out.println(JSON);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void queryInput(String key, Player toQuery) {
        if (toQuery instanceof RemotePlayer) {
            RemotePlayer remote = (RemotePlayer)toQuery;
            CommunicationThread commThread = new CommunicationThread(remote, key);
        } else {
            //TODO forward information to local player
        }
    }

    class CommunicationThread implements Runnable {
        private Socket clientSocket;
        private BufferedReader input;
        private String dictionaryKey;

        public CommunicationThread(RemotePlayer player, String key) {
            this.clientSocket = inputs.get(player);
            this.dictionaryKey = key;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try { 
                    String JSON = input.readLine();
                    if (JSON != null) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Hashtable<String, Object> JSON = new Hashtable<String, Object>();
            //TODO process JSON into dictionary
            game.process(JSON.get(this.dictionaryKey));
        }
    }
}
