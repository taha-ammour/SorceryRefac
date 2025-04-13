package org.example.engine.collision;

import org.example.engine.GameObject;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Physics component for game objects that need physics behavior
 */
public class PhysicsComponent implements PhysicsObject {
    private final GameObject gameObject;

    private float mass;
    private boolean isStatic;
    private float friction;
    private float restitution;

    private Vector2f velocity;
    private Vector2f acceleration;
    private Vector2f force;
    private float gravityScale;

    private boolean useGravity;
    private static final Vector2f GRAVITY = new Vector2f(0, 9.8f);

    /**
     * Creates a new physics component for the given game object
     */
    public PhysicsComponent(GameObject gameObject) {
        this.gameObject = gameObject;
        this.mass = 1.0f;
        this.isStatic = false;
        this.friction = 0.2f;
        this.restitution = 0.3f;
        this.velocity = new Vector2f();
        this.acceleration = new Vector2f();
        this.force = new Vector2f();
        this.gravityScale = 1.0f;
        this.useGravity = true;
    }

    /**
     * Updates the physics component
     */
    public void update(float deltaTime) {
        if (isStatic) {
            return;
        }

        // Apply gravity
        if (useGravity) {
            force.add(
                    GRAVITY.x * gravityScale * mass,
                    GRAVITY.y * gravityScale * mass
            );
        }

        // Calculate acceleration from force (F = ma)
        acceleration.set(force).div(mass);

        // Update velocity using acceleration
        velocity.add(
                acceleration.x * deltaTime,
                acceleration.y * deltaTime
        );

        // Apply friction
        velocity.mul(1.0f - (friction * deltaTime));

        // Update position using velocity
        Vector3f position = getPosition();
        if (position != null) {
            position.x += velocity.x * deltaTime;
            position.y += velocity.y * deltaTime;
            setPosition(position);
        }

        // Reset force for next frame
        force.set(0, 0);
    }

    /**
     * Gets the position of the game object
     */
    private Vector3f getPosition() {
        if (gameObject instanceof org.example.game.Player) {
            return ((org.example.game.Player) gameObject).getPosition();
        } else {
            // Try to get position through reflection
            try {
                java.lang.reflect.Method getPositionMethod = gameObject.getClass().getMethod("getPosition");
                return (Vector3f) getPositionMethod.invoke(gameObject);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Sets the position of the game object
     */
    private void setPosition(Vector3f position) {
        if (gameObject instanceof org.example.game.Player) {
            ((org.example.game.Player) gameObject).setPosition(position.x, position.y, position.z);
        } else {
            // Try to set position through reflection
            try {
                java.lang.reflect.Method setPositionMethod = gameObject.getClass().getMethod("setPosition", float.class, float.class, float.class);
                setPositionMethod.invoke(gameObject, position.x, position.y, position.z);
            } catch (Exception e) {
                // Ignore if we can't set the position
            }
        }
    }

    @Override
    public float getMass() {
        return mass;
    }

    /**
     * Sets the mass of the object
     */
    public void setMass(float mass) {
        this.mass = Math.max(0.1f, mass);
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    /**
     * Sets whether the object is static (immovable)
     */
    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    @Override
    public void applyImpulse(float x, float y) {
        if (isStatic) {
            return;
        }

        // Apply impulse directly to velocity (p = mv)
        velocity.add(x / mass, y / mass);
    }

    /**
     * Applies a force to the object
     */
    public void applyForce(float x, float y) {
        if (isStatic) {
            return;
        }

        force.add(x, y);
    }

    /**
     * Gets the current velocity
     */
    public Vector2f getVelocity() {
        return new Vector2f(velocity);
    }

    /**
     * Sets the velocity
     */
    public void setVelocity(float x, float y) {
        velocity.set(x, y);
    }

    /**
     * Gets the friction coefficient
     */
    public float getFriction() {
        return friction;
    }

    /**
     * Sets the friction coefficient
     */
    public void setFriction(float friction) {
        this.friction = Math.max(0, Math.min(1, friction));
    }

    /**
     * Gets the restitution (bounciness)
     */
    public float getRestitution() {
        return restitution;
    }

    /**
     * Sets the restitution (bounciness)
     */
    public void setRestitution(float restitution) {
        this.restitution = Math.max(0, Math.min(1, restitution));
    }

    /**
     * Gets the gravity scale
     */
    public float getGravityScale() {
        return gravityScale;
    }

    /**
     * Sets the gravity scale
     */
    public void setGravityScale(float gravityScale) {
        this.gravityScale = gravityScale;
    }

    /**
     * Checks if gravity is enabled
     */
    public boolean isUseGravity() {
        return useGravity;
    }

    /**
     * Sets whether gravity is enabled
     */
    public void setUseGravity(boolean useGravity) {
        this.useGravity = useGravity;
    }
}