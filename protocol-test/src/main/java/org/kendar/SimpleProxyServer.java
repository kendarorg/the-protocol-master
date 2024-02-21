package org.kendar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleProxyServer {
    public static boolean write = false;

    public static String toHexByteArray(byte[] byteArray, int bytesRead) {
        StringBuilder hex = new StringBuilder();

        // Iterating through each byte in the array
        var endof = 0;
        for (int j = 0; j < byteArray.length && j < bytesRead; j++) {
            byte i = byteArray[j];
            hex.append("0x").append(String.format("%02X", i)).append(" ");
            if (endof == 16) {
                hex.append("\n");
                endof = 0;
            } else {
                endof++;
            }
        }

        return hex.toString();
    }

    public static void main(String[] args) throws IOException {
        try {
            String host = "your Proxy Server";
            String remoteHost = "remote";
            int remoteport = 100;
            int localport = 111;
            // Print a start-up message
            System.out.println("Starting proxy for " + host + ":" + remoteport
                    + " on port " + localport);
            // And start running the server
            runServer(host, remoteport, localport, remoteHost); // never returns
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * runs a single-threaded proxy server on
     * the specified local port. It never returns.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public static void runServer(String host, int remoteport, int localport, String remoteHost)
            throws IOException {
        // Create a ServerSocket to listen for connections with
        ServerSocket ss = new ServerSocket(localport);
        ss.setSoTimeout(10000);

        final byte[] request = new byte[1024];
        byte[] reply = new byte[4096];
        AtomicInteger connectionId = new AtomicInteger(1);

        while (true) {
            // Wait for a connection on the local port
            Socket client = ss.accept();
            var acceptTh = new Thread(() -> {
                var currentConnection = connectionId.getAndIncrement();
                Socket server = null;
                try {
                    final InputStream streamFromClient = client.getInputStream();
                    final OutputStream streamToClient = client.getOutputStream();

                    // Make a connection to the real server.
                    // If we cannot connect to the server, send an error to the
                    // client, disconnect, and continue waiting for connections.
                    try {
                        server = new Socket(remoteHost, remoteport);
                        server.setSoTimeout(10000);
                    } catch (IOException e) {
                        PrintWriter out = new PrintWriter(streamToClient);
                        out.print("[PROXY ] " + currentConnection + " Proxy server cannot connect to " + host + ":"
                                + remoteport + ":\n" + e + "\n");
                        out.flush();
                        client.close();
                        return;
                    }

                    // Get server streams.
                    final InputStream streamFromServer = server.getInputStream();
                    final OutputStream streamToServer = server.getOutputStream();

                    // a thread to read the client's requests and pass them
                    // to the server. A separate thread for asynchronous.
                    Thread t = new Thread() {
                        public void run() {
                            int bytesRead;
                            try {
                                while ((bytesRead = streamFromClient.read(request)) != -1) {
                                    System.out.println("[PROXY ] " + currentConnection + " TO SERVER:\n" + toHexByteArray(request, bytesRead));
                                    streamToServer.write(request, 0, bytesRead);
                                    streamToServer.flush();
                                }
                            } catch (IOException e) {
                            }

                            // the client closed the connection to us, so close our
                            // connection to the server.
                            try {
                                streamToServer.close();
                            } catch (IOException e) {
                            }
                        }
                    };

                    // Start the client-to-server request thread running
                    t.start();

                    // Read the server's responses
                    // and pass them back to the client.
                    int bytesRead;
                    try {
                        while ((bytesRead = streamFromServer.read(reply)) != -1) {
                            System.out.println("[PROXY ] " + currentConnection + " FR SERVER " + toHexByteArray(reply, bytesRead));
                            streamToClient.write(reply, 0, bytesRead);
                            streamToClient.flush();
                        }
                    } catch (IOException e) {
                    }

                    // The server closed its connection to us, so we close our
                    // connection to our client.
                    streamToClient.close();


                } catch (IOException e) {
                    System.err.println(e);
                } finally {
                    System.out.println("[PROXY ] " + currentConnection + " CLOSING");
                    try {
                        if (server != null)
                            server.close();
                        if (client != null)
                            client.close();
                    } catch (IOException e) {
                    }
                }
            });

            acceptTh.start();

        }
    }
}

