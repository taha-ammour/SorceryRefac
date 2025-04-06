package org.example.game.Spells;

public class SpellFactory {
    public static AbstractSpell createSpell(String type, String playerId) {
        switch (type.toLowerCase()) {
            case "fire":
                return new FireSpell.FireSpellBuilder()
                        .playerId(playerId)
                        .build();
            case "ice":
                return new IceSpell.IceSpellBuilder()
                        .playerId(playerId)
                        .build();
            case "lightning":
                return new LightningSpell.LightningSpellBuilder()
                        .playerId(playerId)
                        .build();
            default:
                throw new IllegalArgumentException("Unknown spell type: " + type);
        }
    }

    public static AbstractSpell upgradeSpell(AbstractSpell spell) {
        return spell.upgrade();
    }
}
