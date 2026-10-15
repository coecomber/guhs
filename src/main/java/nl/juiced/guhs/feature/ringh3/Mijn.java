package nl.juiced.guhs.feature.ringh3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;

/**
 * bbq2 (ring-h3): one copy of De Mijnen van Knabbelmoria in the world. Everything the chapter knows is in template coordinates
 * ({@link Plekken}); a copy is turned any of four ways, so every position goes through here: {@link #wereld} (template ->
 * world) and {@link #lokaal} (world -> template).
 * <p>
 * {@link #van(ServerPlayer)} is the copy a player is at (within 48 blocks of its box), looked up at most once a second per
 * player and only in the Guhbarbecuether (or everywhere while {@link #OVERAL} is set: the game tests).
 */
public final class Mijn {
    public static final String STRUCTUUR = "knabbelmoria";
    /** (tests) the mine may stand in any dimension. */
    public static boolean OVERAL;
    private static final int OPNIEUW = 20;

    private record Gezien(@Nullable Mijn mijn, long tick, BlockPos plek, ServerLevel level) {
    }

    private static final Map<UUID, Gezien> BIJ = new ConcurrentHashMap<>();

    private final ServerLevel level;
    private final StructureStart start;
    private final BlockPos nul;
    private final Rotation draai;

    private Mijn(ServerLevel level, StructureStart start, BlockPos nul) {
        this.level = level;
        this.start = start;
        this.nul = nul;
        this.draai = Kopieen.draai(start, null);
    }

    /** The copy with a piece within 48 blocks of this spot, or null. */
    @Nullable
    public static Mijn bij(ServerLevel level, BlockPos pos) {
        if (!OVERAL && level.dimension() != BarbecuetherFeature.BARBECUETHER) {
            return null;
        }
        StructureStart start = Bezetting.start(level, STRUCTUUR, pos);
        if (start == null) {
            return null;
        }
        BlockPos nul = Kopieen.wereld(start, null, BlockPos.ZERO);
        return nul == null ? null : new Mijn(level, start, nul);
    }

    /** The copy this player is at, or null (remembered for a second, or until they have walked 12 blocks). */
    @Nullable
    public static Mijn van(ServerPlayer p) {
        ServerLevel level = p.level();
        if (!OVERAL && level.dimension() != BarbecuetherFeature.BARBECUETHER) {
            return null;
        }
        long nu = level.getGameTime();
        Gezien g = BIJ.get(p.getUUID());
        if (g != null && g.level == level && nu - g.tick < OPNIEUW && g.plek.closerThan(p.blockPosition(), 12)) {
            return g.mijn;
        }
        Mijn m = bij(level, p.blockPosition());
        BIJ.put(p.getUUID(), new Gezien(m, nu, p.blockPosition(), level));
        return m;
    }

    /** (logout, tests) forget where this player was. */
    public static void vergeet(UUID speler) {
        BIJ.remove(speler);
    }

    static void vergeetAlles() {
        BIJ.clear();
    }

    public ServerLevel level() {
        return level;
    }

    public StructureStart start() {
        return start;
    }

    public Rotation draai() {
        return draai;
    }

    /** The world position of the template's block (0, 0, 0). */
    public BlockPos nul() {
        return nul;
    }

    /** Template block -> world block. */
    public BlockPos wereld(BlockPos lokaal) {
        BlockPos pos = Kopieen.wereld(start, null, lokaal);
        return pos == null ? nul : pos;
    }

    /** A template position (the middle of block (x, y, z) is x + 0.5, y, z + 0.5) -> the world. */
    public Vec3 wereld(Vec3 lokaal) {
        return Cutscene.wereld(nul, draai, lokaal);
    }

    /** The middle of a template block's floor, in the world. */
    public Vec3 midden(BlockPos lokaal) {
        return wereld(new Vec3(lokaal.getX() + 0.5, lokaal.getY(), lokaal.getZ() + 0.5));
    }

    /** World block -> template block. */
    public BlockPos lokaal(BlockPos wereld) {
        BlockPos pos = Kopieen.lokaal(start, null, wereld);
        return pos == null ? BlockPos.ZERO : pos;
    }

    /** A yaw of the template (0 = south) in the world. */
    public float yaw(float lokaal) {
        return Cutscene.wereldYaw(draai, lokaal);
    }

    /** A box of the template as a box in the world. */
    public AABB doos(Plekken.Doos d) {
        BlockPos a = wereld(new BlockPos(d.x0(), d.y0(), d.z0())), b = wereld(new BlockPos(d.x1(), d.y1(), d.z1()));
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    /** Is this world position inside the template's own box? */
    public boolean binnen(BlockPos wereld) {
        BlockPos l = lokaal(wereld);
        return l.getX() >= 0 && l.getY() >= 0 && l.getZ() >= 0 && l.getX() < Plekken.MAAT.getX() && l.getY() < Plekken.MAAT.getY()
                && l.getZ() < Plekken.MAAT.getZ();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Mijn m && m.start == start && m.level == level;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(start);
    }
}
