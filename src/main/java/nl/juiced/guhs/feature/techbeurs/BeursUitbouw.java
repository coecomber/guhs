package nl.juiced.guhs.feature.techbeurs;

import java.util.List;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.Stempel;
import nl.juiced.guhs.feature.raket.LanceerplatformBlockEntity;
import nl.juiced.guhs.feature.raket.RaketBouw;
import nl.juiced.guhs.feature.wereld.Bescherming;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 1.8.0 (thema 2 "Dromen en vadskracht"): the renewed Guhtechniek-beurs. The hall of 1.7.0 stays one jigsaw template; the
 * annex ({@code techbeurs/guhtechniek_beurs_aanbouw}, east of the hall) and the two upper floors (the Fabriekshal and the
 * Koepel, {@code techbeurs/guhtechniek_beurs_boven}, on the hall's glass roof) are stamped by the hall's own
 * {@link BeursBeheer} as soon as it ticks: in a new world right after the beurs generated, in an old world (the server) the
 * first time its chunk loads. The caretaker keeps the version it stamped ({@link #VERSIE}); a higher one stamps again
 * (like the DroomEilanden). Built by tools/features/techbeurs_boven.py.
 * <p>
 * How the hall turned is read from the hall itself: its two Gloeisterkernen ({@link #KENMERK}) only fit one rotation
 * around the caretaker. The new parts are protected like the hall ({@link Bescherming#zetDoos}, again after every load), and
 * the Kaasraket on the Koepel gets its first two stages built.
 */
public final class BeursUitbouw {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Raise this when the templates change in a way old beurzen must get too. */
    public static final int VERSIE = 1;
    /** The caretaker in the hall's template frame (techbeurs.py BEHEER). */
    static final BlockPos BEHEER = new BlockPos(37, 0, 21);
    /** The Gloeisterkernen of the Droomfabriek and of the kern stand, in the hall's template frame. */
    static final List<BlockPos> KENMERK = List.of(new BlockPos(10, 2, 16), new BlockPos(46, 2, 4));
    /** The Lanceerplatform of the Koepel (hall frame) and how many stages stand on it. */
    static final BlockPos RAKET = new BlockPos(6, 22, 35);
    /** The space around the stamps the protection covers too. */
    static final int RAND = 2;

    /** A template that goes on or next to the hall: its min corner in the hall's template frame and its size. */
    public record Stuk(Identifier template, BlockPos plek, Vec3i maat) {
    }

    public static final Stuk AANBOUW = new Stuk(Guhs.id("techbeurs/guhtechniek_beurs_aanbouw"), new BlockPos(73, 0, 6), new Vec3i(37, 14, 30));
    public static final Stuk BOVEN = new Stuk(Guhs.id("techbeurs/guhtechniek_beurs_boven"), new BlockPos(0, 11, 0), new Vec3i(74, 28, 42));
    public static final List<Stuk> STUKKEN = List.of(AANBOUW, BOVEN);

    /** Where a hall stands: its template origin in the world and how it turned. */
    public record Hal(BlockPos oorsprong, Rotation draai) {
        /** A spot of the hall's template frame in the world. */
        public BlockPos wereld(BlockPos lokaal) {
            return oorsprong.offset(StructureTemplate.transform(lokaal, Mirror.NONE, draai, BlockPos.ZERO));
        }

        /** The world box of a part. */
        public BoundingBox doos(Stuk s) {
            BlockPos a = wereld(s.plek());
            BlockPos b = wereld(s.plek().offset(s.maat().getX() - 1, s.maat().getY() - 1, s.maat().getZ() - 1));
            return BoundingBox.fromCorners(a, b);
        }
    }

    private BeursUitbouw() {
    }

    private static boolean is(BlockState s, String id) {
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).equals(Guhs.id(id));
    }

    /** The hall around the caretaker at {@code beheer} (null: its Gloeisterkernen are not where a hall has them). */
    @Nullable
    public static Hal vind(ServerLevel level, BlockPos beheer) {
        for (Rotation r : Rotation.values()) {
            Hal h = new Hal(beheer.subtract(StructureTemplate.transform(BEHEER, Mirror.NONE, r, BlockPos.ZERO)), r);
            boolean past = true;
            for (BlockPos k : KENMERK) {
                past &= is(level.getBlockState(h.wereld(k)), "gloeisterkern");
            }
            if (past) {
                return h;
            }
        }
        return null;
    }

    /**
     * Stamps the annex and the upper floors (and builds the half rocket). {@code eerste}: nothing of ours stood there yet, so
     * it refuses when it would land on somebody's things (a chest, a sign, a guhs block with a block entity: a player built
     * there). True when it stamped.
     */
    public static boolean bouw(ServerLevel level, Hal hal, boolean eerste) {
        for (Stuk s : STUKKEN) {
            if (Stempel.maat(level, s.template()) == null) {
                LOGGER.error("Guhtechniek-beurs: template {} is missing", s.template());
                return false;
            }
            if (eerste) {
                BlockPos bezet = bewoond(level, hal.doos(s));
                if (bezet != null) {
                    LOGGER.warn("Guhtechniek-beurs at {}: not stamping {}, something of a player stands at {}", hal.oorsprong().toShortString(),
                            s.template(), bezet.toShortString());
                    return false;
                }
            }
        }
        for (Stuk s : STUKKEN) {
            BoundingBox doos = hal.doos(s);
            laad(level, doos);
            Stempel.ruim(level, AABB.of(doos));
            StructureTemplate t = level.getStructureManager().get(s.template()).orElseThrow();
            BlockPos min = hal.wereld(s.plek());
            StructurePlaceSettings set = new StructurePlaceSettings().setRotation(hal.draai()).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
            t.placeInWorld(level, min, min, set, level.getRandom(), 2);
        }
        fundering(level, hal);
        raket(level, hal);
        bescherm(level, hal);
        LOGGER.info("Guhtechniek-beurs at {} ({}): annex, Fabriekshal and Koepel stamped (version {})", hal.oorsprong().toShortString(),
                hal.draai(), VERSIE);
        return true;
    }

    /** The protection of the new parts (the hall itself is protected as a structure); after every load again. */
    public static void bescherm(ServerLevel level, Hal hal) {
        for (Stuk s : STUKKEN) {
            Bescherming.zetDoos(level, Beurs.STRUCTUUR, hal.doos(s).inflatedBy(RAND));
        }
    }

    /** The Kaasraket on the Koepel: the stages its platform says are done (romp + motor). */
    static void raket(ServerLevel level, Hal hal) {
        BlockPos p = hal.wereld(RAKET);
        if (level.getBlockEntity(p) instanceof LanceerplatformBlockEntity platform) {
            for (int stap = 1; stap <= platform.klaar(); stap++) {
                RaketBouw.plaats(level, p, stap, false);
            }
        }
    }

    /** Under the annex (it stands on the ground beside the hall): smooth stone down to the ground, at most 16 blocks. */
    static void fundering(ServerLevel level, Hal hal) {
        BoundingBox doos = hal.doos(AANBOUW);
        BlockState steen = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = doos.minX(); x <= doos.maxX(); x++) {
            for (int z = doos.minZ(); z <= doos.maxZ(); z++) {
                for (int y = doos.minY() - 1; y >= Math.max(level.getMinY(), doos.minY() - 16); y--) {
                    m.set(x, y, z);
                    BlockState s = level.getBlockState(m);
                    if (!s.canBeReplaced() && s.getFluidState().isEmpty()) {
                        break;
                    }
                    level.setBlock(m, steen, 2);
                }
            }
        }
    }

    private static void laad(ServerLevel level, BoundingBox doos) {
        for (int cx = doos.minX() >> 4; cx <= doos.maxX() >> 4; cx++) {
            for (int cz = doos.minZ() >> 4; cz <= doos.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
    }

    /** A block entity in the box that a player would have placed (null: none). */
    @Nullable
    static BlockPos bewoond(ServerLevel level, BoundingBox doos) {
        laad(level, doos);
        for (int cx = doos.minX() >> 4; cx <= doos.maxX() >> 4; cx++) {
            for (int cz = doos.minZ() >> 4; cz <= doos.maxZ() >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (!doos.isInside(p)) {
                        continue;
                    }
                    String ns = BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getNamespace();
                    if (be instanceof Container || be instanceof SignBlockEntity || ns.equals(Guhs.MODID)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }
}
