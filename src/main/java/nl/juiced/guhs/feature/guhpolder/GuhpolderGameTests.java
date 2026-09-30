package nl.juiced.guhs.feature.guhpolder;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.bakkerij.Bakken;
import nl.juiced.guhs.feature.bakkerij.BakkerijFeature;
import nl.juiced.guhs.feature.bakkerij.Recept;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the Guhpolder (2.9): its blocks (polderijs never melts, rijpgras gets snowy, the ijsbloempje makes dye),
 * the guh-molentje (grinds knabbelgraan into knabbelmeel, by hand and by hopper, faster in bad weather), the knabbeloven
 * with knabbelmeel (twice as much), the knotwilg, the Pinguh (born in the polder, its looks, its belly-slide on ice) and
 * the meeglijden API for the Elf-Guhjestocht, and the polder's share of the Guhmension with room for the tour (worked
 * out from the dimension's own biome source and noise, so it runs on the GameTest server).
 */
public class GuhpolderGameTests {
    /** 16 x 12 x 16: rijpgras at x < 8, polderijs at x >= 8 (the floor at relative y 1: the template sits on its structure block). */
    private static final String IJSBAAN = "guhpolder_test_ijsbaan";
    private static final String EMPTY = "empty";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("guhs");

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            PinguhMeeglijden.stop(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static GuhEntity guh(GameTestHelper helper, BlockPos at, GuhVariant variant) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.getPersistentData().putBoolean(Pinguh.CHECKED, true);
        guh.setVariant(variant);
        guh.setGuhScale(1.0f);
        return guh;
    }

    // =================================================================================================================
    // blocks
    // =================================================================================================================

    @GuhTest(template = IJSBAAN)
    public static void guhpolderBlokken(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockState ijs = GuhpolderFeature.POLDERIJS.get().defaultBlockState();
        helper.assertTrue(!ijs.isRandomlyTicking() && ijs.getBlock().getFriction() >= 0.97f, "polderijs never melts and is slippery");
        helper.assertTrue(ijs.is(GuhpolderFeature.GLIJIJS) && Blocks.ICE.defaultBlockState().is(GuhpolderFeature.GLIJIJS), "glijijs tag");
        helper.assertTrue(ijs.is(BlockTags.CANNOT_SUPPORT_SNOW_LAYER), "no snow settles on the canal");
        // polderijs next to a campfire, in the light: still ice after a random tick storm
        BlockPos p = new BlockPos(10, 1, 4);
        helper.setBlock(p.above(), Blocks.CAMPFIRE);
        for (int i = 0; i < 50; i++) {
            helper.getBlockState(p).randomTick(level, helper.absolutePos(p), level.getRandom());
        }
        helper.assertBlockPresent(GuhpolderFeature.POLDERIJS.get(), p);
        // rijpgras shows its snowy sides under snow
        BlockPos g = new BlockPos(3, 1, 3);
        helper.setBlock(g.above(), Blocks.SNOW);
        helper.assertTrue(helper.getBlockState(g).getValue(SnowyBlock.SNOWY), "rijpgras is snowy under snow");
        helper.setBlock(g.above(), Blocks.AIR);
        helper.assertTrue(!helper.getBlockState(g).getValue(SnowyBlock.SNOWY), "and not without");
        // plants: the ijsbloempje and the sprietjes grow on rijpgras, the crystal glows
        helper.setBlock(new BlockPos(2, 2, 2), GuhpolderFeature.GUH_IJSBLOEMPJE.get());
        helper.setBlock(new BlockPos(4, 2, 2), GuhpolderFeature.RIJPSPRIETJES.get());
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 2)).canSurvive(level, helper.absolutePos(new BlockPos(2, 2, 2)))
                && helper.getBlockState(new BlockPos(4, 2, 2)).canSurvive(level, helper.absolutePos(new BlockPos(4, 2, 2))), "polder plants on rijpgras");
        helper.assertTrue(GuhpolderFeature.IJSPEGELGUH_KRISTAL.get().defaultBlockState().getLightEmission() >= 10, "the crystal glows");
        helper.assertTrue(new ItemStack(GuhpolderFeature.KNABBELMEEL.get()).is(GuhpolderFeature.KNABBELMEEL_TAG)
                && new ItemStack(GuhpolderFeature.KNABBELMEEL.get()).is(Bakken.KNABBELMEEL), "#guhs:knus/knabbelmeel");
        var recipe = level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, Guhs.id("guh_ijsbloempje_kleurstof")));
        helper.assertTrue(recipe.isPresent() && recipe.get().value() instanceof net.minecraft.world.item.crafting.CraftingRecipe craft
                && craft.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY).is(Items.LIGHT_BLUE_DYE),
                "the ijsbloempje gives light blue dye");
        helper.assertTrue(level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, Guhs.id("guh_molentje"))).isPresent(), "the molentje can be crafted");
        // the knabbelkelder may lie under the polder; the biome: guhs yes, Mika's never
        var biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getValue(GuhpolderFeature.GUHPOLDER);
        helper.assertTrue(biome != null && biome.hasPrecipitation() && biome.coldEnoughToSnow(helper.absolutePos(g), level.getSeaLevel()), "a snowy biome");
        var creatures = biome.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap();
        helper.assertTrue(creatures.stream().allMatch(s -> s.value().type() == ModEntities.GUH.get()) && !creatures.isEmpty(), "only guhs spawn");
        for (var cat : net.minecraft.world.entity.MobCategory.values()) {
            for (var weighted : biome.getMobSettings().getMobs(cat).unwrap()) {
                MobSpawnSettings.SpawnerData s = weighted.value();
                helper.assertTrue(!s.type().builtInRegistryHolder().key().identifier().getPath().contains("mika"), "no Mika's in the polder");
            }
        }
        var kelder = level.registryAccess().lookupOrThrow(Registries.BIOME).get(
                net.minecraft.tags.TagKey.create(Registries.BIOME, Guhs.id("has_structure/knabbelkelder")));
        helper.assertTrue(kelder.isPresent() && kelder.get().stream().anyMatch(h -> h.is(GuhpolderFeature.GUHPOLDER)), "knabbelkelder tag");
        helper.succeed();
    }

    /** A knotwilg from the worldgen: trunk, knobs, twigs that don't fall off, snow caps on top. */
    @GuhTest(template = IJSBAAN)
    public static void guhpolderKnotwilg(GameTestHelper helper) {
        BlockPos grond = new BlockPos(3, 1, 8);
        boolean ok = GuhpolderWorldgen.boom(helper.getLevel(), RandomSource.create(7), helper.absolutePos(grond));
        if (!ok) {
            StringBuilder why = new StringBuilder("ground " + helper.getBlockState(grond) + " / " + GuhpolderWorldgen.grond(helper.getBlockState(grond)));
            for (BlockPos q : BlockPos.betweenClosed(grond.offset(-2, 1, -2), grond.offset(2, 8, 2))) {
                if (!GuhpolderWorldgen.vrij(helper.getBlockState(q))) {
                    why.append("; blocked at ").append(q).append(" by ").append(helper.getBlockState(q));
                    break;
                }
            }
            helper.fail("a knotwilg grows: " + why);
        }
        helper.assertBlockPresent(GuhpolderFeature.KNOTWILG_STAM.get(), grond.above());
        int blad = 0, sneeuw = 0;
        for (BlockPos p : BlockPos.betweenClosed(grond.offset(-3, 1, -3), grond.offset(3, 9, 3))) {
            BlockState s = helper.getBlockState(p);
            if (s.is(GuhpolderFeature.KNOTWILG_BLADEREN.get())) {
                blad++;
                helper.assertTrue(s.getValue(LeavesBlock.DISTANCE) < LeavesBlock.DECAY_DISTANCE, "twigs close to the wood: " + p);
                if (s.getValue(GuhpolderBlocks.SNEEUW)) {
                    sneeuw++;
                }
            }
        }
        helper.assertTrue(blad >= 6 && sneeuw >= 2, "twigs " + blad + ", snow caps " + sneeuw);
        GuhpolderWorldgen.Sneeuwguhheuvel.sneeuwpop(helper.getLevel(), helper.absolutePos(new BlockPos(12, 2, 12)), Direction.SOUTH);
        helper.assertBlockPresent(nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature.SNEEUWPOPGUH.get(), new BlockPos(12, 3, 12));
        helper.succeed();
    }

    // =================================================================================================================
    // the guh-molentje and knabbelmeel
    // =================================================================================================================

    @GuhTest(template = IJSBAAN, timeoutTicks = 400)
    public static void guhpolderMolentjeMaalt(GameTestHelper helper) {
        BlockPos m = new BlockPos(3, 2, 3);
        helper.setBlock(m, GuhpolderFeature.GUH_MOLENTJE.get());
        MolentjeBlockEntity molen = helper.getBlockEntity(m, MolentjeBlockEntity.class);
        helper.assertTrue(MolentjeBlockEntity.maalTijd(2) < MolentjeBlockEntity.maalTijd(1) && MolentjeBlockEntity.maalTijd(1) < MolentjeBlockEntity.maalTijd(0),
                "faster in snow, fastest in a storm");
        ServerPlayer p = player(helper, new BlockPos(3, 2, 5));
        ItemStack wheat = new ItemStack(Items.WHEAT, 3);
        helper.assertTrue(molen.stort(wheat) == 0 && wheat.getCount() == 3, "wheat isn't knabbelgraan");
        // right-click with knabbelgraan pours it in
        ItemStack graan = new ItemStack(nl.juiced.guhs.feature.tuintjes.TuintjesFeature.KNABBELGRAAN.get(), 5);
        helper.assertTrue(graan.is(KnusTags.KNABBELGRAAN), "knabbelgraan");
        p.setItemInHand(InteractionHand.MAIN_HAND, graan);
        helper.useBlock(m, p);
        helper.assertTrue(molen.graan().getCount() == 5 && p.getMainHandItem().isEmpty(), "5 knabbelgraan in the molentje: " + molen.graan());
        // hoppers: in from the side, not from below; meel out
        var side = molen.handler(Direction.NORTH);
        var below = molen.handler(Direction.DOWN);
        ItemStack more = new ItemStack(nl.juiced.guhs.feature.tuintjes.TuintjesFeature.KNABBELGRAAN.get(), 2);
        helper.assertTrue(insert(below, more) == 0 && insert(side, more) == 2, "hopper in from the side");
        helper.assertTrue(molen.graan().getCount() == 7, "7 graan now");
        long start = helper.getLevel().getGameTime();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(molen.meel().getCount() >= 1, "grinding..."))
                .thenExecute(() -> {
                    long took = helper.getLevel().getGameTime() - start;
                    helper.assertTrue(took <= MolentjeBlockEntity.maalTijd(0) + 5, "one meel in " + took + " ticks");
                    helper.assertTrue(molen.graan().getCount() == 6, "one graan used");
                    molen.maal(helper.getLevel());
                    molen.maal(helper.getLevel());
                    helper.assertTrue(extract(below, 1, GuhpolderFeature.KNABBELMEEL.get()) == 1, "a hopper below takes meel out");
                    helper.assertTrue(extract(below, 0, nl.juiced.guhs.feature.tuintjes.TuintjesFeature.KNABBELGRAAN.get()) == 0, "but never the graan");
                    // right-click with an empty hand: the meel
                    p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    helper.useBlock(m, p);
                    helper.assertTrue(count(p, GuhpolderFeature.KNABBELMEEL.get()) == 2 && molen.meel().isEmpty(), "2 knabbelmeel taken out");
                    // breaking it drops the graan
                    helper.destroyBlock(m);
                    helper.assertItemEntityPresent(nl.juiced.guhs.feature.tuintjes.TuintjesFeature.KNABBELGRAAN.get(), m, 2.0);
                    leave(helper, p);
                })
                .thenSucceed();
    }

    /** The knabbeloven takes knabbelmeel instead of knabbelgraan and bakes twice as much. */
    @GuhTest(template = IJSBAAN, timeoutTicks = 300)
    public static void guhpolderKnabbelovenMetMeel(GameTestHelper helper) {
        BlockPos o = new BlockPos(3, 2, 3);
        helper.setBlock(o, BakkerijFeature.KNABBELOVEN.get());
        BlockPos oven = helper.absolutePos(o);
        ServerPlayer p = player(helper, new BlockPos(3, 2, 5));
        p.getInventory().add(new ItemStack(GuhpolderFeature.KNABBELMEEL.get(), 1));
        p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 1));
        p.getInventory().add(new ItemStack(Items.SUGAR, 1));
        p.getInventory().add(new ItemStack(Items.WHEAT, 1));
        helper.assertTrue(Bakken.start(p, oven, Recept.Deeg.KNABBELDEEG, Recept.Vorm.PLAATJE, Recept.Topping.SUIKER) == null, "it bakes");
        helper.assertTrue(count(p, GuhpolderFeature.KNABBELMEEL.get()) == 0 && count(p, Items.WHEAT) == 1, "the knabbelmeel went in first (the wheat stays)");
        long start = helper.getLevel().getGameTime();
        int perfect = Recept.KNABBELKOEKJE.bakTicks * 76 / 100;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.getLevel().getGameTime() - start >= perfect, "baking..."))
                .thenExecute(() -> {
                    Bakken.Uit uit = Bakken.eruit(p, perfect);
                    int verwacht = (Bakken.PERFECT_AANTAL + 1) * Bakken.MEEL_KEER;   // (+1: meel is a fresh Knus ingredient, then x2)
                    helper.assertTrue(uit != null && uit.kwaliteit() == Recept.Kwaliteit.PERFECT && uit.aantal() == verwacht,
                            "twice as many with knabbelmeel: " + uit);
                    helper.assertTrue(count(p, BakkerijFeature.bakje(Recept.KNABBELKOEKJE)) == verwacht, "in the pockets");
                    leave(helper, p);
                })
                .thenSucceed();
    }

    // =================================================================================================================
    // the Pinguh
    // =================================================================================================================

    @GuhTest(template = IJSBAAN)
    public static void guhpolderPinguhGeboren(GameTestHelper helper) {
        int pinguhs = 0, n = 120;
        for (int i = 0; i < n; i++) {
            GuhEntity g = helper.spawn(ModEntities.GUH.get(), new BlockPos(2 + i % 4, 2, 2 + (i / 4) % 10));
            g.getPersistentData().remove(Pinguh.CHECKED);
            g.setVariant(GuhVariant.NORMAL);
            if (Pinguh.decide(g, true)) {
                pinguhs++;
                helper.assertTrue(g.getVariant() == GuhVariant.PINGUH, "it is a Pinguh now");
            }
            helper.assertTrue(!Pinguh.decide(g, true), "only decided once");
            g.discard();
        }
        helper.assertTrue(pinguhs >= n * 0.3 && pinguhs <= n * 0.7, "about half of them: " + pinguhs + " of " + n);
        GuhEntity buiten = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 2, 3));
        buiten.getPersistentData().remove(Pinguh.CHECKED);
        buiten.setVariant(GuhVariant.NORMAL);
        helper.assertTrue(!Pinguh.decide(buiten, false) && buiten.getVariant() == GuhVariant.NORMAL, "not outside the polder");
        GuhEntity mint = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 2, 3));
        mint.getPersistentData().remove(Pinguh.CHECKED);
        mint.setVariant(GuhVariant.MINT);
        mint.addTag(Pinguh.GEEN_PINGUH);
        for (int i = 0; i < 20; i++) {
            mint.getPersistentData().remove(Pinguh.CHECKED);
            Pinguh.decide(mint, true);
        }
        helper.assertTrue(mint.getVariant() == GuhVariant.MINT, "a mint guh stays mint");
        GuhEntity publiek = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 2, 3));
        publiek.setVariant(GuhVariant.NORMAL);
        publiek.addTag(Pinguh.GEEN_PINGUH);
        for (int i = 0; i < 20; i++) {
            publiek.getPersistentData().remove(Pinguh.CHECKED);
            Pinguh.decide(publiek, true);
        }
        helper.assertTrue(publiek.getVariant() == GuhVariant.NORMAL, "a guh tagged " + Pinguh.GEEN_PINGUH + " stays a guh");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void guhpolderPinguhLooks(GameTestHelper helper) {
        Map<Pinguh.Look, Integer> n = new HashMap<>();
        RandomSource r = RandomSource.create(29);
        int total = 4000;
        for (int i = 0; i < total; i++) {
            UUID id = new UUID(r.nextLong(), r.nextLong());
            Pinguh.Look look = Pinguh.look(id, false);
            helper.assertTrue(look == Pinguh.look(id, false), "the same look every time");
            n.merge(look, 1, Integer::sum);
            helper.assertTrue(Pinguh.look(id, true) == Pinguh.Look.PLUIS, "every chick is fluffy");
        }
        double pluis = n.getOrDefault(Pinguh.Look.PLUIS, 0) / (double) total, keizer = n.getOrDefault(Pinguh.Look.KEIZER, 0) / (double) total,
                klassiek = n.getOrDefault(Pinguh.Look.KLASSIEK, 0) / (double) total;
        helper.assertTrue(pluis > 0.04 && pluis < 0.12 && Math.abs(keizer - klassiek) < 0.06, "looks: " + n);
        // a keizer grown up is a bit bigger (once)
        GuhEntity keizerGuh = null;
        for (int i = 0; i < 60 && keizerGuh == null; i++) {
            GuhEntity g = guh(helper, new BlockPos(1, 2, 1), GuhVariant.PINGUH);
            if (Pinguh.look(g) == Pinguh.Look.KEIZER) {
                keizerGuh = g;
            } else {
                g.discard();
            }
        }
        helper.assertTrue(keizerGuh != null, "found a keizer");
        Pinguh.groei(keizerGuh);
        Pinguh.groei(keizerGuh);
        helper.assertTrue(Math.abs(keizerGuh.getGuhScale() - Pinguh.KEIZER_GROEI) < 0.01, "a keizer is bigger: " + keizerGuh.getGuhScale());
        helper.succeed();
    }

    /** On the ice a Pinguh belly-slides faster; on the rijpgras it waddles at its normal speed. */
    @GuhTest(template = IJSBAAN, timeoutTicks = 200)
    public static void guhpolderPinguhGlijdt(GameTestHelper helper) {
        GuhEntity opIJs = guh(helper, new BlockPos(12, 2, 8), GuhVariant.PINGUH);
        GuhEntity opGras = guh(helper, new BlockPos(3, 2, 8), GuhVariant.PINGUH);
        Identifier id = Pinguh.GLIJ_ID;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(opIJs.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(id), "sliding on the ice..."))
                .thenExecute(() -> {
                    helper.assertTrue(Pinguh.opIJs(opIJs) && !Pinguh.opIJs(opGras), "ice or not");
                    helper.assertTrue(!opGras.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(id), "no slide on the grass");
                })
                .thenSucceed();
    }

    /** The API for the Elf-Guhjestocht: a tamed Pinguh slides along next to a skater, and stops when told. */
    @GuhTest(template = IJSBAAN, timeoutTicks = 300)
    public static void guhpolderPinguhGlijdtMee(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(9, 2, 2));
        GuhEntity pinguh = guh(helper, new BlockPos(13, 2, 13), GuhVariant.PINGUH);
        GuhEntity zit = guh(helper, new BlockPos(14, 2, 13), GuhVariant.PINGUH);
        GuhEntity gewoon = guh(helper, new BlockPos(12, 2, 13), GuhVariant.NORMAL);
        GuhEntity wild = guh(helper, new BlockPos(11, 2, 13), GuhVariant.PINGUH);
        for (GuhEntity g : new GuhEntity[]{pinguh, zit, gewoon}) {
            g.tame(p);
        }
        zit.setOrderedToSit(true);
        var mee = PinguhMeeglijden.start(p);
        helper.assertTrue(mee.size() == 1 && mee.get(0) == pinguh && PinguhMeeglijden.glijdtMee(pinguh), "only the tamed, standing Pinguh: " + mee);
        helper.assertTrue(!PinguhMeeglijden.glijdtMee(zit) && !PinguhMeeglijden.glijdtMee(gewoon) && !PinguhMeeglijden.glijdtMee(wild), "not the others");
        helper.assertTrue(PinguhMeeglijden.met(pinguh, p), "with this skater");
        // the skater stands still on the ice, facing south: the Pinguh comes to its place beside them
        p.setYRot(0);
        Vec3 plek = PinguhMeeglijden.plek(p.position(), Vec3.directionFromRotation(0, 0), 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pinguh.position().distanceTo(new Vec3(plek.x, pinguh.getY(), plek.z)) < 1.5,
                        "coming alongside: " + pinguh.position() + " -> " + plek))
                .thenExecute(() -> {
                    PinguhMeeglijden.stop(p);
                    helper.assertTrue(!PinguhMeeglijden.glijdtMee(pinguh) && PinguhMeeglijden.meeglijders(p).isEmpty(), "stopped");
                    helper.assertTrue(PinguhMeeglijden.plek(Vec3.ZERO, new Vec3(0, 0, 1), 0).x < 0 != PinguhMeeglijden.plek(Vec3.ZERO, new Vec3(0, 0, 1), 1).x < 0,
                            "one on each side");
                    leave(helper, p);
                })
                .thenSucceed();
    }

    // =================================================================================================================
    // worldgen: the polder's share of the Guhmension, big flat patches for the Elf-Guhjestocht
    // =================================================================================================================

    // (the numbers of tools/features/guhpolder_wereld.py: keep them the same)
    static final double TERM_FROM = 0.61, TERM_TO = 0.63, DEPTH_SHIFT = 1.0, WEIRD_SHIFT = 1.2, PEAK_MIN = 0.66, TOCHT_RING = 0.63;
    static final double[] SEA_OFF = {0.28, 0.30}, KNUFFEL_OFF = {0.40, 0.46};
    static final int TOCHT_RADIUS = 136;

    /** A Minecraft spline between two points with derivative 0 (smoothstep), clamped outside. */
    static double spline(double v, double a, double b, double va, double vb) {
        if (v <= a) {
            return va;
        }
        if (v >= b) {
            return vb;
        }
        double u = (v - a) / (b - a);
        return va + (vb - va) * u * u * (3 - 2 * u);
    }

    /**
     * Samples the Guhmension's surface biomes (the dimension JSON and its noise settings, three seeds, 12800 x 12800 blocks
     * each). For every sample the polder's shift of the multi-noise point is undone (the term worked out from the three
     * noises), which gives the biome there would have been without the polder: outside the polder it must be the very same
     * biome (the polder changes nothing else), and the polder is ~2-3 % of the surface. Then the polders (connected
     * samples): how wide they are, and on how many the peak of the polder noise is high enough, with the noise dead flat on a
     * ring of 136 blocks, for the Elf-Guhjestocht.
     */
    @GuhTest(template = EMPTY, timeoutTicks = 6000)
    public static void guhpolderDeelEnRuimte(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = server.registryAccess();
        JsonObject source;
        try (var reader = server.getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            source = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        JsonObject without = source.deepCopy();
        JsonArray kept = new JsonArray();
        for (JsonElement e : without.getAsJsonArray("biomes")) {
            if (!e.getAsJsonObject().get("biome").getAsString().equals("guhs:guhpolder")) {
                kept.add(e);
            }
        }
        without.add("biomes", kept);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, access);
        BiomeSource with = BiomeSource.CODEC.parse(ops, source).getOrThrow();
        net.minecraft.world.level.biome.MultiNoiseBiomeSource before =
                (net.minecraft.world.level.biome.MultiNoiseBiomeSource) BiomeSource.CODEC.parse(ops, without).getOrThrow();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        var zeeKey = net.minecraft.resources.ResourceKey.create(Registries.NOISE, Guhs.id("guhmension_zee"));
        var knuffelKey = net.minecraft.resources.ResourceKey.create(Registries.NOISE, Guhs.id("guhmension_knuffel"));
        Map<String, Integer> now = new HashMap<>(), then = new HashMap<>();
        final int step = 32, half = 6400, n = 2 * half / step;
        double[] drempels = {0.58, 0.60, 0.62, 0.64, 0.66, 0.70, 0.75, 0.80, 0.90};
        int[] boven = new int[drempels.length];
        int samples = 0, polders = 0, groot = 0, metTocht = 0, kruimels = 0, breedste = 0, anders = 0;
        StringBuilder report = new StringBuilder();
        for (long seed : new long[]{1L, 20290601L, -778899L}) {
            RandomState random = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed);
            Climate.Sampler sampler = random.sampler();
            var noise = random.getOrCreateNoise(GuhpolderFeature.POLDER_NOISE);
            var zee = random.getOrCreateNoise(zeeKey);
            var knuffel = random.getOrCreateNoise(knuffelKey);
            boolean[][] polder = new boolean[n][n];
            double[][] val = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    int x = -half + i * step, z = -half + j * step;
                    int qx = QuartPos.fromBlock(x), qy = QuartPos.fromBlock(100), qz = QuartPos.fromBlock(z);
                    String b = with.getNoiseBiome(qx, qy, qz, sampler).unwrapKey().orElseThrow().identifier().getPath();
                    double v = noise.getValue(x, 0, z);
                    double buiten = spline(zee.getValue(x, 0, z), SEA_OFF[0], SEA_OFF[1], 1, 0)
                            * spline(knuffel.getValue(x, 0, z), KNUFFEL_OFF[0], KNUFFEL_OFF[1], 1, 0);
                    double t = spline(v, TERM_FROM, TERM_TO, 0, 1) * buiten;
                    Climate.TargetPoint tp = sampler.sample(qx, qy, qz);
                    Climate.TargetPoint undone = new Climate.TargetPoint(tp.temperature(), tp.humidity(), tp.continentalness(), tp.erosion(),
                            tp.depth() + Climate.quantizeCoord((float) (t * DEPTH_SHIFT)), tp.weirdness() + Climate.quantizeCoord((float) (t * WEIRD_SHIFT)));
                    String was = before.getNoiseBiome(undone).unwrapKey().orElseThrow().identifier().getPath();
                    now.merge(b, 1, Integer::sum);
                    then.merge(was, 1, Integer::sum);
                    if (!b.equals("guhpolder") && !b.equals(was)) {
                        anders++;
                    }
                    polder[i][j] = b.equals("guhpolder");
                    val[i][j] = v;
                    if (buiten >= 0.999) {
                        for (int k = 0; k < drempels.length; k++) {
                            if (v >= drempels[k]) {
                                boven[k]++;
                            }
                        }
                    }
                    samples++;
                }
            }
            int[][] label = new int[n][n];
            int labels = 0, seedTocht = 0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (!polder[i][j] || label[i][j] != 0) {
                        continue;
                    }
                    labels++;
                    int area = 0, minI = i, maxI = i, minJ = j, maxJ = j, pi = i, pj = j;
                    ArrayDeque<int[]> todo = new ArrayDeque<>();
                    todo.add(new int[]{i, j});
                    label[i][j] = labels;
                    while (!todo.isEmpty()) {
                        int[] c = todo.poll();
                        area++;
                        minI = Math.min(minI, c[0]);
                        maxI = Math.max(maxI, c[0]);
                        minJ = Math.min(minJ, c[1]);
                        maxJ = Math.max(maxJ, c[1]);
                        if (val[c[0]][c[1]] > val[pi][pj]) {
                            pi = c[0];
                            pj = c[1];
                        }
                        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                            int a = c[0] + d[0], bb = c[1] + d[1];
                            if (a >= 0 && bb >= 0 && a < n && bb < n && polder[a][bb] && label[a][bb] == 0) {
                                label[a][bb] = labels;
                                todo.add(new int[]{a, bb});
                            }
                        }
                    }
                    polders++;
                    breedste = Math.max(breedste, Math.min(maxI - minI + 1, maxJ - minJ + 1) * step);
                    if (area <= 3) {
                        kruimels++;
                        continue;
                    }
                    if (area * step * step < 60000) {
                        continue;
                    }
                    groot++;
                    boolean past = val[pi][pj] >= PEAK_MIN;
                    int r = TOCHT_RADIUS / step + 1;
                    for (int a = -r; a <= r && past; a++) {
                        for (int bb = -r; bb <= r && past; bb++) {
                            if (a * a + bb * bb > r * r) {
                                continue;
                            }
                            int ii = pi + a, jj = pj + bb;
                            past = ii >= 0 && jj >= 0 && ii < n && jj < n && polder[ii][jj] && val[ii][jj] >= TOCHT_RING;
                        }
                    }
                    if (past) {
                        metTocht++;
                        seedTocht++;
                    }
                }
            }
            report.append(String.format("seed %d: %d polders, %d with room for the tour; ", seed, labels, seedTocht));
        }
        for (String b : new java.util.TreeSet<>(then.keySet())) {
            report.append(String.format("%s %.2f%% -> %.2f%%; ", b, 100.0 * then.get(b) / samples, 100.0 * now.getOrDefault(b, 0) / samples));
        }
        report.append("noise >= ");
        for (int k = 0; k < drempels.length; k++) {
            report.append(String.format("%.2f: %.2f%%, ", drempels[k], 100.0 * boven[k] / samples));
        }
        double share = now.getOrDefault("guhpolder", 0) / (double) samples;
        report.append(String.format("guhpolder %.2f%%, polders %d (crumbs %d), big %d, with room for the tour %d, widest %d blocks, changed elsewhere %d",
                100 * share, polders, kruimels, groot, metTocht, breedste, anders));
        LOGGER.info("Guhpolder: {}", report);
        helper.assertTrue(anders <= samples / 2000, "outside the polder every biome stays the same: " + report);
        helper.assertTrue(share >= 0.015 && share <= 0.04, "the polder's share: " + report);
        for (var e : then.entrySet()) {
            double was = e.getValue() / (double) samples, is = now.getOrDefault(e.getKey(), 0) / (double) samples;
            if (was >= 0.01) {
                helper.assertTrue(is >= 0.8 * was, e.getKey() + " keeps most of its share: " + report);
            }
        }
        helper.assertTrue(metTocht >= 6 && metTocht * 4 >= groot, "big polders with room for the Elf-Guhjestocht: " + report);
        helper.succeed();
    }
    // =================================================================================================================
    // 2.10.1: the weather leaves the polder alone
    // =================================================================================================================

    /**
     * Makes the biome around a test spot `biome` (every quart column within 2 quarts, 8 blocks: level.getBiome blurs between
     * neighbouring quarts); returns the biome it had.
     */
    private static net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome(GameTestHelper helper, BlockPos rel,
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);
        int qx = QuartPos.fromBlock(abs.getX()), qz = QuartPos.fromBlock(abs.getZ());
        var oud = level.getBiome(abs);
        for (int cx = (abs.getX() - 12) >> 4; cx <= (abs.getX() + 12) >> 4; cx++) {
            for (int cz = (abs.getZ() - 12) >> 4; cz <= (abs.getZ() + 12) >> 4; cz++) {
                var chunk = level.getChunk(cx, cz);
                chunk.fillBiomesFromNoise((x, y, z, sampler) -> Math.abs(x - qx) <= 2 && Math.abs(z - qz) <= 2 ? biome : chunk.getNoiseBiome(x, y, z),
                        level.getChunkSource().randomState().sampler());
                chunk.markUnsaved();
            }
        }
        return oud;
    }

    /**
     * No snow piling up and no ice forming from the weather in the Guhpolder (GuhpolderWeer + mixin.ServerLevelMixin), while
     * a vanilla snowy biome on the very same spot still freezes its water and gets a snow layer (vanilla stays vanilla).
     */
    @GuhTest(template = EMPTY)
    public static void guhpolderWeerLaatDePolderMetRust(GameTestHelper helper) {
        var level = helper.getLevel();
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        var polder = biomes.getOrThrow(GuhpolderFeature.GUHPOLDER);
        var sneeuwvlakte = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS);
        var hoogte = net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING;
        BlockPos kolomWater = new BlockPos(1, 1, 1), kolomSteen = new BlockPos(3, 1, 1);
        // (the weather works on the top of a column, and the test area has a roof: water and stone go on top of it)
        BlockPos water = level.getHeightmapPos(hoogte, helper.absolutePos(kolomWater));
        BlockPos steen = level.getHeightmapPos(hoogte, helper.absolutePos(kolomSteen));
        var oud = biome(helper, kolomWater, polder);
        biome(helper, kolomSteen, polder);
        boolean regen = level.getServer().getWeatherData().isRaining();
        float regenNu = level.getRainLevel(1f);
        try {
            level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
            level.setBlockAndUpdate(steen, Blocks.STONE.defaultBlockState());
            helper.assertTrue(level.getBiome(water).is(GuhpolderFeature.GUHPOLDER), "the spot is polder now");
            level.getServer().setWeatherParameters(0, 200, true, false);
            level.setRainLevel(1f);                             // (it snows right now, not after the fade-in)
            helper.assertTrue(level.isRaining(), "it snows");
            for (int i = 0; i < 4; i++) {
                level.tickPrecipitation(water);
                level.tickPrecipitation(steen);
            }
            helper.assertTrue(level.getBlockState(water).is(Blocks.WATER), "polder water doesn't freeze from the weather");
            helper.assertTrue(level.getBlockState(steen.above()).isAir(), "no snow piles up in the polder");
            // the same spot in a vanilla snowy biome: vanilla weather as always
            biome(helper, kolomWater, sneeuwvlakte);
            biome(helper, kolomSteen, sneeuwvlakte);
            level.tickPrecipitation(water);
            level.tickPrecipitation(steen);
            helper.assertTrue(level.getBlockState(water).is(Blocks.ICE), "vanilla snowy plains still freeze: " + level.getBlockState(water));
            helper.assertTrue(level.getBlockState(steen.above()).is(Blocks.SNOW), "and still get snow: " + level.getBlockState(steen.above()));
        } finally {
            level.getServer().setWeatherParameters(regen ? 0 : 6000, regen ? 6000 : 0, regen, false);
            level.setRainLevel(regenNu);
            for (BlockPos p : new BlockPos[]{water, steen, steen.above()}) {
                level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
            biome(helper, kolomWater, oud);
            biome(helper, kolomSteen, oud);
        }
        helper.succeed();
    }

    /** A hopper-style insert into a molentje side (1.1.0: transactional item handlers); returns how many went in. */
    private static int insert(net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> h, ItemStack stack) {
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int n = h.insert(0, net.neoforged.neoforge.transfer.item.ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
            return n;
        }
    }

    /** Takes one item of this kind out of a slot of a molentje side; returns how many came out. */
    private static int extract(net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> h, int slot,
            net.minecraft.world.item.Item item) {
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int n = h.extract(slot, net.neoforged.neoforge.transfer.item.ItemResource.of(item), 1, tx);
            tx.commit();
            return n;
        }
    }
}
