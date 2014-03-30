package com.example.digitalDeck;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

public class Stream {
    private Reader reader;
    private Writer writer;
    private Socket socket;
    private StreamDelegate delegate;
    
    public Stream(Socket aSocket) { // Use an existing socket
        socket = aSocket;
        reader = new Reader(aSocket);
        writer = new Writer(aSocket);
        new Thread(reader).start();
        new Thread(writer).start();
    }
    
    public Stream(final Service aService) { // Make the socket ourselves
        new Thread(new Runnable() {
            public void run() {
                try {
                    socket = new Socket(aService.getFirstIP(), aService.getPort());
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).run();
        
        reader = new Reader(socket);
        writer = new Writer(socket);
        new Thread(reader).start();
        new Thread(writer).start();
    }
    
    public void queueWrite(JSONObject data) {
        writer.queueWrite(data);
    }
    
    public void setDelegate(StreamDelegate aDelegate) {
        delegate = aDelegate;
    }
    
    public Reader getReader() {
        return reader;
    }
    
    public Writer getWriter() {
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
                    
                    JSONObject payload = new JSONObject(read);

                    delegate.streamReceivedData(Stream.this, payload);
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
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
    
    private static class Writer implements Runnable {
        private Socket socket;
        private DataOutputStream output;
        private LinkedBlockingQueue<JSONObject> queue;
        private JSONObject DOTA;
        
        public Writer(Socket aSocket) {
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
