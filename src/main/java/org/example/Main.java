package org.example;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;
import org.example.engine.*;
import org.example.engine.ecs.ECSManager;
import org.example.engine.ecs.systems.AudioSystem;
import org.example.engine.ecs.systems.OpenALAudioEngine;
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

    private static void testAudio() {
        System.out.println("=== Audio Test ===");

        // Initialize OpenAL audio engine
        OpenALAudioEngine audioEngine = new OpenALAudioEngine();
        AudioSystem audioSystem = new AudioSystem(audioEngine);
        ECSManager.getInstance().addSystem(audioSystem);
        try {
            // Load sound files (make sure these files exist in your resources folder)
            audioEngine.loadSound("beep", "/sounds/aoe_cast.wav");
            audioEngine.loadSound("explosion", "/sounds/boost_cast.wav");
            audioEngine.loadSound("music", "/sounds/pickup_item.wav");

            System.out.println("Sounds loaded successfully");

            // Play a non-positional sound (UI sound)
            int beepId = audioEngine.playSound(
                    "beep",
                    0, 0, 0,  // position doesn't matter for non-3D sounds
                    0.5f,     // volume at 50%
                    1.0f,     // normal pitch
                    false,    // not looping
                    false,    // not 3D
                    1.0f, 10.0f // min/max distance (not used for 2D sounds)
            );

            System.out.println("Playing beep sound (non-positional)");

            // Wait for beep to finish
            Thread.sleep(1000);

            // Play 3D sounds at different positions
            System.out.println("Playing explosion sounds at different positions (3D)");

            // Left position explosion
            int explosionLeftId = audioEngine.playSound(
                    "explosion",
                    -5.0f, 0, 0,  // left position
                    0.8f,         // volume at 80%
                    0.9f,         // slightly lower pitch
                    false,        // not looping
                    true,         // 3D positioned
                    1.0f, 20.0f   // audible from 1-20 units away
            );

            Thread.sleep(500);

            // Right position explosion
            int explosionRightId = audioEngine.playSound(
                    "explosion",
                    5.0f, 0, 0,   // right position
                    0.8f,         // volume at 80%
                    1.1f,         // slightly higher pitch
                    false,        // not looping
                    true,         // 3D positioned
                    1.0f, 20.0f   // audible from 1-20 units away
            );

            Thread.sleep(1000);

            // Play background music (looping)
            int musicId = audioEngine.playSound(
                    "music",
                    0, 0, 0,      // position (doesn't matter for non-3D)
                    0.3f,         // low volume (background music)
                    1.0f,         // normal pitch
                    true,         // looping
                    false,        // not 3D
                    1.0f, 10.0f   // min/max distance (not used for 2D)
            );

            System.out.println("Playing background music (looping)");

            // Move listener around to test 3D audio
            System.out.println("Moving listener to demonstrate 3D sound positioning...");

            for (int i = 0; i < 10; i++) {
                // Move from left to right and back
                float x = (float)Math.sin(i * 0.6) * 10;
                audioEngine.setListenerPosition(x, 0, 0);
                System.out.println("Listener at position: " + x);
                Thread.sleep(500);
            }

            // Stop the music
            audioEngine.stopSound(musicId);
            System.out.println("Stopped background music");

            // Wait a bit more to let other sounds finish
            Thread.sleep(1000);

        } catch (Exception e) {
            System.err.println("Error in audio test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up audio system
            audioEngine.cleanup();
            System.out.println("Audio test complete");
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Main Menu ===");
        System.out.println("1. Run Game Engine");
        System.out.println("2. Game Lobby System");
        System.out.println("3. Debug Mode");
        System.out.println("4. Test Audio");
        System.out.print("Select option (1, 2 or 3): ");
        int option = scanner.nextInt();
        scanner.nextLine(); // consume newline

        if (option == 1) {
            runGameEngine(scanner);
        } else if (option == 2) {
//            runLobbySystem(scanner);
        } else if (option == 3) {
            runDebugMode(scanner);
        }
        else if (option == 4) {
            testAudio();  // Run our new audio test
        }
        else {
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

        engine.setGameWorld(gameWorld);

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
        // Add particle effects to make the scene more lively
        ParticleSystemDemo.addParticleEffectsToScene(scene, spriteManager);
        ParticleSystemDemo.addPlayerTrail(localPlayer, scene, spriteManager);

        // Create a ring of particles at a point of interest
        ParticleSystemDemo.createParticleRing(scene, spriteManager, 400, 300, 150, 8);

        // Draw all registered sprites in debug mode
        if (DEBUG_MODE) {
            System.out.println("Drawing all registered sprites for debugging");
            gameWorld.drawAllRegisteredSprites();
        }
        //gameWorld.createTerrain(scene, spriteManager);

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

        gameWorld.fixSpellRendering();

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

        if (gameWorld.getLocalPlayer() != null) {
            // Create spell UI for local player
            SpellUI spellUI = new SpellUI(10, 70, 300, 120,
                    gameWorld, gameWorld.getLocalPlayer().getPlayerId(),
                    spriteManager, fontSheet, fontShader);
            uiManager.addComponent(spellUI, true);
        }

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
        int tileSize = 16;
        int gridWidth = 2;
        int gridHeight = 2;

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

        UIParticleIntegration.UIParticleButton fancyButton = new UIParticleIntegration.UIParticleButton(
                100, 100, 200, 50,
                "button_up_1", "Click Me",
                fontSheet, fontShader,
                spriteManager, engine.getInput(), scene
        );

        // Example: Create a particle-enhanced text element
        UIParticleIntegration.UIParticleText fancyText = new UIParticleIntegration.UIParticleText(
                fontSheet, fontShader, "Fancy Text",
                300, 200, spriteManager, scene
        );

        // Add to UI manager
        uiManager.addComponent(fancyButton, true);
        uiManager.addComponent(fancyText, true);

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



}