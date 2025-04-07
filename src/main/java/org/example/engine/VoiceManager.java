package org.example.engine;

import com.esotericsoftware.kryonet.Client;
import org.example.Packets;
import org.example.engine.GameObject;
import org.example.game.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VoiceManager extends GameObject {
    private boolean voiceEnabled = false;
    private float voiceVolume = 1.0f;
    private float proximityRange = 500.0f;

    private Client networkClient;
    private TargetDataLine micLine;
    private Thread captureThread;
    private volatile boolean isCapturing = false;

    // Queue for incoming voice packets
    private final ConcurrentLinkedQueue<Packets.VoicePacket> incomingVoiceQueue = new ConcurrentLinkedQueue<>();

    // Player voice sources
    private final Map<String, VoiceSource> playerVoiceSources = new HashMap<>();

    // Local player reference
    private Player localPlayer;

    public VoiceManager(Player localPlayer, Client networkClient) {
        this.localPlayer = localPlayer;
        this.networkClient = networkClient;
    }

    public void initializeVoice() {
        try {
            // Set up audio format for voice capture
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Microphone not supported");
                return;
            }

            micLine = (TargetDataLine) AudioSystem.getLine(info);
            micLine.open(format);

            System.out.println("Voice system initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize voice system: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startVoiceCapture() {
        if (micLine == null || isCapturing) return;

        micLine.start();
        isCapturing = true;

        captureThread = new Thread(this::captureLoop);
        captureThread.setDaemon(true);
        captureThread.start();

        System.out.println("Voice capture started");
    }

    public void stopVoiceCapture() {
        if (!isCapturing) return;

        isCapturing = false;
        if (micLine != null) {
            micLine.stop();
            micLine.flush();
        }

        System.out.println("Voice capture stopped");
    }

    private void captureLoop() {
        byte[] buffer = new byte[1024];

        while (isCapturing) {
            try {
                int bytesRead = micLine.read(buffer, 0, buffer.length);

                if (bytesRead > 0 && networkClient != null && networkClient.isConnected()) {
                    // Check if there's actual voice (simple energy detection)
                    if (hasVoiceEnergy(buffer, bytesRead)) {
                        sendVoicePacket(buffer, bytesRead);
                    }
                }

                Thread.sleep(10); // Small delay to prevent CPU overuse
            } catch (Exception e) {
                System.err.println("Error in voice capture: " + e.getMessage());
            }
        }
    }

    private boolean hasVoiceEnergy(byte[] buffer, int bytesRead) {
        // Simple energy detection - check if audio data has enough energy to be voice
        long sum = 0;
        for (int i = 0; i < bytesRead; i += 2) {
            int sample = ((buffer[i+1] << 8) | (buffer[i] & 0xFF));
            sum += Math.abs(sample);
        }

        long average = sum / (bytesRead / 2);
        return average > 500; // Threshold for voice detection
    }

    private void sendVoicePacket(byte[] buffer, int bytesRead) {
        if (localPlayer == null) return;

        Packets.VoicePacket packet = new Packets.VoicePacket();
        packet.playerId = localPlayer.getPlayerId().toString();
        packet.audioData = new byte[bytesRead];
        System.arraycopy(buffer, 0, packet.audioData, 0, bytesRead);

        Vector3f position = localPlayer.getPosition();
        packet.x = position.x;
        packet.y = position.y;
        packet.z = position.z;
        packet.timestamp = System.currentTimeMillis();

        // Send using UDP for lower latency
        networkClient.sendUDP(packet);
    }

    public void processVoicePacket(Packets.VoicePacket packet) {
        // Add to processing queue
        incomingVoiceQueue.add(packet);
    }

    @Override
    public void update(float deltaTime) {
        // Process incoming voice packets
        processVoiceQueue();
    }

    private void processVoiceQueue() {
        Packets.VoicePacket packet;
        while ((packet = incomingVoiceQueue.poll()) != null) {
            // Don't process our own voice
            if (localPlayer != null && packet.playerId.equals(localPlayer.getPlayerId().toString())) {
                continue;
            }

            // Get or create voice source for this player
            VoiceSource source = playerVoiceSources.computeIfAbsent(
                    packet.playerId, id -> new VoiceSource());

            // Set position of voice source
            source.setPosition(packet.x, packet.y, packet.z);

            // Calculate distance and volume based on proximity
            if (localPlayer != null) {
                Vector3f localPos = localPlayer.getPosition();
                float distance = (float) Math.sqrt(
                        Math.pow(localPos.x - packet.x, 2) +
                                Math.pow(localPos.y - packet.y, 2) +
                                Math.pow(localPos.z - packet.z, 2)
                );

                // Calculate volume based on distance
                float volume = Math.max(0, 1.0f - (distance / proximityRange));
                source.setVolume(volume * voiceVolume);

                // Only play if in range
                if (volume > 0.01f) {
                    source.playAudio(packet.audioData);
                }
            }
        }
    }

    @Override
    public void render(Matrix4f viewProjectionMatrix) {
        // No rendering needed for voice manager
    }

    @Override
    public void cleanup() {
        stopVoiceCapture();

        if (micLine != null) {
            micLine.close();
        }

        // Clean up all voice sources
        for (VoiceSource source : playerVoiceSources.values()) {
            source.cleanup();
        }
    }

    public void setVoiceEnabled(boolean enabled) {
        this.voiceEnabled = enabled;
        if (enabled) {
            startVoiceCapture();
        } else {
            stopVoiceCapture();
        }
    }

    public void setVoiceVolume(float volume) {
        this.voiceVolume = Math.max(0, Math.min(1, volume));
    }

    public void setProximityRange(float range) {
        this.proximityRange = Math.max(50, range);
    }

    public boolean isVoiceEnabled() {
        return voiceEnabled;
    }

    public float getVoiceVolume() {
        return voiceVolume;
    }

    public float getProximityRange() {
        return proximityRange;
    }

    // Inner class to handle voice audio playback
    private class VoiceSource {
        private SourceDataLine line;
        private float volume = 1.0f;
        private Vector3f position = new Vector3f();

        public VoiceSource() {
            try {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
            } catch (Exception e) {
                System.err.println("Error creating voice source: " + e.getMessage());
            }
        }

        public void setPosition(float x, float y, float z) {
            position.set(x, y, z);
        }

        public void setVolume(float volume) {
            this.volume = volume;
            if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                // Convert linear volume to dB scale
                float dB = 20f * (float) Math.log10(Math.max(0.0001f, volume));
                dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                gainControl.setValue(dB);
            }
        }

        public void playAudio(byte[] audioData) {
            if (line != null) {
                // Apply volume by modifying audio data
                byte[] volumeAdjustedData = applyVolume(audioData, volume);
                line.write(volumeAdjustedData, 0, volumeAdjustedData.length);
            }
        }

        private byte[] applyVolume(byte[] audioData, float volume) {
            // For 16-bit audio (2 bytes per sample)
            byte[] result = new byte[audioData.length];

            for (int i = 0; i < audioData.length; i += 2) {
                // Convert bytes to short (16-bit sample)
                short sample = (short) (((audioData[i+1] & 0xff) << 8) | (audioData[i] & 0xff));

                // Apply volume
                sample = (short) (sample * volume);

                // Convert back to bytes
                result[i] = (byte) (sample & 0xff);
                result[i+1] = (byte) ((sample >> 8) & 0xff);
            }

            return result;
        }

        public void cleanup() {
            if (line != null) {
                line.stop();
                line.close();
            }
        }
    }
}