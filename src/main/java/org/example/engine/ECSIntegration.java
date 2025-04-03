package org.example.engine;

import org.example.engine.ecs.ECSManager;
import org.example.engine.ecs.Entity;
import org.example.engine.ecs.components.*;
import org.example.engine.ecs.systems.*;
import org.example.game.Direction;
import org.example.game.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.esotericsoftware.kryonet.Client;
import org.example.engine.utils.Logger;

import java.util.*;

/**
 * Integration class that bridges between existing engine classes and the new ECS architecture
 */
public class ECSIntegration {
    // Singleton instance
    private static ECSIntegration instance;
    private final ECSManager ecsManager;
    private final Scene scene;
    private final SpriteManager spriteManager;
    private final Camera camera;
    private final Input input;

    private RenderSystem renderSystem;
    private AnimationSystem animationSystem;
    private PlayerInputSystem playerInputSystem;
    private MovementSystem movementSystem;
    private CollisionSystem collisionSystem;
    private NetworkSystem networkSystem;
    private LightSystem lightSystem;
    private InteractionSystem interactionSystem;
    private AudioSystem audioSystem;

    /**
     * Get the singleton instance
     */
    public static ECSIntegration getInstance() {
        return instance;
    }

    /**
     * Initialize the singleton instance
     */
    public static void initialize(Scene scene, SpriteManager spriteManager, Camera camera, Input input) {
        if (instance == null) {
            instance = new ECSIntegration(scene, spriteManager, camera, input);
        }
    }

    /**
     * Creates the integration between engine and ECS
     */
    private ECSIntegration(Scene scene, SpriteManager spriteManager, Camera camera, Input input) {
        this.ecsManager = ECSManager.getInstance();
        this.scene = scene;
        this.spriteManager = spriteManager;
        this.camera = camera;
        this.input = input;

        // Create ECS systems
        setupSystems();
    }

    /**
     * Set up all ECS systems
     */
    private void setupSystems() {
        // Create systems with proper priorities
        renderSystem = new RenderSystem(camera.getViewProjectionMatrix());
        animationSystem = new AnimationSystem();
        playerInputSystem = new PlayerInputSystem(input);
        movementSystem = new MovementSystem();
        collisionSystem = new CollisionSystem();
        lightSystem = new LightSystem();
        interactionSystem = new InteractionSystem(input);

        // Initialize audio system if we have an audio engine
        try {
            // Try to create a basic implementation of AudioEngine
            AudioSystem.AudioEngine audioEngine = createAudioEngine();
            audioSystem = new AudioSystem(audioEngine);
        } catch (Exception e) {
            Logger.error("Failed to initialize audio system: " + e.getMessage());
            audioSystem = null;
        }

        // Add systems to ECS manager in order of execution
        ecsManager.addSystem(playerInputSystem);
        ecsManager.addSystem(interactionSystem);
        ecsManager.addSystem(movementSystem);
        ecsManager.addSystem(collisionSystem);
        ecsManager.addSystem(animationSystem);
        ecsManager.addSystem(lightSystem);

        // Add audio system if available
        if (audioSystem != null) {
            ecsManager.addSystem(audioSystem);
        }

        // Add render system last
        ecsManager.addSystem(renderSystem);
    }

    /**
     * Create a basic implementation of the AudioEngine interface
     * This is a placeholder that logs operations instead of actually playing sounds
     */
    private AudioSystem.AudioEngine createAudioEngine() {
        return new AudioSystem.AudioEngine() {
            private final Map<String, String> loadedSounds = new HashMap<>();
            private final Map<Integer, Boolean> playingSounds = new HashMap<>();
            private int nextSourceId = 1;

            @Override
            public int playSound(String soundId, float x, float y, float z, float volume, float pitch,
                                 boolean looping, boolean is3D, float minDistance, float maxDistance) {
                Logger.info("Playing sound: " + soundId + " at (" + x + "," + y + "," + z + ")");
                int sourceId = nextSourceId++;
                playingSounds.put(sourceId, true);
                return sourceId;
            }

            @Override
            public boolean isPlaying(int sourceId) {
                return playingSounds.getOrDefault(sourceId, false);
            }

            @Override
            public void stopSound(int sourceId) {
                Logger.info("Stopping sound: " + sourceId);
                playingSounds.put(sourceId, false);
            }

            @Override
            public void setSourcePosition(int sourceId, float x, float y, float z) {
                // Just a placeholder
            }

            @Override
            public void setSourceVolume(int sourceId, float volume) {
                // Just a placeholder
            }

            @Override
            public void setSourcePitch(int sourceId, float pitch) {
                // Just a placeholder
            }

            @Override
            public void setListenerPosition(float x, float y, float z) {
                // Just a placeholder
            }

            @Override
            public void loadSound(String soundId, String filePath) {
                Logger.info("Loading sound: " + soundId + " from " + filePath);
                loadedSounds.put(soundId, filePath);
            }

            @Override
            public void unloadSound(String soundId) {
                Logger.info("Unloading sound: " + soundId);
                loadedSounds.remove(soundId);
            }
        };
    }

    /**
     * Called each frame to update the ECS systems
     */
    public void update(float deltaTime) {
        // Update camera data for rendering system
        renderSystem = ecsManager.getSystem(RenderSystem.class);
        if (renderSystem != null) {
            // TODO: Update view-projection matrix if needed
        }

        // Update ECS
        ecsManager.update(deltaTime);
    }

    /**
     * Convert an existing Player to an ECS Entity
     */
    public Entity convertPlayerToEntity(Player player) {
        // Create a new entity with the same UUID as the player
        UUID playerId = player.getPlayerId();
        Entity entity = new Entity(player.getUsername());

        // Add components
        TransformComponent transform = new TransformComponent(
                player.getPosition().x,
                player.getPosition().y,
                player.getPosition().z
        );
        entity.addComponent(transform);

        // Create sprite component
        SpriteComponent spriteComponent = new SpriteComponent(
                spriteManager.getSprite("player_sprite_d")
        );
        // Set color if needed
        entity.addComponent(spriteComponent);

        // Create player component
        PlayerComponent playerComponent = new PlayerComponent(
                player.getUsername(),
                player.getColor(),
                player == GameEngine.getLocalPlayer()
        );
        playerComponent.setNetworkId(playerId);
        entity.addComponent(playerComponent);

        // Create movement component
        MovementComponent movementComponent = new MovementComponent(player.getMoveSpeed());

        // Set initial direction
        int direction = 0; // Down by default
        Direction playerDirection = player.getCurrentDirection();
        if (playerDirection != null) {
            switch (playerDirection) {
                case UP:
                    direction = 1;
                    break;
                case DOWN:
                    direction = 0;
                    break;
                case RIGHT:
                    direction = 2;
                    break;
                case LEFT:
                    direction = 3;
                    break;
            }
        }
        movementComponent.setDirection(direction);
        entity.addComponent(movementComponent);

        // Create collision component
        CollisionComponent collisionComponent = new CollisionComponent(32, 32);
        entity.addComponent(collisionComponent);

        // Create network component if it's a networked game
        if (GameEngine.isNetworkGame()) {
            NetworkComponent networkComponent = new NetworkComponent(playerId);
            entity.addComponent(networkComponent);
        }

        // Add health component
        HealthComponent healthComponent = new HealthComponent(player.getHealth());
        entity.addComponent(healthComponent);

        // Add to ECS manager
        ecsManager.addEntity(entity);

        return entity;
    }

    /**
     * Create a new player entity from scratch
     */
    public Entity createPlayerEntity(String username, String color, float x, float y, float z, boolean isLocalPlayer) {
        Entity entity = new Entity(username);
        UUID playerId = UUID.randomUUID();

        // Add transform component
        TransformComponent transform = new TransformComponent(x, y, z);
        entity.addComponent(transform);

        // Create sprite component
        Sprite playerSprite = spriteManager.getSprite("player_sprite_d");
        SpriteComponent spriteComponent = new SpriteComponent(playerSprite);

        // Set palette based on color
        String[] palette = getColorPalette(color);
        spriteComponent.setPalette(palette);
        entity.addComponent(spriteComponent);

        // Create player component
        PlayerComponent playerComponent = new PlayerComponent(username, color, isLocalPlayer);
        playerComponent.setNetworkId(playerId);
        entity.addComponent(playerComponent);

        // Create movement component
        MovementComponent movementComponent = new MovementComponent(120.0f);
        entity.addComponent(movementComponent);

        // Create collision component
        CollisionComponent collisionComponent = new CollisionComponent(32, 32);
        entity.addComponent(collisionComponent);

        // Create network component if it's a networked game
        if (GameEngine.isNetworkGame()) {
            NetworkComponent networkComponent = new NetworkComponent(playerId);
            entity.addComponent(networkComponent);
        }

        // Add health component
        HealthComponent healthComponent = new HealthComponent(100);
        entity.addComponent(healthComponent);

        // Add inventory component
        InventoryComponent inventoryComponent = new InventoryComponent();
        entity.addComponent(inventoryComponent);

        // Add to ECS manager
        ecsManager.addEntity(entity);

        return entity;
    }

    /**
     * Create a tile entity in the ECS
     */
    public Entity createTileEntity(int tileId, float x, float y, float z, boolean isWalkable) {
        Entity entity = new Entity("tile_" + tileId);

        // Add transform component
        TransformComponent transform = new TransformComponent(x, y, z);
        entity.addComponent(transform);

        // Get the sprite based on tile ID
        String spriteName = "tile_w_1"; // Default tile

        // Map tile IDs to sprite names (extend this based on your tile definitions)
        switch(tileId) {
            case 1:
                spriteName = "tile_w_1";
                break;
            case 2:
                spriteName = "tile_l_1";
                break;
            case 3:
                spriteName = "tile_r_1";
                break;
            // Add more mappings as needed
        }

        // Create sprite component
        Sprite tileSprite = spriteManager.getSprite(spriteName);
        SpriteComponent spriteComponent = new SpriteComponent(tileSprite);
        entity.addComponent(spriteComponent);

        // Add tile component
        TileComponent tileComponent = new TileComponent(tileId);
        tileComponent.setWalkable(isWalkable);
        entity.addComponent(tileComponent);

        // Add collision component if not walkable
        if (!isWalkable) {
            CollisionComponent collisionComponent = new CollisionComponent(32, 32);
            collisionComponent.setCollisionLayer("tile");
            entity.addComponent(collisionComponent);
        }

        // Add to ECS manager
        ecsManager.addEntity(entity);

        return entity;
    }

    /**
     * Create a light entity in the ECS
     */
    public Entity createLightEntity(float x, float y, float z, int color, float intensity, float radius, int lightType) {
        Entity entity = new Entity("light");

        // Add transform component
        TransformComponent transform = new TransformComponent(x, y, z);
        entity.addComponent(transform);

        // Add light component
        LightComponent lightComponent = new LightComponent(color, intensity, radius);
        lightComponent.setLightType(lightType);
        entity.addComponent(lightComponent);

        // Add to ECS manager
        ecsManager.addEntity(entity);

        return entity;
    }

    /**
     * Create an interactable entity in the ECS
     */
    public Entity createInteractableEntity(String spriteName, float x, float y, float z,
                                           String interactionType, String interactionData) {
        Entity entity = new Entity("interactable_" + interactionType);

        // Add transform component
        TransformComponent transform = new TransformComponent(x, y, z);
        entity.addComponent(transform);

        // Create sprite component
        Sprite interactableSprite = spriteManager.getSprite(spriteName);
        SpriteComponent spriteComponent = new SpriteComponent(interactableSprite);
        entity.addComponent(spriteComponent);

        // Add interactable component
        InteractableComponent interactableComponent = new InteractableComponent(interactionType);
        interactableComponent.setInteractionData(interactionData);
        entity.addComponent(interactableComponent);

        // Add collision component (as a trigger)
        CollisionComponent collisionComponent = new CollisionComponent(32, 32);
        collisionComponent.setTrigger(true);
        collisionComponent.setCollisionLayer("interactable");
        entity.addComponent(collisionComponent);

        // Add to ECS manager
        ecsManager.addEntity(entity);

        return entity;
    }

    /**
     * Create a particle effect entity in the ECS
     */
    public Entity createParticleEffectEntity(int effectType, float x, float y, float z, int color) {
        Entity entity = new Entity("particle_effect");

        // Add transform component
        TransformComponent transform = new TransformComponent(x, y, z);
        entity.addComponent(transform);

        // Add particle effect component
        ParticleEffectComponent particleComponent = new ParticleEffectComponent(effectType);
        particleComponent.setColor(color);
        entity.addComponent(particleComponent);

        // Add to ECS manager
        ecsManager.addEntity(entity);

        return entity;
    }

    /**
     * Create a sound entity in the ECS
     */
    public Entity createSoundEntity(String soundId, float x, float y, float z, boolean is3D, boolean looping) {
        Entity entity = new Entity("sound_" + soundId);

        // Add transform component
        TransformComponent transform = new TransformComponent(x, y, z);
        entity.addComponent(transform);

        // Add audio component
        AudioComponent audioComponent = new AudioComponent(soundId);
        audioComponent.setLooping(looping);
        audioComponent.set3D(is3D);
        audioComponent.setAutoPlay(true);
        entity.addComponent(audioComponent);

        // Add to ECS manager
        ecsManager.addEntity(entity);

        return entity;
    }

    /**
     * Create a whole level from a tile map
     * @param tileMap 2D array of tile IDs
     * @param tileSize Size of each tile
     * @return List of all created entities
     */
    public List<Entity> createLevelFromTileMap(int[][] tileMap, int tileSize) {
        List<Entity> entities = new ArrayList<>();

        // Process tile map
        for (int y = 0; y < tileMap.length; y++) {
            for (int x = 0; x < tileMap[y].length; x++) {
                int tileId = tileMap[y][x];

                // Skip empty tiles (0)
                if (tileId == 0) continue;

                // Determine if tile is walkable (example: odd numbers are walkable)
                boolean isWalkable = (tileId % 2 == 1);

                // Create tile entity
                Entity tileEntity = createTileEntity(
                        tileId,
                        x * tileSize,
                        y * tileSize,
                        0,
                        isWalkable
                );

                entities.add(tileEntity);
            }
        }

        return entities;
    }

    /**
     * Create an enemy entity in the ECS
     */
    public Entity createEnemyEntity(String enemyType, float x, float y, float z, int health, float moveSpeed) {
        Entity entity = new Entity("enemy_" + enemyType);

        // Add transform component
        TransformComponent transform = new TransformComponent(x, y, z);
        entity.addComponent(transform);

        // Create sprite component based on enemy type
        String spriteName = getEnemySpriteNameByType(enemyType);
        Sprite enemySprite = spriteManager.getSprite(spriteName);
        SpriteComponent spriteComponent = new SpriteComponent(enemySprite);
        entity.addComponent(spriteComponent);

        // Add movement component
        MovementComponent movementComponent = new MovementComponent(moveSpeed);
        entity.addComponent(movementComponent);

        // Add collision component
        CollisionComponent collisionComponent = new CollisionComponent(32, 32);
        collisionComponent.setCollisionLayer("enemy");
        entity.addComponent(collisionComponent);

        // Add health component
        HealthComponent healthComponent = new HealthComponent(health);
        entity.addComponent(healthComponent);

        // Add AI component
        AIComponent aiComponent = new AIComponent(AIComponent.AIType.PATROL);
        aiComponent.setTargetTag("player");
        entity.addComponent(aiComponent);

        // Add to ECS manager
        ecsManager.addEntity(entity);

        return entity;
    }

    /**
     * Helper method to map enemy types to sprite names
     */
    private String getEnemySpriteNameByType(String enemyType) {
        switch (enemyType.toLowerCase()) {
            case "demon":
                return "enemy_demon_sprite_d";
            case "ghost":
                return "enemy_ghost_sprite_d";
            case "orc":
                return "enemy_orc_sprite_d";
            case "slime":
                return "enemy_slime_sprite_d";
            default:
                return "enemy_demon_sprite_d"; // Default
        }
    }

    /**
     * Set up network system with KryoNet client
     */
    public void setupNetworking(Client client) {
        if (client != null && client.isConnected()) {
            networkSystem = new NetworkSystem(client);
            ecsManager.addSystem(networkSystem);
        }
    }

    /**
     * Clear all entities and reset ECS
     */
    public void reset() {
        ecsManager.clear();
        setupSystems();
    }

    /**
     * Convenience method to get color palette based on color name
     */
    private String[] getColorPalette(String color) {
        switch (color.toUpperCase()) {
            case "RED":
                return new String[]{"000", "500", "300", "555"};
            case "BLUE":
                return new String[]{"000", "005", "003", "555"};
            case "GREEN":
                return new String[]{"000", "050", "030", "555"};
            case "YELLOW":
                return new String[]{"000", "550", "530", "555"};
            case "PINK":
                return new String[]{"000", "505", "303", "555"};
            case "ORANGE":
                return new String[]{"000", "530", "320", "555"};
            case "PURPLE":
                return new String[]{"000", "305", "203", "555"};
            case "CYAN":
                return new String[]{"000", "055", "033", "555"};
            case "BLACK":
                return new String[]{"000", "111", "222", "555"};
            case "WHITE":
                return new String[]{"000", "444", "333", "555"};
            default:
                return new String[]{"000", "333", "222", "555"};
        }
    }
}