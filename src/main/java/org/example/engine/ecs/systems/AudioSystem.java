package org.example.engine.ecs.systems;

import org.example.engine.ecs.Entity;
import org.example.engine.ecs.System;
import org.example.engine.ecs.components.AudioComponent;
import org.example.engine.ecs.components.TransformComponent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System that handles audio playback using components
 */
public class AudioSystem extends System {
    // Used for tracking active sounds and their sources
    private final Map<String, Integer> activeSounds = new ConcurrentHashMap<>();

    // The listener position (typically camera or player position)
    private Vector3f listenerPosition = new Vector3f();

    // Audio engine abstraction
    private final AudioEngine audioEngine;

    public AudioSystem(AudioEngine audioEngine) {
        super(80, TransformComponent.class, AudioComponent.class);
        this.audioEngine = audioEngine;
    }

    @Override
    public void begin(float deltaTime) {
        // Update listener position with camera/player position each frame
        audioEngine.setListenerPosition(listenerPosition.x, listenerPosition.y, listenerPosition.z);
    }

    @Override
    public void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = entity.getComponent(TransformComponent.class);
        AudioComponent audio = entity.getComponent(AudioComponent.class);

        // Get entity position for 3D audio
        Vector3f position = transform.getPosition();

        // Generate a unique ID for this entity's sound
        String instanceId = entity.getId().toString() + "_" + audio.getSoundId();

        // Check if sound is already playing
        Integer sourceId = activeSounds.get(instanceId);

        if (sourceId != null) {
            // Sound already exists, update its parameters
            audioEngine.setSourcePosition(sourceId, position.x, position.y, position.z);
            audioEngine.setSourceVolume(sourceId, audio.getVolume());
            audioEngine.setSourcePitch(sourceId, audio.getPitch());

            // Check if the sound is still playing
            if (!audioEngine.isPlaying(sourceId)) {
                // Sound finished, remove tracking
                activeSounds.remove(instanceId);

                // If it should loop but stopped, restart it
                if (audio.isLooping()) {
                    int newSourceId = audioEngine.playSound(
                            audio.getSoundId(),
                            position.x, position.y, position.z,
                            audio.getVolume(),
                            audio.getPitch(),
                            audio.isLooping(),
                            audio.is3D(),
                            audio.getMinDistance(),
                            audio.getMaxDistance()
                    );

                    if (newSourceId >= 0) {
                        activeSounds.put(instanceId, newSourceId);
                    }
                }
            }
        }
        else if (audio.isAutoPlay()) {
            // Start playing the sound
            int newSourceId = audioEngine.playSound(
                    audio.getSoundId(),
                    position.x, position.y, position.z,
                    audio.getVolume(),
                    audio.getPitch(),
                    audio.isLooping(),
                    audio.is3D(),
                    audio.getMinDistance(),
                    audio.getMaxDistance()
            );

            if (newSourceId >= 0) {
                activeSounds.put(instanceId, newSourceId);
            }
        }
    }

    @Override
    public void end(float deltaTime) {
        // Could be used for cleanup or stats
    }

    public void setListenerPosition(Vector3f position) {
        this.listenerPosition.set(position);
    }

    public void playSound(Entity entity) {
        if (!entity.hasAllComponents(TransformComponent.class, AudioComponent.class)) {
            return;
        }

        TransformComponent transform = entity.getComponent(TransformComponent.class);
        AudioComponent audio = entity.getComponent(AudioComponent.class);
        Vector3f position = transform.getPosition();

        // Generate a unique ID for this entity's sound
        String instanceId = entity.getId().toString() + "_" + audio.getSoundId();

        // Stop any existing playback
        Integer existingSource = activeSounds.get(instanceId);
        if (existingSource != null) {
            audioEngine.stopSound(existingSource);
            activeSounds.remove(instanceId);
        }

        // Start playing the sound
        int sourceId = audioEngine.playSound(
                audio.getSoundId(),
                position.x, position.y, position.z,
                audio.getVolume(),
                audio.getPitch(),
                audio.isLooping(),
                audio.is3D(),
                audio.getMinDistance(),
                audio.getMaxDistance()
        );

        if (sourceId >= 0) {
            activeSounds.put(instanceId, sourceId);
        }
    }

    public void stopSound(Entity entity) {
        if (!entity.hasComponent(AudioComponent.class)) {
            return;
        }

        AudioComponent audio = entity.getComponent(AudioComponent.class);
        String instanceId = entity.getId().toString() + "_" + audio.getSoundId();

        Integer sourceId = activeSounds.get(instanceId);
        if (sourceId != null) {
            audioEngine.stopSound(sourceId);
            activeSounds.remove(instanceId);
        }
    }

    /**
     * Audio engine interface to abstract the actual implementation
     * This could be implemented using OpenAL, JavaSound, etc.
     */
    public interface AudioEngine {
        /**
         * Play a sound with the given parameters
         * @return The source ID for tracking, or -1 if failed
         */
        int playSound(String soundId, float x, float y, float z, float volume, float pitch,
                      boolean looping, boolean is3D, float minDistance, float maxDistance);

        /**
         * Check if a source is still playing
         */
        boolean isPlaying(int sourceId);

        /**
         * Stop a sound
         */
        void stopSound(int sourceId);

        /**
         * Set the position of a sound source
         */
        void setSourcePosition(int sourceId, float x, float y, float z);

        /**
         * Set the volume of a sound source
         */
        void setSourceVolume(int sourceId, float volume);

        /**
         * Set the pitch of a sound source
         */
        void setSourcePitch(int sourceId, float pitch);

        /**
         * Set the position of the audio listener (camera/player)
         */
        void setListenerPosition(float x, float y, float z);

        /**
         * Load a sound
         */
        void loadSound(String soundId, String filePath);

        /**
         * Unload a sound
         */
        void unloadSound(String soundId);
    }
}