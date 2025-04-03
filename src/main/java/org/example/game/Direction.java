package org.example.game;

public enum Direction {
    UP("u"),
    DOWN("d"),
    LEFT("rr"),  //rr is left
    RIGHT("r");

    private final String spriteSuffix;

    Direction(String spriteSuffix) {
        this.spriteSuffix = spriteSuffix;
    }


    public String getSpriteName() {
        return "player_sprite_" + spriteSuffix;
    }


    public String getSpriteSuffix() {
        return spriteSuffix;
    }
}
