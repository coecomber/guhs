package nl.juiced.guhs.feature.wereld;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.balto.BaltoFeature;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadBlocks;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.4.2 (fixes, bugs 1 and 2): the copies of the Nomguh and the Knuffelbad built before 1.4.2 are mended once: the hole in
 * the boat's deck (Nomguh) and a way out of every foam spot (Knuffelbad). A test copy stands in the test level (turned, for
 * the Knuffelbad); its spots get the OLD blocks, the repair runs, then every spot holds the new block, a second run
 * changes nothing, and a spot that holds something else (changed by a player in creative mode) is left alone. The
 * templates themselves: the repair's spots already hold the new block there (checked by the python builders).
 */
public class ReparatiesGameTests {

    private static StructureStart kopie(ServerLevel level, String structuur, BlockPos hoek, Rotation draai) {
        Structure s = Kopieen.structuur(level, structuur);
        StructurePoolElement element = StructurePoolElement.single("guhs:" + structuur).apply(StructureTemplatePool.Projection.RIGID);
        BoundingBox box = element.getBoundingBox(level.getStructureManager(), hoek, draai);
        StructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, hoek, 0, draai, box, LiquidSettings.IGNORE_WATERLOGGING);
        return new StructureStart(s, ChunkPos.containing(hoek), 0, new PiecesContainer(List.of(piece)));
    }

    /** The old block of a spot (air when that is one of them: a lone snow layer would not stay in the air). */
    private static BlockState oud(Reparaties.Blok b) {
        return b.oud().contains(Blocks.AIR) ? Blocks.AIR.defaultBlockState() : b.oud().stream().sorted(java.util.Comparator.comparing(Object::toString))
                .findFirst().orElseThrow().defaultBlockState();
    }

    private static void herstelTest(GameTestHelper helper, String id, String structuur, Rotation draai) {
        ServerLevel level = helper.getLevel();
        Reparaties.Reparatie r = Reparaties.lees(id);
        helper.assertTrue(r.structuur().equals(structuur) && !r.blokken().isEmpty(), id + ": the repair list of " + structuur + ": " + r.blokken().size());
        // the copy: its template spot (0, 0, 0) far enough from the test room, the spots high in the air (nothing else there)
        BlockPos hoek = helper.absolutePos(new BlockPos(0, 40, 0));
        StructureStart start = kopie(level, structuur, hoek, draai);
        List<BlockPos> plekken = new ArrayList<>();
        for (Reparaties.Blok b : r.blokken()) {
            BlockPos w = Kopieen.wereld(start, r.stuk(), b.lokaal());
            helper.assertTrue(w != null && w.equals(hoek.offset(StructureTemplate.transform(b.lokaal(), net.minecraft.world.level.block.Mirror.NONE, draai, BlockPos.ZERO))),
                    "the spot " + b.lokaal() + " in the world: " + w);
            level.getChunk(w);
            plekken.add(w);
        }
        try {
            // as a copy from before 1.4.2: the old blocks; one spot was changed by somebody (stone): that one stays
            for (int i = 0; i < plekken.size(); i++) {
                level.setBlock(plekken.get(i), i == 0 ? Blocks.STONE.defaultBlockState() : oud(r.blokken().get(i)), 2);
            }
            Reparaties.vergeet(level, r, start);
            int n = Reparaties.herstel(level, r, start);
            helper.assertTrue(n == plekken.size() - 1, id + ": every old spot mended: " + n + " of " + (plekken.size() - 1));
            helper.assertTrue(level.getBlockState(plekken.get(0)).is(Blocks.STONE), "a spot somebody changed is left alone");
            for (int i = 1; i < plekken.size(); i++) {
                BlockState nu = level.getBlockState(plekken.get(i));
                helper.assertTrue(nu == r.blokken().get(i).nieuw().rotate(draai), id + ": " + r.blokken().get(i).lokaal() + " is " + nu);
            }
            // once: a second time nothing happens, also when the old block is back
            level.setBlock(plekken.get(1), oud(r.blokken().get(1)), 2);
            helper.assertTrue(Reparaties.herstel(level, r, start) == 0, "a copy is mended only once");
        } finally {
            for (BlockPos w : plekken) {
                level.setBlock(w, Blocks.AIR.defaultBlockState(), 2);
            }
            Reparaties.vergeet(level, r, start);
        }
    }

    /** Nomguh: the two broken planks of the boat's deck become sagging half slabs (a step of half a block, not a pit). */
    @GuhTest(template = "empty", batch = "reparaties", timeoutTicks = 200)
    public static void reparatiesNomguhBoot(GameTestHelper helper) {
        Reparaties.Reparatie r = Reparaties.lees(BaltoFeature.NOMGUH_HERSTEL);
        for (Reparaties.Blok b : r.blokken()) {
            helper.assertTrue(b.oud().contains(Blocks.AIR) && b.nieuw().getBlock() instanceof SlabBlock
                    && b.nieuw().getValue(SlabBlock.TYPE) == SlabType.BOTTOM, "the hole becomes a bottom slab: " + b);
        }
        helper.assertTrue(r.blokken().size() == 2, "two planks");
        herstelTest(helper, BaltoFeature.NOMGUH_HERSTEL, "nomguh", Rotation.NONE);
        helper.succeed();
    }

    /** Knuffelbad: every foam bath gets its ledges (and the steps in the walls), also in a turned copy. */
    @GuhTest(template = "empty", batch = "reparaties", timeoutTicks = 200)
    public static void reparatiesKnuffelbadRichels(GameTestHelper helper) {
        Reparaties.Reparatie r = Reparaties.lees(KnuffelbadFeature.HERSTEL);
        long richels = r.blokken().stream().filter(b -> b.oud().contains(KnuffelbadFeature.SCHUIM_BLOK.get())).count();
        helper.assertTrue(richels >= 30, "ledges in the foam: " + richels);
        herstelTest(helper, KnuffelbadFeature.HERSTEL, "knuffelbad", Rotation.CLOCKWISE_90);
        helper.succeed();
    }

    /** The foam slows you down while you sink into it, but never while you go up or swim in the water under it. */
    @GuhTest(template = "empty", batch = "reparaties")
    public static void reparatiesSchuimHoudtNietVast(GameTestHelper helper) {
        ArmorStand a = new ArmorStand(EntityType.ARMOR_STAND, helper.getLevel());
        a.setDeltaMovement(new Vec3(0, -0.3, 0));
        helper.assertTrue(!KnuffelbadBlocks.Schuim.vrijOmhoog(a), "sinking into the foam: slowed down");
        a.setDeltaMovement(new Vec3(0, 0.42, 0));
        helper.assertTrue(KnuffelbadBlocks.Schuim.vrijOmhoog(a), "a jump goes up freely");
        helper.succeed();
    }
}
