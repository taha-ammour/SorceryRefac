package org.example.testF;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;
import org.example.GameNetworking;
import org.example.GameWorld;
import org.example.GameWorldObject;
import org.example.PortUtil;
import org.example.engine.*;
import org.example.engine.ecs.ECSManager;
import org.example.engine.ecs.Entity;
import org.example.engine.ecs.components.AudioComponent;
import org.example.engine.ecs.components.TransformComponent;
import org.example.engine.ecs.systems.AudioSystem;
import org.example.engine.ecs.systems.OpenALAudioEngine;
import org.example.game.Player;
import org.example.ui.UIManager;
import org.example.ui.UIManagerGameObject;
import org.example.ui.UIText;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

import static org.lwjgl.glfw.GLFW.*;

public class AudioRunTest {
    private static OpenALAudioEngine audioEngine;
    private static AudioSystem audioSystem;
    private static boolean musicPlaying = false;
    private static int musicSourceId = -1;

    // Audio-enabled entity IDs for tracking
    private static UUID lampPostEntityId;
    private static UUID chestEntityId;
    private static UUID enemyEntityId;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Main Menu ===");
        System.out.println("1. Run Game Engine");
        System.out.println("2. Game Lobby System");
        System.out.println("3. Debug Mode");
        System.out.println("4. Test Audio");
        System.out.print("Select option (1-4): ");
        int option = scanner.nextInt();
        scanner.nextLine(); // consume newline

        if (option == 1) {
            runGameEngine(scanner);
        } else if (option == 2) {
            // runLobbySystem(scanner);
        } else if (option == 3) {
            runDebugMode(scanner);
        } else if (option == 4) {
            testAudio();
        } else {
            System.out.println("Invalid option. Exiting.");
        }

        scanner.close();
    }

    private static void runGameEngine(Scanner scanner) {
        System.out.println("=== Game Engine With Audio ===");
        System.out.println("1. Single Player");
        System.out.println("2. Multiplayer");
        System.out.print("Select mode (1 or 2): ");
        int gameMode = scanner.nextInt();
        scanner.nextLine(); // consume newline

        // Create and initialize the engine with window dimensions and a title
        Engine engine = new Engine();
        engine.init(800, 600, "Game Engine with Audio");

        // Create a new scene
        Scene scene = new Scene();

        // Set up sprite resources
        SpriteManager spriteManager = new SpriteManager();
        EntityRegistry.registerEntities(spriteManager);
        EntityRegistry.registerUi(spriteManager);
        EntityRegistry.registerTiles(spriteManager);

        // Initialize audio system
        initializeAudioSystem();

        // Create the game world with scene, sprite manager, input and camera
        GameWorld gameWorld = new GameWorld(scene, spriteManager, engine.getInput(), engine.getCamera());
        gameWorld.setDebug(false);

        // Set up UI with audio information
        setupUIWithAudio(engine, gameWorld, spriteManager);

        // Create the local player
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        System.out.print("Choose color (RED, BLUE, GREEN, YELLOW, PINK, CYAN): ");
        String color = scanner.nextLine().toUpperCase();

        Player localPlayer = gameWorld.createLocalPlayer(username, color);

        // Create some terrain tiles with audio
        createTerrainWithAudio(scene, spriteManager);

        // Add lights to the scene
        setupLights(scene);

        // Add a sound controller object to handle input for audio
        SoundControllerObject soundController = new SoundControllerObject(engine.getInput(), localPlayer);
        scene.addGameObject(soundController);

        // Display audio controls info
        System.out.println("\n--- Audio Controls ---");
        System.out.println("WASD - Move player (automatic footstep sounds)");
        System.out.println("SPACE - Play action sound");
        System.out.println("M - Toggle background music");
        System.out.println("1/2/3 - Interact with lamp/chest/enemy sounds");
        System.out.println("--------------------\n");

        // Set up multiplayer if selected
        if (gameMode == 2) {
            setupMultiplayer(scanner, gameWorld);
        }

        // Set the active scene and start the engine's main loop
        engine.setActiveScene(scene);

        // Add game world object for updates
        GameWorldObject gameWorldObject = new GameWorldObject(gameWorld);
        scene.addGameObject(gameWorldObject);

        // Start the engine
        engine.setGameWorld(gameWorld);
        engine.run();

        // Clean up audio resources
        if (audioEngine != null) {
            audioEngine.cleanup();
        }
    }

    private static void initializeAudioSystem() {
        // Create audio engine and system
        audioEngine = new OpenALAudioEngine();
        audioSystem = new AudioSystem(audioEngine);

        // Add audio system to ECS
        ECSManager.getInstance().addSystem(audioSystem);

        // Load sound resources (using fake paths - you'll need to add real WAV files)
        try {
            audioEngine.loadSound("footstep", "/sounds/aoe_cast.wav");
            audioEngine.loadSound("ambient", "/sounds/boost_cast.wav");
            audioEngine.loadSound("chest", "/sounds/pickup_item.wav");
            audioEngine.loadSound("enemy", "/sounds/aoe_cast.wav");
            audioEngine.loadSound("action", "/sounds/pickup_item.wav");

            System.out.println("Sound resources loaded");
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
        }
    }

    private static void createTerrainWithAudio(Scene scene, SpriteManager spriteManager) {
        // Create a grid of floor tiles
        int tileSize = 32; // Set a larger tile size
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

        // Add some decorative elements with audio components
        // Lamp post with ambient sound
        Sprite lampPost = spriteManager.getSprite("lamp_post_0");
        lampPost.setPosition(200, 200);
        lampPost.setScale(2.0f, 2.0f);
        scene.addGameObject(lampPost);

        // Create entity with audio component for lamp post
        Entity lampEntity = new Entity("lamp_sound");
        lampEntity.addComponent(new TransformComponent(200, 200, 0));

        AudioComponent lampAudio = new AudioComponent("ambient");
        lampAudio.setVolume(0.3f);
        lampAudio.setPitch(1.0f);
        lampAudio.setLooping(true);
        lampAudio.setAutoPlay(true);
        lampAudio.set3D(true);
        lampAudio.setMinDistance(50.0f);
        lampAudio.setMaxDistance(300.0f);
        lampEntity.addComponent(lampAudio);

        ECSManager.getInstance().addEntity(lampEntity);
        lampPostEntityId = lampEntity.getId();

        // Add a chest with sound
        Sprite chest = spriteManager.getSprite("ChestE_id_1");
        chest.setPosition(400, 300);
        chest.setScale(2.0f, 2.0f);
        scene.addGameObject(chest);

        // Create entity with audio component for chest
        Entity chestEntity = new Entity("chest_sound");
        chestEntity.addComponent(new TransformComponent(400, 300, 0));

        AudioComponent chestAudio = new AudioComponent("chest");
        chestAudio.setVolume(0.5f);
        chestAudio.setPitch(1.0f);
        chestAudio.setLooping(false);
        chestAudio.setAutoPlay(false); // Only plays when interacted with
        chestAudio.set3D(true);
        chestEntity.addComponent(chestAudio);

        ECSManager.getInstance().addEntity(chestEntity);
        chestEntityId = chestEntity.getId();

        // Add an enemy with sound
        Sprite enemy = spriteManager.getSprite("enemy_demon_sprite_d");
        enemy.setPosition(500, 100);
        enemy.setScale(2.0f, 2.0f);
        scene.addGameObject(enemy);

        // Create entity with audio component for enemy
        Entity enemyEntity = new Entity("enemy_sound");
        enemyEntity.addComponent(new TransformComponent(500, 100, 0));

        AudioComponent enemyAudio = new AudioComponent("enemy");
        enemyAudio.setVolume(0.7f);
        enemyAudio.setPitch(0.9f);
        enemyAudio.setLooping(false);
        enemyAudio.setAutoPlay(false); // Only plays when interacted with
        enemyAudio.set3D(true);
        enemyEntity.addComponent(enemyAudio);

        ECSManager.getInstance().addEntity(enemyEntity);
        enemyEntityId = enemyEntity.getId();
    }

    private static void setupUIWithAudio(Engine engine, GameWorld gameWorld, SpriteManager spriteManager) {
        // Get UI manager from engine
        UIManagerGameObject uiGameObject = engine.getUIManagerGameObject();
        UIManager uiManager = uiGameObject.getUiManager();

        // Create font resources with adjusted scale
        FontSheet fontSheet = new FontSheet();
        fontSheet.setScale(2.0f);

        Shader fontShader = Shader.loadFromFiles("/shaders/sprite.vs.glsl", "/shaders/Fontsh.fs.glsl");

        // Create audio status text
        UIText audioStatusText = new UIText(fontSheet, fontShader, "Audio: Enabled", 10, 30);

        // Add UI elements to the manager
        uiManager.addComponent(audioStatusText, true);

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

        // Create a point light
        Light pointLight = new Light();
        pointLight.setPosition(new Vector3f(400.0f, 300.0f, 5.0f));
        pointLight.setColor(new Vector3f(0.0f, 1.0f, 1.0f));
        pointLight.setIntensity(50.0f);
        pointLight.setConstant(0.5f);
        pointLight.setLinear(0.5f);
        pointLight.setQuadratic(0.05f);
        pointLight.setType(1);
        pointLight.setDirection(new Vector3f(0.0f, 0.0f, -1.0f).normalize());
        LightObject pointLightObject = new LightObject(pointLight);
        scene.addGameObject(pointLightObject);
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

    private static void runDebugMode(Scanner scanner) {
        // Your existing implementation
    }

    private static void testAudio() {
        System.out.println("=== Audio Test ===");

        // Initialize OpenAL audio engine
        OpenALAudioEngine audioEngine = new OpenALAudioEngine();
        AudioSystem audioSystem = new AudioSystem(audioEngine);

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

    // Sound controller object to handle input and play sounds
    private static class SoundControllerObject extends GameObject {
        private Input input;
        private Player player;
        private float footstepTimer = 0;
        private final float footstepInterval = 0.4f; // seconds between footsteps
        private boolean wasMoving = false;

        public SoundControllerObject(Input input, Player player) {
            this.input = input;
            this.player = player;
        }

        @Override
        public void update(float deltaTime) {
            // Update camera with player position for 3D audio
            Vector3f playerPos = player.getPosition();
            audioEngine.setListenerPosition(playerPos.x, playerPos.y, playerPos.z);

            // Check for player movement to play footsteps
            boolean isMoving = input.isKeyDown(GLFW_KEY_W) || input.isKeyDown(GLFW_KEY_A) ||
                    input.isKeyDown(GLFW_KEY_S) || input.isKeyDown(GLFW_KEY_D);

            if (isMoving) {
                footstepTimer += deltaTime;
                if (footstepTimer >= footstepInterval) {
                    audioEngine.playSound(
                            "footstep",
                            playerPos.x, playerPos.y, playerPos.z,
                            0.3f,
                            0.9f + (float)(Math.random() * 0.2f), // random pitch variation
                            false,
                            true,
                            5.0f, 50.0f
                    );
                    footstepTimer = 0;
                }
            } else {
                footstepTimer = footstepInterval; // Ready to play immediately when moving starts
            }

            wasMoving = isMoving;

            // Toggle background music with M key
            if (input.isKeyJustPressed(GLFW_KEY_M)) {
                if (musicPlaying && musicSourceId != -1) {
                    audioEngine.stopSound(musicSourceId);
                    musicPlaying = false;
                    System.out.println("Background music stopped");
                } else {
                    musicSourceId = audioEngine.playSound(
                            "ambient",
                            0, 0, 0,
                            0.2f,
                            1.0f,
                            true,
                            false,
                            0, 0
                    );
                    musicPlaying = true;
                    System.out.println("Background music started");
                }
            }

            // Play action sound with Space
            if (input.isKeyJustPressed(GLFW_KEY_SPACE)) {
                audioEngine.playSound(
                        "action",
                        0, 0, 0,
                        0.7f,
                        1.0f,
                        false,
                        false,
                        0, 0
                );
                System.out.println("Action sound played");
            }

            // Interact with specific objects using number keys
            if (input.isKeyJustPressed(GLFW_KEY_1)) {
                // Play lamp sound
                Entity lampEntity = ECSManager.getInstance().getEntity(lampPostEntityId);
                if (lampEntity != null) {
                    AudioComponent audioComp = lampEntity.getComponent(AudioComponent.class);
                    if (audioComp != null) {
                        audioSystem.playSound(lampEntity);
                        System.out.println("Lamp sound played");
                    }
                }
            }

            if (input.isKeyJustPressed(GLFW_KEY_2)) {
                // Play chest sound
                Entity chestEntity = ECSManager.getInstance().getEntity(chestEntityId);
                if (chestEntity != null) {
                    AudioComponent audioComp = chestEntity.getComponent(AudioComponent.class);
                    if (audioComp != null) {
                        audioSystem.playSound(chestEntity);
                        System.out.println("Chest sound played");
                    }
                }
            }

            if (input.isKeyJustPressed(GLFW_KEY_3)) {
                // Play enemy sound
                Entity enemyEntity = ECSManager.getInstance().getEntity(enemyEntityId);
                if (enemyEntity != null) {
                    AudioComponent audioComp = enemyEntity.getComponent(AudioComponent.class);
                    if (audioComp != null) {
                        audioSystem.playSound(enemyEntity);
                        System.out.println("Enemy sound played");
                    }
                }
            }
        }

        @Override
        public void render(org.joml.Matrix4f viewProjectionMatrix) {
            // No visual rendering
        }

        @Override
        public void cleanup() {
            // Stop background music if playing
            if (musicPlaying && musicSourceId != -1) {
                audioEngine.stopSound(musicSourceId);
            }
        }
    }
}