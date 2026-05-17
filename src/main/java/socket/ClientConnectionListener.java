package socket;

import java.net.Socket;

public interface ClientConnectionListener {
    void clientConnected(Socket socket);

    void clientDisconnected(Socket socket);
}
