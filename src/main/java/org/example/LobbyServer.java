package org.example;

// LobbyServer.java
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import org.example.Packets.*;

public class LobbyServer {
    private Server server;
    private String lobbyCode;
    private final ArrayList<String> players;
    private final HashMap<Connection, String> connectionToUsername;
    private String currentHost;
    private static final int MAX_PLAYERS = 5;

    public LobbyServer(int tcpPort, int udpPort) throws IOException {
        server = new Server();
        NetworkRegistration.registerClasses(server.getKryo());
        players = new ArrayList<>();
        connectionToUsername = new HashMap<>();
        lobbyCode = generateLobbyCode();

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Packets.JoinRequest) {
                    Packets.JoinRequest req = (Packets.JoinRequest) object;
                    // Check if the lobby code matches.
                    if (!req.lobbyCode.equals(lobbyCode)) {
                        Packets.JoinResponse response = new Packets.JoinResponse();
                        response.accepted = false;
                        response.message = "Invalid lobby code.";
                        connection.sendTCP(response);
                    } else if (players.size() >= MAX_PLAYERS) {
                        // Lobby is full.
                        Packets.JoinResponse response = new Packets.JoinResponse();
                        response.accepted = false;
                        response.message = "Lobby is full.";
                        connection.sendTCP(response);
                    } else {
                        // Accept the player.
                        players.add(req.username);
                        connectionToUsername.put(connection, req.username);
                        // Prepare an update with the current players.
                        if (players.size() == 1) {
                            currentHost = req.username;
                        }

                        Packets.JoinResponse response = new Packets.JoinResponse();
                        response.accepted = true;
                        response.message = "Welcome to lobby " + lobbyCode +
                                (req.username.equals(currentHost) ? " (You are the host)" : "");
                        response.currentPlayers = new ArrayList<>(players);
                        server.sendToAllTCP(response);

                        Packets.ChatMessage chatMsg = new Packets.ChatMessage();
                        chatMsg.username = "SERVER";
                        chatMsg.message = req.username + " joined the lobby.";
                        server.sendToAllTCP(chatMsg);
                    }
                }else if (object instanceof ChatMessage) {
                    // Broadcast received chat messages.
                    ChatMessage chat = (ChatMessage) object;
                    System.out.println("[" + chat.username + "]: " + chat.message);
                    server.sendToAllTCP(chat);
                }
            }
            @Override
            public void disconnected(Connection connection) {
                String username = connectionToUsername.remove(connection);
                if (username != null) {
                    players.remove(username);
                    System.out.println("Player removed: " + username);
                    System.out.println("Current players: " + players);

                    Packets.ChatMessage chatMsg = new Packets.ChatMessage();
                    chatMsg.username = "SERVER";
                    chatMsg.message = username + " has disconnected.";
                    server.sendToAllTCP(chatMsg);

                    if (username.equals(currentHost)) {
                        if (!players.isEmpty()) {
                            currentHost = players.get(0);
                            Packets.ChatMessage hostMsg = new Packets.ChatMessage();
                            hostMsg.username = "SERVER";
                            hostMsg.message = "New host is " + currentHost;
                            server.sendToAllTCP(hostMsg);
                        } else {
                            System.out.println("No players left. Shutting down server.");
                            server.stop();
                        }
                    }
                }
            }

        });
        // Bind to ports (adjust these if needed)
        server.bind(tcpPort, udpPort);
        server.start();
        System.out.println("Lobby server started with code: " + lobbyCode + " on TCP " + tcpPort + " and UDP " + udpPort);
    }

    private String generateLobbyCode() {
        // Generate a random 4-letter code.
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rand = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            code.append(letters.charAt(rand.nextInt(letters.length())));
        }
        return code.toString();
    }

    public String getLobbyCode() {
        return lobbyCode;
    }

    public void shutdown() {
        server.stop();
    }
}
