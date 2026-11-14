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
 * </ul>
 */
public final class Z3BazenApi {
    public static final List<BiConsumer<ServerPlayer, String>> MOMENT = new CopyOnWriteArrayList<>();
    public static volatile Predicate<ServerPlayer> HEEFT_SPLINTERS = p -> true;
    /**
     * (M-BAZEN) Does the story send this player into the Hart van de Boom yet (npc: hq13 active or done)? The Hart bosses fight
     * anyone; this only decides the "De wortels slapen nog..." hint at the gate. Default true (no npc slice = no hint).
     */
    public static volatile Predicate<ServerPlayer> HART_VERHAAL = p -> true;

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
        return p.level() instanceof ServerLevel sl && startVahoegnir(herinnering, sl);
    }

    private static boolean startVahoegnir(boolean herinnering, ServerLevel sl) {
        // 1.10.1: also while a beaten one still plays its ending (it is "geplofd", so Arenas.baas misses it): a second one would
        // spawn and the old one's clean-up would take the new fight's shield branches away; the caller retries a bit later
        if (Arenas.baas(sl, Z3BazenRegistry.ARENA_NEST) != null || !alleVahoegnirs(sl).isEmpty()) {
            return false;
        }
        aanvraag = new Aanvraag(herinnering);
        Arenas.reset(sl.getServer(), Z3BazenRegistry.ARENA_NEST);
        return true;
    }

    // ---- (M-BAZEN) dev: find him, take the player there, clean up ----------------------------------------------------------

    /** Every Vahoegnir in the loaded level (the arena's own and any orphan: one that wandered off or outlived a reload). */
    public static List<Vahoegnir> alleVahoegnirs(ServerLevel sl) {
        List<Vahoegnir> l = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.Entity e : nl.juiced.guhs.world.TikWacht.alle(sl.getAllEntities())) {
            if (e instanceof Vahoegnir v && !v.isRemoved()) {
                l.add(v);
            }
        }
        return l;
    }

    private static String pos(BlockPos b) {
        return b.getX() + " " + b.getY() + " " + b.getZ();
    }

    /** Where is he? (dev) */
    public static String vahoegnirWaar(ServerLevel sl) {
        List<Vahoegnir> l = alleVahoegnirs(sl);
        if (l.isEmpty()) {
            return "Geen Vahoegnir geladen" + (aanvraag != null ? " (hij landt zodra het nest geladen is)" : "") + ". Het nest: "
                    + pos(Z3BazenPlekken.NEST_CENTRUM) + ".";
        }
        StringBuilder t = new StringBuilder();
        for (Vahoegnir v : l) {
            t.append(t.isEmpty() ? "" : "\n").append("Vahoegnir @ ").append(pos(v.blockPosition())).append(" (fase ").append(v.fase()).append(", ")
                    .append((int) v.vads()).append("/").append((int) v.maxVads()).append(" Vads").append(v.geplofd() ? ", in zijn einde" : "")
                    .append(v.herinnering ? ", herinnering" : "").append(v.arena() == null ? ", ZONDER arena: reset hem" : "").append(")");
        }
        return t.toString();
    }

    /**
     * {@code /guhs vadsrijk z3bazen vahoegnir [herinnering]}: already there = you go to the nest edge and get his coordinates;
     * otherwise he is called and you go to the nest edge (he only lands while the nest is loaded, so being there matters).
     */
    public static String devVahoegnir(ServerPlayer p, boolean herinnering) {
        ServerLevel sl = nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.level(p.level().getServer());
        if (sl == null) {
            return "Geen Vadsrijk-wereld.";
        }
        sl.getChunk(Z3BazenPlekken.NEST_CENTRUM);
        boolean er = Arenas.baas(sl, Z3BazenRegistry.ARENA_NEST) != null || !alleVahoegnirs(sl).isEmpty();
        boolean nieuw = !er && startVahoegnir(herinnering, sl);
        BlockPos d = Z3BazenPlekken.NEST_DEUR;
        p.teleportTo(sl, d.getX() + 0.5, d.getY() + 0.1, d.getZ() + 0.5, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
        if (nieuw) {
            return (herinnering ? "Een herinnering aan Vahoegnir..." : "Vahoegnir landt in het oude nest.") + " Je staat aan de nestrand " + pos(d)
                    + "; het nest (arena) is " + pos(Z3BazenPlekken.NEST_CENTRUM) + ".";
        }
        return "Hij is er al. " + vahoegnirWaar(sl) + "\nJe staat aan de nestrand " + pos(d) + ". Kwijt of vast? /guhs vadsrijk z3bazen vahoegnir reset";
    }

    /** Dev: every Vahoegnir away (the arena's and orphans; the nest branches go back), no pending landing. Returns how many. */
    public static int resetVahoegnir(ServerLevel sl) {
        aanvraag = null;
        int n = 0;
        for (Vahoegnir v : alleVahoegnirs(sl)) {
            v.discard();
            n++;
        }
        Arenas.reset(sl.getServer(), Z3BazenRegistry.ARENA_NEST);   // (no aanvraag: the maker makes nothing)
        nl.juiced.guhs.feature.vadsrijk.bazen.Herstel.terug(sl, Vahoegnir.H_TAKKEN);
        return n;
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
