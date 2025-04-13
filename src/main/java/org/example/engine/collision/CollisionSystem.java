package org.example.engine.collision;

import org.example.engine.GameObject;
import org.example.engine.Scene;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages collision detection and resolution between game objects
 */
public class CollisionSystem {
    private final Scene scene;
    private final Map<GameObject, Collider> colliders;
    private final List<CollisionLayer> collisionLayers;
    private final Map<CollisionPair, CollisionHandler> collisionHandlers;
    private boolean debugDraw = false;

    // Collision response settings
    private float responseStrength = 1.0f;
    private boolean continuousDetection = false;
    private int maxIterations = 3;

    /**
     * Creates a new collision system for the given scene
     */
    public CollisionSystem(Scene scene) {
        this.scene = scene;
        this.colliders = new HashMap<>();
        this.collisionLayers = new ArrayList<>();
        this.collisionHandlers = new HashMap<>();

        // Create default layers
        createDefaultLayers();
    }

    /**
     * Create default collision layers
     */
    private void createDefaultLayers() {
        // Create standard layers
        CollisionLayer defaultLayer = new CollisionLayer("Default", 0);
        CollisionLayer staticLayer = new CollisionLayer("Static", 1);
        CollisionLayer playerLayer = new CollisionLayer("Player", 2);
        CollisionLayer enemyLayer = new CollisionLayer("Enemy", 3);
        CollisionLayer projectileLayer = new CollisionLayer("Projectile", 4);
        CollisionLayer triggerLayer = new CollisionLayer("Trigger", 5);

        // Add layers to the system
        addCollisionLayer(defaultLayer);
        addCollisionLayer(staticLayer);
        addCollisionLayer(playerLayer);
        addCollisionLayer(enemyLayer);
        addCollisionLayer(projectileLayer);
        addCollisionLayer(triggerLayer);

        // Set up default collision matrix (which layers can collide with which)
        // By default, everything collides with the default layer
        setLayerCollision(defaultLayer.getId(), staticLayer.getId(), true);
        setLayerCollision(defaultLayer.getId(), playerLayer.getId(), true);
        setLayerCollision(defaultLayer.getId(), enemyLayer.getId(), true);
        setLayerCollision(defaultLayer.getId(), projectileLayer.getId(), true);
        setLayerCollision(defaultLayer.getId(), triggerLayer.getId(), true);

        // Players collide with static objects and enemies
        setLayerCollision(playerLayer.getId(), staticLayer.getId(), true);
        setLayerCollision(playerLayer.getId(), enemyLayer.getId(), true);
        setLayerCollision(playerLayer.getId(), projectileLayer.getId(), true);

        // Projectiles collide with static objects, players, and enemies
        setLayerCollision(projectileLayer.getId(), staticLayer.getId(), true);
        setLayerCollision(projectileLayer.getId(), playerLayer.getId(), true);
        setLayerCollision(projectileLayer.getId(), enemyLayer.getId(), true);

        // Enemies collide with static objects and other enemies
        setLayerCollision(enemyLayer.getId(), staticLayer.getId(), true);
        setLayerCollision(enemyLayer.getId(), enemyLayer.getId(), true);

        // Triggers don't collide with anything (they only detect)
        // We don't need to set this explicitly since it defaults to false
    }

    /**
     * Add a collision layer to the system
     */
    public void addCollisionLayer(CollisionLayer layer) {
        collisionLayers.add(layer);
    }

    /**
     * Get a collision layer by ID
     */
    public CollisionLayer getLayer(int layerId) {
        for (CollisionLayer layer : collisionLayers) {
            if (layer.getId() == layerId) {
                return layer;
            }
        }
        return null;
    }

    /**
     * Get a collision layer by name
     */
    public CollisionLayer getLayer(String layerName) {
        for (CollisionLayer layer : collisionLayers) {
            if (layer.getName().equals(layerName)) {
                return layer;
            }
        }
        return null;
    }

    /**
     * Set whether two layers should collide with each other
     */
    public void setLayerCollision(int layer1, int layer2, boolean shouldCollide) {
        CollisionLayer l1 = getLayer(layer1);
        CollisionLayer l2 = getLayer(layer2);

        if (l1 != null && l2 != null) {
            if (shouldCollide) {
                l1.addCollidingLayer(layer2);
                l2.addCollidingLayer(layer1);
            } else {
                l1.removeCollidingLayer(layer2);
                l2.removeCollidingLayer(layer1);
            }
        }
    }

    /**
     * Register a game object with a collider
     */
    public void registerCollider(GameObject gameObject, Collider collider) {
        colliders.put(gameObject, collider);
    }

    /**
     * Unregister a game object from the collision system
     */
    public void unregisterCollider(GameObject gameObject) {
        colliders.remove(gameObject);
    }

    /**
     * Get the collider for a game object
     */
    public Collider getCollider(GameObject gameObject) {
        return colliders.get(gameObject);
    }

    /**
     * Register a custom collision handler for specific collider types
     */
    public void registerCollisionHandler(Class<? extends Collider> type1, Class<? extends Collider> type2, CollisionHandler handler) {
        CollisionPair pair = new CollisionPair(type1, type2);
        collisionHandlers.put(pair, handler);
    }

    /**
     * Update the collision system
     */
    public void update(float deltaTime) {
        // First, update all colliders with their game objects' positions
        updateColliders();

        // Then perform collision detection and resolution
        detectAndResolveCollisions();
    }

    /**
     * Update all colliders to match their game objects' positions
     */
    private void updateColliders() {
        for (Map.Entry<GameObject, Collider> entry : colliders.entrySet()) {
            GameObject gameObject = entry.getKey();
            Collider collider = entry.getValue();

            // Update the collider's position based on the game object
            if (gameObject != null) {
                collider.updatePosition(gameObject);
            }
        }
    }

    /**
     * Detect and resolve collisions between all objects
     */
    private void detectAndResolveCollisions() {
        List<Collision> collisions = new ArrayList<>();

        // Check all pairs of colliders for collisions
        List<GameObject> gameObjects = new ArrayList<>(colliders.keySet());

        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject objA = gameObjects.get(i);
            Collider colliderA = colliders.get(objA);

            if (colliderA == null || !colliderA.isActive()) continue;

            for (int j = i + 1; j < gameObjects.size(); j++) {
                GameObject objB = gameObjects.get(j);
                Collider colliderB = colliders.get(objB);

                if (colliderB == null || !colliderB.isActive()) continue;

                // Check if layers can collide
                if (!canLayersCollide(colliderA.getLayer(), colliderB.getLayer())) {
                    continue;
                }

                // Check for collision
                CollisionResult result = checkCollision(colliderA, colliderB);

                if (result.isColliding()) {
                    // Create a collision object
                    Collision collision = new Collision(objA, objB, colliderA, colliderB, result);
                    collisions.add(collision);

                    // Handle the collision based on collider types and response settings
                    handleCollision(collision);
                }
            }
        }
    }

    /**
     * Check if two layers can collide with each other
     */
    private boolean canLayersCollide(int layer1, int layer2) {
        CollisionLayer l1 = getLayer(layer1);
        CollisionLayer l2 = getLayer(layer2);

        if (l1 == null || l2 == null) {
            return false;
        }

        return l1.canCollideWith(layer2);
    }

    /**
     * Check for collision between two colliders
     */
    private CollisionResult checkCollision(Collider a, Collider b) {
        // Find the appropriate handler for these collider types
        CollisionHandler handler = findCollisionHandler(a.getClass(), b.getClass());

        if (handler != null) {
            // Use the specialized handler
            return handler.checkCollision(a, b);
        } else {
            // Use the default handler based on collider types
            if (a instanceof BoxCollider && b instanceof BoxCollider) {
                return checkBoxToBoxCollision((BoxCollider) a, (BoxCollider) b);
            } else if (a instanceof CircleCollider && b instanceof CircleCollider) {
                return checkCircleToCircleCollision((CircleCollider) a, (CircleCollider) b);
            } else if (a instanceof BoxCollider && b instanceof CircleCollider) {
                return checkBoxToCircleCollision((BoxCollider) a, (CircleCollider) b);
            } else if (a instanceof CircleCollider && b instanceof BoxCollider) {
                CollisionResult result = checkBoxToCircleCollision((BoxCollider) b, (CircleCollider) a);
                result.flipDirection(); // Flip the direction since we swapped the order
                return result;
            }

            // Unsupported collision types
            return new CollisionResult(false);
        }
    }

    /**
     * Handle a collision between two objects
     */
    private void handleCollision(Collision collision) {
        GameObject objA = collision.getObjectA();
        GameObject objB = collision.getObjectB();
        Collider colliderA = collision.getColliderA();
        Collider colliderB = collision.getColliderB();
        CollisionResult result = collision.getResult();

        // Skip if either collider is a trigger
        if (colliderA.isTrigger() || colliderB.isTrigger()) {
            // Handle trigger events
            if (colliderA.isTrigger()) {
                if (objA instanceof Collidable) {
                    ((Collidable) objA).onTriggerEnter(objB, result);
                }
            }
            if (colliderB.isTrigger()) {
                if (objB instanceof Collidable) {
                    ((Collidable) objB).onTriggerEnter(objA, result);
                }
            }
            return;
        }

        // Calculate response
        if (responseStrength > 0 && result.isColliding()) {
            resolveCollision(collision);
        }

        // Notify objects of collision
        if (objA instanceof Collidable) {
            ((Collidable) objA).onCollision(objB, result);
        }

        if (objB instanceof Collidable) {
            // Flip the direction for the second object
            CollisionResult flippedResult = new CollisionResult(result);
            flippedResult.flipDirection();
            ((Collidable) objB).onCollision(objA, flippedResult);
        }
    }

    /**
     * Resolve a collision by moving objects apart
     */
    private void resolveCollision(Collision collision) {
        GameObject objA = collision.getObjectA();
        GameObject objB = collision.getObjectB();
        CollisionResult result = collision.getResult();

        if (!result.isColliding() || result.getDepth() <= 0) {
            return;
        }

        // Calculate response weights based on mass or if the object is static
        float massA = 1.0f;
        float massB = 1.0f;
        boolean isAStatic = isStatic(objA);
        boolean isBStatic = isStatic(objB);

        if (objA instanceof PhysicsObject) {
            massA = ((PhysicsObject) objA).getMass();
        }

        if (objB instanceof PhysicsObject) {
            massB = ((PhysicsObject) objB).getMass();
        }

        // If both objects are static, no resolution occurs
        if (isAStatic && isBStatic) {
            return;
        }

        // Calculate how much each object should move
        float totalMass = massA + massB;
        float ratioA = isAStatic ? 0 : (massB / totalMass);
        float ratioB = isBStatic ? 0 : (massA / totalMass);

        // Apply the movement - scale by responseStrength
        Vector2f mtv = result.getNormal().mul(result.getDepth() * responseStrength);

        if (!isAStatic && objA instanceof PhysicsObject) {
            ((PhysicsObject) objA).applyImpulse(
                    mtv.x * -ratioA,
                    mtv.y * -ratioA
            );
        }

        if (!isBStatic && objB instanceof PhysicsObject) {
            ((PhysicsObject) objB).applyImpulse(
                    mtv.x * ratioB,
                    mtv.y * ratioB
            );
        }
    }

    /**
     * Check if a game object should be treated as static
     */
    private boolean isStatic(GameObject obj) {
        if (obj == null) return true;

        if (obj instanceof PhysicsObject) {
            return ((PhysicsObject) obj).isStatic();
        }

        // By default, treat objects as dynamic
        return false;
    }

    /**
     * Find a collision handler for two collider types
     */
    private CollisionHandler findCollisionHandler(Class<? extends Collider> type1, Class<? extends Collider> type2) {
        // Try direct match
        CollisionPair directPair = new CollisionPair(type1, type2);
        CollisionHandler handler = collisionHandlers.get(directPair);

        if (handler != null) {
            return handler;
        }

        // Try reversed match
        CollisionPair reversedPair = new CollisionPair(type2, type1);
        return collisionHandlers.get(reversedPair);
    }

    /**
     * Check for collision between two box colliders
     */
    private CollisionResult checkBoxToBoxCollision(BoxCollider a, BoxCollider b) {
        // Get box properties
        float ax = a.getX();
        float ay = a.getY();
        float aWidth = a.getWidth();
        float aHeight = a.getHeight();

        float bx = b.getX();
        float by = b.getY();
        float bWidth = b.getWidth();
        float bHeight = b.getHeight();

        // Calculate overlap
        float xOverlap = Math.min(ax + aWidth, bx + bWidth) - Math.max(ax, bx);
        float yOverlap = Math.min(ay + aHeight, by + bHeight) - Math.max(ay, by);

        // No collision if there's no overlap in either axis
        if (xOverlap <= 0 || yOverlap <= 0) {
            return new CollisionResult(false);
        }

        // Determine the separation direction (minimum penetration direction)
        Vector2f normal;
        float depth;

        if (xOverlap < yOverlap) {
            depth = xOverlap;

            // Calculate the normal direction (which way to push out)
            float centerA = ax + aWidth / 2;
            float centerB = bx + bWidth / 2;

            if (centerA < centerB) {
                normal = new Vector2f(-1, 0); // Push A to the left
            } else {
                normal = new Vector2f(1, 0);  // Push A to the right
            }
        } else {
            depth = yOverlap;

            // Calculate the normal direction (which way to push out)
            float centerA = ay + aHeight / 2;
            float centerB = by + bHeight / 2;

            if (centerA < centerB) {
                normal = new Vector2f(0, -1); // Push A upward
            } else {
                normal = new Vector2f(0, 1);  // Push A downward
            }
        }

        // Create the collision result
        CollisionResult result = new CollisionResult(true);
        result.setNormal(normal);
        result.setDepth(depth);
        result.setContactPoint(calculateContactPoint(a, b, normal));

        return result;
    }

    /**
     * Check for collision between two circle colliders
     */
    private CollisionResult checkCircleToCircleCollision(CircleCollider a, CircleCollider b) {
        // Get circle properties
        float ax = a.getX() + a.getRadius();
        float ay = a.getY() + a.getRadius();
        float aRadius = a.getRadius();

        float bx = b.getX() + b.getRadius();
        float by = b.getY() + b.getRadius();
        float bRadius = b.getRadius();

        // Calculate distance between centers
        float dx = bx - ax;
        float dy = by - ay;
        float distanceSquared = dx * dx + dy * dy;
        float radiusSum = aRadius + bRadius;

        // No collision if distance is greater than sum of radii
        if (distanceSquared >= radiusSum * radiusSum) {
            return new CollisionResult(false);
        }

        // Calculate actual distance
        float distance = (float) Math.sqrt(distanceSquared);

        // Create normal and contact point
        Vector2f normal;
        if (distance > 0) {
            normal = new Vector2f(dx / distance, dy / distance);
        } else {
            // Circles are at the same position, pick an arbitrary direction
            normal = new Vector2f(1, 0);
        }

        float depth = radiusSum - distance;

        // Calculate contact point (halfway between perimeters)
        Vector2f contactPoint = new Vector2f(
                ax + normal.x * aRadius - normal.x * depth / 2,
                ay + normal.y * aRadius - normal.y * depth / 2
        );

        // Create collision result
        CollisionResult result = new CollisionResult(true);
        result.setNormal(normal);
        result.setDepth(depth);
        result.setContactPoint(contactPoint);

        return result;
    }

    /**
     * Check for collision between a box and a circle collider
     */
    private CollisionResult checkBoxToCircleCollision(BoxCollider box, CircleCollider circle) {
        // Get box properties
        float boxLeft = box.getX();
        float boxTop = box.getY();
        float boxRight = boxLeft + box.getWidth();
        float boxBottom = boxTop + box.getHeight();

        // Get circle properties
        float circleX = circle.getX() + circle.getRadius();
        float circleY = circle.getY() + circle.getRadius();
        float radius = circle.getRadius();

        // Find the closest point on the box to the circle center
        float closestX = Math.max(boxLeft, Math.min(circleX, boxRight));
        float closestY = Math.max(boxTop, Math.min(circleY, boxBottom));

        // Calculate distance to the closest point
        float dx = circleX - closestX;
        float dy = circleY - closestY;
        float distanceSquared = dx * dx + dy * dy;

        // No collision if distance is greater than radius
        if (distanceSquared > radius * radius) {
            return new CollisionResult(false);
        }

        // Calculate normal and depth
        Vector2f normal;
        float depth;

        if (distanceSquared > 0) {
            float distance = (float) Math.sqrt(distanceSquared);
            normal = new Vector2f(dx / distance, dy / distance);
            depth = radius - distance;
        } else {
            // Circle center is inside the box, find the closest edge
            float left = circleX - boxLeft;
            float right = boxRight - circleX;
            float top = circleY - boxTop;
            float bottom = boxBottom - circleY;

            // Find the smallest distance
            if (left <= right && left <= top && left <= bottom) {
                normal = new Vector2f(-1, 0);
                depth = left + radius;
            } else if (right <= left && right <= top && right <= bottom) {
                normal = new Vector2f(1, 0);
                depth = right + radius;
            } else if (top <= left && top <= right && top <= bottom) {
                normal = new Vector2f(0, -1);
                depth = top + radius;
            } else {
                normal = new Vector2f(0, 1);
                depth = bottom + radius;
            }
        }

        // Calculate contact point
        Vector2f contactPoint = new Vector2f(
                circleX - normal.x * radius,
                circleY - normal.y * radius
        );

        // Create collision result
        CollisionResult result = new CollisionResult(true);
        result.setNormal(normal);
        result.setDepth(depth);
        result.setContactPoint(contactPoint);

        return result;
    }

    /**
     * Calculate a contact point between two colliders
     */
    private Vector2f calculateContactPoint(Collider a, Collider b, Vector2f normal) {
        // This is a simplified contact point calculation
        // For more accurate physics, you might want a more sophisticated approach

        if (a instanceof BoxCollider && b instanceof BoxCollider) {
            BoxCollider boxA = (BoxCollider) a;
            BoxCollider boxB = (BoxCollider) b;

            // Calculate centers
            float centerAx = boxA.getX() + boxA.getWidth() / 2;
            float centerAy = boxA.getY() + boxA.getHeight() / 2;
            float centerBx = boxB.getX() + boxB.getWidth() / 2;
            float centerBy = boxB.getY() + boxB.getHeight() / 2;

            // Calculate midpoint as a simple estimate
            return new Vector2f(
                    (centerAx + centerBx) / 2,
                    (centerAy + centerBy) / 2
            );
        }

        // For other collider combinations, calculate based on position and normal
        // (This is a simplification)
        return new Vector2f(
                a.getPosition().x + normal.x * a.getBounds().getWidth() / 2,
                a.getPosition().y + normal.y * a.getBounds().getHeight() / 2
        );
    }

    /**
     * Get debug drawing status
     */
    public boolean isDebugDraw() {
        return debugDraw;
    }

    /**
     * Set debug drawing status
     */
    public void setDebugDraw(boolean debugDraw) {
        this.debugDraw = debugDraw;
    }

    /**
     * Get collision response strength
     */
    public float getResponseStrength() {
        return responseStrength;
    }

    /**
     * Set collision response strength (0-1)
     */
    public void setResponseStrength(float responseStrength) {
        this.responseStrength = Math.max(0, Math.min(1, responseStrength));
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        colliders.clear();
        collisionLayers.clear();
        collisionHandlers.clear();
    }
}