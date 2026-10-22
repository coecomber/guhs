package nl.juiced.guhs.feature.safari.bus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.6.0 (F1b): the busstation and the Guh-safaribus. The test server has neither the Guhmension nor the park, so a copy
 * of the station is a structure start made by hand around the real template ({@link Kopieen#test}), and the two trips go
 * to spots in the test level ({@link Busrit#testHeen}, {@link Busrit#testTerug}).
 */
public class BusstationGameTests {
    private static final String BATCH = "busstation";

    private static StructureTemplate template(GameTestHelper helper) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id(Busstation.STRUCTUUR)).orElse(null);
        helper.assertTrue(t != null, "the template guhs:safari_busstation is in the jar");
        return t;
    }

    /** Every block of the template, by template position (the blocks of every kind, through filterBlocks). */
    private static Map<BlockPos, BlockState> blokken(StructureTemplate t) {
        Map<BlockPos, BlockState> m = new HashMap<>();
        StructurePlaceSettings s = new StructurePlaceSettings();
        for (Block b : BuiltInRegistries.BLOCK) {
            for (StructureTemplate.StructureBlockInfo info : t.filterBlocks(BlockPos.ZERO, s, b)) {
                m.put(info.pos(), info.state());
            }
        }
        return m;
    }

    private static boolean leeg(Map<BlockPos, BlockState> m, BlockPos p) {
        BlockState s = m.get(p);
        return s == null || s.isAir();
    }

    /**
     * The template: its size, the anchor, the bus only made of its own yellow (so a click on yellow terracotta is a click on
     * the bus), free floored spots for the chauffeur and the shop guh, every animal home inside its pen, and the rule of
     * round 36: next to a raise inside a pen (within 2 blocks of the fence) the fence is at least 2 blocks higher.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void busstationTemplateKlopt(GameTestHelper helper) {
        StructureTemplate t = template(helper);
        Vec3i maat = t.getSize();
        helper.assertTrue(maat.getX() == 64 && maat.getZ() == 64 && maat.getY() >= 30, "64 x 64 and high enough: " + maat);
        Map<BlockPos, BlockState> m = blokken(t);
        helper.assertTrue(m.getOrDefault(new BlockPos(32, 0, 32), Blocks.AIR.defaultBlockState()).is(Blocks.JIGSAW), "the anchor jigsaw in the middle");
        int bus = 0;
        for (Map.Entry<BlockPos, BlockState> e : m.entrySet()) {
            if (e.getValue().is(Blocks.YELLOW_TERRACOTTA)) {
                BlockPos p = e.getKey();
                helper.assertTrue(p.getX() >= Busstation.BUS_MIN.getX() && p.getX() <= Busstation.BUS_MAX.getX() && p.getZ() >= Busstation.BUS_MIN.getZ()
                        && p.getZ() <= Busstation.BUS_MAX.getZ(), "yellow terracotta only in the bus: " + p);
                bus++;
            }
        }
        helper.assertTrue(bus > 200, "the bus is there: " + bus + " blocks");
        helper.assertTrue(m.getOrDefault(Busstation.BEL, Blocks.AIR.defaultBlockState()).is(Blocks.BELL), "the bell at the stop");
        for (BlockPos p : List.of(Busstation.CHAUFFEUR, Busstation.VERKOPER)) {
            helper.assertTrue(!leeg(m, p.below()) && leeg(m, p) && leeg(m, p.above()), "a free spot on a floor at " + p);
        }
        int[][] pennen = {{2, 28, 13, 38}, {2, 40, 13, 52}, {2, 54, 13, 62}};
        String[] soorten = {"stokstaartje", "flamingo", "pinguin"};
        int g = Busstation.G;
        for (int k = 0; k < pennen.length; k++) {
            int x1 = pennen[k][0], z1 = pennen[k][1], x2 = pennen[k][2], z2 = pennen[k][3];
            int hekTop = Integer.MAX_VALUE;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    if (x == x1 || x == x2 || z == z1 || z == z2) {
                        int top = g;
                        for (int y = g + 1; y < maat.getY(); y++) {
                            if (!leeg(m, new BlockPos(x, y, z))) {
                                top = y;
                            }
                        }
                        hekTop = Math.min(hekTop, top);
                    }
                }
            }
            helper.assertTrue(hekTop >= g + 4, soorten[k] + ": the fence stands 4 high everywhere (lowest top " + (hekTop - g) + ")");
            for (int x = x1 + 1; x < x2; x++) {
                for (int z = z1 + 1; z < z2; z++) {
                    int top = g;
                    for (int y = g + 1; y < maat.getY(); y++) {
                        BlockState s = m.get(new BlockPos(x, y, z));
                        if (s != null && !s.isAir() && !s.is(Blocks.SUGAR_CANE) && !s.is(Blocks.WATER)) {
                            top = y;
                        }
                    }
                    boolean bijHek = Math.min(Math.min(x - x1, x2 - x), Math.min(z - z1, z2 - z)) <= 2;
                    helper.assertTrue(top == g || !bijHek || hekTop >= top + 2, soorten[k] + ": a raise at " + x + "," + z + " (" + (top - g)
                            + " high) right beside the fence");
                }
            }
            for (Busstation.Dier d : Busstation.DIEREN) {
                if (d.soort().equals(soorten[k])) {
                    helper.assertTrue(d.x() > x1 && d.x() < x2 && d.z() > z1 && d.z() < z2, d.soort() + " lives inside its pen");
                }
            }
        }
        helper.succeed();
    }

    /** A copy of the busstation with its corner at this helper position (the real template's box; no blocks placed). */
    private static StructureStart kopie(GameTestHelper helper, BlockPos hoek, Rotation draai) {
        ServerLevel level = helper.getLevel();
        Structure structure = Kopieen.structuur(level, Busstation.STRUCTUUR);
        helper.assertTrue(structure != null, "the structure guhs:safari_busstation exists");
        BlockPos pos = helper.absolutePos(hoek);
        StructurePoolElement element = StructurePoolElement.single("guhs:" + Busstation.STRUCTUUR).apply(StructureTemplatePool.Projection.RIGID);
        BoundingBox box = element.getBoundingBox(level.getStructureManager(), pos, draai);
        StructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, pos, 0, draai, box, LiquidSettings.IGNORE_WATERLOGGING);
        StructureStart start = new StructureStart(structure, ChunkPos.containing(pos), 0, new PiecesContainer(List.of(piece)));
        Kopieen.test(level, start);
        return start;
    }

    /**
     * A copy is found from nearby, protected, its bus (and bell) is the bus and the chauffeur's spot is not; the scene's
     * anchor is the chauffeur's spot of the turned copy.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void busstationKopieBeschermdEnBus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        try {
            StructureStart start = kopie(helper, new BlockPos(1, -Busstation.G, 1), Rotation.CLOCKWISE_90);
            BlockPos midden = Kopieen.wereld(start, null, new BlockPos(32, Busstation.G + 1, 32));
            Busstation.Oord o = Busstation.oord(level, midden);
            helper.assertTrue(o != null, "the copy is found from its middle");
            helper.assertTrue(o.anker().equals(Kopieen.wereld(start, null, Busstation.CHAUFFEUR)) && o.draai() == Rotation.CLOCKWISE_90,
                    "the anchor is the chauffeur's spot, turned with the copy");
            BlockPos lijf = Kopieen.wereld(start, null, new BlockPos(52, Busstation.G + 4, 30));
            helper.assertTrue(o.isBus(lijf), "the bus body is the bus");
            helper.assertTrue(o.isBus(Kopieen.wereld(start, null, Busstation.BEL)), "so is its bell");
            helper.assertTrue(!o.isBus(o.anker()) && !o.isBus(midden), "the chauffeur's spot and the square are not");
            helper.assertTrue(Bescherming.beschermd(level, lijf) && Bescherming.beschermd(level, midden), "the station is protected");
            helper.assertTrue(Cutscene.van("safari_bus_terug") != null && Cutscene.van("safari_bus_heen_0") != null && Cutscene.van("safari_bus_heen_2") != null,
                    "the rides are registered");
        } finally {
            Kopieen.testWissen(level);
        }
        helper.succeed();
    }

    /**
     * Instappen: the station is remembered, the player goes (here: to a spot in the test level), the own guh that follows
     * comes along and gets a safari hat (a guh that already wears a hat keeps its own); uitstappen: back, the guhs along,
     * the bus' hat comes off, the station is forgotten.
     */
    @SuppressWarnings("removal")
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void busstationRitHeenEnTerugMetGuhs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        BlockPos start = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos park = helper.absolutePos(new BlockPos(2, 2, 40));
        p.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
        GuhEntity kaal = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        GuhEntity gehoed = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        helper.assertTrue(kaal != null && gehoed != null, "two guhs");
        kaal.snapTo(start.getX() + 2.5, start.getY(), start.getZ() + 0.5);
        gehoed.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 2.5);
        gehoed.wear(GuhClothes.STRAW_HAT);
        for (GuhEntity g : List.of(kaal, gehoed)) {
            level.addFreshEntity(g);
            g.tame(p);
            g.setOrderedToSit(false);
        }
        Busrit.testHeen = q -> {
            q.teleportTo(park.getX() + 0.5, park.getY(), park.getZ() + 0.5);
            return true;
        };
        Busrit.testTerug = q -> {
            q.teleportTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
            return true;
        };
        try {
            Busstation.Oord o = new Busstation.Oord(null, helper.absolutePos(new BlockPos(5, 2, 5)), Rotation.CLOCKWISE_180);
            helper.assertTrue(Busrit.instappen(p, o), "the bus goes");
            helper.assertTrue(p.blockPosition().distSqr(park) < 4, "the player is at the other stop");
            helper.assertTrue(kaal.blockPosition().distSqr(park) < 16 && gehoed.blockPosition().distSqr(park) < 16, "both own guhs came along");
            helper.assertTrue(kaal.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.SAFARI_HOEDJE, "the bare one got a safari hat");
            helper.assertTrue(gehoed.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.SAFARI_HOEDJE, "the other one too (its own hat is kept for later)");
            helper.assertTrue(Busrit.station(p) != null && Busrit.station(p).getIntOr("Draai", -1) == Rotation.CLOCKWISE_180.ordinal(),
                    "the station is remembered");
            helper.assertTrue(Busrit.uitstappen(p), "and back");
            helper.assertTrue(p.blockPosition().distSqr(start) < 4, "the player is back where they got on");
            helper.assertTrue(kaal.blockPosition().distSqr(start) < 16 && gehoed.blockPosition().distSqr(start) < 16, "with both guhs");
            helper.assertTrue(kaal.getClothes(GuhClothes.Slot.HEAD) == null, "the safari hat came off");
            helper.assertTrue(gehoed.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.STRAW_HAT, "the straw hat is back");
            helper.assertTrue(Busrit.station(p) == null, "the station is forgotten");
        } finally {
            Busrit.testHeen = null;
            Busrit.testTerug = null;
            kaal.discard();
            gehoed.discard();
        }
        helper.succeed();
    }
}
