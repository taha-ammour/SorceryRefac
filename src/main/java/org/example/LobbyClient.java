package org.example;

// LobbyClient.java
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import java.io.IOException;
import java.util.ArrayList;

public class LobbyClient {
    private Client client;

    // Constructor now accepts a server IP address.
    public LobbyClient(String serverIp, int tcpPort, int udpPort) throws IOException {
        client = new Client();
        NetworkRegistration.registerClasses(client.getKryo());
        client.start();
        // Connect to the server using the provided IP.
        client.connect(5000, serverIp, tcpPort, udpPort);
        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Packets.JoinResponse) {
                    Packets.JoinResponse response = (Packets.JoinResponse) object;
                    if (response.accepted) {
                        System.out.println("Joined successfully: " + response.message);
                        System.out.println("Current players: " + response.currentPlayers);
                    } else {
                        System.out.println("Failed to join: " + response.message);
                    }
                }
            }
        });
    }

    public void joinLobby(String username, String lobbyCode) {
        Packets.JoinRequest req = new Packets.JoinRequest();
        req.username = username;
        req.lobbyCode = lobbyCode;
        client.sendTCP(req);
    }

    public void sendChatMessage(String username, String message) {
        Packets.ChatMessage chat = new Packets.ChatMessage();
        chat.username = username;
        chat.message = message;
        client.sendTCP(chat);
    }
}
