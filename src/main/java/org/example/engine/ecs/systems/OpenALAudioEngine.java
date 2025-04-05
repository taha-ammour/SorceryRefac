package org.example.engine.ecs.systems;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class OpenALAudioEngine implements org.example.engine.ecs.systems.AudioSystem.AudioEngine {
    private long device;
    private long context;
    private final Map<String, Integer> soundBuffers = new HashMap<>();
    private final Map<Integer, SoundSource> soundSources = new HashMap<>();
    private int nextSourceId = 1;

    public OpenALAudioEngine() {
        initOpenAL();
    }

    private void initOpenAL() {
        // Open the default audio device
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        device = alcOpenDevice(defaultDeviceName);
        if (device == NULL) {
            throw new IllegalStateException("Failed to open the default OpenAL device.");
        }

        // Create context
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        IntBuffer contextAttribs = BufferUtils.createIntBuffer(16);
        context = alcCreateContext(device, contextAttribs);
        if (context == NULL) {
            throw new IllegalStateException("Failed to create OpenAL context.");
        }
        alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);

        // Set listener properties (default values)
        alListener3f(AL_POSITION, 0f, 0f, 0f);
        alListener3f(AL_VELOCITY, 0f, 0f, 0f);
        float[] orientation = {0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f};
        alListenerfv(AL_ORIENTATION, orientation);
        alDistanceModel(AL_INVERSE_DISTANCE_CLAMPED);
    }

    @Override
    public int playSound(String soundId, float x, float y, float z, float volume, float pitch,
                         boolean looping, boolean is3D, float minDistance, float maxDistance) {
        // Check if sound is loaded
        Integer bufferId = soundBuffers.get(soundId);
        if (bufferId == null) {
            System.err.println("Sound not loaded: " + soundId);
            return -1;
        }

        // Create a source
        int sourceId = alGenSources();
        int id = nextSourceId++;

        // Configure source
        alSourcei(sourceId, AL_BUFFER, bufferId);
        alSourcef(sourceId, AL_GAIN, volume);
        alSourcef(sourceId, AL_PITCH, pitch);
        alSourcei(sourceId, AL_LOOPING, looping ? AL_TRUE : AL_FALSE);

        if (is3D) {
            alSource3f(sourceId, AL_POSITION, x, y, z);
            alSourcef(sourceId, AL_REFERENCE_DISTANCE, minDistance);
            alSourcef(sourceId, AL_MAX_DISTANCE, maxDistance);
        } else {
            // For 2D sounds, position at listener (no attenuation)
            alSource3f(sourceId, AL_POSITION, 0f, 0f, 0f);
            alSourcei(sourceId, AL_SOURCE_RELATIVE, AL_TRUE);
        }

        // Start playing
        alSourcePlay(sourceId);

        // Track the source
        soundSources.put(id, new SoundSource(sourceId, is3D));

        return id;
    }

    @Override
    public boolean isPlaying(int sourceId) {
        SoundSource source = soundSources.get(sourceId);
        if (source == null) return false;

        int state = alGetSourcei(source.alSourceId, AL_SOURCE_STATE);
        return state == AL_PLAYING;
    }

    @Override
    public void stopSound(int sourceId) {
        SoundSource source = soundSources.get(sourceId);
        if (source == null) return;

        alSourceStop(source.alSourceId);
    }

    @Override
    public void setSourcePosition(int sourceId, float x, float y, float z) {
        SoundSource source = soundSources.get(sourceId);
        if (source == null || !source.is3D) return;

        alSource3f(source.alSourceId, AL_POSITION, x, y, z);
    }

    @Override
    public void setSourceVolume(int sourceId, float volume) {
        SoundSource source = soundSources.get(sourceId);
        if (source == null) return;

        alSourcef(source.alSourceId, AL_GAIN, volume);
    }

    @Override
    public void setSourcePitch(int sourceId, float pitch) {
        SoundSource source = soundSources.get(sourceId);
        if (source == null) return;

        alSourcef(source.alSourceId, AL_PITCH, pitch);
    }

    @Override
    public void setListenerPosition(float x, float y, float z) {
        alListener3f(AL_POSITION, x, y, z);
    }

    @Override
    public void loadSound(String soundId, String filePath) {
        // Skip if already loaded
        if (soundBuffers.containsKey(soundId)) {
            return;
        }

        // Create buffer
        int bufferId = alGenBuffers();

        try {
            // Load WAV file
            InputStream inputStream = getClass().getResourceAsStream(filePath);
            if (inputStream == null) {
                System.err.println("Sound file not found: " + filePath);
                alDeleteBuffers(bufferId);
                return;
            }

            BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedInput);

            // Get format information
            AudioFormat format = audioStream.getFormat();

            // Read all audio data
            byte[] data = new byte[(int)(audioStream.getFrameLength() * format.getFrameSize())];
            audioStream.read(data);

            // Convert bytes to buffer
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();

            // Get AL format
            int alFormat = getOpenALFormat(format);
            if (alFormat == -1) {
                System.err.println("Unsupported audio format for OpenAL: " + format);
                alDeleteBuffers(bufferId);
                return;
            }

            // Upload to OpenAL
            alBufferData(bufferId, alFormat, buffer, (int)format.getSampleRate());

            // Store for later use
            soundBuffers.put(soundId, bufferId);

            audioStream.close();
            inputStream.close();

        } catch (IOException | UnsupportedAudioFileException e) {
            System.err.println("Error loading sound: " + filePath);
            e.printStackTrace();
            alDeleteBuffers(bufferId);
            return;
        }
    }

    private int getOpenALFormat(AudioFormat format) {
        // Determine format based on channels and sample size
        if (format.getChannels() == 1) {
            if (format.getSampleSizeInBits() == 8) {
                return AL_FORMAT_MONO8;
            } else if (format.getSampleSizeInBits() == 16) {
                return AL_FORMAT_MONO16;
            }
        } else if (format.getChannels() == 2) {
            if (format.getSampleSizeInBits() == 8) {
                return AL_FORMAT_STEREO8;
            } else if (format.getSampleSizeInBits() == 16) {
                return AL_FORMAT_STEREO16;
            }
        }
        return -1; // Unsupported format
    }

    @Override
    public void unloadSound(String soundId) {
        Integer bufferId = soundBuffers.remove(soundId);
        if (bufferId != null) {
            // First, stop any sources using this buffer
            for (SoundSource source : soundSources.values()) {
                int sourceBuffer = alGetSourcei(source.alSourceId, AL_BUFFER);
                if (sourceBuffer == bufferId) {
                    alSourceStop(source.alSourceId);
                    alSourcei(source.alSourceId, AL_BUFFER, 0);
                }
            }

            // Delete the buffer
            alDeleteBuffers(bufferId);
        }
    }

    public void cleanup() {
        // Stop all sound sources
        for (SoundSource source : soundSources.values()) {
            alSourceStop(source.alSourceId);
            alDeleteSources(source.alSourceId);
        }
        soundSources.clear();

        // Delete all buffers
        for (int bufferId : soundBuffers.values()) {
            alDeleteBuffers(bufferId);
        }
        soundBuffers.clear();

        // Clean up OpenAL
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    private static class SoundSource {
        final int alSourceId;
        final boolean is3D;

        SoundSource(int alSourceId, boolean is3D) {
            this.alSourceId = alSourceId;
            this.is3D = is3D;
        }
    }
}