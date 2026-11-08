package nl.juiced.guhs.feature.vadsrijk.grootte;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.feature.vadsrijk.kern.wereld.KaartFeature;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-GROOTTE: the compact map pack places exactly the same world as the old .nbt tiles. The dev build/resources still hold the
 * .nbt tiles (only the jar drops them), so both can be loaded side by side. Run: {@code runGameTestServer -Pgt=VadsrijkGrootte}.
 */
public class VadsrijkGrootteGameTests {
    private static final String BATCH = "vadsrijk_grootte";

    /** Every 8th tile (+ the first): the vanilla-loaded template and the pack template save to the same NBT. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void pakTemplatesGelijk(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        List<long[]> tegels = steekproef(sl, 8);
        helper.assertTrue(!tegels.isEmpty(), "no map pack in the resources (kaartPak did not run?)");
        int n = 0;
        for (long[] t : tegels) {
            int tx = (int) t[0], tz = (int) t[1];
            StructureTemplate oud = sl.getStructureManager().get(KaartFeature.tegelId(tx, tz)).orElse(null);
            helper.assertTrue(oud != null, "no .nbt tile " + tx + "," + tz + " in build/resources");
            StructureTemplate nieuw = KaartPakLader.tegel(sl.getServer(), tx, tz).orElseThrow();
            CompoundTag a = oud.save(new CompoundTag()), b = nieuw.save(new CompoundTag());
            helper.assertTrue(a.equals(b), "tile " + tx + "," + tz + " differs between .nbt and pack");
            sl.getStructureManager().remove(KaartFeature.tegelId(tx, tz));
            KaartPakLader.vergeet(tx, tz);
            n++;
        }
        helper.assertTrue(n > 50, "too few tiles compared: " + n);
        helper.succeed();
    }

    /** Every 6th other guhs structure: the vanilla-loaded .nbt and the structure pack (structuren.gpak) save to the same NBT. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void structuurPakGelijk(GameTestHelper helper) throws java.io.IOException {
        ServerLevel sl = helper.getLevel();
        List<String> namen = new ArrayList<>(StructuurPakLader.namen(sl.getServer()));
        helper.assertTrue(namen.size() > 500, "no structure pack in the resources (kaartPak did not run?)");
        namen.sort(null);
        int n = 0;
        for (int i = 0; i < namen.size(); i += 6) {
            String naam = namen.get(i);
            net.minecraft.resources.Identifier id = nl.juiced.guhs.Guhs.id(naam);
            StructureTemplate oud = sl.getStructureManager().get(id).orElse(null);
            helper.assertTrue(oud != null, "no .nbt structure " + naam + " in the dev resources");
            StructureTemplate nieuw = StructuurPakLader.uitPak(sl.getServer(), naam).orElseThrow();
            helper.assertTrue(oud.save(new CompoundTag()).equals(nieuw.save(new CompoundTag())), "structure " + naam + " differs between .nbt and pack");
            n++;
        }
        org.slf4j.LoggerFactory.getLogger("guhs/kaartpak").info("structuurPakGelijk: {} structures compared", n);
        helper.succeed();
    }

    /**
     * Block by block: one chunk of 12 sample tiles is placed twice far away (old .nbt at chunk A, pack at chunk A+2), with
     * exactly KaartFeature's settings and box, and every block state + block entity of the column is compared.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void pakWereldGelijk(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        List<long[]> alle = steekproef(sl, 1);
        helper.assertTrue(alle.size() > 12, "no map pack in the resources");
        List<long[]> keuze = new ArrayList<>();
        int stap = alle.size() / 12;
        for (int i = 0; i < 12; i++) keuze.add(alle.get(i * stap + stap / 2));
        int vergeleken = 0, blokken = 0;
        for (int i = 0; i < keuze.size(); i++) {
            int tx = (int) keuze.get(i)[0], tz = (int) keuze.get(i)[1];
            int lcx = (i * 3) % 4, lcz = (i * 7 / 3) % 4;   // a different chunk of the tile each time
            StructureTemplate oud = sl.getStructureManager().get(KaartFeature.tegelId(tx, tz)).orElseThrow();
            StructureTemplate nieuw = KaartPakLader.tegel(sl.getServer(), tx, tz).orElseThrow();
            int cxA = 20000, czA = 20000 + i * 4;
            plaats(sl, oud, cxA, czA, lcx, lcz);
            plaats(sl, nieuw, cxA + 2, czA, lcx, lcz);
            for (int y = sl.getMinY(); y < sl.getMaxY(); y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos a = new BlockPos(cxA * 16 + x, y, czA * 16 + z), b = a.offset(32, 0, 0);
                        BlockState sa = sl.getBlockState(a), sb = sl.getBlockState(b);
                        helper.assertTrue(sa == sb, "tile " + tx + "," + tz + " local " + x + "," + y + "," + z + ": " + sa + " vs " + sb);
                        if (!sa.isAir()) blokken++;
                        BlockEntity ea = sl.getBlockEntity(a), eb = sl.getBlockEntity(b);
                        helper.assertTrue((ea == null) == (eb == null), "block entity presence differs at " + a);
                        if (ea != null) {
                            helper.assertTrue(Objects.equals(ea.saveWithoutMetadata(sl.registryAccess()), eb.saveWithoutMetadata(sl.registryAccess())),
                                    "block entity differs at tile " + tx + "," + tz + " local " + x + "," + y + "," + z);
                        }
                        vergeleken++;
                    }
                }
            }
            sl.getStructureManager().remove(KaartFeature.tegelId(tx, tz));
            KaartPakLader.vergeet(tx, tz);
        }
        helper.assertTrue(blokken > 1000, "the sample chunks were (almost) empty: " + blokken);
        org.slf4j.LoggerFactory.getLogger("guhs/kaartpak").info("pakWereldGelijk: {} positions compared, {} blocks", vergeleken, blokken);
        helper.succeed();
    }

    /** Clears the chunk column (cx, cz) and places chunk (lcx, lcz) of the tile there, like KaartFeature.place. */
    private static void plaats(ServerLevel sl, StructureTemplate t, int cx, int cz, int lcx, int lcz) {
        sl.getChunk(cx, cz);
        int x0 = cx * 16, z0 = cz * 16;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = sl.getMinY(); y < sl.getMaxY(); y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    m.set(x0 + x, y, z0 + z);
                    if (!sl.getBlockState(m).isAir()) sl.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
        BoundingBox chunk = new BoundingBox(x0, sl.getMinY(), z0, x0 + 15, sl.getMaxY(), z0 + 15);
        StructurePlaceSettings s = new StructurePlaceSettings().setBoundingBox(chunk).setIgnoreEntities(true).setKnownShape(true);
        BlockPos hoek = new BlockPos(x0 - lcx * 16, 0, z0 - lcz * 16);
        t.placeInWorld(sl, hoek, hoek, s, sl.getRandom(), 2);
    }

    /** Every n-th tile of the pack (sorted), as {tx, tz}. Empty when there is no pack. */
    private static List<long[]> steekproef(ServerLevel sl, int elke) {
        List<long[]> l = new ArrayList<>();
        Optional<StructureTemplate> probe = KaartPakLader.tegel(sl.getServer(), 0, 0);
        if (probe == null) return l;
        List<Long> sleutels = new ArrayList<>(KaartPakLader.sleutels(sl.getServer()));
        sleutels.sort(null);
        for (int i = 0; i < sleutels.size(); i += elke) {
            long k = sleutels.get(i);
            l.add(new long[]{(int) (k >> 32), (int) k});
        }
        return l;
    }
}
