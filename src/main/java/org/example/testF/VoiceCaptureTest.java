// Create a simple test class in your project
package org.example.testF;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public class VoiceCaptureTest {
    public static void main(String[] args) {
        try {
            // Set up audio format for voice capture
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(micInfo)) {
                System.err.println("Microphone not supported");
                return;
            }

            System.out.println("Starting microphone test. Speak into your microphone...");

            // Set up microphone
            TargetDataLine micLine = (TargetDataLine) AudioSystem.getLine(micInfo);
            micLine.open(format);
            micLine.start();

            // Set up speaker
            SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            speaker.open(format);
            speaker.start();

            // Capture and playback loop
            byte[] buffer = new byte[1024];
            System.out.println("Recording... Speak into your microphone (test will run for 10 seconds)");
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < 10000) {
                int bytesRead = micLine.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    // Play back immediately to test
                    speaker.write(buffer, 0, bytesRead);
                }
            }

            // Clean up
            micLine.stop();
            micLine.close();
            speaker.stop();
            speaker.close();

            System.out.println("Microphone test complete");

        } catch (Exception e) {
            System.err.println("Error in voice capture test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}