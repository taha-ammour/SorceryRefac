package org.example;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import org.example.game.Direction;
import org.example.game.Player;
import org.joml.Vector3f;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enhanced networking system that handles server discovery, connection management,
 * and host migration, similar to Among Us.
 */
public class GameNetworking {
    // Networking constants
    private static final int TCP_PORT = 54555;
    private static final int UDP_PORT = 54777;
    private static final int DISCOVERY_PORT = 54999;
    private static final int TIMEOUT = 5000;

    // Server side player tracking
    private static final ConcurrentHashMap<Integer, UUID> connectionToPlayer = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, String> connectionToUsername = new ConcurrentHashMap<>();

    // List of active servers on the network
    private static final List<DiscoveredServer> discoveredServers = new CopyOnWriteArrayList<>();

    // Server discovery thread
    private static Thread discoveryThread;
    private static boolean discoveryRunning = false;

    // Listener management
    private static final List<GameNetworkListener> listeners = new ArrayList<>();

    // Player ordering for host migration
    private static final List<UUID> playerOrder = new ArrayList<>();
    private static UUID hostId = null;

    // Track if server is running
    private static final Map<Server, AtomicBoolean> serverRunningMap = new HashMap<>();

    /**
     * Server information discovered through network broadcast
     */
    public static class DiscoveredServer {
        private String ip;
        private int tcpPort;
        private int udpPort;
        private String name;
        private String hostUsername;
        private int playerCount;
        private long lastUpdated;

        public DiscoveredServer(String ip, int tcpPort, int udpPort, String name, String hostUsername, int playerCount) {
            this.ip = ip;
            this.tcpPort = tcpPort;
            this.udpPort = udpPort;
            this.name = name;
            this.hostUsername = hostUsername;
            this.playerCount = playerCount;
            this.lastUpdated = System.currentTimeMillis();
        }

        public String getIp() { return ip; }
        public int getTcpPort() { return tcpPort; }
        public int getUdpPort() { return udpPort; }
        public String getName() { return name; }
        public String getHostUsername() { return hostUsername; }
        public int getPlayerCount() { return playerCount; }
        public long getLastUpdated() { return lastUpdated; }

        @Override
        public String toString() {
            return name + " (" + hostUsername + ") - " + playerCount + " players";
        }
    }

    /**
     * Listener interface for game network events
     */
    public interface GameNetworkListener {
        void onServerDiscovered(DiscoveredServer server);
        void onPlayerJoined(UUID playerId, String username, String color, float x, float y);
        void onPlayerLeft(UUID playerId);
        void onHostMigration(UUID newHostId);
        void onChatMessage(String username, String message);
    }

    /**
     * Creates and starts a game server
     * @param serverName The name of the server
     * @param hostUsername The username of the host
     * @param tcpPort TCP port to bind to
     * @param udpPort UDP port to bind to
     * @return The initialized server
     * @throws IOException If server fails to start
     */
    public static Server createServer(String serverName, String hostUsername, int tcpPort, int udpPort) throws IOException {
        Server server = new Server();
        registerPackets(server.getKryo());

        server.start();
        server.bind(tcpPort, udpPort);

        // Mark server as running
        AtomicBoolean running = new AtomicBoolean(true);
        serverRunningMap.put(server, running);

        // Set the host ID
        hostId = UUID.randomUUID(); // Initial host ID
        playerOrder.add(hostId); // Add host to player order

        // Set up server listeners
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("Client connected: " + connection.getID());
            }

            @Override
            public void disconnected(Connection connection) {
                UUID playerId = connectionToPlayer.get(connection.getID());
                String username = connectionToUsername.get(connection.getID());

                if (playerId != null) {
                    // Update player order for host migration
                    playerOrder.remove(playerId);

                    // Check if this was the host
                    if (playerId.equals(hostId) && !playerOrder.isEmpty()) {
                        // Migrate host to next player in order
                        hostId = playerOrder.get(0);

                        // Notify all clients about new host
                        Packets.HostMigration migration = new Packets.HostMigration();
                        migration.newHostId = hostId.toString();
                        server.sendToAllTCP(migration);

                        System.out.println("Host migrated to: " + hostId);
                    }

                    // Broadcast player disconnect to all clients
                    Packets.PlayerDisconnect disconnect = new Packets.PlayerDisconnect();
                    disconnect.playerId = playerId.toString();
                    server.sendToAllTCP(disconnect);

                    // Remove tracking
                    connectionToPlayer.remove(connection.getID());
                    connectionToUsername.remove(connection.getID());

                    System.out.println("Player disconnected: " + username);
                }

                System.out.println("Client disconnected: " + connection.getID());
            }

            @Override
            public void received(Connection connection, Object object) {
                // Handle player join
                if (object instanceof Packets.PlayerJoin) {
                    Packets.PlayerJoin joinPacket = (Packets.PlayerJoin) object;
                    UUID playerId = UUID.fromString(joinPacket.playerId);

                    // Add player to connection tracking
                    connectionToPlayer.put(connection.getID(), playerId);
                    connectionToUsername.put(connection.getID(), joinPacket.username);

                    // Add to player order for host migration (if not already there)
                    if (!playerOrder.contains(playerId)) {
                        playerOrder.add(playerId);
                    }

                    // Tell the new player who the current host is
                    Packets.HostMigration hostInfo = new Packets.HostMigration();
                    hostInfo.newHostId = hostId.toString();
                    connection.sendTCP(hostInfo);

                    // Forward join to all other clients
                    server.sendToAllExceptTCP(connection.getID(), joinPacket);
                }
                // Handle position updates
                else if (object instanceof Packets.PlayerPositionUpdate) {
                    Packets.PlayerPositionUpdate update = (Packets.PlayerPositionUpdate) object;
                    // Forward to all clients except sender
                    server.sendToAllExceptUDP(connection.getID(), update);
                }
                // Handle chat messages
                else if (object instanceof Packets.ChatMessage) {
                    Packets.ChatMessage chatMsg = (Packets.ChatMessage) object;
                    // Forward to all clients
                    server.sendToAllTCP(chatMsg);
                }
            }
        });

        // Start server discovery broadcast
        startDiscoveryBroadcast(server, serverName, hostUsername, tcpPort, udpPort);

        return server;
    }

    /**
     * Creates and connects a game client
     * @param host Server hostname or IP address
     * @param tcpPort TCP port to connect to
     * @param udpPort UDP port to connect to
     * @return The initialized and connected client
     * @throws IOException If connection fails
     */
    public static Client createClient(String host, int tcpPort, int udpPort) throws IOException {
        Client client = new Client();
        registerPackets(client.getKryo());

        client.start();
        client.connect(TIMEOUT, host, tcpPort, udpPort);

        return client;
    }

    /**
     * Registers all packet classes with KryoNet
     * @param kryo The Kryo instance to register with
     */
    private static void registerPackets(Kryo kryo) {
        // Register basic types
        kryo.register(String.class);
        kryo.register(int.class);
        kryo.register(float.class);
        kryo.register(boolean.class);
        kryo.register(ArrayList.class);

        // Register packet classes
        kryo.register(Packets.PlayerJoin.class);
        kryo.register(Packets.PlayerDisconnect.class);
        kryo.register(Packets.PlayerPositionUpdate.class);
        kryo.register(Packets.ChatMessage.class);
        kryo.register(Packets.ServerInfo.class);
        kryo.register(Packets.HostMigration.class);
        kryo.register(Packets.GameStart.class);
        kryo.register(Packets.GameEnd.class);

        kryo.register(Packets.SpellCast.class);
        kryo.register(Packets.SpellUpgrade.class);
        kryo.register(Packets.GameAction.class);

        kryo.register(Packets.VoicePacket.class);
        kryo.register(byte[].class);
        kryo.register(Vector3f.class);

        kryo.register(Direction.class);
    }

    /**
     * Sets up client listeners for game events
     * @param client The KryoNet client
     * @param game Reference to the game for callbacks
     */
    public static void setupClientListeners(Client client, GameWorld game) {
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("Connected to server!");

                // Send local player info to server
                Packets.PlayerJoin joinPacket = new Packets.PlayerJoin();
                joinPacket.playerId = game.getLocalPlayer().getPlayerId().toString();
                joinPacket.username = game.getLocalPlayer().getUsername();
                joinPacket.color = game.getLocalPlayer().getColor();
                joinPacket.x = game.getLocalPlayer().getPosition().x;
                joinPacket.y = game.getLocalPlayer().getPosition().y;
                client.sendTCP(joinPacket);
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("Disconnected from server!");

                // Notify listeners
                for (GameNetworkListener listener : listeners) {
                    listener.onHostMigration(null); // null indicates no host (disconnected)
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                // Handle new player joining
                if (object instanceof Packets.PlayerJoin) {
                    Packets.PlayerJoin joinPacket = (Packets.PlayerJoin) object;
                    UUID playerId = UUID.fromString(joinPacket.playerId);

                    // Add player to game
                    game.addRemotePlayer(
                            playerId,
                            joinPacket.username,
                            joinPacket.color,
                            joinPacket.x,
                            joinPacket.y
                    );

                    // Notify listeners
                    for (GameNetworkListener listener : listeners) {
                        listener.onPlayerJoined(playerId, joinPacket.username, joinPacket.color,
                                joinPacket.x, joinPacket.y);
                    }
                }
                // Handle player disconnect
                else if (object instanceof Packets.PlayerDisconnect) {
                    Packets.PlayerDisconnect disconnectPacket = (Packets.PlayerDisconnect) object;
                    UUID playerId = UUID.fromString(disconnectPacket.playerId);

                    // Remove player from game
                    game.removePlayer(playerId);

                    // Notify listeners
                    for (GameNetworkListener listener : listeners) {
                        listener.onPlayerLeft(playerId);
                    }
                }
                // Handle position updates
                else if (object instanceof Packets.PlayerPositionUpdate) {
                    Packets.PlayerPositionUpdate update = (Packets.PlayerPositionUpdate) object;
                    UUID playerId = UUID.fromString(update.playerId);

                    // Update player position
                    game.updatePlayerPosition(
                            playerId,
                            update.x,
                            update.y,
                            update.direction,
                            update.isMoving,
                            update.color,
                            update.FlipX,
                            update.FlipY,
                            update.animationFrame,
                            update.currentLegSprite
                    );
                }
                // Handle chat messages
                else if (object instanceof Packets.ChatMessage) {
                    Packets.ChatMessage chatPacket = (Packets.ChatMessage) object;

                    // Display chat message
                    game.displayChatMessage(chatPacket.username, chatPacket.message);

                    // Notify listeners
                    for (GameNetworkListener listener : listeners) {
                        listener.onChatMessage(chatPacket.username, chatPacket.message);
                    }
                }
                // Handle host migration
                else if (object instanceof Packets.HostMigration) {
                    Packets.HostMigration migration = (Packets.HostMigration) object;
                    UUID newHostId = UUID.fromString(migration.newHostId);

                    System.out.println("Host migrated to: " + newHostId);

                    // Notify listeners
                    for (GameNetworkListener listener : listeners) {
                        listener.onHostMigration(newHostId);
                    }
                } else if (object instanceof Packets.SpellCast) {
                    Packets.SpellCast spellCast = (Packets.SpellCast) object;

                    // Pass to game for processing
                    game.handleSpellCast(spellCast);
                }
                // Handle spell upgrade packets
                else if (object instanceof Packets.SpellUpgrade) {
                    Packets.SpellUpgrade spellUpgrade = (Packets.SpellUpgrade) object;

                    // Pass to game for processing
                    game.handleSpellUpgrade(spellUpgrade);
                }
                // Handle game action packets (may include spell effects)
                else if (object instanceof Packets.GameAction) {
                    Packets.GameAction gameAction = (Packets.GameAction) object;

                    // Spell-related game actions (1-3 are spell types)
                    if (gameAction.actionType >= 1 && gameAction.actionType <= 3) {
                        game.getSpellSystem().processSpellAction(gameAction);
                    }
                }
                else if (object instanceof Packets.VoicePacket) {
                    Packets.VoicePacket voicePacket = (Packets.VoicePacket) object;

                    // Process voice packet
                    if (game.getVoiceManager() != null) {
                        game.getVoiceManager().processVoicePacket(voicePacket);
                    }
                }
            }
        });
    }

    /**
     * Add a listener for network events
     * @param listener The listener to add
     */
    public static void addListener(GameNetworkListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a listener
     * @param listener The listener to remove
     */
    public static void removeListener(GameNetworkListener listener) {
        listeners.remove(listener);
    }

    /**
     * Start broadcasting server info for discovery
     */
    private static void startDiscoveryBroadcast(Server server, String serverName, String hostUsername,
                                                int tcpPort, int udpPort) {
        // Create a thread to periodically broadcast server info
        Thread broadcastThread = new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);

                while (isServerRunning(server)) {
                    try {
                        // Create server info packet
                        Packets.ServerInfo info = new Packets.ServerInfo();
                        info.serverName = serverName;
                        info.hostUsername = hostUsername;
                        info.tcpPort = tcpPort;
                        info.udpPort = udpPort;
                        info.playerCount = connectionToPlayer.size() + 1; // +1 for host

                        // Serialize packet
                        byte[] sendData = serializeServerInfo(info);

                        // Broadcast to subnet
                        broadcastToSubnet(socket, sendData);

                        // Wait before next broadcast
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        System.err.println("Error broadcasting server info: " + e.getMessage());
                    }
                }

                socket.close();
            } catch (Exception e) {
                System.err.println("Broadcast thread error: " + e.getMessage());
            }
        });

        broadcastThread.setDaemon(true);
        broadcastThread.start();
    }

    /**
     * Check if a server is still running
     * @param server The server to check
     * @return true if the server is running, false otherwise
     */
    public static boolean isServerRunning(Server server) {
        AtomicBoolean running = serverRunningMap.get(server);
        return running != null && running.get();
    }

    /**
     * Mark a server as stopped
     * @param server The server to mark as stopped
     */
    public static void stopServer(Server server) {
        AtomicBoolean running = serverRunningMap.get(server);
        if (running != null) {
            running.set(false);
        }
        serverRunningMap.remove(server);
    }

    /**
     * Start listening for server discovery broadcasts
     */
    public static void startDiscoveryListener() {
        if (discoveryThread != null && discoveryThread.isAlive()) {
            return; // Already running
        }

        discoveryRunning = true;
        discoveryThread = new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT);
                socket.setSoTimeout(1000); // 1 second timeout for blocking operations

                byte[] receiveData = new byte[1024];

                while (discoveryRunning) {
                    try {
                        // Prepare packet for receiving
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                        // Wait for packet
                        socket.receive(receivePacket);

                        // Process packet
                        processDiscoveryPacket(receivePacket);
                    } catch (SocketTimeoutException e) {
                        // Timeout is expected, continue loop
                    } catch (Exception e) {
                        if (discoveryRunning) {
                            System.err.println("Error in discovery listener: " + e.getMessage());
                        }
                    }
                }

                socket.close();
            } catch (Exception e) {
                System.err.println("Discovery listener error: " + e.getMessage());
            }
        });

        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    /**
     * Stop listening for server discovery broadcasts
     */
    public static void stopDiscoveryListener() {
        discoveryRunning = false;
        if (discoveryThread != null) {
            try {
                discoveryThread.join(2000); // Wait up to 2 seconds for thread to exit
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        discoveryThread = null;
        discoveredServers.clear();
    }

    /**
     * Get the list of discovered servers
     * @return List of discovered servers
     */
    public static List<DiscoveredServer> getDiscoveredServers() {
        // Remove servers that haven't been updated in 10 seconds
        long now = System.currentTimeMillis();
        discoveredServers.removeIf(server -> (now - server.lastUpdated) > 10000);

        return new ArrayList<>(discoveredServers);
    }

    /**
     * Process a discovery packet from a server
     */
    private static void processDiscoveryPacket(DatagramPacket packet) {
        try {
            // Deserialize packet
            Packets.ServerInfo info = deserializeServerInfo(packet.getData(), packet.getLength());

            // Get server IP
            String ip = packet.getAddress().getHostAddress();

            // Check if we already have this server
            boolean found = false;
            for (DiscoveredServer server : discoveredServers) {
                if (server.getIp().equals(ip) && server.getTcpPort() == info.tcpPort) {
                    // Update existing server
                    server.playerCount = info.playerCount;
                    server.lastUpdated = System.currentTimeMillis();
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Add new server
                DiscoveredServer server = new DiscoveredServer(
                        ip, info.tcpPort, info.udpPort, info.serverName, info.hostUsername, info.playerCount);
                discoveredServers.add(server);

                // Notify listeners
                for (GameNetworkListener listener : listeners) {
                    listener.onServerDiscovered(server);
                }

                System.out.println("Discovered server: " + info.serverName + " at " + ip + ":" + info.tcpPort);
            }
        } catch (Exception e) {
            System.err.println("Error processing discovery packet: " + e.getMessage());
        }
    }

    /**
     * Broadcast a packet to the local subnet
     */
    private static void broadcastToSubnet(DatagramSocket socket, byte[] data) throws IOException {
        // Get list of all network interfaces
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            // Skip loopback or inactive interfaces
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress broadcast = interfaceAddress.getBroadcast();

                // Skip if no broadcast address
                if (broadcast == null) {
                    continue;
                }

                // Create broadcast packet
                DatagramPacket packet = new DatagramPacket(
                        data, data.length, broadcast, DISCOVERY_PORT);

                // Send packet
                socket.send(packet);
            }
        }
    }

    /**
     * Serialize server info into a byte array
     */
    private static byte[] serializeServerInfo(Packets.ServerInfo info) {
        // Simple serialization - in a real implementation, use proper serialization
        StringBuilder sb = new StringBuilder();
        sb.append(info.serverName).append("\n");
        sb.append(info.hostUsername).append("\n");
        sb.append(info.tcpPort).append("\n");
        sb.append(info.udpPort).append("\n");
        sb.append(info.playerCount);

        return sb.toString().getBytes();
    }

    /**
     * Deserialize server info from a byte array
     */
    private static Packets.ServerInfo deserializeServerInfo(byte[] data, int length) {
        // Simple deserialization - in a real implementation, use proper deserialization
        String str = new String(data, 0, length);
        String[] parts = str.split("\n");

        Packets.ServerInfo info = new Packets.ServerInfo();
        info.serverName = parts[0];
        info.hostUsername = parts[1];
        info.tcpPort = Integer.parseInt(parts[2]);
        info.udpPort = Integer.parseInt(parts[3]);
        info.playerCount = Integer.parseInt(parts[4]);

        return info;
    }
}