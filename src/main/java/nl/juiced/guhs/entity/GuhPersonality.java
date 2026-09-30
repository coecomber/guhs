package nl.juiced.guhs.entity;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * Every guh has a personality (shown in the Guh menu). What each one does is handled in GuhEntity and its goals:
 * <ul>
 *   <li>PLAYFUL  - a bit faster, and now and then does happy "zoomies"</li>
 *   <li>LAZY     - slower, and wanders around much less</li>
 *   <li>VADSIG   - walks to kaas knabbels lying on the ground and eats them; easier to tame</li>
 *   <li>SHY      - wild: keeps away from players, unless they hold kaas knabbels; harder to tame</li>
 *   <li>BRAVE    - hits harder and never runs away</li>
 *   <li>CHATTY   - makes guh noises twice as often</li>
 *   <li>CUDDLY   - tamed: heals its owner half a heart now and then when close by</li>
 *   <li>CURIOUS  - walks up to nearby players to have a look at them</li>
 * </ul>
 */
public enum GuhPersonality {
    PLAYFUL(0.15, 3),
    LAZY(-0.2, 3),
    VADSIG(0, 2),
    SHY(0, 5),
    BRAVE(0, 3),
    CHATTY(0, 3),
    CUDDLY(0, 3),
    CURIOUS(0, 3);

    /** Extra walking speed (fraction of the base speed). */
    public final double speedBonus;
    /** 1 in this many kaas knabbels tames a wild guh. */
    public final int tameChance;

    GuhPersonality(double speedBonus, int tameChance) {
        this.speedBonus = speedBonus;
        this.tameChance = tameChance;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable("gui.guhs.personality." + id());
    }

    public Component description() {
        return Component.translatable("gui.guhs.personality." + id() + ".description");
    }

    public static GuhPersonality random(RandomSource random) {
        return values()[random.nextInt(values().length)];
    }

    public static GuhPersonality byId(String id) {
        for (GuhPersonality p : values()) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }
}
