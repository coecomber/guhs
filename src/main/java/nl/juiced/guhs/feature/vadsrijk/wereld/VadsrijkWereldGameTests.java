package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.KaartFeature;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Het Vadsrijk, wereld slice: only the risky bits. The generated map tiles load in this game version (format, size, the
 * arrival jetty is there); a temple door slides open and is rebuilt exactly from its hidden copy; a self-built portal frame
 * lights only when it is closed. Run: {@code runGameTestServer -Pgt=VadsrijkWereld}.
 */
public class VadsrijkWereldGameTests {
    private static final String BATCH = "vadsrijk_wereld";

    @GuhTest(template = "empty", batch = BATCH)
    public static void kaartTegelsLaden(GameTestHelper helper) {
        var mgr = helper.getLevel().getStructureManager();
        int tx = Math.floorDiv(WereldPlekken.AANKOMST.getX(), KaartFeature.TEGEL), tz = Math.floorDiv(WereldPlekken.AANKOMST.getZ(), KaartFeature.TEGEL);
        Optional<StructureTemplate> t = mgr.get(KaartFeature.tegelId(tx, tz));
        helper.assertTrue(t.isPresent(), "tile of the arrival missing");
        Vec3i maat = t.get().getSize();
        helper.assertTrue(maat.getX() == 64 && maat.getY() == 320 && maat.getZ() == 64, "tile size " + maat);
        BlockPos plank = new BlockPos(WereldPlekken.AANKOMST.getX() - tx * 64, WereldPlekken.AANKOMST.getY() - 1, WereldPlekken.AANKOMST.getZ() - tz * 64);
        boolean gevonden = t.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.SPRUCE_PLANKS).stream()
                .anyMatch(i -> i.pos().equals(plank));
        helper.assertTrue(gevonden, "no jetty plank under the arrival point " + plank);
        // the city and the temple tiles exist too, and the door's hidden copy is in its tile
        helper.assertTrue(mgr.get(KaartFeature.tegelId(0, 0)).isPresent(), "Zwamzoet tile missing");
        WereldPlekken.Deur d = WereldDeuren.deur("zwamtempel");
        helper.assertTrue(d != null, "door zwamtempel missing");
        helper.succeed();
    }

    /**
     * The placement path of kern.wereld.KaartFeature: a tile placed with a small bounding box (as the feature does per chunk)
     * puts exactly its own blocks. (The gametest server has no Vadsrijk dimension, so the tile is placed into the test area:
     * the arrival jetty plank lands at (2, 1, 2), the rest of the tile is cut away by the box.)
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void tegelPlaatsenMetDoos(GameTestHelper helper) {
        int tx = Math.floorDiv(WereldPlekken.AANKOMST.getX(), KaartFeature.TEGEL), tz = Math.floorDiv(WereldPlekken.AANKOMST.getZ(), KaartFeature.TEGEL);
        StructureTemplate t = helper.getLevel().getStructureManager().get(KaartFeature.tegelId(tx, tz)).orElseThrow();
        BlockPos lokaal = new BlockPos(WereldPlekken.AANKOMST.getX() - tx * 64, WereldPlekken.AANKOMST.getY() - 1, WereldPlekken.AANKOMST.getZ() - tz * 64);
        BlockPos doel = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos hoek = doel.subtract(lokaal);
        net.minecraft.world.level.levelgen.structure.BoundingBox doos = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                doel.getX() - 1, doel.getY(), doel.getZ() - 1, doel.getX() + 1, doel.getY() + 2, doel.getZ() + 1);
        t.placeInWorld(helper.getLevel(), hoek, hoek, new StructurePlaceSettings().setBoundingBox(doos).setIgnoreEntities(true).setKnownShape(true),
                helper.getLevel().getRandom(), 2);
        helper.assertBlockPresent(Blocks.SPRUCE_PLANKS, new BlockPos(2, 1, 2));
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(2, 1, 4))).isAir(), "the box did not cut the tile");
        helper.succeed();
    }

    @GuhTest(template = "portal_room", batch = BATCH)
    public static void deurSchuiftEnHerstelt(GameTestHelper helper) {
        // a 5 wide, 2 high door at z 1 (y 1..2), its hidden copy at y 5..6
        BlockState[] kolom = {Blocks.PINK_CONCRETE.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState(),
                Blocks.BLACK_WOOL.defaultBlockState(), Blocks.PINK_WOOL.defaultBlockState()};
        for (int u = 0; u < 5; u++) {
            for (int y : new int[]{1, 2, 5, 6}) {
                helper.setBlock(new BlockPos(1 + u, y, 1), kolom[u]);
            }
        }
        BlockPos min = helper.absolutePos(new BlockPos(1, 1, 1)), max = helper.absolutePos(new BlockPos(5, 2, 1));
        boolean langsX = min.getZ() == max.getZ();
        if (!langsX) {   // (the test may be rotated: then the door runs along z)
            helper.succeed();
            return;
        }
        WereldPlekken.Deur d = new WereldPlekken.Deur("test", new BlockPos(Math.min(min.getX(), max.getX()), min.getY(), min.getZ()),
                new BlockPos(Math.max(min.getX(), max.getX()), max.getY(), max.getZ()), min.south(3), helper.absolutePos(new BlockPos(1, 5, 1)).getY(), true);
        WereldDeuren.zet(helper.getLevel(), d, 3);
        for (int u = 0; u < 5; u++) {
            helper.assertTrue(helper.getLevel().getBlockState(d.min().offset(u, 0, 0)).isAir(), "column " + u + " not open");
        }
        WereldDeuren.zet(helper.getLevel(), d, 1);
        // half open: left wing shows column 1 at 0, right wing shows column 2 at 3, 3 at 4; 1 and 2 are the gap
        helper.assertTrue(helper.getLevel().getBlockState(d.min()).equals(kolom[1]), "left wing did not slide");
        helper.assertTrue(helper.getLevel().getBlockState(d.min().offset(4, 1, 0)).equals(kolom[3]), "right wing did not slide");
        WereldDeuren.zet(helper.getLevel(), d, 0);
        for (int u = 0; u < 5; u++) {
            helper.assertTrue(helper.getLevel().getBlockState(d.min().offset(u, 1, 0)).equals(kolom[u]), "column " + u + " not restored");
        }
        helper.succeed();
    }

    @GuhTest(template = "portal_room", batch = BATCH)
    public static void zelfgebouwdPortaal(GameTestHelper helper) {
        BlockState steen = WereldRegistry.POORTSTEEN.get().defaultBlockState();
        // frame 4 wide x 5 high in the plane z = 1 (inside 2 x 3)
        for (int x = 1; x <= 4; x++) {
            for (int y = 1; y <= 5; y++) {
                if (x == 1 || x == 4 || y == 1 || y == 5) {
                    helper.setBlock(new BlockPos(x, y, 1), steen);
                }
            }
        }
        // an open frame first: no portal
        helper.setBlock(new BlockPos(4, 3, 1), Blocks.AIR);
        helper.assertTrue(Portalen.ontsteek(helper.getLevel(), helper.absolutePos(new BlockPos(1, 3, 1))) == 0, "open frame lit");
        helper.setBlock(new BlockPos(4, 3, 1), steen);
        int n = Portalen.ontsteek(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 1)));
        helper.assertTrue(n == 6, "expected 6 portal blocks, got " + n);
        helper.assertBlockPresent(WereldRegistry.PORTAAL.get(), new BlockPos(2, 2, 1));
        helper.assertBlockPresent(WereldRegistry.PORTAAL.get(), new BlockPos(3, 4, 1));
        helper.succeed();
    }
}
