package nl.juiced.guhs.feature.vissen;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * The float of the Guhvis-hengel (2.9): a vanilla fishing hook (it looks and bobs just the same), but when a fish bites
 * the time you have to reel in (the bite window, vanilla 1-2 seconds) depends on the level of your contest: makkelijk
 * 2-3,5 seconds, medium as it always was, lastig barely more than half a second. The window is vanilla's private
 * "nibble" counter, set right after vanilla picked its own.
 */
public class GuhvisDobber extends FishingHook {
    @Nullable
    private static final Field NIBBLE = nibbleField();
    private final Niveau niveau;
    private int lastNibble;

    public GuhvisDobber(Player player, Level level, int luck, int lureSpeed, Niveau niveau) {
        super(player, level, luck, lureSpeed);
        this.niveau = niveau;
    }

    public Niveau niveau() {
        return niveau;
    }

    /** The bite window in ticks for a level (medium: -1, vanilla picks its own 20-40). */
    public static int venster(Niveau niveau, RandomSource random) {
        return switch (niveau) {
            case MAKKELIJK -> Mth.nextInt(random, 40, 70);
            case MEDIUM -> -1;
            case LASTIG -> Mth.nextInt(random, 10, 18);
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || NIBBLE == null || niveau == Niveau.MEDIUM || isRemoved()) {
            return;
        }
        int nibble = nibble();
        if (nibble > 0 && lastNibble <= 0) {                   // a bite, just now: our window instead of vanilla's
            nibble = venster(niveau, random);
            setNibble(nibble);
        }
        lastNibble = nibble;
    }

    /** How many ticks are left to reel in (0 = nothing on the hook). */
    public int nibble() {
        try {
            return NIBBLE == null ? 0 : NIBBLE.getInt(this);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    void setNibble(int ticks) {
        try {
            if (NIBBLE != null) {
                NIBBLE.setInt(this, ticks);
            }
        } catch (IllegalAccessException ignored) {
            // (then it's just the vanilla window)
        }
    }

    /** Is the level's bite window in place? (False would mean: the vanilla window everywhere, the rest still works.) */
    public static boolean werkt() {
        return NIBBLE != null;
    }

    @Nullable
    private static Field nibbleField() {
        try {
            Field f = FishingHook.class.getDeclaredField("nibble");
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException e) {
            com.mojang.logging.LogUtils.getLogger().warn("Guhvis-dobber: no bite window per level ({}); the vanilla one is used", e.toString());
            return null;
        }
    }
}
