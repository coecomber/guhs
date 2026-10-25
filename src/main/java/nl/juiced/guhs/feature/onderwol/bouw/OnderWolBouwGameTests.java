package nl.juiced.guhs.feature.onderwol.bouw;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.eilanden.EilandenFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.world.GegarandeerdPlacement;

/**
 * Game tests of the underground buildings (feature/onderwol/bouw): the structures and their pools, the hall, the shaft,
 * the rock processor and the floor search. Template {@code onderwol_bouw_test}: 24 x 16 x 24 of air
 * (tools/features/onderwol_structuren1.py).
 */
public class OnderWolBouwGameTests {
    private static final String VELD = "onderwol_bouw_test";
    private static final String BATCH = "onderwol_bouw";

    private static ResourceKey<Structure> structuur(String naam) {
        return ResourceKey.create(Registries.STRUCTURE, Guhs.id(naam));
    }

    /** Every structure of this slice is a guhs:grot_bouw with a sane band (above the saus sea), a reach, and biomes. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolBouwStructurenBestaan(GameTestHelper helper) {
        var structuren = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String naam : OnderWolBouw.STRUCTUREN) {
            Optional<Structure> s = structuren.getOptional(structuur(naam));
            helper.assertTrue(s.isPresent() && s.get() instanceof GrotBouwStructure, naam + ": a guhs:grot_bouw");
            GrotBouwStructure g = (GrotBouwStructure) s.get();
            helper.assertTrue(g.bandOnder() > GrotBouwStructure.SAUS_TOT && g.maxY() >= g.bandOnder(), naam + ": its band lies above the saus sea");
            helper.assertTrue(g.keepClear() > 0, naam + ": it claims its room (keep_clear)");
            helper.assertTrue(g.biomes().size() > 5, naam + ": it can stand under the Guhmension's biomes");
        }
        helper.succeed();
    }

    /** The Mijnguhdorp: one guaranteed copy within ~1000 blocks of spawn, only in new terrain (the server world is never reset). */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolBouwDorpGegarandeerdAlleenNieuw(GameTestHelper helper) {
        var sets = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        Optional<StructureSet> set = sets.getOptional(ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id("mijnguhdorp_gegarandeerd")));
        helper.assertTrue(set.isPresent() && set.get().placement() instanceof GegarandeerdPlacement, "the Mijnguhdorp has a guaranteed set");
        GegarandeerdPlacement p = (GegarandeerdPlacement) set.get().placement();
        helper.assertTrue(p.alleenNieuw(), "only in new terrain (alleen_nieuw)");
        helper.assertTrue(p.maxAfstand() <= 1000, "at most ~1000 blocks from spawn: " + p.maxAfstand());
        helper.succeed();
    }

    /** Every start template has its anchor; every build with an entrance has the shaft's marker with a free column above it. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolBouwTemplatesHebbenAnkers(GameTestHelper helper) {
        var pools = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var manager = helper.getLevel().getStructureManager();
        var structuren = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String naam : OnderWolBouw.STRUCTUREN) {
            GrotBouwStructure g = (GrotBouwStructure) structuren.getOptional(structuur(naam)).orElseThrow();
            StructureTemplatePool pool = pools.getOptional(ResourceKey.create(Registries.TEMPLATE_POOL, Guhs.id("onderwol/" + naam + "/start"))).orElse(null);
            helper.assertTrue(pool != null, naam + ": its start pool");
            for (StructurePoolElement e : pool.getShuffledTemplates(RandomSource.create(1))) {
                var jigsaws = e.getShuffledJigsawBlocks(manager, BlockPos.ZERO, net.minecraft.world.level.block.Rotation.NONE, RandomSource.create(1));
                helper.assertTrue(jigsaws.stream().anyMatch(j -> j.name().equals(Guhs.id(naam + "_midden"))), naam + ": the anchor guhs:" + naam + "_midden in " + e);
                if (g.ingang().isPresent() && g.ingang().get().verplicht()) {
                    helper.assertTrue(jigsaws.stream().anyMatch(j -> j.name().equals(GrotBouwStructure.INGANG)), naam + ": the shaft's marker (ingang verplicht)");
                }
            }
        }
        // the entrance hut: its marker in the floor opening, its top side horizontal (where the lift lets you off)
        StructureTemplatePool hut = pools.getOptional(ResourceKey.create(Registries.TEMPLATE_POOL, Guhs.id("onderwol/ingang_hutje/start"))).orElseThrow();
        for (StructurePoolElement e : hut.getShuffledTemplates(RandomSource.create(1))) {
            var boven = e.getShuffledJigsawBlocks(manager, BlockPos.ZERO, net.minecraft.world.level.block.Rotation.NONE, RandomSource.create(1)).stream()
                    .filter(j -> j.name().equals(GrotBouwStructure.INGANG_BOVEN)).findFirst();
            helper.assertTrue(boven.isPresent(), "the entrance hut has guhs:grot_ingang_boven");
            helper.assertTrue(JigsawBlock.getFrontFacing(boven.get().info().state()) == Direction.DOWN
                    && JigsawBlock.getTopFacing(boven.get().info().state()).getAxis().isHorizontal(), "facing down, its top horizontal");
        }
        helper.succeed();
    }

    /** The shape of the hall: inside in the middle, outside past its radii, a dome, a floor that rises towards the wall. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolBouwZaalVorm(GameTestHelper helper) {
        helper.assertTrue(GrotZaalPiece.afstand(0, 0, 8, 6) == 0.0, "the middle is inside");
        helper.assertTrue(GrotZaalPiece.afstand(9, 0, 8, 6) > 1.0 && GrotZaalPiece.afstand(0, 7, 8, 6) > 1.0, "past the radii is outside");
        helper.assertTrue(GrotZaalPiece.afstand(6, 4, 8, 6) < 1.0, "a rounded rectangle: a corner of the build fits");
        GrotZaalPiece z = new GrotZaalPiece(0, 0, 40, 8, 8, 10, 1.0f, 6, 1L);
        helper.assertTrue(z.plafond(0.0, 0.0) > z.plafond(0.95, 0.0), "a dome: lower towards the wall");
        helper.assertTrue(z.plafond(0.0, 0.0) <= 40 + 10 + 2, "not higher than asked (plus the wobble)");
        helper.assertTrue(z.vloerOp(0.5) == 40 && z.vloerOp(1.0) > 40, "a flat floor that rises at the wall");
        helper.succeed();
    }

    /** The hall carves the rock, fills the hole under its floor and dams a lake next to it. */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 200)
    public static void onderwolBouwZaalHaktUit(GameTestHelper helper) {
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        for (int x = 0; x < 24; x++) {
            for (int z = 0; z < 24; z++) {
                for (int y = 0; y < 14; y++) {
                    helper.getLevel().setBlock(o.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        int vloer = o.getY() + 3;
        BlockPos gat = new BlockPos(o.getX() + 12, vloer - 1, o.getZ() + 12);
        helper.getLevel().setBlock(gat, Blocks.AIR.defaultBlockState(), 2);                        // (a hole under the floor)
        BlockPos meer = new BlockPos(o.getX() + 12, vloer + 1, o.getZ() + 18);
        helper.getLevel().setBlock(meer, Blocks.WATER.defaultBlockState(), 2);                     // (a lake just outside the wall)
        GrotZaalPiece zaal = new GrotZaalPiece(o.getX() + 12, o.getZ() + 12, vloer, 6, 6, 7, 0.0f, 0, 7L);
        BoundingBox heel = new BoundingBox(o.getX(), o.getY(), o.getZ(), o.getX() + 23, o.getY() + 15, o.getZ() + 23);
        zaal.postProcess(helper.getLevel(), helper.getLevel().structureManager(), helper.getLevel().getChunkSource().getGenerator(),
                RandomSource.create(1), heel, ChunkPos.containing(o), o);
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(o.getX() + 12, vloer + 1, o.getZ() + 12)).isAir(), "the middle is carved");
        helper.assertTrue(!helper.getLevel().getBlockState(gat).isAir(), "the hole under the floor is filled");
        helper.assertTrue(!helper.getLevel().getBlockState(new BlockPos(o.getX() + 1, vloer + 1, o.getZ() + 1)).isAir(), "the corner rock stays");
        helper.assertTrue(!helper.getLevel().getBlockState(new BlockPos(o.getX() + 12, vloer + 1, o.getZ() + 17)).isAir(), "the lake is dammed");
        helper.succeed();
    }

    /** The rock marker: the world's own solid block stays, an open spot becomes layer stone; outside the box nothing is read. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolBouwSchilProcessor(GameTestHelper helper) {
        BlockPos o = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(o, Blocks.DIRT.defaultBlockState(), 2);
        BlockPos open = o.east();
        helper.getLevel().setBlock(open, Blocks.AIR.defaultBlockState(), 2);
        StructurePlaceSettings settings = new StructurePlaceSettings().setBoundingBox(new BoundingBox(o.getX() - 1, o.getY() - 1, o.getZ() - 1,
                o.getX() + 3, o.getY() + 1, o.getZ() + 1));
        BlockState marker = GrotSchilProcessor.MARKER.defaultBlockState();
        var vast = GrotSchilProcessor.INSTANCE.processBlock(helper.getLevel(), BlockPos.ZERO, BlockPos.ZERO,
                new StructureTemplate.StructureBlockInfo(o, marker, null), new StructureTemplate.StructureBlockInfo(o, marker, null), settings);
        helper.assertTrue(vast == null, "solid ground stays the world's own");
        var gevuld = GrotSchilProcessor.INSTANCE.processBlock(helper.getLevel(), BlockPos.ZERO, BlockPos.ZERO,
                new StructureTemplate.StructureBlockInfo(open, marker, null), new StructureTemplate.StructureBlockInfo(open, marker, null), settings);
        helper.assertTrue(gevuld != null && !gevuld.state().isAir() && !gevuld.state().is(GrotSchilProcessor.MARKER), "an open spot becomes stone");
        BlockPos ver = o.offset(10, 0, 0);
        var buiten = GrotSchilProcessor.INSTANCE.processBlock(helper.getLevel(), BlockPos.ZERO, BlockPos.ZERO,
                new StructureTemplate.StructureBlockInfo(ver, marker, null), new StructureTemplate.StructureBlockInfo(ver, marker, null), settings);
        helper.assertTrue(buiten != null && buiten.state().is(GrotSchilProcessor.MARKER), "outside the box: untouched (placed by another chunk)");
        helper.assertTrue(GrotSchilProcessor.laagsteen(10).getBlock() != Blocks.AIR && GrotSchilProcessor.laagsteen(90).is(Blocks.PINK_WOOL),
                "the layer by height: stone deep down, wool on top");
        helper.succeed();
    }

    /**
     * The old underground structures (OudeBouwLaagProcessor): imitation rock always and hidden wall fill become layer stone,
     * walls seen from inside and decorative blocks stay, a mantle closes them off from open world, outside the box nothing
     * changes; their pools use the processor list and the ones at cave depth come less often.
     */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolBouwOudeBouwLagen(GameTestHelper helper) {
        var level = helper.getLevel();
        java.util.List<StructureTemplate.StructureBlockInfo> mal = new java.util.ArrayList<>();
        // a 3 x 3 x 3 room of pink wool around one block of air (local 5..7, 2..4, 5..7); its floor block in the middle is dirt
        for (int x = 5; x <= 7; x++) {
            for (int y = 2; y <= 4; y++) {
                for (int z = 5; z <= 7; z++) {
                    BlockState st = x == 6 && y == 3 && z == 6 ? Blocks.AIR.defaultBlockState()
                            : x == 6 && y == 2 && z == 6 ? Blocks.DIRT.defaultBlockState()
                            : x == 7 && y == 3 && z == 6 ? Blocks.YELLOW_STAINED_GLASS.defaultBlockState() : Blocks.PINK_WOOL.defaultBlockState();
                    mal.add(new StructureTemplate.StructureBlockInfo(helper.absolutePos(new BlockPos(x, y, z)), st, null));
                }
            }
        }
        BlockPos ver = helper.absolutePos(new BlockPos(14, 3, 6));
        mal.add(new StructureTemplate.StructureBlockInfo(ver, Blocks.PINK_WOOL.defaultBlockState(), null));
        BlockPos hoek = helper.absolutePos(new BlockPos(5, 2, 5));
        level.setBlock(hoek, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Guhs.id("viltsteen")).defaultBlockState(), 2);
        level.setBlock(helper.absolutePos(new BlockPos(6, 14, 5)), Blocks.STONE.defaultBlockState(), 2);   // (deep under "the surface")
        BlockPos a = helper.absolutePos(new BlockPos(4, 1, 4)), b = helper.absolutePos(new BlockPos(8, 5, 8));
        BoundingBox box = BoundingBox.fromCorners(a, b);
        var uit = OudeBouwLaagProcessor.verwerk(level, box, mal);
        java.util.Map<BlockPos, BlockState> m = new java.util.HashMap<>();
        for (var i : uit) {
            m.put(i.pos(), i.state());
        }
        BlockState muur = m.get(helper.absolutePos(new BlockPos(5, 3, 6)));
        helper.assertTrue(muur != null && muur.is(Blocks.PINK_WOOL), "a wall seen from inside stays pink: " + muur);
        BlockState h = m.get(hoek);
        helper.assertTrue(h != null && h.getBlock() == net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Guhs.id("viltsteen")),
                "a hidden corner takes the world's own layer stone: " + h);
        BlockState vloer = m.get(helper.absolutePos(new BlockPos(6, 2, 6)));
        helper.assertTrue(vloer != null && vloer.is(GrotSchilProcessor.LAAGSTEEN) && !vloer.is(Blocks.DIRT), "imitation rock always: " + vloer);
        helper.assertTrue(m.get(helper.absolutePos(new BlockPos(7, 3, 6))).is(Blocks.YELLOW_STAINED_GLASS), "decorative blocks stay");
        helper.assertTrue(m.get(helper.absolutePos(new BlockPos(6, 3, 6))).isAir(), "the room stays air");
        helper.assertTrue(m.get(ver).is(Blocks.PINK_WOOL), "outside the box: untouched (placed by another chunk)");
        BlockState mantel = m.get(helper.absolutePos(new BlockPos(6, 3, 4)));
        helper.assertTrue(mantel != null && mantel.is(GrotSchilProcessor.LAAGSTEEN), "a mantle against the open world: " + mantel);
        helper.assertTrue(!m.containsKey(helper.absolutePos(new BlockPos(6, 3, 9))), "no mantle outside the box");
        // the data
        var lijst = level.registryAccess().lookupOrThrow(Registries.PROCESSOR_LIST)
                .get(ResourceKey.create(Registries.PROCESSOR_LIST, Guhs.id("onderwol_oude_bouw")));
        helper.assertTrue(lijst.isPresent() && lijst.get().value().list().size() == 1
                && lijst.get().value().list().get(0) instanceof OudeBouwLaagProcessor, "the processor list");
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (var e : java.util.Map.of("guh_caves", 13, "challenging_guh_caves", 30, "gatenkaas_mijnschacht", 18, "stille_voorraadkelder", 40).entrySet()) {
            var set = sets.getOptional(ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id(e.getKey())));
            helper.assertTrue(set.isPresent() && set.get().placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement r
                    && r.spacing() == e.getValue(), e.getKey() + ": spacing " + e.getValue());
        }
        for (String pool : java.util.List.of("guh_caves/start", "guh_caves/tubes", "challenging_guh_caves/start", "kaasmijn/start",
                "gatenkaas_mijnschacht/start", "stille_voorraadkelder/start")) {
            try (InputStream in = OnderWolBouwGameTests.class.getResourceAsStream("/data/guhs/worldgen/template_pool/" + pool + ".json")) {
                JsonObject js = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var el : js.getAsJsonArray("elements")) {
                    helper.assertTrue("guhs:onderwol_oude_bouw".equals(el.getAsJsonObject().getAsJsonObject("element").get("processors").getAsString()),
                            pool + ": the layer processor");
                }
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        helper.succeed();
    }

    /** The shaft: pads at the bottom, a column of wolkenstroom up and down, a ladder, a lining in the rock. */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 200)
    public static void onderwolBouwSchacht(GameTestHelper helper) {
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        for (int x = 6; x < 18; x++) {
            for (int z = 6; z < 18; z++) {
                for (int y = 0; y < 14; y++) {
                    helper.getLevel().setBlock(o.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        int x = o.getX() + 12, z = o.getZ() + 12, onder = o.getY() + 1, boven = o.getY() + 12;
        GrotSchachtPiece schacht = new GrotSchachtPiece(x, z, onder, boven, boven, Direction.SOUTH);
        schacht.postProcess(helper.getLevel(), helper.getLevel().structureManager(), helper.getLevel().getChunkSource().getGenerator(),
                RandomSource.create(1), schacht.getBoundingBox(), ChunkPos.containing(o), o);
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x, onder, z)).is(EilandenFeature.WOLKENLIFT.get()), "a pad at the bottom");
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x, onder + 5, z)).is(EilandenFeature.WOLKENSTROOM.get()), "a column above it");
        Direction dwars = Direction.SOUTH.getClockWise();
        helper.assertTrue(helper.getLevel().getBlockState(new BlockPos(x - dwars.getStepX(), onder + 5, z - dwars.getStepZ())).is(Blocks.LADDER), "a ladder");
        helper.assertTrue(!helper.getLevel().getBlockState(new BlockPos(x, onder + 5, z - 1)).is(Blocks.STONE), "the lining replaced the rock");
        helper.succeed();
    }

    /** The floor search: the highest cave floor in the band with room above it. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolBouwGrotVloerZoeken(GameTestHelper helper) {
        BlockState[] kolom = new BlockState[64];
        Arrays.fill(kolom, Blocks.STONE.defaultBlockState());
        for (int y = 20; y < 28; y++) {
            kolom[y] = Blocks.AIR.defaultBlockState();       // a cave: floor at y 20 (stone at 19)
        }
        for (int y = 40; y < 43; y++) {
            kolom[y] = Blocks.AIR.defaultBlockState();       // too low a gap
        }
        NoiseColumn column = new NoiseColumn(0, kolom);
        helper.assertTrue(GrotBouwStructure.zoekGrotVloer(column, 10, 50) == 20, "the cave floor at y 20, not the low gap");
        helper.assertTrue(GrotBouwStructure.zoekGrotVloer(column, 30, 50) == Integer.MIN_VALUE, "none in a band without a cave");
        helper.succeed();
    }

    /** The talking guhs have their lines (nl_nl), and every structure its Superkompas name. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void onderwolBouwTeksten(GameTestHelper helper) {
        try (InputStream in = OnderWolBouwGameTests.class.getResourceAsStream("/assets/guhs/lang/nl_nl.json")) {
            JsonObject nl = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            OnderWolBouw.PRAAT.forEach((plek, p) -> {
                for (int i = 1; i <= p.regels(); i++) {
                    helper.assertTrue(nl.has("quest.guhs.onderwol_bouw." + plek + "." + i), plek + " line " + i);
                }
            });
            for (String s : OnderWolBouw.STRUCTUREN) {
                helper.assertTrue(nl.has("structure.guhs." + s) && nl.has("structure.guhs." + s + ".tooltip"), s + ": its Superkompas texts");
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }
}
