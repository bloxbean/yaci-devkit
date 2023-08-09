package com.bloxbean.cardano.yacicli.util;

import java.io.IOException;
import java.net.*;

public class PortUtil {

    public static boolean isPortAvailable(int port) {
        boolean isAvailable = true;
        SocketAddress socketAddress = new InetSocketAddress("localhost", port);
        Socket socket = new Socket();
        int timeout = 2000;
        try {
            socket.connect(socketAddress, timeout);
            socket.close();
            isAvailable = false;
        } catch (SocketTimeoutException exception) {
            isAvailable = true;
        } catch (IOException exception) {
            isAvailable = true;
        }
        return isAvailable;
    }
}
