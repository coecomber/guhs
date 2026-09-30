package nl.juiced.guhs.world.grond;

import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;

/**
 * 2.10 (DESIGN 9.1, "het omgekeerde trapje"): the land meets sunk buildings at their floor. The pool element keeps its ground
 * (codec round trip, saved pieces), every sunk structure's start pool says its real ground (sjoelhuisje 5, sterrenwacht 25,
 * knabbelkatapult 13, circuit/racebaan 4, the Knuffeldal town and the Elf-Guhjestocht: the anchor's layer + 1), plain
 * buildings stay vanilla, the Beardifier fills the land up to the floor next to the wall instead of digging a moat, and the
 * ring measurement of /guhs bouwcheck.
 */
public class GrondGameTests {
    private static final String EMPTY = "empty";

    private static StructureTemplatePool pool(GameTestHelper helper, String id) {
        StructureTemplatePool pool = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL).getValue(Guhs.id(id));
        helper.assertTrue(pool != null, "pool " + id);
        return pool;
    }

    private static StructurePoolElement start(GameTestHelper helper, String id) {
        StructureTemplatePool pool = pool(helper, id + "/start");
        helper.assertTrue(pool.size() >= 1, id + ": a start element");
        return pool.getShuffledTemplates(RandomSource.create(1L)).get(0);
    }

    /** guhs:grond_single_pool_element survives the codec (JSON and back) with its ground, and says its type. */
    @GuhTest(template = EMPTY, batch = "grond")
    public static void grondPoolElementCodec(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Holder<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList> empty =
                access.lookupOrThrow(Registries.PROCESSOR_LIST).getOrThrow(
                        net.minecraft.resources.ResourceKey.create(Registries.PROCESSOR_LIST, net.minecraft.resources.Identifier.withDefaultNamespace("empty")));
        GrondPoolElement element = new GrondPoolElement(Either.left(Guhs.id("sjoelhuisje")), empty, StructureTemplatePool.Projection.RIGID,
                Optional.empty(), 13);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        JsonElement json = StructurePoolElement.CODEC.encodeStart(ops, element).getOrThrow();
        helper.assertTrue(json.getAsJsonObject().get("element_type").getAsString().equals("guhs:grond_single_pool_element"), "type in json: " + json);
        helper.assertTrue(json.getAsJsonObject().get("ground_level_delta").getAsInt() == 13, "delta in json: " + json);
        helper.assertTrue(json.getAsJsonObject().get("location").getAsString().equals("guhs:sjoelhuisje"), "location in json: " + json);
        StructurePoolElement back = StructurePoolElement.CODEC.parse(ops, json).getOrThrow();
        helper.assertTrue(back instanceof GrondPoolElement g && g.getGroundLevelDelta() == 13
                && g.getProjection() == StructureTemplatePool.Projection.RIGID, "round trip: " + back);
        helper.assertTrue(back.toString().contains("guhs:sjoelhuisje"), "toString keeps the location (PleinSlot reads it): " + back);
        helper.assertTrue(back.getType() == nl.juiced.guhs.registry.ModStructureTypes.GROND_SINGLE_POOL_ELEMENT.get(), "its type");
        helper.succeed();
    }

    /** Every sunk building's start element knows its real ground; the buildings that stand on the surface stay vanilla (1). */
    @GuhTest(template = EMPTY, batch = "grond")
    public static void grondStartPoolsKennenHunVloer(GameTestHelper helper) {
        Map<String, Integer> sunk = Map.ofEntries(Map.entry("sjoelhuisje", 5), Map.entry("guh_sterrenwacht", 25),
                Map.entry("knabbelkatapult", 13), Map.entry("guh_circuit", 4), Map.entry("guh_racebaan", 4), Map.entry("guhvis_vijver", 8),
                Map.entry("knabbelspelen", 5), Map.entry("guhdoolhof", 5), Map.entry("guhboerderij", 5), Map.entry("moerasheks_hut", 7),
                Map.entry("ballonfestival", 5), Map.entry("boomhutdorp", 5), Map.entry("kampeerplekje", 5), Map.entry("knuffelbad", 5),
                Map.entry("kaasknabbel_nest", 5), Map.entry("block_guh", 2), Map.entry("guh_fossil", 2), Map.entry("mini_picnic", 2));
        sunk.forEach((id, g) -> {
            StructurePoolElement e = start(helper, id);
            helper.assertTrue(e instanceof GrondPoolElement && e.getGroundLevelDelta() == g, id + ": ground " + g + ", got " + e.getGroundLevelDelta() + " " + e);
        });
        for (String flat : new String[]{"guh_picnic", "cheese_fountain", "guh_golfbaan", "guh_disco"}) {
            StructurePoolElement e = start(helper, flat);
            helper.assertTrue(!(e instanceof GrondPoolElement) && e.getGroundLevelDelta() == 1, flat + " stays vanilla: " + e);
        }
        // the two custom starts (no heightmap projection): the ground is the anchor's layer (the top block of the ground) + 1
        var templates = helper.getLevel().getServer().getStructureManager();
        for (String[] custom : new String[][]{{"knuffeldal_stadje", "guhs:knuffeldal_midden"}, {"elfguhjestocht", "guhs:elfguhjestocht_midden"}}) {
            StructurePoolElement e = start(helper, custom[0]);
            int anchorY = Integer.MIN_VALUE;
            for (StructureTemplate.JigsawBlockInfo jigsaw : e.getShuffledJigsawBlocks(templates, BlockPos.ZERO, Rotation.NONE, RandomSource.create(0L))) {
                if (custom[1].equals(String.valueOf(jigsaw.name()))) {
                    anchorY = jigsaw.info().pos().getY();
                }
            }
            helper.assertTrue(anchorY != Integer.MIN_VALUE, custom[0] + ": its anchor");
            helper.assertTrue(e instanceof GrondPoolElement && e.getGroundLevelDelta() == anchorY + 1,
                    custom[0] + ": ground = anchor " + anchorY + " + 1, got " + e.getGroundLevelDelta());
            // the anchor still lands on the top block of the ground: vanilla sinks the piece by its delta
            var holder = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL).getOrThrow(
                    net.minecraft.resources.ResourceKey.create(Registries.TEMPLATE_POOL, Guhs.id(custom[0] + "/start")));
            helper.assertTrue(Grond.startY(holder, 70) - e.getGroundLevelDelta() == 69, custom[0] + ": anchor on the top block (69)");
        }
        helper.succeed();
    }

    /** A sunk piece keeps its ground when the chunk is saved and loaded again (so the beard of later chunks is right too). */
    @GuhTest(template = EMPTY, batch = "grond")
    public static void grondStukBewaartZijnVloer(GameTestHelper helper) {
        var templates = helper.getLevel().getServer().getStructureManager();
        StructurePoolElement e = start(helper, "knabbelkatapult");
        BlockPos at = new BlockPos(1000, 40, 1000);
        PoolElementStructurePiece piece = new PoolElementStructurePiece(templates, e, at, e.getGroundLevelDelta(), Rotation.NONE,
                e.getBoundingBox(templates, at, Rotation.NONE), LiquidSettings.IGNORE_WATERLOGGING);
        helper.assertTrue(piece.getGroundLevelDelta() == 13, "katapult: ground 13, got " + piece.getGroundLevelDelta());
        StructurePieceSerializationContext ctx = StructurePieceSerializationContext.fromLevel(helper.getLevel());
        CompoundTag tag = piece.createTag(ctx);
        PoolElementStructurePiece back = new PoolElementStructurePiece(ctx, tag);
        helper.assertTrue(back.getGroundLevelDelta() == 13 && back.getElement() instanceof GrondPoolElement g && g.getGroundLevelDelta() == 13,
                "saved and loaded: " + back.getGroundLevelDelta() + " " + back.getElement());
        helper.assertTrue(back.getElement() instanceof SinglePoolElement && back.getBoundingBox().equals(piece.getBoundingBox()), "same box");
        helper.succeed();
    }

    /** The Beardifier next to the wall of a sunk building (ground at template y 4, you walk at 5): the land is filled up to the
     *  floor and there is air above it. With vanilla's ground (1) the floor layer next to the wall was dug out: the moat. */
    @GuhTest(template = EMPTY, batch = "grond")
    public static void grondBaardSluitAanOpDeVloer(GameTestHelper helper) {
        BoundingBox box = new BoundingBox(0, 60, 0, 43, 91, 57);   // like the sjoelhuisje: minY 60, floor block 64, walk at 65
        int floor = 64;
        for (int dx = 1; dx <= 3; dx++) {
            double fixedFloor = beard(box, 5, -dx, floor, 20);
            double fixedAir = beard(box, 5, -dx, floor + 1, 20);
            double oldFloor = beard(box, 1, -dx, floor, 20);
            double oldBelow = beard(box, 1, -dx, floor - 2, 20);
            helper.assertTrue(fixedFloor > 0, dx + " next to the wall: the floor layer is filled (" + fixedFloor + ")");
            helper.assertTrue(fixedAir < 0, dx + " next to the wall: air above the floor (" + fixedAir + ")");
            helper.assertTrue(oldFloor < 0 && oldBelow < 0, dx + " the old ground dug the floor layer out (" + oldFloor + ", " + oldBelow + ")");
        }
        helper.succeed();
    }

    private static double beard(BoundingBox box, int delta, int x, int y, int z) {
        Beardifier b = new Beardifier(java.util.List.of(new Beardifier.Rigid(box, TerrainAdjustment.BEARD_BOX, delta)),
                java.util.List.<JigsawJunction>of(), box.inflatedBy(24));   // 26.1: affected box like forStructuresInChunk
        return b.compute(new DensityFunction.SinglePointContext(x, y, z));
    }

    /** The ring measurement of /guhs bouwcheck: land at the floor = 0, the old moat = -3, -2, -1. */
    @GuhTest(template = EMPTY, batch = "grond")
    public static void grondRandMeting(GameTestHelper helper) {
        BoundingBox box = new BoundingBox(0, 60, 0, 9, 80, 9);
        int delta = 5;   // floor block at 64, walk at 65 (first free y)
        double[] flat = Grond.rand(box, delta, 4, (x, z) -> 65);
        double[] moat = Grond.rand(box, delta, 4, (x, z) -> {
            int d = Math.max(Math.max(-x, x - 9), Math.max(-z, z - 9));
            return 65 - Math.max(0, 4 - d);
        });
        for (int d = 1; d <= 4; d++) {
            helper.assertTrue(Math.abs(flat[d]) < 1e-9, "flat ring " + d + ": " + flat[d]);
        }
        helper.assertTrue(Math.abs(moat[1] + 3) < 1e-9 && Math.abs(moat[2] + 2) < 1e-9 && Math.abs(moat[3] + 1) < 1e-9 && Math.abs(moat[4]) < 1e-9,
                "moat rings: " + moat[1] + " " + moat[2] + " " + moat[3] + " " + moat[4]);
        double[] none = Grond.rand(box, delta, 2, (x, z) -> Integer.MIN_VALUE);
        helper.assertTrue(Double.isNaN(none[1]), "unknown columns: NaN");
        helper.succeed();
    }
}
