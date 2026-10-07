package nl.juiced.guhs.feature.snuffel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.snuffel.Geuren.Geur;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Where the scents ARE: the scent sources. A source is a spot in the world or an entity that carries a {@link Geur}; a dog
 * that sniffs sees its scent meter swing harder the closer it comes, and finds the source by digging on the spot (a
 * source with {@code graven}) or by sniffing right next to it for a second (the others). Three ways to make one:
 * <ul>
 *   <li>data: {@code "geurbronnen"} in the island's eiland.json (positions relative to the island);</li>
 *   <li>code: {@link #plaats} puts one at a block of a level (kept with the level), {@link #haalWeg} removes it;</li>
 *   <li>an entity: {@link #opEntiteit} gives any entity a scent (kept with the entity).</li>
 * </ul>
 * Everything about finding is per player: a source a player found is gone for that player's nose only ({@link #vergeet}
 * brings it back). By id you hang a condition on a source ({@link #voorwaarde}: only while the story asks for it) and
 * something to do when it is found ({@link #bijVondst}); the rank rule is always there ({@link Geuren#ruikt}).
 */
public final class Geurbronnen {
    /** How far away a source is smelled when its data does not say. */
    public static final int BEREIK = 24;
    /** "On the spot": this close sideways, and not more than {@link #PLEK_HOOGTE} above or below. */
    public static final double PLEK = 1.75, PLEK_HOOGTE = 2.5;
    /** Persistent data of an entity that carries a scent: the scent id, and the source id (default: "entiteit_" + scent). */
    public static final String ENTITEIT_GEUR = "guhs_snuffel_geur", ENTITEIT_BRON = "guhs_snuffel_bron";

    /** A source as a dog meets it: where it is now (an entity moves), and the entity when it is one. */
    public record Bron(String id, Geur geur, Vec3 plek, @Nullable Entity entiteit, boolean graven, int bereik) {
    }

    /** What a nose smells: the source, how far, how strong (0..1) and whether the dog stands on the spot. */
    public record Neus(Bron bron, double afstand, float sterkte, boolean opDePlek) {
    }

    private static final Map<String, Predicate<ServerPlayer>> VOORWAARDEN = new ConcurrentHashMap<>();
    private static final Map<String, List<Consumer<ServerPlayer>>> BIJ_VONDST = new ConcurrentHashMap<>();
    private static final List<BiConsumer<ServerPlayer, Bron>> LUISTERAARS = new CopyOnWriteArrayList<>();

    private Geurbronnen() {
    }

    // =====================================================================================================================
    // making sources
    // =====================================================================================================================

    /** The sources that code placed in a level. */
    public static final class Geplaatst extends SavedData {
        public static final SavedDataType<Geplaatst> TYPE = GuhSavedData.tagType("snuffel_bronnen", Geplaatst::new, Geplaatst::load, Geplaatst::save);

        record Plek(String geur, BlockPos pos, boolean graven, int bereik) {
        }

        final Map<String, Plek> bronnen = new LinkedHashMap<>();

        private static Geplaatst load(CompoundTag tag) {
            Geplaatst g = new Geplaatst();
            ListTag l = tag.getListOrEmpty("Bronnen");
            for (int i = 0; i < l.size(); i++) {
                CompoundTag t = l.getCompoundOrEmpty(i);
                g.bronnen.put(t.getStringOr("Id", ""), new Plek(t.getStringOr("Geur", ""), BlockPos.of(t.getLongOr("Pos", 0L)),
                        t.getBooleanOr("Graven", true), t.getIntOr("Bereik", BEREIK)));
            }
            return g;
        }

        private CompoundTag save() {
            ListTag l = new ListTag();
            bronnen.forEach((id, p) -> {
                CompoundTag t = new CompoundTag();
                t.putString("Id", id);
                t.putString("Geur", p.geur());
                t.putLong("Pos", p.pos().asLong());
                t.putBoolean("Graven", p.graven());
                t.putInt("Bereik", p.bereik());
                l.add(t);
            });
            CompoundTag uit = new CompoundTag();
            uit.put("Bronnen", l);
            return uit;
        }
    }

    /** Puts a source at this block (a source with the same id in this level moves here). False: unknown scent. */
    public static boolean plaats(ServerLevel level, BlockPos pos, String id, String geur, boolean graven, int bereik) {
        if (Geuren.van(geur) == null) {
            return false;
        }
        Geplaatst g = level.getDataStorage().computeIfAbsent(Geplaatst.TYPE);
        g.bronnen.put(id, new Geplaatst.Plek(geur, pos.immutable(), graven, Math.max(2, bereik)));
        g.setDirty();
        return true;
    }

    public static boolean haalWeg(ServerLevel level, String id) {
        Geplaatst g = level.getDataStorage().computeIfAbsent(Geplaatst.TYPE);
        boolean weg = g.bronnen.remove(id) != null;
        if (weg) {
            g.setDirty();
        }
        return weg;
    }

    /** This entity carries a scent from now on (bronId null: "entiteit_" + the scent, shared by every entity that carries it). */
    public static void opEntiteit(Entity e, String geur, @Nullable String bronId) {
        e.getPersistentData().putString(ENTITEIT_GEUR, geur);
        if (bronId != null) {
            e.getPersistentData().putString(ENTITEIT_BRON, bronId);
        } else {
            e.getPersistentData().remove(ENTITEIT_BRON);
        }
    }

    /** Only players for whom this holds can smell (and find) the source with this id. One condition per id. */
    public static void voorwaarde(String bronId, Predicate<ServerPlayer> mag) {
        VOORWAARDEN.put(bronId, mag);
    }

    /** Runs every time a player finds the source with this id. */
    public static void bijVondst(String bronId, Consumer<ServerPlayer> dan) {
        BIJ_VONDST.computeIfAbsent(bronId, k -> new CopyOnWriteArrayList<>()).add(dan);
    }

    /** Hears every find. */
    public static void opVondst(BiConsumer<ServerPlayer, Bron> l) {
        LUISTERAARS.add(l);
    }

    // =====================================================================================================================
    // per player
    // =====================================================================================================================

    public static boolean gevonden(ServerPlayer p, String bronId) {
        return SnuffelData.bevat(p, "Gevonden", bronId);
    }

    /** The source can be found by this player again. */
    public static void vergeet(ServerPlayer p, String bronId) {
        SnuffelData.haalWeg(p, "Gevonden", bronId);
    }

    /** Can this player's nose pick up this source at all (rank, not found yet, the story's condition)? Distance aside. */
    public static boolean ruikbaar(ServerPlayer p, Bron b) {
        if (!Geuren.ruikt(p, b.geur()) || gevonden(p, b.id())) {
            return false;
        }
        Predicate<ServerPlayer> mag = VOORWAARDEN.get(b.id());
        return mag == null || mag.test(p);
    }

    /** Every source around this player (whether they can smell it or not): the island's, the placed ones, the entities. */
    public static List<Bron> bronnen(ServerPlayer p) {
        List<Bron> uit = new ArrayList<>();
        ServerLevel level = p.level();
        Eiland.Plaats plaats = Eiland.van(p);
        if (plaats != null) {
            for (Eiland.BronPlek b : plaats.opzet().bronnen()) {
                Geur g = Geuren.van(b.geur());
                if (g != null) {
                    uit.add(new Bron(b.id(), g, Vec3.atBottomCenterOf(plaats.wereld(b.plek())), null, b.graven(), b.bereik()));
                }
            }
        }
        Geplaatst geplaatst = level.getDataStorage().get(Geplaatst.TYPE);
        if (geplaatst != null) {
            geplaatst.bronnen.forEach((id, pl) -> {
                Geur g = Geuren.van(pl.geur());
                if (g != null) {
                    uit.add(new Bron(id, g, Vec3.atBottomCenterOf(pl.pos()), null, pl.graven(), pl.bereik()));
                }
            });
        }
        for (Entity e : level.getEntities(p, p.getBoundingBox().inflate(BEREIK + 8), x -> x.getPersistentData().contains(ENTITEIT_GEUR))) {
            String geur = e.getPersistentData().getStringOr(ENTITEIT_GEUR, "");
            Geur g = Geuren.van(geur);
            if (g != null) {
                uit.add(new Bron(e.getPersistentData().getStringOr(ENTITEIT_BRON, "entiteit_" + geur), g, e.position(), e, false, BEREIK));
            }
        }
        return uit;
    }

    private static boolean opDePlek(ServerPlayer p, Bron b) {
        double dx = p.getX() - b.plek().x, dz = p.getZ() - b.plek().z;
        return dx * dx + dz * dz <= PLEK * PLEK && Math.abs(p.getY() - b.plek().y) <= PLEK_HOOGTE;
    }

    /** What this player's nose smells now: the strongest source they can smell (null: nothing in the air). */
    @Nullable
    public static Neus ruik(ServerPlayer p) {
        Neus beste = null;
        for (Bron b : bronnen(p)) {
            double afstand = Math.sqrt(p.position().distanceToSqr(b.plek()));
            if (afstand > b.bereik() || !ruikbaar(p, b)) {
                continue;
            }
            boolean plek = opDePlek(p, b);
            float sterkte = plek ? 1f : (float) Math.max(0.04, 1.0 - afstand / b.bereik());
            if (beste == null || sterkte > beste.sterkte()) {
                beste = new Neus(b, afstand, sterkte, plek);
            }
        }
        return beste;
    }

    /** The source this player can dig up where they stand (null: nothing lies here for them). */
    @Nullable
    public static Bron graafbaarBij(ServerPlayer p) {
        Bron beste = null;
        double besteD = Double.MAX_VALUE;
        for (Bron b : bronnen(p)) {
            if (b.graven() && opDePlek(p, b) && ruikbaar(p, b)) {
                double d = p.position().distanceToSqr(b.plek());
                if (d < besteD) {
                    beste = b;
                    besteD = d;
                }
            }
        }
        return beste;
    }

    /**
     * The player finds this source: it is gone for their nose, its scent is learned (when new), and whoever listens hears it.
     * False when the player had found it already.
     */
    public static boolean vind(ServerPlayer p, Bron b) {
        if (!SnuffelData.voegToe(p, "Gevonden", b.id())) {
            return false;
        }
        Snuffelen.vondst(p, b, Geuren.leer(p, b.geur().id()));
        List<Consumer<ServerPlayer>> dan = BIJ_VONDST.get(b.id());
        if (dan != null) {
            for (Consumer<ServerPlayer> c : dan) {
                c.accept(p);
            }
        }
        for (BiConsumer<ServerPlayer, Bron> l : LUISTERAARS) {
            l.accept(p, b);
        }
        return true;
    }
}
