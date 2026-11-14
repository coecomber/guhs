package nl.juiced.guhs.feature.vadsrijk.wereld;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeon;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeons;
import nl.juiced.guhs.feature.vadsrijk.indeling.Barrieres;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.Ondergrond;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;

/**
 * 1.10.2: back where you left. When a player leaves the Vadsrijk (a return portal, a self-built portal, {@code /guhs vadsrijk}),
 * {@link #onthoud} keeps the spot he left from ({@code SpelerData.extra("wereld").uitgang}); the next entry from outside
 * ({@link #ingang}) puts him there again, on a free floor next to the portal, facing away from it (so he does not walk straight
 * back through). Rules:
 * <ul>
 *   <li>never been in (nothing remembered): the normal start (the jetty of Knuffelkamp), see {@code VadsrijkCommando.naarBinnen};</li>
 *   <li>leaving from inside a dungeon: the spot in front of that dungeon's main door is kept, never the instance itself;</li>
 *   <li>not kept at all: during a scene, outside the island sea, in the proeftuin;</li>
 *   <li>on entry the spot must still be good: its zone open for this player (zone gate), no dungeon, inside a free area of the
 *       barriers, and a real floor with room (searched a few blocks round). Otherwise: the arrival spot of the highest open zone
 *       up to the spot's own zone (De Haven for zone 1, the jetty for the camp), and the jetty when that fails too.</li>
 * </ul>
 */
public final class Terugkeer {
    private Terugkeer() {
    }

    static final String SLEUTEL = "uitgang";
    /** How far round the kept spot a free floor is searched. */
    static final int ZOEK = 4;

    /** A destination: where to stand and which way to look. */
    public record Doel(Vec3 pos, float yaw, String waarom) {
    }

    // ---- leaving --------------------------------------------------------------------------------------------------------------

    /** The player is leaving the Vadsrijk now (called before the teleport out). */
    public static void onthoud(ServerPlayer p) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        BlockPos plek = uitgangVoor(p.getX(), p.getY(), p.getZ(), RijkScenes.bezig(p));
        CompoundTag w = Spelers.data(p).extra("wereld");
        if (plek == null) {
            return;   // (keep the previous good spot: a scene / the proeftuin never overwrites it)
        }
        CompoundTag t = new CompoundTag();
        t.putInt("x", plek.getX());
        t.putInt("y", plek.getY());
        t.putInt("z", plek.getZ());
        w.put(SLEUTEL, t);
    }

    /**
     * The spot to keep for someone leaving at x/y/z (null: keep nothing). In a dungeon: in front of its main door.
     * Public for the gametests.
     */
    @Nullable
    public static BlockPos uitgangVoor(double x, double y, double z, boolean inScene) {
        if (inScene || !Barrieres.inZeegebied(x, z) || Ondergrond.proeftuin(BlockPos.containing(x, y, z))) {
            return null;
        }
        Dungeon d = dungeonOp(x, y, z);
        if (d != null) {
            DungeonDeurData.Deur deur = DungeonDeur.hoofd(d.id());
            return deur == null ? null : deur.voor();
        }
        return BlockPos.containing(x, y, z);
    }

    @Nullable
    static Dungeon dungeonOp(double x, double y, double z) {
        try {
            for (Dungeon d : Dungeons.alle()) {
                if (d.bevat(x, y, z)) {
                    return d;
                }
            }
        } catch (RuntimeException | LinkageError e) {
            // (no dungeons)
        }
        return null;
    }

    @Nullable
    public static BlockPos onthouden(ServerPlayer p) {
        CompoundTag w = Spelers.data(p).extra("wereld");
        if (!w.contains(SLEUTEL)) {
            return null;
        }
        CompoundTag t = w.getCompoundOrEmpty(SLEUTEL);
        return new BlockPos(t.getIntOr("x", 0), t.getIntOr("y", 0), t.getIntOr("z", 0));
    }

    /** Tests / dev: forget the kept spot (null) or set one. */
    public static void zet(ServerPlayer p, @Nullable BlockPos plek) {
        CompoundTag w = Spelers.data(p).extra("wereld");
        if (plek == null) {
            w.remove(SLEUTEL);
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putInt("x", plek.getX());
        t.putInt("y", plek.getY());
        t.putInt("z", plek.getZ());
        w.put(SLEUTEL, t);
    }

    // ---- coming back ----------------------------------------------------------------------------------------------------------

    /**
     * Where an entry from outside goes for this player, or null for the normal start (nothing kept: the first time).
     * {@code start} = the normal start (the jetty) as the last fallback.
     */
    @Nullable
    public static Doel ingang(ServerPlayer p, ServerLevel sl, BlockPos start) {
        BlockPos plek = onthouden(p);
        if (plek == null) {
            return null;
        }
        int zone = ZonePoort.zoneOp(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5);
        boolean dicht = ZonePoort.dicht(p, zone);
        if (!dicht && dungeonOp(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5) == null && Barrieres.vrij(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5)) {
            Doel d = naast(sl, plek);
            if (d != null) {
                return d;
            }
        }
        // fallback: the arrival spot of the highest open zone up to the spot's own zone
        int z = Math.min(zone, ZonePoort.open());
        while (z > 0 && ZonePoort.dicht(p, z)) {
            z--;
        }
        BlockPos hub = hub(z);
        if (hub != null) {
            Vec3 v = WereldReizen.veiligeLanding(sl, hub);
            return new Doel(v, 180f, dicht ? "zone_dicht" : "ongeldig");
        }
        return new Doel(Vec3.atBottomCenterOf(start), 180f, dicht ? "zone_dicht" : "ongeldig");
    }

    /** The arrival spot of a zone (the airship landing / the travel map's spot); null = the camp (the normal start). */
    @Nullable
    static BlockPos hub(int zone) {
        try {
            return switch (zone) {
                case 1 -> WereldPlekken.HAVEN_LANDING;
                case 2 -> nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.DOK_LANDING;
                case 3 -> nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.DOK_LANDING;
                case 4 -> nl.juiced.guhs.feature.vadsrijk.zone4.verhaal.Z4VerhaalPlekken.LANDING;
                case 5 -> nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5VerhaalPlekken.AANKOMST;
                case 6 -> nl.juiced.guhs.feature.vadsrijk.zone6.verhaal.Z6VerhaalPlekken.AANKOMST;
                default -> null;
            };
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /**
     * A free floor next to {@code plek} (any level: also the survival side), out of every portal block, at least 2 blocks
     * (horizontally) from all portal blocks round it, facing away from them. Null = no floor with room within {@link #ZOEK} blocks.
     */
    @Nullable
    public static Doel naast(ServerLevel sl, BlockPos plek) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sl.getChunk((plek.getX() >> 4) + dx, (plek.getZ() >> 4) + dz);
            }
        }
        java.util.List<BlockPos> poort = portalen(sl, plek, ZOEK + 2);
        BlockPos beste = null;
        double bs = Double.MAX_VALUE;
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -ZOEK; dx <= ZOEK; dx++) {
                for (int dz = -ZOEK; dz <= ZOEK; dz++) {
                    BlockPos s = plek.offset(dx, dy, dz);
                    double score = dx * dx + dz * dz + dy * dy * 2;
                    if (score >= bs || !staan(sl, s) || teDicht(poort, s)) {
                        continue;
                    }
                    bs = score;
                    beste = s;
                }
            }
        }
        if (beste == null) {
            return null;
        }
        float yaw = 180f;
        if (!poort.isEmpty()) {
            double mx = 0, mz = 0;
            for (BlockPos q : poort) {
                mx += q.getX();
                mz += q.getZ();
            }
            double ax = beste.getX() - mx / poort.size(), az = beste.getZ() - mz / poort.size();
            if (ax * ax + az * az > 1e-4) {
                yaw = (float) Math.toDegrees(Math.atan2(-ax, az));   // Minecraft: looking along (-sin yaw, cos yaw) = away from the portal
            }
        }
        return new Doel(Vec3.atBottomCenterOf(beste), yaw, "terug");
    }

    /** Closer than 2 blocks (horizontally, within 3 high) to a portal block: one step and you are through it again. */
    static boolean teDicht(java.util.List<BlockPos> poort, BlockPos s) {
        for (BlockPos q : poort) {
            double dx = q.getX() - s.getX(), dz = q.getZ() - s.getZ();
            if (dx * dx + dz * dz < 4 && Math.abs(q.getY() - s.getY()) <= 3) {
                return true;
            }
        }
        return false;
    }

    static boolean isPortaal(BlockState b) {
        return b.getBlock() instanceof PortaalBlok || b.getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock;
    }

    /**
     * The survival side: the remembered entry spot stands in (or right by) a portal: a free floor next to it, facing away.
     * Null = keep the spot as it is (no portal there, or no room round it).
     */
    @Nullable
    public static Doel uitPortaal(ServerLevel l, Vec3 v) {
        BlockPos b = BlockPos.containing(v);
        if (!isPortaal(l.getBlockState(b)) && !isPortaal(l.getBlockState(b.above())) && !isPortaal(l.getBlockState(b.below()))) {
            return null;
        }
        return naast(l, b);
    }

    /** Can a player stand at s: a solid floor below, two free blocks (no fluid, no portal) for the body? */
    static boolean staan(ServerLevel sl, BlockPos s) {
        BlockState vloer = sl.getBlockState(s.below());
        if (vloer.getCollisionShape(sl, s.below()).isEmpty() || !vloer.getFluidState().isEmpty() || isPortaal(vloer)) {
            return false;
        }
        for (BlockPos q : new BlockPos[] {s, s.above()}) {
            BlockState b = sl.getBlockState(q);
            if (!b.getCollisionShape(sl, q).isEmpty() || !b.getFluidState().isEmpty() || isPortaal(b)) {
                return false;
            }
        }
        return true;
    }

    /** Every portal block (ours or a nether portal) within r blocks round plek. */
    static java.util.List<BlockPos> portalen(ServerLevel sl, BlockPos plek, int r) {
        java.util.List<BlockPos> l = new java.util.ArrayList<>();
        for (BlockPos q : BlockPos.betweenClosed(plek.offset(-r, -3, -r), plek.offset(r, 4, r))) {
            if (isPortaal(sl.getBlockState(q))) {
                l.add(q.immutable());
            }
        }
        return l;
    }
}
