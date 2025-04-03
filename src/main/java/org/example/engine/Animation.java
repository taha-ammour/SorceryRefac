package org.example.engine;

/**
 * Animation class that manages frame-based sprite animations
 */
public class Animation {
    private final String[] frameNames;
    private final float frameDuration;
    private float currentTime = 0;
    private int currentFrame = 0;
    private boolean isLooping = true;
    private boolean isFinished = false;

    /**
     * Creates a new animation
     *
     * @param frameNames Array of sprite names that make up the animation
     * @param frameDuration Duration of each frame in seconds
     */
    public Animation(String[] frameNames, float frameDuration) {
        this.frameNames = frameNames;
        this.frameDuration = frameDuration;
    }

    /**
     * Creates a new animation with looping control
     *
     * @param frameNames Array of sprite names that make up the animation
     * @param frameDuration Duration of each frame in seconds
     * @param looping Whether the animation should loop
     */
    public Animation(String[] frameNames, float frameDuration, boolean looping) {
        this.frameNames = frameNames;
        this.frameDuration = frameDuration;
        this.isLooping = looping;
    }

    /**
     * Updates the animation state
     *
     * @param deltaTime Time elapsed since last frame in seconds
     */
    public void update(float deltaTime) {
        if (isFinished) {
            return;
        }

        currentTime += deltaTime;

        if (currentTime >= frameDuration) {
            currentTime -= frameDuration;
            currentFrame++;

            if (currentFrame >= frameNames.length) {
                if (isLooping) {
                    currentFrame = 0;
                } else {
                    currentFrame = frameNames.length - 1;
                    isFinished = true;
                }
            }
        }
    }

    /**
     * Gets the sprite name for the current animation frame
     *
     * @return Current frame's sprite name
     */
    public String getCurrentFrameName() {
        return frameNames[currentFrame];
    }

    /**
     * Gets the current frame index
     *
     * @return Current frame index (0-based)
     */
    public int getCurrentFrameIndex() {
        return currentFrame;
    }

    /**
     * Sets the animation to a specific frame
     *
     * @param frameIndex Frame index to set (0-based)
     */
    public void setFrame(int frameIndex) {
        if (frameIndex >= 0 && frameIndex < frameNames.length) {
            currentFrame = frameIndex;
            currentTime = 0;
            isFinished = false;
        }
    }

    /**
     * Restarts the animation from the beginning
     */
    public void restart() {
        currentFrame = 0;
        currentTime = 0;
        isFinished = false;
    }

    /**
     * Checks if the animation has finished (only relevant for non-looping animations)
     *
     * @return True if the animation has finished
     */
    public boolean isFinished() {
        return isFinished;
    }

    /**
     * Sets whether the animation should loop
     *
     * @param looping True if the animation should loop
     */
    public void setLooping(boolean looping) {
        this.isLooping = looping;
    }

    /**
     * Gets the total number of frames in the animation
     *
     * @return Number of frames
     */
    public int getFrameCount() {
        return frameNames.length;
    }
}