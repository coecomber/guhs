package nl.juiced.guhs.feature.einde2.ballon;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;

/**
 * einde2 phase C, slice trap5_ballon: each player sees their OWN Vredesballon on the building site (CONTRACT_E2_B 7.12).
 * <ul>
 *   <li>A {@link Schijn} source: the overlay {@code laag_vredesballon_<n>} of the temple's build (bouw_lucht's, cumulative)
 *       for a player whose stage n is done ({@link Vredesballon#laag}); the real yard stays empty for everybody.</li>
 *   <li>While the player is in the air (the launch scene) the moving parts are gone and only the yard's gear of the last
 *       stage stays: the four masts, the crane frame and its flags, what lies on the ground ({@link #blijftStaan}).</li>
 *   <li>The basket and the cloth are blocks only that client knows: a player who climbs onto them would float for the
 *       server, so inside the overlay's box the server lets them "fly" (mayfly) and takes that back when they leave.</li>
 * </ul>
 * The blocks come from the overlay templates (read once).
 */
public final class BallonLagen {
    /** The overlay's size (einde2_bouw_lucht_ballon.SX, SY, SZ) and the rows that matter for "what stays on the ground". */
    static final int SX = 25, SY = 42, SZ = 25, MID = 12, MAND_Y = 2, KRAAN_Y = SY - 4;

    /** stage (1..5) -> its blocks relative to vredesballon_anker (air left out). */
    private static final Map<Integer, Map<BlockPos, BlockState>> BLOKKEN = new ConcurrentHashMap<>();
    /** Persistent player data: we switched their mayfly on (kept over a restart, so it is always taken back). */
    static final String ZWEEFT = "guhs_einde2_trap5_ballon_zweeft";
    @Nullable
    private static Field palettes;

    private BallonLagen() {
    }

    /** Schijn source. */
    static void vul(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        int laag = Vredesballon.laag(p);
        if (laag == 0) {
            return;
        }
        BlockPos anker = Vredesballon.plek(p.level(), Vredesballon.ANKER);
        if (anker == null || !Schijn.dichtbij(p, anker.offset(MID, 0, MID))) {
            return;
        }
        boolean weg = Vredesballon.OPGESTEGEN.contains(p.getUUID());
        for (Map.Entry<BlockPos, BlockState> e : blokken(p.level(), laag).entrySet()) {
            if (weg && !blijftStaan(e.getKey())) {
                continue;
            }
            BlockPos pos = anker.offset(e.getKey());
            if (Schijn.dichtbij(p, pos)) {
                gewenst.put(pos, e.getValue());
            }
        }
    }

    /**
     * Does this cell of the overlay stay in the yard while the balloon is up? Everything on the ground (below the basket's
     * floor), the four masts with their rungs, the crane frame up top with its flags and lanterns and the chain that hangs
     * from it, and what stands further out than the basket and its bags (the pile of knabbels, the chute, the cable box).
     */
    static boolean blijftStaan(BlockPos rel) {
        int x = rel.getX(), y = rel.getY(), z = rel.getZ();
        if (y < MAND_Y || y >= KRAAN_Y) {
            return true;
        }
        boolean mastX = x <= 2 || x >= SX - 3, mastZ = z <= 2 || z >= SZ - 3;
        if (mastX && mastZ) {
            return true;                                   // a mast and its rungs
        }
        if (x == MID && z == MID && y > 35) {
            return true;                                   // the chain from the frame down to the top of the balloon
        }
        if (y > MAND_Y + 1) {
            return false;                                  // the cloth, the ropes, the burner: all of it goes up
        }
        return Math.abs(x - MID) > 4 || Math.abs(z - MID) > 4;
    }

    /** The blocks of stage n, relative to the anchor (empty when the build has no such overlay). */
    static Map<BlockPos, BlockState> blokken(ServerLevel level, int n) {
        Map<BlockPos, BlockState> b = BLOKKEN.get(n);
        if (b != null) {
            return b;
        }
        b = Map.copyOf(lees(level, n));
        if (!b.isEmpty()) {
            BLOKKEN.put(n, b);
        }
        return b;
    }

    @SuppressWarnings("unchecked")
    private static Map<BlockPos, BlockState> lees(ServerLevel level, int n) {
        Map<BlockPos, BlockState> uit = new HashMap<>();
        Plekken.Laag laag = Plekken.blad(Vredesballon.STRUCTUUR).lagen().get("vredesballon_" + n);
        StructureTemplate t = laag == null ? null : level.getStructureManager().get(laag.template()).orElse(null);
        if (t == null) {
            return uit;
        }
        StructurePlaceSettings zo = new StructurePlaceSettings();
        try {
            if (palettes == null) {
                Field f = StructureTemplate.class.getDeclaredField("palettes");
                f.setAccessible(true);
                palettes = f;
            }
            List<StructureTemplate.Palette> lijst = (List<StructureTemplate.Palette>) palettes.get(t);
            if (!lijst.isEmpty()) {
                for (StructureTemplate.StructureBlockInfo info : zo.getRandomPalette(lijst, BlockPos.ZERO).blocks()) {
                    zet(uit, info);
                }
                return uit;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            com.mojang.logging.LogUtils.getLogger().debug("Guhs einde2 trap5_ballon: the palettes by name failed, by block then", e);
        }
        for (Block blok : BuiltInRegistries.BLOCK) {
            for (StructureTemplate.StructureBlockInfo info : t.filterBlocks(BlockPos.ZERO, zo, blok)) {
                zet(uit, info);
            }
        }
        return uit;
    }

    private static void zet(Map<BlockPos, BlockState> uit, StructureTemplate.StructureBlockInfo info) {
        BlockState s = info.state();
        if (!s.isAir() && !s.is(Blocks.STRUCTURE_VOID) && !s.is(Blocks.JIGSAW) && !s.is(Blocks.STRUCTURE_BLOCK)) {
            uit.put(info.pos().immutable(), s);
        }
    }

    // =====================================================================================================================
    // standing in the basket
    // =====================================================================================================================

    /** Every 10 ticks per player: mayfly on inside the overlay's box (while they see a balloon), off when they leave it. */
    static void tick(ServerPlayer p) {
        boolean wil = false;
        if (!p.isSpectator() && !p.getAbilities().instabuild && Vredesballon.laag(p) > 0) {
            BlockPos anker = Vredesballon.plek(p.level(), Vredesballon.ANKER);
            wil = anker != null && new AABB(anker.getX() - 1, anker.getY() - 1, anker.getZ() - 1, anker.getX() + SX + 1, anker.getY() + SY + 2,
                    anker.getZ() + SZ + 1).contains(p.position());
        }
        boolean gaven = p.getPersistentData().getBooleanOr(ZWEEFT, false);
        if (wil && !p.getAbilities().mayfly) {
            p.getAbilities().mayfly = true;
            p.onUpdateAbilities();
            p.getPersistentData().putBoolean(ZWEEFT, true);
        } else if (!wil && gaven) {
            terug(p);
        }
    }

    static void vergeet(ServerPlayer p) {
        if (p.getPersistentData().getBooleanOr(ZWEEFT, false)) {
            terug(p);
        }
    }

    private static void terug(ServerPlayer p) {
        p.getPersistentData().remove(ZWEEFT);
        if (!p.getAbilities().instabuild && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
    }

    /** (tests, dev) forget what was read. */
    static void vergeetBlokken() {
        BLOKKEN.clear();
    }
}
