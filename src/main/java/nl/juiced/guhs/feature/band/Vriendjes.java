package nl.juiced.guhs.feature.band;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;

/**
 * Friendships between band guhs (2.10): the data side. Points only ever go up; at {@link #VRIENDJES} two guhs are
 * friends, at {@link #BESTIES} besties (the best-friend duo in the Guhdex). The behaviour (walking, cuddling, sleeping and
 * playing together, who earns points how) is the samen feature.
 */
public final class Vriendjes {
    public static final int VRIENDJES = 6000;
    public static final int BESTIES = 24000;

    @FunctionalInterface
    public interface VriendLuisteraar {
        void nieuw(MinecraftServer s, UUID a, UUID b, boolean besties);
    }

    private static final List<VriendLuisteraar> NIEUW = new CopyOnWriteArrayList<>();

    private Vriendjes() {
    }

    public static void opNieuw(VriendLuisteraar l) {
        NIEUW.add(l);
    }

    static String sleutel(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    /** Adds points to two band guhs' friendship (never subtracts); returns the new total (0 when not two band guhs). */
    public static int samen(Mob a, Mob b, int punten) {
        if (a == b || !Band.isBandGuh(a) || !Band.isBandGuh(b) || a.getServer() == null) {
            return 0;
        }
        return samen(a.getServer(), Band.id(a), Band.id(b), punten);
    }

    /** {@link #samen(Mob, Mob, int)} by band id. */
    public static int samen(MinecraftServer s, UUID a, UUID b, int punten) {
        if (a.equals(b)) {
            return 0;
        }
        BandData data = BandData.get(s);
        String k = sleutel(a, b);
        int voor = data.vriendjes.getOrDefault(k, 0);
        if (punten <= 0) {
            return voor;
        }
        int na = (int) Math.min(Integer.MAX_VALUE, (long) voor + punten);
        data.vriendjes.put(k, na);
        data.setDirty();
        boolean vriend = voor < VRIENDJES && na >= VRIENDJES, bestie = voor < BESTIES && na >= BESTIES;
        if (vriend || bestie) {
            for (VriendLuisteraar l : NIEUW) {
                try {
                    if (vriend) {
                        l.nieuw(s, a, b, false);
                    }
                    if (bestie) {
                        l.nieuw(s, a, b, true);
                    }
                } catch (RuntimeException e) {
                    LogUtils.getLogger().warn("Vriendjes listener failed", e);
                }
            }
        }
        return na;
    }

    public static int punten(MinecraftServer s, UUID a, UUID b) {
        return BandData.get(s).vriendjes.getOrDefault(sleutel(a, b), 0);
    }

    public static boolean vrienden(MinecraftServer s, UUID a, UUID b) {
        return punten(s, a, b) >= VRIENDJES;
    }

    public static boolean besties(MinecraftServer s, UUID a, UUID b) {
        return punten(s, a, b) >= BESTIES;
    }

    /** The friends of a guh, most points first. */
    public static List<UUID> vriendenVan(MinecraftServer s, UUID bandId) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>();
        String self = bandId.toString();
        for (Map.Entry<String, Integer> e : BandData.get(s).vriendjes.entrySet()) {
            if (e.getValue() < VRIENDJES) {
                continue;
            }
            String[] p = e.getKey().split("\\|");
            if (p.length == 2 && (p[0].equals(self) || p[1].equals(self))) {
                list.add(Map.entry(UUID.fromString(p[0].equals(self) ? p[1] : p[0]), e.getValue()));
            }
        }
        list.sort((x, y) -> Integer.compare(y.getValue(), x.getValue()));
        return list.stream().map(Map.Entry::getKey).toList();
    }

    /** Its best friend (besties only), or null. */
    @Nullable
    public static UUID bestie(MinecraftServer s, UUID bandId) {
        List<UUID> v = vriendenVan(s, bandId);
        return !v.isEmpty() && besties(s, bandId, v.get(0)) ? v.get(0) : null;
    }
}
