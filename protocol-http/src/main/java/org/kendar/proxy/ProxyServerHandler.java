package org.kendar.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class ProxyServerHandler {

    private static final Logger log = LoggerFactory.getLogger("org.kendar.http.Proxy");
    private final ExecutorService executor;
    //    public static final Pattern CONNECT_PATTERN = Pattern.compile("CONNECT (.+):(.+) HTTP/(1\\.[01])",
//            Pattern.CASE_INSENSITIVE);
    private final Socket clientSocket;
    private final int httpRedirect;
    private final int httpsRedirect;
    private final Function<String, String> dnsResolver;
    private final HashSet<String> ignore;
    private boolean previousWasR = false;

    public ProxyServerHandler(ExecutorService executor, Socket clientSocket, int httpRedirect, int httpsRedirect,
                              Function<String, String> dnsResolver, HashSet<String> ignore) {
        this.executor = executor;
        this.clientSocket = clientSocket;
        this.httpRedirect = httpRedirect;
        this.httpsRedirect = httpsRedirect;
        this.dnsResolver = dnsResolver;
        this.ignore = ignore;
    }

    private static void forwardData(Socket inputSocket, Socket outputSocket) {
        try {
            InputStream inputStream = inputSocket.getInputStream();
            try {
                OutputStream outputStream = outputSocket.getOutputStream();
                try {
                    byte[] buffer = new byte[4096];
                    int read;
                    do {
                        read = inputStream.read(buffer);
                        if (read > 0) {
                            outputStream.write(buffer, 0, read);
                            if (inputStream.available() < 1) {
                                outputStream.flush();
                            }
                        }
                    } while (read >= 0);
                } finally {
                    if (!outputSocket.isOutputShutdown()) {
                        outputSocket.shutdownOutput();
                    }
                }
            } finally {
                if (!inputSocket.isInputShutdown()) {
                    inputSocket.shutdownInput();
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();  // TODO: implement catch
        }
    }

    private static void connectionEstablished(ProxyRequest proxyRequest, OutputStreamWriter outputStreamWriter) throws IOException {
        outputStreamWriter.write(proxyRequest.getProtocol() + " 200 Connection established\r\n");
        outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
        outputStreamWriter.write("\r\n");
        outputStreamWriter.flush();
    }

    private static void forwardFirstLine(ProxyRequest proxyRequest, Socket forwardSocket) throws IOException {
        OutputStream outputStream = forwardSocket.getOutputStream();
        var forwardRequest = proxyRequest.getVerb() + " " + proxyRequest.getPath() + " " + proxyRequest.getProtocol() + "\r\n";
        outputStream.write(forwardRequest.getBytes(StandardCharsets.UTF_8));
        outputStream.write("Proxy-agent: Simple/0.1\r\n".getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private static void badGateway(ProxyRequest proxyRequest, OutputStreamWriter outputStreamWriter) throws IOException {
        outputStreamWriter.write(proxyRequest.getProtocol() + " 502 Bad Gateway\r\n");
        outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
        outputStreamWriter.write("\r\n");
        outputStreamWriter.flush();
    }

    public void run() {
        try {
            String request = readLine(clientSocket);
            if (request.isEmpty()) {
                return;
            }
            var proxyRequest = new ProxyRequest(request);
            if (ignore.contains(proxyRequest.getHost())) {
                return;
            }
            log.debug(request);
            if (proxyRequest.isConnect()) {
                handleHttpsRequest(proxyRequest);
            } else {
                handleHttpRequest(proxyRequest);
            }
        } catch (IOException e) {
            //e.printStackTrace();  // TODO: implement catch
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // e.printStackTrace();  // TODO: implement catch
            }
        }
    }

    private void handleHttpsRequest(ProxyRequest proxyRequest) throws IOException {
        cleanConnectHeader();
        var outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream(),
                StandardCharsets.ISO_8859_1);


        final Socket forwardSocket;
        try {
            forwardSocket = new Socket(dnsResolver.apply(proxyRequest.getHost()),
                    changePort(httpsRedirect, proxyRequest.getPort()));

        } catch (IOException | NumberFormatException e) {
            badGateway(proxyRequest, outputStreamWriter);
            return;
        }

        connectionEstablished(proxyRequest, outputStreamWriter);
        executor.submit(() -> forwardData(forwardSocket, clientSocket));
        try {
            emptyForwardSocket(forwardSocket);
        } finally {
            forwardSocket.close();
        }
    }

    private void cleanConnectHeader() throws IOException {
        String header;
        do {
            header = readLine(clientSocket);
        } while (!"".equals(header));
    }

    private String readLine(Socket socket) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int next;
        readerLoop:
        while ((next = socket.getInputStream().read()) != -1) {
            if (previousWasR && next == '\n') {
                previousWasR = false;
                continue;
            }
            previousWasR = false;
            switch (next) {
                case '\r':
                    previousWasR = true;
                    break readerLoop;
                case '\n':
                    break readerLoop;
                default:
                    byteArrayOutputStream.write(next);
                    break;
            }
        }
        return byteArrayOutputStream.toString(StandardCharsets.ISO_8859_1);
    }

    private void handleHttpRequest(ProxyRequest proxyRequest) throws IOException {
        var outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream(),
                StandardCharsets.ISO_8859_1);

        final Socket forwardSocket;
        try {
            forwardSocket = new Socket(dnsResolver.apply(proxyRequest.getHost()),
                    changePort(httpRedirect, proxyRequest.getPort()));

        } catch (IOException | NumberFormatException e) {
            badGateway(proxyRequest, outputStreamWriter);
            return;
        }
        try {
            forwardFirstLine(proxyRequest, forwardSocket);
            executor.submit(() -> forwardData(forwardSocket, clientSocket));
            emptyForwardSocket(forwardSocket);
        } finally {
            forwardSocket.close();
        }
    }

    private void emptyForwardSocket(Socket forwardSocket) throws IOException {
        if (previousWasR) {
            int read = clientSocket.getInputStream().read();
            if (read != -1) {
                if (read != '\n') {
                    forwardSocket.getOutputStream().write(read);
                }
                forwardData(clientSocket, forwardSocket);
            } else {
                if (!forwardSocket.isOutputShutdown()) {
                    forwardSocket.shutdownOutput();
                }
                if (!clientSocket.isInputShutdown()) {
                    clientSocket.shutdownInput();
                }
            }
        } else {
            forwardData(clientSocket, forwardSocket);
        }
    }

    private int changePort(int redirect, int port) {
        return redirect == -1 ? port : redirect;
    }
}
