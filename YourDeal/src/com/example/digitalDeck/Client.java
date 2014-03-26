package com.example.digitalDeck;

import java.util.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;

public class Client {

    private Socket serverOut;
    private ServerSocket serverIn;
    private Player player;
    //private RemoteGame game;

    public Client(ServerSocket in, Socket out) {
        serverOut = out;
        serverIn = in;
    }

    public void updateProperties() {
        CommunicationThread commThread = new CommunicationThread();
    }

    class CommunicationThread implements Runnable {
        private BufferedReader input;
        
        public CommunicationThread() {
            try {
                input = new BufferedReader(new InputStreamReader(serverOut.getInputStream()));
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
            //TODO interperate JSON
            //TODO send updates to remote game
        }
    }
}
