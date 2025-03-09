package org.example;

public class Packets {

    public static class JoinRequest {
        public String username;
        public String lobbyCode;
    }

    public static class JoinResponse {
        public boolean accepted;
        public String message;
        public java.util.ArrayList<String> currentPlayers;
    }

    public static class ChatMessage {
        public String username;
        public String message;
    }

    public static class MovementUpdate {
        public int playerId;
        public float x, y;
    }
}
