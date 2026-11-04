package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.roofvogel.Kruinroofvogel;

/**
 * The zone-3 bazen API for the other slices (npc: the story steps, systemen: the Gouden Veren at Veertje, wereld: nothing).
 * <ul>
 *   <li>{@link #MOMENT}: story moments per player: "piepvel_weg" (after flash F1: hartsplinter 4/5, Piepje comes out of your
 *       bag), "nestwachter_gered" (splinter 5/5, the Nestwachter NPC), "vahoegnir_splinter_1..3", "vahoegnir_opgevrolijkt",
 *       "vahoegje" (hatched), "hoogvads" (the beam), "troonzaal_klaar" (after flash F2: zone3_klaar, z3_hq15),
 *       "baas:&lt;id&gt;" (any zone-3 boss beaten: taleggio, brokkel, roquefort, piepvel, wortelwurger, nestwachter, bleu,
 *       kruinroofvogel, vahoegnir).</li>
 *   <li>{@link #startVahoegnir}: hq14 (after the ride) or the Droomsteen in the Nestkapel ("herinnering").</li>
 *   <li>{@link #HEEFT_SPLINTERS}: who may put splinters back in the fight (default everyone: npc can narrow it).</li>
 *   <li>{@link Z3BazenLoot#ruilVeren}: 25 Gouden Veren -> the gold Pluimguh (Veertje may call it; the feather item does it too).</li>
 * </ul>
 */
public final class Z3BazenApi {
    public static final List<BiConsumer<ServerPlayer, String>> MOMENT = new CopyOnWriteArrayList<>();
    public static volatile Predicate<ServerPlayer> HEEFT_SPLINTERS = p -> true;

    private record Aanvraag(boolean herinnering) {
    }

    @Nullable
    private static volatile Aanvraag aanvraag;

    private Z3BazenApi() {
    }

    public static void moment(ServerPlayer p, String id) {
        for (BiConsumer<ServerPlayer, String> m : MOMENT) {
            try {
                m.accept(p, id);
            } catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger("guhs/z3bazen").warn("z3bazen moment {} failed", id, e);
            }
        }
    }

    static void verslagen(Z3Baas b) {
        if (!(b.level() instanceof ServerLevel sl)) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (b.bijdragen().spelers().contains(p.getUUID())) {
                moment(p, "baas:" + b.baasId());
            }
        }
    }

    /**
     * Vahoegnir lands in the old nest (the arena spawns him on its next tick, if he isn't there already). {@code herinnering}:
     * a Droomsteen memory (repeatable, loot; no story moments like the throne room). Returns false when he is already there.
     */
    public static boolean startVahoegnir(ServerPlayer p, boolean herinnering) {
        if (!(p.level() instanceof ServerLevel sl) || Arenas.baas(sl, Z3BazenRegistry.ARENA_NEST) != null) {
            return false;
        }
        aanvraag = new Aanvraag(herinnering);
        Arenas.reset(sl.getServer(), Z3BazenRegistry.ARENA_NEST);
        return true;
    }

    /** The arena's maker: only when asked. */
    @Nullable
    static Vahoegnir maakVahoegnir(ServerLevel sl) {
        Aanvraag a = aanvraag;
        if (a == null) {
            return null;
        }
        aanvraag = null;
        Vahoegnir v = Z3BazenRegistry.VAHOEGNIR.get().create(sl, net.minecraft.world.entity.EntitySpawnReason.SPAWNER);
        if (v != null) {
            v.herinnering = a.herinnering();
            v.stelIn(50, null, false, java.util.Set.of());
        }
        return v;
    }

    /** A player right-clicked an unlit lantern post: the boss whose arena it is decides; outside a fight it simply lights. */
    static boolean paal(ServerLevel sl, BlockPos pos, ServerPlayer p) {
        Arena a = Arenas.op(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (a != null) {
            VadsrijkBaas b = Arenas.baas(sl, a.id());
            if (b instanceof Z3Baas z && !z.geplofd()) {
                return z.paalAan(sl, pos, p);
            }
        }
        sl.setBlock(pos, sl.getBlockState(pos).setValue(Z3Blokken.Lantaarnpaal.LIT, true), 3);
        return true;
    }

    /** (economie Rijdieren hook) May one ride at this spot although it is in a boss arena? (the Kruinroofvogel chase) */
    public static boolean magRijden(Vec3 v) {
        return Kruinroofvogel.magRijden(v);
    }
}
