package com.example.digitalDeck;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class Stream {
    private Reader reader;
    private Writer writer;
    private Socket socket;
    private StreamDelegate delegate;
    
    private Handler callbackHandler;
    
    public Stream(Socket aSocket, StreamDelegate aDelegate, Looper aLooper) {
        // TODO: If this gets any more complex, the entire Stream should run on its own Thread.
        
        socket = aSocket;
        delegate = aDelegate;
        reader = new Reader(aSocket);
        writer = new Writer(aSocket);
        
        // Build a Handler on the same thread that this Stream was created on, so that
        // the delegate methods get called on it.
        callbackHandler = new Handler(aLooper, new Handler.Callback() {
            public boolean handleMessage(Message msg) {
                if (msg.what == 0) {
                    delegate.streamReceivedData(Stream.this, (JSONObject)msg.obj);                            
                }
                else if (msg.what == 1) {
                    // TODO: Write tags can be handled here if I ever get around to that
                }
                
                return true;
            }
        });
        
        new Thread(reader).start();
        new Thread(writer).start();
    }
    
    public void queueWrite(JSONObject data) {
        writer.queueWrite(data);
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
    
    private class Reader implements Runnable {
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
        public Reader(Socket clientSocket) {
            System.out.println("starting reader");
            socket = clientSocket;
            try {
                input = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**run
         * Continuously reads from the input buffer of the socket and if the input changes
         * forwards the information to the updateUIThread
         */
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
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
                    
                    JSONObject payload = new JSONObject(read);

                    callbackHandler.obtainMessage(0, payload).sendToTarget();
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                // Closed socket to stop loop
            }
        }
        
        public void stop() {
            try {
                input.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static class Writer implements Runnable {
        private Socket socket;
        private DataOutputStream output;
        private LinkedBlockingQueue<JSONObject> queue;
        private JSONObject DOTA;
        
        public Writer(Socket aSocket) {
            System.out.println("starting writer");
            socket = aSocket;
            queue = new LinkedBlockingQueue<JSONObject>();
            
            try {
                output = new DataOutputStream(socket.getOutputStream());
                DOTA = new JSONObject();
                DOTA.put("DOTA", "DOTA");
            } 
            catch (JSONException e) {
                e.printStackTrace();
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                    JSONObject payload = queue.take(); // Blocks until there is something to take
                    if (payload.equals(DOTA)) return;
                    
                    String payloadString = payload.toString();
                    System.out.println("writing " + payloadString);
                    
                    // Build the header:
                    ByteBuffer buf = ByteBuffer.allocate(9);
                    buf.putInt(0x444c4447);
                    buf.put((byte)0x0f);
                    buf.putInt(payloadString.length());
                    
                    // Now write them to the socket in order:
                    output.write(buf.array(), 0, 9);
                    output.writeBytes(payloadString);
                    System.out.println("wrote " + payloadString);
                }
            }
            catch (InterruptedException e) {
                System.out.println("shit broke");
                e.printStackTrace();
            }
            catch (IOException e) {
                System.out.println("writer socket closed");
            }
        }
        
        public void queueWrite(JSONObject payload) {
            try {
                queue.put(payload);
                System.out.println("hey queued write");
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
