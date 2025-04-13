package org.example.engine;

import org.example.GameWorld;
import org.example.engine.GameObject;
import org.example.engine.Scene;
import org.example.engine.collision.*;
import org.example.game.Player;
import org.example.game.SpellEntity;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages collisions between game objects like players and spells
 */
public class GameCollisionManager extends GameObject {
    private final GameWorld gameWorld;
    private final CollisionSystem collisionSystem;
    private final Map<GameObject, Collider> colliderMap = new HashMap<>();
    private boolean debug = false;

    public GameCollisionManager(GameWorld gameWorld, Scene scene) {
        this.gameWorld = gameWorld;
        this.collisionSystem = new CollisionSystem(scene);

        // Set up default collision layers and rules
        setupCollisionLayers();
    }

    /**
     * Configure the collision layers and rules
     */
    private void setupCollisionLayers() {
        // Define standard collision layers if they don't exist
        if (collisionSystem.getLayer("Player") == null) {
            // Already create default layers in the CollisionSystem constructor
            // but we can customize additional layers here if needed

            // Make sure player-spell collisions are enabled
            collisionSystem.setLayerCollision(2, 4, true); // Player (2) - Projectile (4)
        }

        // Set response strength (how strongly objects push apart when colliding)
        collisionSystem.setResponseStrength(0.8f);
    }

    /**
     * Register a player with the collision system
     */
    public void registerPlayer(Player player) {
        if (player == null || colliderMap.containsKey(player)) return;

        try {
            // Create a box collider for the player
            BoxCollider collider = new BoxCollider(32, 32);

            // Center the collider on the player sprite
            collider.setOffset(-16, -16);

            // Set the collider's layer to Player (layer 2)
            collider.setLayer(4); // Player layer

            // Register with collision system
            collisionSystem.registerCollider(player, collider);

            // Keep track of this association
            colliderMap.put(player, collider);

            if (debug) {
                System.out.println("Registered player collision: " + player.getPlayerId());
            }
        } catch (Exception e) {
            System.err.println("Error registering player collision: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Register a spell with the collision system
     */
    public void registerSpell(SpellEntity spell) {
        if (spell == null || colliderMap.containsKey(spell)) return;

        try {
            // Create a circle collider for the spell
            CircleCollider collider = new CircleCollider(12f);

            // Set the collider's layer to Projectile (layer 4)
            collider.setLayer(4); // Projectile layer

            // Make spells act as triggers
            collider.setTrigger(true);

            // Register with collision system
            collisionSystem.registerCollider(spell, collider);

            // Keep track of this association
            colliderMap.put(spell, collider);

            if (debug) {
                System.out.println("Registered spell collision for " + spell.getSpellType());
            }
        } catch (Exception e) {
            System.err.println("Error registering spell collision: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Unregister a game object from the collision system
     */
    public void unregisterObject(GameObject object) {
        if (object == null || !colliderMap.containsKey(object)) return;

        try {
            collisionSystem.unregisterCollider(object);
            colliderMap.remove(object);

            if (debug) {
                String objectType = object instanceof Player ? "player" :
                        (object instanceof SpellEntity ? "spell" : "object");
                System.out.println("Unregistered " + objectType + " collision");
            }
        } catch (Exception e) {
            System.err.println("Error unregistering collision: " + e.getMessage());
        }
    }

    /**
     * Called when the game world adds a new player
     */
    public void onPlayerAdded(Player player) {
        registerPlayer(player);
    }

    /**
     * Called when the game world adds a new spell
     */
    public void onSpellAdded(SpellEntity spell) {
        registerSpell(spell);
    }

    /**
     * Called when a game object is removed
     */
    public void onGameObjectRemoved(GameObject object) {
        unregisterObject(object);
    }

    @Override
    public void update(float deltaTime) {
        // Process all players in the game
        if (gameWorld.getLocalPlayer() != null) {
            ensureRegistered(gameWorld.getLocalPlayer());
        }

        for (UUID playerId : gameWorld.getRemotePlayers().keySet()) {
            Player player = gameWorld.getRemotePlayers().get(playerId);
            if (player != null) {
                ensureRegistered(player);
            }
        }

        // Update the collision system
        collisionSystem.update(deltaTime);
    }

    /**
     * Ensure an object is registered with the collision system
     */
    private void ensureRegistered(GameObject object) {
        if (!colliderMap.containsKey(object)) {
            if (object instanceof Player) {
                registerPlayer((Player)object);
            } else if (object instanceof SpellEntity) {
                registerSpell((SpellEntity)object);
            }
        }
    }

    @Override
    public void render(org.joml.Matrix4f viewProjectionMatrix) {
        // Optionally render debug visualization of colliders
        if (debug && collisionSystem != null) {
            collisionSystem.setDebugDraw(true);
            // Note: The actual rendering would be done in the CollisionSystem
        }
    }

    @Override
    public void cleanup() {
        if (collisionSystem != null) {
            collisionSystem.cleanup();
        }
        colliderMap.clear();
    }

    /**
     * Set debug mode
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
        if (collisionSystem != null) {
            collisionSystem.setDebugDraw(debug);
        }
    }
}