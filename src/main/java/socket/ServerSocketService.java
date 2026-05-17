package socket;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

public class ServerSocketService {
    private final int port;
    private final Consumer<String> logConsumer;
    private final Consumer<String> statusConsumer;
    private final ClientConnectionListener connectionListener;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public ServerSocketService(int port,
                               Consumer<String> logConsumer,
                               Consumer<String> statusConsumer,
                               ClientConnectionListener connectionListener) {
        this.port = port;
        this.logConsumer = logConsumer;
        this.statusConsumer = statusConsumer;
        this.connectionListener = connectionListener;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        acceptThread = new Thread(this::acceptClients, "server-accept-thread");
        acceptThread.start();
        log("server started - port " + port);
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException exception) {
            log("Lỗi khi dừng server: " + exception.getMessage());
        }
    }

    private void acceptClients() {
        status("Running");
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                status("client connected: " + clientIp);
                log("client connected: " + clientIp + ":" + clientSocket.getPort());
                if (connectionListener != null) {
                    connectionListener.clientConnected(clientSocket);
                }
                new Thread(new ClientHandler(clientSocket, logConsumer, statusConsumer, connectionListener),
                        "client-handler-" + clientSocket.getPort()).start();
            } catch (IOException exception) {
                if (running) {
                    log("Lỗi khi nhận client: " + exception.getMessage());
                }
            }
        }
    }

    private void log(String message) {
        if (logConsumer != null) {
            logConsumer.accept(message);
        }
    }

    private void status(String message) {
        if (statusConsumer != null) {
            statusConsumer.accept(message);
        }
    }

    public static String getLanIpAddress() {
        try {
            List<String> ipAddresses = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!isUsableNetworkInterface(networkInterface)) {
                    continue;
                }
                Enumeration<java.net.InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
                while (interfaceAddresses.hasMoreElements()) {
                    java.net.InetAddress address = interfaceAddresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        ipAddresses.add(address.getHostAddress());
                    }
                }
            }
            return ipAddresses.stream()
                    .sorted(Comparator.comparingInt(ServerSocketService::ipPriority))
                    .findFirst()
                    .orElse("127.0.0.1");
        } catch (SocketException ignored) {
            // Fallback cho localhost.
        }
        return "127.0.0.1";
    }

    private static boolean isUsableNetworkInterface(NetworkInterface networkInterface) throws SocketException {
        if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
            return false;
        }
        String name = (networkInterface.getName() + " " + networkInterface.getDisplayName()).toLowerCase();
        return !name.contains("virtual")
                && !name.contains("vmware")
                && !name.contains("virtualbox")
                && !name.contains("hyper-v")
                && !name.contains("wsl")
                && !name.contains("bluetooth")
                && !name.contains("loopback");
    }

    private static int ipPriority(String ip) {
        if (ip.startsWith("192.168.")) {
            return 0;
        }
        if (ip.startsWith("10.")) {
            return 1;
        }
        if (ip.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
            return 2;
        }
        return 3;
    }
}
