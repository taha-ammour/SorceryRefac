package org.example;

import org.example.engine.*;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Main Menu ===");
        System.out.println("1. Run Game Engine");
        System.out.println("2. Among Us Lobby");
        System.out.print("Select option (1 or 2): ");
        int option = scanner.nextInt();
        scanner.nextLine(); // consume newline

        if (option == 1) {
            // Create and initialize the engine with window dimensions and a title.
            Engine engine = new Engine();
            engine.init(800, 600, "Atlas Example");

            // Create a new scene.
            Scene scene = new Scene();

            // Example 1: Add a simple colored triangle to the scene.
            Triangle triangle = new Triangle();
            scene.addGameObject(triangle);

            // Example 2: Load a sprite sheet and extract sub-sprites.
            SpriteSheet sheet = new SpriteSheet("/textures/entities.png");

            // Extract a small tile from the sprite sheet.
            // The coordinates and size here assume that the atlas image has the expected layout.
            Sprite smallTile = sheet.getSprite(0, 256 - 16, 16, 16);
            smallTile.setPosition(100, 100);
            // Define a palette using three-digit codes (each digit scaled from 0 to 5).
            String[] paletteCodes = {"020", "100", "532", "430"};
            smallTile.setPaletteFromCodes(paletteCodes);
            smallTile.setScale(2.4f, 2.5f);
            scene.addGameObject(smallTile);


            FontSheet fontSheet = new FontSheet();
            BatchedFontObject font = new BatchedFontObject(
                    fontSheet,
                    "abcdefghi\njklmnop\nqrstuvwxyz\n1234567890",
                    100.0f, 400.f, 0.0f,
                    FontSheet.FONT_NONE,
                    0xFF0000, // Red (in 0xRRGGBB format).
                    1.0f, fontSheet.getSpriteShader()
            );
            scene.addGameObject(font);
            // Extract another sprite (a bigger tile) from a different region.
            Sprite biggerTile = sheet.getSprite(32, 32, 16, 16);
            biggerTile.setPosition(200, 100);
            biggerTile.setPaletteFromCodes(paletteCodes);
            scene.addGameObject(biggerTile);

            // Extract a sprite using a grid-based method.
            Sprite gridSprite = sheet.getSpriteByGrid(0, 15, 16, 16);
            gridSprite.setPaletteFromCodes(paletteCodes);
            gridSprite.setPosition(400, 400);
            gridSprite.setScale(4,4);
            scene.addGameObject(gridSprite);

            // Set the active scene and start the engine's main loop.
            engine.setActiveScene(scene);
            engine.run();
        } else if (option == 2) {
            // Lobby system for creating or joining a server.
            System.out.println("=== Among Us Lobby ===");
            System.out.println("1. Create Server");
            System.out.println("2. Join Server");
            System.out.print("Select option (1 or 2): ");
            int lobbyOption = scanner.nextInt();
            scanner.nextLine(); // consume newline

            if (lobbyOption == 1) {
                try {
                    // Automatically find free ports.
                    int tcpPort = PortUtil.getFreeTcpPort();
                    int udpPort = PortUtil.getFreeUdpPort();
                    System.out.println("Assigning TCP port: " + tcpPort);
                    System.out.println("Assigning UDP port: " + udpPort);

                    LobbyServer server = new LobbyServer(tcpPort, udpPort);
                    String lobbyCode = server.getLobbyCode();
                    System.out.println("Server created successfully.");
                    System.out.println("Your Lobby Code: " + lobbyCode);
                    System.out.println("Your IP: localhost (if running locally)");
                    System.out.print("Enter your username: ");
                    String username = scanner.nextLine();

                    // The host also joins using a client.
                    LobbyClient client = new LobbyClient("localhost", tcpPort, udpPort);
                    client.joinLobby(username, lobbyCode);
                    System.out.println("You are the host. Type messages to chat.");
                    System.out.println("Type '/exit' to shut down the server.");

                    while (true) {
                        String message = scanner.nextLine();
                        if (message.equalsIgnoreCase("/exit")) {
                            break;
                        }
                        client.sendChatMessage(username, message);
                    }
                    server.shutdown();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (lobbyOption == 2) {
                try {
                    System.out.print("Enter server IP: ");
                    String serverIp = scanner.nextLine();
                    System.out.print("Enter TCP port: ");
                    int tcpPort = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print("Enter UDP port: ");
                    int udpPort = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print("Enter lobby code: ");
                    String lobbyCode = scanner.nextLine();
                    System.out.print("Enter your username: ");
                    String username = scanner.nextLine();

                    LobbyClient client = new LobbyClient(serverIp, tcpPort, udpPort);
                    client.joinLobby(username, lobbyCode);
                    System.out.println("Attempting to join lobby...");
                    System.out.println("Type messages to chat. Type '/exit' to exit.");

                    while (true) {
                        String message = scanner.nextLine();
                        if (message.equalsIgnoreCase("/exit")) {
                            break;
                        }
                        client.sendChatMessage(username, message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Invalid lobby option. Exiting.");
            }
        } else {
            System.out.println("Invalid option. Exiting.");
        }

        scanner.close();
    }
}