package nl.juiced.guhs.feature.guhpad;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.GrootVerhaal;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.Wereld;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.item.GuhCompassItem;

/**
 * Het Guhpad: where the Superkompas option "Mijn verhaal" points when the player follows no questline with a goal of
 * its own ({@code feature.verhaal.Doelen.kompas} asks {@link #doel}): the NEAREST structure of a big story the player
 * has not finished yet and may begin (a story of a later world only once every big story of the worlds before it is
 * finished, and only when the player may go into the world where it begins). When such a story begins in another
 * dimension, the compass shows the portal last used, as it does for a questline's own goal.
 * <p>
 * It has nothing to do with the "been there" ticks of the Superkompas list (1.3.1, per place): a story counts as done when
 * its questline is finished, not when its building was seen.
 */
public final class GuhpadKompas {
    /** How long (ticks) an answer is kept per player, and how far (blocks) they may walk before it is looked up again. */
    public static final int ZOEK_TICKS = 600, ZOEK_LOOP = 96;

    private record Gezocht(String kandidaten, ResourceKey<Level> dim, BlockPos vanaf, long tick, Optional<Doel> doel) {
    }

    private static final Map<UUID, Gezocht> GEZOCHT = new ConcurrentHashMap<>();
    /** The middle of the nearest copy of a structure for a player (null: none). The game tests put their own in. */
    static volatile BiFunction<ServerPlayer, String, BlockPos> zoeker = GuhpadKompas::zoek;

    private static BlockPos zoek(ServerPlayer p, String structuur) {
        return GuhCompassItem.findCenter(p.level(), ResourceKey.create(Registries.STRUCTURE, Guhs.id(structuur)), p.blockPosition());
    }

    /** May this player begin this big story now (see the class text)? */
    public static boolean magBeginnen(ServerPlayer p, GrootVerhaal v) {
        for (Wereld w : Wereld.values()) {
            if (w.ordinal() >= v.wereld().ordinal()) {
                break;
            }
            for (GrootVerhaal eerder : GroteVerhalen.van(w)) {
                if (!eerder.klaar(p)) {
                    return false;
                }
            }
        }
        return Guhpad.open(p, v.begin());
    }

    /** The big stories "Mijn verhaal" can point at for this player: not finished, and they may begin them. */
    public static List<GrootVerhaal> kandidaten(ServerPlayer p) {
        List<GrootVerhaal> out = new ArrayList<>();
        for (GrootVerhaal v : GroteVerhalen.alle()) {
            if (!v.klaar(p) && !v.structuren().isEmpty() && magBeginnen(p, v)) {
                out.add(v);
            }
        }
        return out;
    }

    /**
     * Where "Mijn verhaal" points for a player without a goal of a followed questline: the nearest structure of a story
     * they have not done (a fixed spot in their own dimension), else such a story in another dimension (the compass then
     * shows the portal), else null. The text is the name of the place.
     */
    @Nullable
    public static Doel doel(ServerPlayer p) {
        return doel(p, p.level().dimension());
    }

    /** The same with the dimension the player is in passed in (the game tests have no Guhmensie). */
    @Nullable
    static Doel doel(ServerPlayer p, ResourceKey<Level> hier) {
        List<GrootVerhaal> kandidaten = kandidaten(p);
        if (kandidaten.isEmpty()) {
            GEZOCHT.remove(p.getUUID());
            return null;
        }
        StringBuilder ids = new StringBuilder();
        for (GrootVerhaal v : kandidaten) {
            ids.append(v.id()).append(',');
        }
        long nu = p.level().getServer().getTickCount();
        Gezocht g = GEZOCHT.get(p.getUUID());
        if (g != null && g.kandidaten().contentEquals(ids) && g.dim() == hier && nu - g.tick() < ZOEK_TICKS && nu >= g.tick()
                && g.vanaf().closerThan(p.blockPosition(), ZOEK_LOOP)) {
            return g.doel().orElse(null);
        }
        Doel beste = null, elders = null;
        double besteAfstand = Double.MAX_VALUE;
        for (GrootVerhaal v : kandidaten) {
            for (Doel plek : plekken(p, v)) {
                if (plek.structuur() != null && Sluiers.isVerborgen(p, plek.structuur())) {
                    continue;
                }
                if (plek.dim() != hier) {
                    if (elders == null) {
                        elders = plek;
                    }
                    continue;
                }
                BlockPos daar = plek.plek() != null ? plek.plek() : zoeker.apply(p, plek.structuur());
                if (daar != null) {
                    double afstand = Math.hypot(daar.getX() - p.getX(), daar.getZ() - p.getZ());
                    if (afstand < besteAfstand) {
                        besteAfstand = afstand;
                        beste = Doel.plek(plek.dim(), daar, plek.tekst());
                    }
                }
            }
        }
        Doel doel = beste != null ? beste : elders;
        GEZOCHT.put(p.getUUID(), new Gezocht(ids.toString(), hier, p.blockPosition(), nu, Optional.ofNullable(doel)));
        return doel;
    }

    /**
     * Where this big story goes on for this player: the goal its own questline names for the step they are at (the
     * Knabbelring knows the nearest Guhdalf, also at an old barbecue pit; a story they are half way through knows its next
     * place), else the structures where it begins.
     */
    static List<Doel> plekken(ServerPlayer p, GrootVerhaal v) {
        for (String id : v.lijnen()) {
            Verhaallijn l = Verhaallijnen.van(id);
            if (l != null && !l.klaar(p)) {
                Doel eigen = l.aanDeBeurt(p) ? l.doel(p) : null;
                if (eigen != null) {
                    return List.of(eigen);
                }
                break;   // (only the questline the player is at)
            }
        }
        List<Doel> out = new ArrayList<>();
        ResourceKey<Level> dim = v.begin().dimensie();
        if (dim != null) {
            for (String structuur : v.structuren()) {
                out.add(Doel.structuur(dim, structuur, Component.translatable("structure.guhs." + structuur)));
            }
        }
        return out;
    }

    /** Forget what was looked up for this player (logout; a test that changes the world). */
    static void vergeet(UUID speler) {
        GEZOCHT.remove(speler);
    }

    private GuhpadKompas() {
    }
}
