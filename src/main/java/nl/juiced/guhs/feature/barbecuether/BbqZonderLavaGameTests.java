package nl.juiced.guhs.feature.barbecuether;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.7.0 (ronde 25/26): no real lava in the Barbecuether. The one-time conversion of old chunks (only inside the test's
 * box, so nothing around it changes), the carver of new chunks and the dimension's own fluid.
 */
public class BbqZonderLavaGameTests {
    private static final String EMPTY = "empty";

    /** Runs the conversion on every chunk the test box touches, but only inside the box. */
    private static int zetOm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(0, 0, 0)), b = helper.absolutePos(new BlockPos(4, 3, 4));
        BoundingBox box = BoundingBox.fromCorners(a, b);
        int om = 0;
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                om += BbqZonderLava.zetOm(level, level.getChunk(cx, cz), box);
            }
        }
        return om;
    }

    @GuhTest(template = EMPTY)
    public static void bbqLavaWordtKaasfrituursaus(GameTestHelper helper) {
        BlockState lava = Blocks.LAVA.defaultBlockState();
        // a stone tub so nothing runs away before the conversion
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 2; y++) {
                    boolean rand = x == 0 || z == 0 || x == 4 || z == 4;
                    helper.setBlock(new BlockPos(x, y, z), rand ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
        BlockPos bron = new BlockPos(1, 1, 1), stromend = new BlockPos(3, 1, 3), vallend = new BlockPos(2, 2, 2), water = new BlockPos(1, 1, 3);
        helper.setBlock(bron, lava);
        helper.setBlock(stromend, lava.setValue(LiquidBlock.LEVEL, 3));
        helper.setBlock(vallend, lava.setValue(LiquidBlock.LEVEL, 8));
        helper.setBlock(water, Blocks.WATER.defaultBlockState());

        int om = zetOm(helper);
        helper.assertTrue(om == 3, "three lava blocks converted (got " + om + ")");
        BlockState saus = BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get().defaultBlockState();
        { BlockState s = helper.getBlockState(bron); helper.assertTrue(s.is(saus.getBlock()) && s.getValue(LiquidBlock.LEVEL) == 0, "source -> source"); }
        helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(bron)).isSource(), "the sauce source is a source fluid");
        { BlockState s = helper.getBlockState(stromend); helper.assertTrue(s.is(saus.getBlock()) && s.getValue(LiquidBlock.LEVEL) == 3, "flowing keeps its level"); }
        { BlockState s = helper.getBlockState(vallend); helper.assertTrue(s.is(saus.getBlock()) && s.getValue(LiquidBlock.LEVEL) == 8, "falling stays falling"); }
        helper.assertTrue(Kaasfrituursaus.isSauce(helper.getLevel().getFluidState(helper.absolutePos(stromend)).getType()), "flowing sauce fluid");
        { BlockState s = helper.getBlockState(water); helper.assertTrue(s.is(Blocks.WATER), "water is left alone"); }
        helper.assertTrue(zetOm(helper) == 0, "a second pass finds nothing");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void bbqLavaAlleenInDeBarbecuether(GameTestHelper helper) {
        helper.assertTrue(!BbqZonderLava.hoortErbij(helper.getLevel()), "the test level is not the Barbecuether: no conversion there");
        helper.assertTrue(BbqZonderLava.saus(Blocks.WATER.defaultBlockState()) == null, "only lava is converted");
        helper.assertTrue(BbqZonderLava.saus(Blocks.STONE.defaultBlockState()) == null, "only lava is converted");
        BbqZonderLava.Stand stand = new BbqZonderLava.Stand();
        ChunkPos pos = new ChunkPos(12, -7);
        helper.assertTrue(!stand.heeft(pos), "a chunk starts unconverted");
        stand.markeer(pos);
        helper.assertTrue(stand.heeft(pos) && BbqZonderLava.Stand.load(stand.save(new net.minecraft.nbt.CompoundTag())).heeft(pos),
                "a converted chunk stays marked, also after saving and loading");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void bbqNieuweGrottenZonderLava(GameTestHelper helper) {
        var carvers = helper.getLevel().registryAccess().lookupOrThrow(Registries.CONFIGURED_CARVER);
        ConfiguredWorldCarver<?> grot = carvers.getValue(ResourceKey.create(Registries.CONFIGURED_CARVER, Guhs.id("barbecuegrot")));
        helper.assertTrue(grot != null && grot.worldCarver() == BbqZonderLava.SAUS_GROT.get(),
                "the Barbecuether caves use guhs:sausgrot (vanilla's nether_cave fills them with lava below y 31)");
        NoiseGeneratorSettings noise = helper.getLevel().registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)
                .getValue(ResourceKey.create(Registries.NOISE_SETTINGS, Guhs.id("barbecuether")));
        helper.assertTrue(noise != null && noise.defaultFluid().is(BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get())
                && !noise.aquifersEnabled() && noise.noiseSettings().minY() >= -54,
                "the sea is kaasfrituursaus, no aquifers and nothing below y -54 (where the generator puts lava)");
        helper.succeed();
    }
}
