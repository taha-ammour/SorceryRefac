package org.example;

import  com.esotericsoftware.kryo.Kryo;

public class NetworkRegistration {
    public static void registerClasses(Kryo kryo) {
        kryo.register(Packets.JoinRequest.class, 1);
        kryo.register(Packets.JoinResponse.class, 2);
        kryo.register(Packets.ChatMessage.class, 3);
        kryo.register(java.util.ArrayList.class, 4);
        kryo.register(String.class, 5);
        // Register any other classes that might be used.
    }
}
