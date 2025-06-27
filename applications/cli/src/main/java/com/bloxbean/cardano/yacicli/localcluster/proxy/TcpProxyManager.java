package com.bloxbean.cardano.yacicli.localcluster.proxy;

import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

@Component
public class TcpProxyManager {
    private final Map<Integer, ProxyInstance> proxies = new ConcurrentHashMap<>();

    public void startProxy(int localPort, String targetHost, int targetPort) throws IOException {
        if (proxies.containsKey(localPort)) {
            throw new IllegalStateException("Proxy already running on port " + localPort);
        }

        ProxyInstance proxy = new ProxyInstance(localPort, targetHost, targetPort);
        proxy.start();
        proxies.put(localPort, proxy);
    }

    public void stopProxy(int localPort) {
        ProxyInstance proxy = proxies.remove(localPort);
        if (proxy != null) proxy.stop();
    }

    public void stopAll() {
        for (int port : new ArrayList<>(proxies.keySet())) {
            stopProxy(port);
        }
    }

    private static class ProxyInstance {
        private final int localPort;
        private final String targetHost;
        private final int targetPort;
        private volatile boolean running = true;
        private ServerSocket serverSocket;
        private final List<Socket> connections = new CopyOnWriteArrayList<>();

        ProxyInstance(int localPort, String targetHost, int targetPort) {
            this.localPort = localPort;
            this.targetHost = targetHost;
            this.targetPort = targetPort;
        }

        public void start() throws IOException {
            serverSocket = new ServerSocket(localPort);
            new Thread(() -> {
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        Socket server = new Socket(targetHost, targetPort);
                        connections.add(client);
                        connections.add(server);
                        pipe(client.getInputStream(), server.getOutputStream());
                        pipe(server.getInputStream(), client.getOutputStream());
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("Proxy error on port " + localPort + ": " + e.getMessage());
                        }
                    }
                }
            }, "TcpProxy-" + localPort).start();
        }

        private void pipe(InputStream in, OutputStream out) {
            new Thread(() -> {
                try (in; out) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        out.flush();
                    }
                } catch (IOException e) {
                    // normal on disconnect
                }
            }).start();
        }

        public void stop() {
            running = false;
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
            for (Socket socket : connections) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
            connections.clear();
        }
    }
}

