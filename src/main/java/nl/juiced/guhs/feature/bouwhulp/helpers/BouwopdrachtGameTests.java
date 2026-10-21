package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.bouwhulp.kern.Spookbouw;
import nl.juiced.guhs.feature.safari.verblijf.ThuisDier;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfData;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfFeature;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfsbordBlockEntity;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Deel 2 F10b (design 2.8 + 2.9): the helpers' templates fit the terrain (the beaver's dam across the stream with its pool,
 * the igloo, the pond, the zebra crossing...), one job per board, nothing beyond 48 blocks, "Hier stroomt niks" without
 * water, the fitting home's fence keeps the escape-proof rule, the family goes on its work outing and comes home.
 */
public class BouwopdrachtGameTests {
    private static final String BATCH = "bouwopdracht";
    private static final String VELD = "px_test_48";
    /** The ground: dirt from y1 to y4, grass on top. */
    private static final int GROND = 4;

    private static void veld(GameTestHelper helper) {
        Bouwopdracht.testZoekStraal = 20;
        for (int x = 0; x < 48; x++) {
            for (int z = 0; z < 48; z++) {
                for (int y = 1; y <= GROND; y++) {
                    helper.setBlock(new BlockPos(x, y, z), y == GROND ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                }
            }
        }
    }

    /** A stream along z at x 30..32: the bed at y1, water y2..y3 (the banks at y4). */
    private static void beek(GameTestHelper helper) {
        for (int x = 30; x <= 32; x++) {
            for (int z = 0; z < 48; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER.defaultBlockState());
                helper.setBlock(new BlockPos(x, 3, z), Blocks.WATER.defaultBlockState());
                helper.setBlock(new BlockPos(x, 4, z), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static VerblijfsbordBlockEntity bord(GameTestHelper helper, BlockPos plek, String soort) {
        helper.setBlock(plek, VerblijfFeature.BORD.get().defaultBlockState());
        VerblijfsbordBlockEntity be = (VerblijfsbordBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(plek));
        helper.assertTrue(be != null, "the board has its block entity");
        be.plaats(helper.getLevel(), null, soort);
        be.zetStraal(helper.getLevel(), VerblijfsbordBlockEntity.MIN_STRAAL);
        return be;
    }

    private static Klus klus(GameTestHelper helper, BlockPos bord, String soort, String taak, int variant) {
        return new Klus(helper.getLevel(), helper.absolutePos(bord), soort, taak, variant, 0, 20, RandomSource.create(7));
    }

    private static long tel(Map<BlockPos, BlockState> m, java.util.function.Predicate<BlockState> p) {
        return m.values().stream().filter(p).count();
    }

    /** The beaver dam: across the stream (from bank to bank), the water upstream 1 higher, and the dam holds it. */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 40)
    public static void bouwopdrachtDamOverDeBeek(GameTestHelper helper) {
        veld(helper);
        beek(helper);
        BlockPos bord = new BlockPos(14, GROND + 1, 24);
        Klus k = klus(helper, bord, "bever", "dam", 0);
        List<BlockPos> plek = Sjablonen.van("dam").zoek(k);
        helper.assertTrue(plek.size() == 1 && k.isWater(plek.get(0).getX(), plek.get(0).getZ()), "the beavers found the stream: " + plek);
        Map<BlockPos, BlockState> m = Sjablonen.van("dam").maak(k, plek);
        BlockPos abs = helper.absolutePos(BlockPos.ZERO);
        int damZ = plek.get(0).getZ();
        // the dam: on every stream column at its line, from the bed up to the new level (4)
        for (int x = 30; x <= 32; x++) {
            BlockState s = m.get(new BlockPos(abs.getX() + x, abs.getY() + 4, damZ));
            helper.assertTrue(s != null && !s.isAir() && s.getFluidState().isEmpty(), "dam block over the stream at x " + x + ": " + s);
        }
        long water = m.entrySet().stream().filter(e -> e.getValue().is(Blocks.WATER) && e.getKey().getY() == abs.getY() + 4).count();
        helper.assertTrue(water >= 6, "the water behind the dam rises a block (" + water + " new water blocks at y4)");
        boolean eenKant = m.entrySet().stream().filter(e -> e.getValue().is(Blocks.WATER)).allMatch(e -> e.getKey().getZ() > damZ)
                || m.entrySet().stream().filter(e -> e.getValue().is(Blocks.WATER)).allMatch(e -> e.getKey().getZ() < damZ);
        helper.assertTrue(eenKant, "the pool is on one side of the dam (upstream)");
        helper.succeed();
    }

    /** No water near: "Hier stroomt niks, njeg". */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 20)
    public static void bouwopdrachtGeenWater(GameTestHelper helper) {
        veld(helper);
        Klus k = klus(helper, new BlockPos(24, GROND + 1, 24), "bever", "dam", 0);
        try {
            Sjablonen.van("dam").zoek(k);
            helper.fail("a dam without water");
        } catch (Klus.Nee nee) {
            helper.assertTrue(nee.sleutel.equals("geen_water"), "the message is geen_water: " + nee.sleutel);
        }
        helper.succeed();
    }

    /** One job per board, and nothing further than 48 blocks from it. */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 40)
    public static void bouwopdrachtEenPerBordEnAfstand(GameTestHelper helper) {
        veld(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        VerblijfsbordBlockEntity be = bord(helper, new BlockPos(24, GROND + 1, 24), "capibara");
        BlockPos ver = helper.absolutePos(new BlockPos(24, GROND, 24)).offset(60, 0, 0);
        helper.assertFalse(Bouwopdracht.start(p, level, be, "vijvertje", List.of(ver)), "60 blocks away: no");
        helper.assertTrue("te_ver".equals(Bouwopdracht.laatsteNee), "because it is too far: " + Bouwopdracht.laatsteNee);
        helper.assertTrue(Bouwopdracht.start(p, level, be, "vijvertje", List.of(helper.absolutePos(new BlockPos(30, GROND, 30)))), "a pond nearby: yes");
        Spook s = Bouwopdracht.spook(level, be);
        helper.assertTrue(s != null && helper.absolutePos(new BlockPos(24, GROND + 1, 24)).equals(s.bouwerPos()), "a ghost, built by the board");
        helper.assertFalse(Bouwopdracht.start(p, level, be, "badje", List.of(helper.absolutePos(new BlockPos(16, GROND, 16)))), "a second job: no");
        Bouwopdracht.stop(level, be);
        helper.assertTrue(Spookbouw.get(level, s.id()) == null && !Bouwopdracht.bezig(level, be), "stopped: the ghost is gone");
        p.discard();
        helper.succeed();
    }

    /** "Bouw!": the family goes on its work outing to the site; stopped: home again. */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 80)
    public static void bouwopdrachtWerkuitje(GameTestHelper helper) {
        veld(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        VerblijfsbordBlockEntity be = bord(helper, new BlockPos(10, GROND + 1, 10), "pinguin");
        helper.assertTrue(Bouwopdracht.start(p, level, be, "iglo", List.of(helper.absolutePos(new BlockPos(30, GROND, 30)))), "an igloo ghost");
        Spook s = Bouwopdracht.spook(level, be);
        helper.assertTrue(Bouwopdracht.tempo(level, be, s) == 0, "no building before the family is there");
        Spookbouw.bouw(p, s, true);
        helper.runAfterDelay(25, () -> {
            List<ThuisDier> f = be.dieren(level);
            helper.assertTrue(!f.isEmpty() && f.stream().allMatch(d -> d.werkplek() != null), "the family is on its work outing");
            Bouwopdracht.stop(level, be);
            helper.assertTrue(be.dieren(level).stream().allMatch(d -> d.werkplek() == null), "stopped: they walk home");
            p.discard();
            helper.succeed();
        });
    }

    /** Several helpers on flat ground: igloo, pond, zebra crossing, burrows, planting, ice sculpture, mud pool, hot tub. */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 20)
    public static void bouwopdrachtSjablonen(GameTestHelper helper) {
        veld(helper);
        BlockPos bord = new BlockPos(24, GROND + 1, 24);
        BlockPos plek = helper.absolutePos(new BlockPos(24, GROND, 24));
        Map<BlockPos, BlockState> iglo = BouwWerk.iglo(klus(helper, bord, "pinguin", "iglo", 0), plek);
        helper.assertTrue(tel(iglo, s -> s.is(Blocks.SNOW_BLOCK)) > 20 && tel(iglo, s -> s.is(Blocks.ICE)) >= 1, "an igloo of snow with an ice window");
        Map<BlockPos, BlockState> vijver = GraafWerk.vijver(klus(helper, bord, "das", "vijver", 0), plek, "vijver");
        helper.assertTrue(tel(vijver, s -> s.is(Blocks.WATER)) > 10, "a pond of water");
        Map<BlockPos, BlockState> pad = TerreinWerk.zebrapad(klus(helper, bord, "zebra", "zebrapad", 1), plek, plek.offset(16, 0, 0));
        helper.assertTrue(tel(pad, s -> s.is(Blocks.WHITE_CONCRETE)) > 3 && tel(pad, s -> s.is(Blocks.BLACK_CONCRETE)) > 3, "a striped zebra crossing");
        Map<BlockPos, BlockState> hol = GraafWerk.holletjesnet(klus(helper, bord, "stokstaartje", "holletjesnet", 0), plek);
        helper.assertTrue(tel(hol, BlockState::isAir) > 20, "burrows dug out");
        Map<BlockPos, BlockState> plant = TerreinWerk.planten(klus(helper, bord, "koala", "planten", 2), plek);
        helper.assertTrue(tel(plant, s -> s.is(BlockTags.FLOWERS) || s.is(BlockTags.SMALL_FLOWERS)) > 5, "flowers planted");
        Map<BlockPos, BlockState> ijs = BouwWerk.ijssculptuur(klus(helper, bord, "pinguin", "ijssculptuur", 0), plek);
        helper.assertTrue(tel(ijs, s -> s.is(Blocks.PACKED_ICE)) > 5 && tel(ijs, s -> s.is(Blocks.BLUE_ICE)) == 2, "an ice guh with two ears");
        Map<BlockPos, BlockState> modder = GraafWerk.vijver(klus(helper, bord, "olifant", "modderpoel", 0), plek, "modderpoel");
        helper.assertTrue(tel(modder, s -> s.is(Blocks.MUD)) > 5, "a mud pool");
        Map<BlockPos, BlockState> tub = GraafWerk.hottub(klus(helper, bord, "capibara", "hottub", 0), plek);
        helper.assertTrue(tel(tub, s -> s.is(Blocks.WATER)) > 10 && tel(tub, s -> s.is(Blocks.SMOOTH_STONE_SLAB)) > 5, "a hot tub with a rim");
        helper.succeed();
    }

    /** "Bouw passend verblijf": what makes the lion happy, a fence round its home, and the fence keeps the rule near a rock. */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 20)
    public static void bouwopdrachtPassendVerblijfHekregel(GameTestHelper helper) {
        veld(helper);
        BlockPos plek = new BlockPos(24, GROND + 1, 24);
        // a rock of 2 just inside the home's edge (the home has radius 8)
        helper.setBlock(new BlockPos(24 + 8, GROND + 1, 24), Blocks.STONE.defaultBlockState());
        helper.setBlock(new BlockPos(24 + 8, GROND + 2, 24), Blocks.STONE.defaultBlockState());
        VerblijfsbordBlockEntity be = bord(helper, plek, "leeuw");
        Klus k = Bouwopdracht.klus(helper.getLevel(), be, "verblijf");
        Map<BlockPos, BlockState> m = PassendVerblijf.maak(k, be);
        helper.assertTrue(tel(m, s -> s.is(BlockTags.FENCES)) > 30, "a fence round the home (" + tel(m, s -> s.is(BlockTags.FENCES)) + ")");
        helper.assertTrue(tel(m, s -> s.is(BlockTags.SAND)) >= 12, "sand for the lion");
        helper.assertTrue(tel(m, s -> s.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) == 1, "one gate");
        helper.assertTrue(PassendVerblijf.regelKlopt(k, m), "every fence keeps the escape-proof rule");
        BlockPos abs = helper.absolutePos(new BlockPos(24 + 9, GROND + 3, 24));
        long hoog = m.entrySet().stream().filter(e -> e.getKey().getX() == abs.getX() + 0 && e.getKey().getZ() == abs.getZ() && e.getValue().is(BlockTags.FENCES)).count();
        helper.assertTrue(hoog >= 3, "next to the rock the fence is higher (" + hoog + ")");
        helper.succeed();
    }

    /** Every task of verblijf.json has a template, and every text (NL and EN) is there. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 20)
    public static void bouwopdrachtTakenEnTeksten(GameTestHelper helper) {
        JsonObject nl = lang("nl_nl"), en = lang("en_us");
        StringBuilder mist = new StringBuilder();
        for (VerblijfData.Dier d : VerblijfData.alle().values()) {
            for (String t : d.taken()) {
                if (!Sjablonen.bestaat(t)) {
                    mist.append(" template:").append(t);
                }
            }
        }
        for (String t : Sjablonen.taken()) {
            for (String key : List.of("gui.guhs.verblijf.taak." + t, "gui.guhs.bouwopdracht.uitleg." + t)) {
                if (!nl.has(key) || !en.has(key)) {
                    mist.append(' ').append(key);
                }
            }
            for (int v = 0; v < Sjablonen.varianten(t) && Sjablonen.varianten(t) > 1; v++) {
                String key = "gui.guhs.bouwopdracht.variant." + t + "." + v;
                if (!nl.has(key) || !en.has(key)) {
                    mist.append(' ').append(key);
                }
            }
        }
        for (String n : List.of("geen_water", "al_bezig", "te_ver", "andere_helper", "niets_te_doen", "wijs_aan", "geen_keet", "twee_kisten")) {
            if (!nl.has("gui.guhs.bouwopdracht.nee." + n) || !en.has("gui.guhs.bouwopdracht.nee." + n)) {
                mist.append(" nee.").append(n);
            }
        }
        helper.assertTrue(mist.isEmpty(), "missing:" + mist);
        helper.succeed();
    }

    private static JsonObject lang(String taal) {
        try (InputStream in = BouwopdrachtGameTests.class.getResourceAsStream("/assets/guhs/lang/" + taal + ".json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
