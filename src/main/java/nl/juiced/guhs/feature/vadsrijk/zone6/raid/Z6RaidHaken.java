package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;

/**
 * U-Z6RAID: what other slices hear from the raid.
 * <ul>
 *   <li>{@link #OPGEVROLIJKT}: Ringeling was cheered up in a clear (per player of the group). Add-only list.</li>
 *   <li>The soft hook to u-z6verhaal: {@code nl.juiced.guhs.feature.vadsrijk.zone6.verhaal.Z6Verhaal.ringelingOpgevrolijkt(ServerPlayer)}
 *       is called by reflection when that class exists (it sets the story flag, starts the flash F4, makes Ringeling an NPC in
 *       Saffraanpoort...). Without it nothing breaks.</li>
 * </ul>
 */
public final class Z6RaidHaken {
    public static final List<Consumer<ServerPlayer>> OPGEVROLIJKT = new CopyOnWriteArrayList<>();
    private static boolean gezocht;
    @Nullable
    private static Method verhaal;
    /** Gametests: how often the hook ran. */
    public static int geroepen;

    private Z6RaidHaken() {
    }

    static void opgevrolijkt(ServerPlayer p) {
        geroepen++;
        for (Consumer<ServerPlayer> c : OPGEVROLIJKT) {
            c.accept(p);
        }
        if (!gezocht) {
            gezocht = true;
            try {
                verhaal = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone6.verhaal.Z6Verhaal").getMethod("ringelingOpgevrolijkt", ServerPlayer.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                verhaal = null;
            }
        }
        if (verhaal != null) {
            try {
                verhaal.invoke(null, p);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // the story slice failed: the raid's own reward stands
            }
        }
    }
}
