package org.example;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;
import org.example.engine.*;
import org.example.game.Player;
import org.example.ui.UIManagerGameObject;
import org.example.ui.*;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    // Enable debug mode globally
    private static final boolean DEBUG_MODE = true;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Main Menu ===");
        System.out.println("1. Run Game Engine");
        System.out.println("2. Game Lobby System");
        System.out.println("3. Debug Mode");
        System.out.print("Select option (1, 2 or 3): ");
        int option = scanner.nextInt();
        scanner.nextLine(); // consume newline

        if (option == 1) {
            runGameEngine(scanner);
        } else if (option == 2) {
            runLobbySystem(scanner);
        } else if (option == 3) {
            runDebugMode(scanner);
        } else {
            System.out.println("Invalid option. Exiting.");
        }

        scanner.close();
    }

    private static void runGameEngine(Scanner scanner) {
        System.out.println("=== Game Engine ===");
        System.out.println("1. Single Player");
        System.out.println("2. Multiplayer");
        System.out.print("Select mode (1 or 2): ");
        int gameMode = scanner.nextInt();
        scanner.nextLine(); // consume newline

        // Create and initialize the engine with window dimensions and a title
        Engine engine = new Engine();
        engine.init(800, 600, "Game Engine Example");

        // Create a new scene
        Scene scene = new Scene();

        // Set up sprite resources
        SpriteManager spriteManager = new SpriteManager();
        EntityRegistry.registerEntities(spriteManager);
        EntityRegistry.registerUi(spriteManager);
        EntityRegistry.registerTiles(spriteManager);

        // Create the game world with scene, sprite manager, input and camera
        GameWorld gameWorld = new GameWorld(scene, spriteManager, engine.getInput(), engine.getCamera());
        gameWorld.setDebug(DEBUG_MODE);

        // Set up UI with smaller text scale
        setupUI(engine, gameWorld,spriteManager);

        // Create the local player
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        System.out.print("Choose color (RED, BLUE, GREEN, YELLOW, PINK, CYAN): ");
        String color = scanner.nextLine().toUpperCase();

        Player localPlayer = gameWorld.createLocalPlayer(username, color);

        // Create some terrain tiles
        createTerrain(scene, spriteManager);

        // Add lights to the scene
        setupLights(scene);

        // Draw all registered sprites in debug mode
        if (DEBUG_MODE) {
            System.out.println("Drawing all registered sprites for debugging");
            gameWorld.drawAllRegisteredSprites();
        }

        // Set up multiplayer if selected
        if (gameMode == 2) {
            setupMultiplayer(scanner, gameWorld);
        }

        // Set the active scene and start the engine's main loop
        engine.setActiveScene(scene);

        // Add an update hook for the game world
        gameWorld.update(0); // Initial update


        // Add this to your game initialization
        UUID testPlayerId = UUID.fromString("ee6f5f74-c93a-41fe-83ae-a2113db84dbc");
        gameWorld.addRemotePlayer(testPlayerId, "TestPlayer", "RED", 500, 300);

        GameWorldObject gameWorldObject = new GameWorldObject(gameWorld);
        scene.addGameObject(gameWorldObject);

        engine.run();
    }

    private static void setupUI(Engine engine, GameWorld gameWorld, SpriteManager spriteManager) {
        // Get UI manager from engine
        UIManagerGameObject uiGameObject = engine.getUIManagerGameObject();
        UIManager uiManager = uiGameObject.getUiManager();

        // Create font resources with adjusted scale
        FontSheet fontSheet = new FontSheet();
        fontSheet.setScale(2.0f); // Reduce from default scale (e.g., 4.0f) to 2.0f

        Shader fontShader = Shader.loadFromFiles("/shaders/sprite.vs.glsl", "/shaders/Fontsh.fs.glsl");

        // Create UI elements
        UIText healthText = new UIText(fontSheet, fontShader, "100", 40, 30);
        //UISprite healthSp = new UISprite(20,30,8,8,"health_1",spriteManager);
        UIText chatDisplay = new UIText(fontSheet, fontShader, "Chat messages will appear here", 10, 700);

        // Add debug mode indicator
        UIText debugText = null;
        if (DEBUG_MODE) {
            debugText = new UIText(fontSheet, fontShader, "DEBUG MODE ACTIVE", 800, 30);
            debugText.setText("$500 DEBUG MODE ACTIVE"); // Red text for emphasis
            uiManager.addComponent(debugText, true);
        }

        // Add UI elements to the manager
        uiManager.addComponent(healthText, true);
        //uiManager.addComponent(healthSp, true);
        uiManager.addComponent(chatDisplay, true);

        // Set chat display in the game world
        gameWorld.setChatDisplay(chatDisplay);

        // Add UI game object to the scene
        gameWorld.getGameScene().addGameObject(uiGameObject);
    }

    private static void setupLights(Scene scene) {
        // Create a directional light
        Light directionalLight = new Light();
        directionalLight.setDirection(new Vector3f(0.0f, 0.0f, -1.0f));
        directionalLight.setIntensity(0.5f);
        directionalLight.setType(0);
        directionalLight.setConstant(100.0f);
        directionalLight.setLinear(0.0f);
        directionalLight.setQuadratic(0.0f);
        LightObject lightObject = new LightObject(directionalLight);
        scene.addGameObject(lightObject);

        // Create a more visible point light
        Light pointLight = new Light();
        pointLight.setPosition(new Vector3f(400.0f, 300.0f, 5.0f)); // Match Z with sprites
        pointLight.setColor(new Vector3f(0.0f, 1.0f, 1.0f)); // White light for more visibility
        pointLight.setIntensity(500.0f); // Higher intensity
        pointLight.setConstant(0.5f); // Lower constant for less attenuation
        pointLight.setLinear(0.5f); // Lower linear for wider reach
        pointLight.setQuadratic(0.01f); // Lower quadratic for wider reach
        pointLight.setType(1); // Point light type
        pointLight.setDirection(new Vector3f(0.0f, 0.0f, -1.0f).normalize());
        LightObject pointLightObject = new LightObject(pointLight);
        scene.addGameObject(pointLightObject);
    }

    private static void createTerrain(Scene scene, SpriteManager spriteManager) {
        // Create a grid of floor tiles
        int tileSize = 32; // Set a larger tile size (original was 16)
        int gridWidth = 20;
        int gridHeight = 20;

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                // Alternate between tile types for visual variety
                String tileName = (x + y) % 2 == 0 ? "tile_w_1" : "tile_l_1";
                Sprite tile = spriteManager.getSprite(tileName);
                tile.setPosition(x * tileSize, y * tileSize);
                tile.setScale(2.0f, 2.0f); // Scale up the tiles
                scene.addGameObject(tile);
            }
        }

        // Add some decorative elements
        for (int i = 0; i < 5; i++) {
            Sprite decoration = spriteManager.getSprite("lamp_post_0");
            decoration.setPosition(100 + i * 120, 200);
            decoration.setScale(2.0f, 2.0f);
            scene.addGameObject(decoration);
        }
    }

    private static void setupMultiplayer(Scanner scanner, GameWorld gameWorld) {
        System.out.println("=== Multiplayer Setup ===");
        System.out.println("1. Host Game");
        System.out.println("2. Join Game");
        System.out.print("Select option (1 or 2): ");
        int multiplayerOption = scanner.nextInt();
        scanner.nextLine(); // consume newline

        try {
            if (multiplayerOption == 1) {
                // Host a game
                int tcpPort = PortUtil.getFreeTcpPort();
                int udpPort = PortUtil.getFreeUdpPort();

                System.out.print("Enter server name: ");
                String serverName = scanner.nextLine();

                System.out.println("Creating server on TCP port " + tcpPort + " and UDP port " + udpPort);
                Server server = GameNetworking.createServer(serverName, gameWorld.getLocalPlayer().getUsername(), tcpPort, udpPort);

                System.out.println("Starting client to connect to your own server...");
                Client client = GameNetworking.createClient("localhost", tcpPort, udpPort);

                // Set up the game world with the network client
                gameWorld.setupNetworking(client);
                Player.setupServer(server);

                System.out.println("Server is running. Tell other players your IP address and ports.");
                System.out.println("TCP Port: " + tcpPort);
                System.out.println("UDP Port: " + udpPort);

            } else if (multiplayerOption == 2) {
                // Join a game
                System.out.print("Enter server IP: ");
                String serverIp = scanner.nextLine();

                System.out.print("Enter TCP port: ");
                int tcpPort = scanner.nextInt();
                scanner.nextLine(); // consume newline

                System.out.print("Enter UDP port: ");
                int udpPort = scanner.nextInt();
                scanner.nextLine(); // consume newline

                System.out.println("Connecting to server at " + serverIp + "...");
                Client client = GameNetworking.createClient(serverIp, tcpPort, udpPort);

                // Set up the game world with the network client
                gameWorld.setupNetworking(client);

                System.out.println("Connected to server successfully!");
            }
        } catch (IOException e) {
            System.out.println("Error setting up multiplayer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Special debug mode that displays all registered sprites
    private static void runDebugMode(Scanner scanner) {
        System.out.println("=== Debug Mode ===");

        // Create and initialize the engine
        Engine engine = new Engine();
        engine.init(1024, 768, "Debug Mode");

        // Create scene
        Scene scene = new Scene();

        // Set up sprite resources
        SpriteManager spriteManager = new SpriteManager();
        EntityRegistry.registerEntities(spriteManager);
        EntityRegistry.registerUi(spriteManager);
        EntityRegistry.registerTiles(spriteManager);

        // Create the game world
        GameWorld gameWorld = new GameWorld(scene, spriteManager, engine.getInput(), engine.getCamera());
        gameWorld.setDebug(true);

        // Set up UI
        UIManagerGameObject uiGameObject = engine.getUIManagerGameObject();
        UIManager uiManager = uiGameObject.getUiManager();

        // Create font resources
        FontSheet fontSheet = new FontSheet();
        fontSheet.setScale(2.0f);
        Shader fontShader = Shader.loadFromFiles("/shaders/sprite.vs.glsl", "/shaders/Fontsh.fs.glsl");

        // Add debug info text
        UIText debugTitle = new UIText(fontSheet, fontShader, "DEBUG MODE - All Registered Sprites", 10, 10);
        uiManager.addComponent(debugTitle, true);

        // Create debug panels
        UIPanel mainPanel = new UIPanel(0, 40, 1024, 728);

        // Add to UI
        uiManager.addComponent(mainPanel, false);
        scene.addGameObject(uiGameObject);

        // Call method to display all registered sprites
        displayAllSprites(scene, spriteManager, engine.getCamera());

        // Set active scene and run
        engine.setActiveScene(scene);
        engine.run();
    }

    // Display all registered sprites in a grid pattern
    private static void displayAllSprites(Scene scene, SpriteManager spriteManager, Camera camera) {
        // Set camera to a good viewing position
        camera.setPosition(0, 0);
        camera.setZoom(0.5f); // Zoom out to see more sprites

        // Place sprites in a grid pattern
        int gridSize = 50;
        int cols = 16;
        int row = 0;
        int col = 0;

        System.out.println("Displaying all registered sprites");

        // Try to get all entity sprites first
        for (int i = 1; i <= 100; i++) {
            try {
                Sprite sprite = spriteManager.getSprite(i);
                if (sprite != null) {
                    float x = col * gridSize;
                    float y = row * gridSize;

                    sprite.setPosition(x, y);
                    sprite.setScale(2.0f, 2.0f);

                    System.out.println("Adding sprite " + i + " at position (" + x + "," + y + ")");
                    scene.addGameObject(sprite);

                    col++;
                    if (col >= cols) {
                        col = 0;
                        row++;
                    }
                }
            } catch (Exception e) {
                // Skip invalid sprites
            }
        }

        // Now try to get all named sprites
        String[] commonNames = {
                "player_sprite_d", "player_sprite_u", "player_sprite_r", "player_sprite_rr",
                "enemy_demon_sprite_d", "enemy_ghost_sprite_d", "enemy_orc_sprite_d",
                "enemy_slime_sprite_d", "dead_p_sprite", "spike_sprite_0", "lamp_post_0",
                "tile_w_1", "tile_l_1", "tile_r_1", "tile_up_1", "tile_down_1",
                "ChestE_id_1", "Emerald_id_1", "ChestH_id_1"
        };

        for (String name : commonNames) {
            try {
                Sprite sprite = spriteManager.getSprite(name);
                if (sprite != null) {
                    float x = col * gridSize;
                    float y = row * gridSize;

                    sprite.setPosition(x, y);
                    sprite.setScale(2.0f, 2.0f);

                    System.out.println("Adding named sprite '" + name + "' at position (" + x + "," + y + ")");
                    scene.addGameObject(sprite);

                    col++;
                    if (col >= cols) {
                        col = 0;
                        row++;
                    }
                }
            } catch (Exception e) {
                // Skip invalid sprites
                System.out.println("Failed to get sprite '" + name + "': " + e.getMessage());
            }
        }
    }

    private static void runLobbySystem(Scanner scanner) {
        System.out.println("=== Game Lobby System ===");

        // Create and initialize the engine for lobby UI
        Engine engine = new Engine();
        engine.init(1024, 768, "Game Lobby");

        // Create scene for lobby
        Scene lobbyScene = new Scene();

        // Get UI manager
        UIManagerGameObject uiGameObject = engine.getUIManagerGameObject();
        UIManager uiManager = uiGameObject.getUiManager();

        // Create font resources
        FontSheet fontSheet = new FontSheet();
        fontSheet.setScale(2.0f);
        Shader fontShader = Shader.loadFromFiles("/shaders/sprite.vs.glsl", "/shaders/Fontsh.fs.glsl");

        // Create lobby system
        GameLobby gameLobby = new GameLobby(uiManager, fontSheet, fontShader);

        // Add UI to scene
        lobbyScene.addGameObject(uiGameObject);

        // Set scene
        engine.setActiveScene(lobbyScene);

        // Start server discovery
        gameLobby.startServerDiscovery();

        // Run lobby menu in console (for simplicity)
        Thread lobbyMenuThread = new Thread(() -> {
            try {
                boolean running = true;

                // Get user info first
                System.out.print("Enter your username: ");
                String username = scanner.nextLine();

                System.out.print("Choose color (RED, BLUE, GREEN, YELLOW, PINK, CYAN): ");
                String color = scanner.nextLine().toUpperCase();

                while (running) {
                    System.out.println("\n=== Lobby Menu ===");
                    System.out.println("1. Refresh Server List");
                    System.out.println("2. Host New Game");
                    System.out.println("3. Join Game");
                    System.out.println("4. Send Chat Message");
                    System.out.println("5. Start Game (Host only)");
                    System.out.println("6. Exit");
                    System.out.print("Select option: ");

                    int option = scanner.nextInt();
                    scanner.nextLine(); // consume newline

                    switch (option) {
                        case 1: // Refresh server list
                            gameLobby.stopServerDiscovery();
                            gameLobby.startServerDiscovery();
                            System.out.println("Refreshing server list...");
                            break;

                        case 2: // Host new game
                            System.out.print("Enter server name: ");
                            String serverName = scanner.nextLine();

                            boolean success = gameLobby.hostGame(serverName, username, color);
                            if (success) {
                                System.out.println("Server created successfully!");
                            } else {
                                System.out.println("Failed to create server.");
                            }
                            break;

                        case 3: // Join game
                            System.out.println("Available Servers:");
                            for (int i = 0; i < gameLobby.getServerCount(); i++) {
                                GameNetworking.DiscoveredServer server = gameLobby.getServerByIndex(i);
                                System.out.println((i+1) + ". " + server.getName() + " - Host: " +
                                        server.getHostUsername() + " - Players: " + server.getPlayerCount());
                            }

                            if (gameLobby.getServerCount() == 0) {
                                System.out.println("No servers found. Try refreshing the server list.");
                            } else {
                                System.out.print("Enter server number to join: ");
                                int serverIndex = scanner.nextInt() - 1;
                                scanner.nextLine(); // consume newline

                                GameNetworking.DiscoveredServer server = gameLobby.getServerByIndex(serverIndex);
                                if (server != null) {
                                    boolean joinSuccess = gameLobby.joinGame(server, username, color);
                                    if (joinSuccess) {
                                        System.out.println("Successfully joined server!");
                                    } else {
                                        System.out.println("Failed to join server.");
                                    }
                                } else {
                                    System.out.println("Invalid server selection.");
                                }
                            }
                            break;

                        case 4: // Send chat message
                            System.out.print("Enter chat message: ");
                            String message = scanner.nextLine();
                            gameLobby.sendChatMessage(message);
                            break;

                        case 5: // Start game (host only)
                            if (gameLobby.isHost()) {
                                System.out.println("Starting game...");
                                // Create a new game engine instance for the actual game
                                Engine gameEngine = new Engine();
                                gameEngine.init(1024, 768, "Game");

                                // Create a new scene
                                Scene gameScene = new Scene();

                                // Set up sprite resources
                                SpriteManager spriteManager = new SpriteManager();
                                EntityRegistry.registerEntities(spriteManager);
                                EntityRegistry.registerUi(spriteManager);
                                EntityRegistry.registerTiles(spriteManager);

                                // Create the game world
                                GameWorld gameWorld = new GameWorld(gameScene, spriteManager, gameEngine.getInput(), gameEngine.getCamera());
                                gameWorld.setDebug(DEBUG_MODE);

                                // Set up UI
                                setupUI(gameEngine, gameWorld, spriteManager);

                                // Create local player with the same username and color
                                Player localPlayer = gameWorld.createLocalPlayer(username, color);

                                // Connect the game world to the lobby client
                                gameWorld.setupNetworking(gameLobby.getClient());

                                // Create terrain
                                createTerrain(gameScene, spriteManager);

                                // Add lights
                                setupLights(gameScene);

                                // Set the active scene
                                gameEngine.setActiveScene(gameScene);

                                // Exit lobby menu thread and lobby engine
                                running = false;

                                // Run the game engine (this will block until the game ends)
                                gameEngine.run();
                            } else {
                                System.out.println("Only the host can start the game.");
                            }
                            break;

                        case 6: // Exit
                            gameLobby.disconnect();
                            gameLobby.stopServerDiscovery();
                            running = false;
                            break;

                        default:
                            System.out.println("Invalid option.");
                            break;
                    }
                }

                // Signal to close the engine window
                System.exit(0);

            } catch (Exception e) {
                System.err.println("Error in lobby menu: " + e.getMessage());
                e.printStackTrace();
            }
        });

        lobbyMenuThread.setDaemon(true);
        lobbyMenuThread.start();

        // Run the engine (this will block until the window is closed)
        engine.run();
    }
}