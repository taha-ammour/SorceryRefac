package org.example.game.Spells;

public interface Spell {
    void cast();
    Spell upgrade();

    int getBuyCost();
    int getUpgradeCost();
    int getEnergyCost();
    double getDamage();
    double getCooldown();

    String getPlayerId();
    int getLevel();
    int getMaxLevel();

}
