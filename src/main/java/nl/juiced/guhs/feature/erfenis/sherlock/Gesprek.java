package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * A dialogue of the story in the talking screen (verhaal.Praat): the lines of a group {@code sherlock.<groep>.<i>} with the
 * speakers of {@link Regels}. Story characters stand next to the player for it (Sherlock, Vadson, Morris, ... via
 * {@link Cast#bijSpeler}); a witness of the case who is already there speaks herself; anybody else (Hendrik de Haan, the
 * babyguhs, the narrator "") is a name without a picture. A speaker id ending in "!" is Morris-Mika shoving you aside
 * (gently: a little push, never damage).
 */
public final class Gesprek {
    /** The default sleutel: nothing has to happen after reading. */
    public static final String VERHAAL = "sherlock_verhaal";

    private Gesprek() {
    }

    /** Plays a group of lines; the answers (if any) come back to the {@link Praat#luister} listener of the sleutel. */
    public static boolean scene(ServerPlayer p, String groep, String sleutel, Object[] args, Praat.Optie... opties) {
        List<String> sprekers = Regels.sprekers(groep);
        if (sprekers.isEmpty()) {
            return false;
        }
        Map<String, Entity> erbij = new LinkedHashMap<>();
        List<Praat.Regel> regels = new ArrayList<>();
        boolean duw = false;
        for (int i = 0; i < sprekers.size(); i++) {
            String wie = sprekers.get(i);
            if (wie.endsWith("!")) {
                wie = wie.substring(0, wie.length() - 1);
                duw = true;
            }
            Entity e = erbij.containsKey(wie) ? erbij.get(wie) : spreker(p, wie, erbij.size());
            erbij.putIfAbsent(wie, e);
            String naam = e != null ? "" : wie.isEmpty() ? "sherlock.wie.verteller" : "sherlock.wie." + wie;
            regels.add(new Praat.Regel(e, naam, "sherlock." + groep + "." + i, args));
        }
        Praat.scene(p, sleutel, regels, opties);
        if (duw) {
            Entity morris = erbij.get("morris");
            Vec3 van = morris != null ? morris.position() : p.position().add(p.getLookAngle());
            Vec3 weg = p.position().subtract(van).multiply(1, 0, 1);
            weg = weg.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : weg.normalize();
            p.knockback(0.55, -weg.x, -weg.z);
            p.hurtMarked = true;
        }
        return true;
    }

    public static boolean scene(ServerPlayer p, String groep) {
        return scene(p, groep, VERHAAL, new Object[0]);
    }

    /** Who says it: a witness of this player who stands close, else a story character beside the player, else nobody. */
    @Nullable
    private static Entity spreker(ServerPlayer p, String wie, int n) {
        if (wie.isEmpty()) {
            return null;
        }
        Entity getuige = Cast.van(p.level().getServer(), p.getUUID(), "getuige:" + wie);
        if (getuige != null && getuige.distanceTo(p) < 16) {
            return getuige;
        }
        return Cast.bijSpeler(p, wie, n);
    }
}
