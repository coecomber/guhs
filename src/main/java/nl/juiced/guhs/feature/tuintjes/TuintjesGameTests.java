package nl.juiced.guhs.feature.tuintjes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.KnusSignalen;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the guhtuintjes: plant, grow, harvest (the tuinboek), the gieter, a tamed guh that waters, singing that
 * makes plants grow, the feestboeket for the Knusfeest, the tags. (Template tuintjes_test_tuin: a grass floor at helper y = 1.)
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class TuintjesGameTests {
    private static final String TUIN = "tuintjes_test_tuin";

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos pos) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(pos);
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.server.getAdvancements().get(Guhs.id(name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static BlockPos pot(GameTestHelper helper, BlockPos pos, boolean bak, TuinPlant plant) {
        helper.setBlock(pos, (bak ? TuintjesFeature.GUH_MOESTUINBAK.get() : TuintjesFeature.GUH_BLOEMPOT.get()).defaultBlockState()
                .setValue(TuinBlock.PLANT, plant));
        return helper.absolutePos(pos);
    }

    @GameTest(template = TUIN)
    public static void tuintjesPlantenGroeienEnOogsten(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        BlockPos pos = pot(helper, new BlockPos(3, 2, 3), false, TuinPlant.LEEG);
        var level = helper.getLevel();
        try {
            TuinBlock.plant(level, pos, TuinPlant.KNABBELPLANTJE, p);
            BlockState s = level.getBlockState(pos);
            helper.assertTrue(s.getValue(TuinBlock.PLANT) == TuinPlant.KNABBELPLANTJE && TuinBlock.groeit(s) && TuinBlock.dorstig(s), "planted");
            helper.assertTrue(KnusVoortgang.heeft(p, TuintjesVoortgang.TUINBOEK, "knabbelzaadjes"), "the seeds are in the tuinboek");
            helper.assertTrue(TuinBlock.oogst(level, pos, p) == 0, "not ripe: nothing to harvest");
            for (int i = 0; i < TuinBlock.RIJP; i++) {
                TuinBlock.groei(level, pos);
            }
            helper.assertTrue(TuinBlock.rijp(level.getBlockState(pos)), "ripe after three steps");
            int n = TuinBlock.oogst(level, pos, p);
            helper.assertTrue(n >= 2 && count(p, TuintjesFeature.KNABBELGRAAN.get()) == n, "harvested knabbelgraan: " + n);
            s = level.getBlockState(pos);
            helper.assertTrue(s.getValue(TuinBlock.GROEI) == 0 && s.getValue(TuinBlock.PLANT) == TuinPlant.KNABBELPLANTJE, "it stays and grows again");
            helper.assertTrue(KnusVoortgang.heeft(p, TuintjesVoortgang.TUINBOEK, "knabbelgraan") && KnusVoortgang.teller(p, TuintjesVoortgang.OOGST) == 1,
                    "the harvest in the tuinboek and counted");
            helper.assertTrue(advancement(p, "quest/tuintjes_eerste_oogst") && advancement(p, "knuffeldal/tuintjes_eerste_oogst"), "first harvest");
            // the whole tuinboek
            for (TuinPlant plant : List.of(TuinPlant.THEEKRUID, TuinPlant.GUHBLOEM)) {
                BlockPos q = pot(helper, new BlockPos(plant == TuinPlant.THEEKRUID ? 5 : 7, 2, 3), true, TuinPlant.LEEG);
                TuinBlock.plant(level, q, plant, p);
                for (int i = 0; i < TuinBlock.RIJP; i++) {
                    TuinBlock.groei(level, q);
                }
                TuinBlock.oogst(level, q, p);
            }
            helper.assertTrue(KnusVoortgang.ontdekt(p, TuintjesVoortgang.TUINBOEK).containsAll(TuintjesVoortgang.TUINBOEK_LIJST)
                    && advancement(p, "quest/tuintjes_tuinboek_vol"), "the tuinboek is full");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = TUIN)
    public static void tuintjesGieterGeeftWater(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        var level = helper.getLevel();
        try {
            BlockPos a = pot(helper, new BlockPos(4, 2, 4), true, TuinPlant.THEEKRUID);
            BlockPos b = pot(helper, new BlockPos(5, 2, 4), false, TuinPlant.GUHBLOEM);
            BlockPos leeg = pot(helper, new BlockPos(4, 2, 5), false, TuinPlant.LEEG);
            ItemStack gieter = new ItemStack(TuintjesFeature.GUH_GIETER.get());
            helper.assertTrue(GuhGieterItem.water(gieter) == GuhGieterItem.VOL, "a new gieter is full");
            int n = GuhGieterItem.giet(level, a, gieter);
            helper.assertTrue(n == 2 && level.getBlockState(a).getValue(TuinBlock.GEWATERD) && level.getBlockState(b).getValue(TuinBlock.GEWATERD),
                    "both plants in the 3 x 3 watered: " + n);
            helper.assertTrue(!level.getBlockState(leeg).getValue(TuinBlock.GEWATERD), "an empty pot needs no water");
            helper.assertTrue(GuhGieterItem.water(gieter) == GuhGieterItem.VOL - 2, "two sips used");
            helper.assertTrue(GuhGieterItem.giet(level, a, gieter) == 0, "already wet");
            TuinBlock.groei(level, a);
            helper.assertTrue(!level.getBlockState(a).getValue(TuinBlock.GEWATERD), "after a step it is thirsty again");
            gieter.setDamageValue(GuhGieterItem.VOL);
            helper.assertTrue(GuhGieterItem.giet(level, a, gieter) == 0, "an empty gieter gives no water");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    /** A tamed guh walks to a thirsty plant nearby and waters it (its owner's Knus tab counts it). */
    @GameTest(template = TUIN, timeoutTicks = 500)
    public static void tuintjesTammeGuhGeeftWater(GameTestHelper helper) {
        ServerPlayer owner = player(helper, new BlockPos(2, 2, 9));
        BlockPos pos = pot(helper, new BlockPos(8, 2, 8), true, TuinPlant.GUHBLOEM);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 2, 3));
        guh.setPersistenceRequired();
        guh.tame(owner);
        guh.setOrderedToSit(false);
        guh.setInSittingPose(false);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(TuinBlock.GEWATERD), "the guh watered the plant");
                    helper.assertTrue(KnusVoortgang.teller(owner, TuintjesVoortgang.GUHS_GIETEN) == 1, "counted for its owner");
                })
                .thenIdle(20)                                  // (the goal ends cleanly: it ticks once more after watering)
                .thenExecute(() -> {
                    helper.assertTrue(guh.isAlive(), "the guh is still fine");
                    leave(helper, owner);
                })
                .thenSucceed();
    }

    @GameTest(template = TUIN)
    public static void tuintjesZangLaatPlantjesGroeien(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        var level = helper.getLevel();
        try {
            List<BlockPos> potten = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                potten.add(pot(helper, new BlockPos(3 + i, 2, 5), false, TuinPlant.THEEKRUID));
            }
            BlockPos far = pot(helper, new BlockPos(10, 2, 10), false, TuinPlant.THEEKRUID);
            BlockPos midden = helper.absolutePos(new BlockPos(5, 2, 5));
            for (BlockPos q : potten) {
                TuinBlock.vergeetZang(level, q);                   // (a rerun on the dev server starts fresh)
            }
            TuinBlock.vergeetZang(level, far);
            helper.assertTrue(TuinBlock.KANS_ZANG <= 0.05f && TuinBlock.ZANG_RUST >= 1200, "singing is an extra, it doesn't beat watering");
            KnusSignalen.zang(level, midden, 3);                   // (the real signal: a small chance per plant)
            TuintjesFeature.zang(level, midden, 3, 1f);            // (certain: every plant that didn't grow yet grows a step)
            for (int i = 0; i < 12; i++) {
                KnusSignalen.zang(level, midden, 3);               // (a long song: they rest now, nothing grows more)
                TuintjesFeature.zang(level, midden, 3, 1f);
            }
            for (BlockPos q : potten) {
                helper.assertTrue(level.getBlockState(q).getValue(TuinBlock.GROEI) == 1,
                        "one step per plant from the song, then it rests: " + level.getBlockState(q).getValue(TuinBlock.GROEI));
            }
            helper.assertTrue(level.getBlockState(far).getValue(TuinBlock.GROEI) == 0, "the far one didn't");
            helper.assertTrue(KnusVoortgang.teller(p, TuintjesVoortgang.ZANG) >= 1, "the player nearby gets it on the Knus tab");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = TUIN)
    public static void tuintjesFeestboeketVoorHetKnusfeest(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        try {
            Knusfeest.vergeet(p);
            TuintjesFeature.feestboeketGemaakt(p);
            helper.assertTrue(Knusfeest.stap(p, Feesttaak.FEESTBLOEMEN) == null, "not asked: nothing happens to the feast");
            Knusfeest.zet(p, Feesttaak.FEESTBLOEMEN, Knusfeest.Stap.GEVRAAGD);
            TuintjesFeature.feestboeketGemaakt(p);
            helper.assertTrue(Knusfeest.stap(p, Feesttaak.FEESTBLOEMEN) == Knusfeest.Stap.GEMAAKT, "the feestboeket is made for the Knusfeest");
            helper.assertTrue(advancement(p, "quest/knusfeest_feestbloemen_gemaakt"), "its quest advancement");
            helper.assertTrue(KnusVoortgang.teller(p, TuintjesVoortgang.FEESTBOEKET) == 2, "counted");
            var recipe = helper.getLevel().getRecipeManager().byKey(Guhs.id("feestboeket"));
            helper.assertTrue(recipe.isPresent() && recipe.get().value().getResultItem(helper.getLevel().registryAccess()).is(TuintjesFeature.FEESTBOEKET.get()),
                    "the recipe");
            helper.assertTrue(recipe.get().value().getIngredients().stream().filter(i -> !i.isEmpty())
                    .allMatch(i -> i.test(new ItemStack(TuintjesFeature.GUHBLOEMETJE.get()))), "made of guhbloemetjes only");
            helper.assertTrue(recipe.get().value().getIngredients().stream().filter(i -> !i.isEmpty()).count() == 5, "five of them");
        } finally {
            Knusfeest.vergeet(p);
            leave(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = TUIN)
    public static void tuintjesTags(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(TuintjesFeature.FEESTBOEKET.get()).is(KnusTags.FEESTBLOEMEN), "feestboeket in #knus/feestbloemen");
        helper.assertTrue(new ItemStack(TuintjesFeature.THEEKRUID.get()).is(KnusTags.THEEKRUID), "#knus/theekruid");
        helper.assertTrue(new ItemStack(TuintjesFeature.KNABBELGRAAN.get()).is(KnusTags.KNABBELGRAAN), "#knus/knabbelgraan");
        helper.assertTrue(new ItemStack(TuintjesFeature.GUHBLOEMETJE.get()).is(KnusTags.GUHBLOEM), "#knus/guhbloem");
        for (TuinPlant plant : List.of(TuinPlant.KNABBELPLANTJE, TuinPlant.THEEKRUID, TuinPlant.GUHBLOEM)) {
            helper.assertTrue(new ItemStack(plant.oogst()).is(KnusTags.OOGST), "#knus/oogst: " + plant);
            helper.assertTrue(TuinPlant.vanZaadje(new ItemStack(plant.zaadje())) == plant, "seeds of " + plant);
        }
        helper.assertTrue(KnusVoortgang.verzameling(TuintjesVoortgang.TUINBOEK) != null && KnusVoortgang.mijlpalen("tuintjes").size() >= 3,
                "the tuinboek and at least three milestones");
        helper.succeed();
    }
}
