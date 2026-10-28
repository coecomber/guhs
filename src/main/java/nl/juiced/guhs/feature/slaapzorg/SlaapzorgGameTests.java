package nl.juiced.guhs.feature.slaapzorg;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.huisje.HuisjeFeature;
import nl.juiced.guhs.feature.kamperen.KamperenFeature;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.feature.wereldleven.Dagritme;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of slaapzorg (1.8.0, batch "slaapzorg"): a guh finds a sleeping spot of the tag at night and curls up in it,
 * the Muziekdoosje sings the guhs around asleep (and they stay curled up with their eyes shut), breakfast in bed wakes a
 * sleeper in the morning ("uitgerust" once a day) but not at night, the lamps click on and off, a tame guh hops onto a
 * krabpaal, and the recipes and tag are there.
 */
public class SlaapzorgGameTests {
    private static final String EMPTY = "empty", BATCH = "slaapzorg";

    private static GuhEntity guh(GameTestHelper helper, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.setPersistenceRequired();
        return guh;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    /** At night a guh walks to the guh bed (a sleeping spot of the tag), curls up in it with its eyes shut. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void guhZoektEenSlaapplekje(GameTestHelper helper) {
        BlockPos bed = new BlockPos(3, 1, 3);
        helper.setBlock(bed, HuisjeFeature.BEDJE.get());
        helper.assertTrue(Slapen.isSlaapplek(helper.getBlockState(bed)), "the guh bed is a sleeping spot (#guhs:guh_slaapplekken)");
        GuhEntity guh = guh(helper, new BlockPos(1, 1, 1));
        SlaapplekGoal.TEST_NACHT.add(guh.getUUID());
        Dagritme.TEST_DAGDEEL.put(guh.getUUID(), Dagdeel.NACHT);
        helper.succeedWhen(() -> {
            helper.assertTrue(SlaapplekGoal.slaaptInPlek(guh), "asleep in its spot");
            helper.assertTrue(GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT), "eyes shut");
            helper.assertTrue(guh.blockPosition().equals(helper.absolutePos(bed)), "lying in the bed");
            SlaapplekGoal.TEST_NACHT.remove(guh.getUUID());
            Dagritme.TEST_DAGDEEL.remove(guh.getUUID());
        });
    }

    /** The Muziekdoosje plays (no second song on top), the guh yawns and falls asleep in a moment, curled up, eyes shut. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 300)
    public static void muziekdoosjeZingtGuhsInSlaap(GameTestHelper helper) {
        BlockPos doos = new BlockPos(2, 1, 2);
        helper.setBlock(doos, SlaapzorgFeature.MUZIEKDOOSJE.get());
        GuhEntity guh = guh(helper, new BlockPos(1, 1, 3));
        Dagritme.TEST_DAGDEEL.put(guh.getUUID(), Dagdeel.AVOND);
        BlockState s = helper.getBlockState(doos);
        int luisteraars = SlaapzorgFeature.MUZIEKDOOSJE.get().speel(helper.getLevel(), helper.absolutePos(doos), s);
        helper.assertTrue(luisteraars == 1, "one guh listens, got " + luisteraars);
        helper.assertTrue(helper.getBlockState(doos).getValue(SlaapBlokken.Speeldoos.SPEELT) == 12, "it plays");
        helper.assertTrue(guh.getPersistentData().contains(Slapen.INSLAAP_OM), "it will fall asleep in a moment");
        guh.getPersistentData().putLong(Slapen.INSLAAP_OM, helper.getLevel().getGameTime() + 5);   // (no need to wait the whole song)
        helper.succeedWhen(() -> {
            helper.assertTrue(Slapen.isIngezongen(guh) && guh.isInSittingPose() && GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT),
                    "sung asleep: curled up, eyes shut");
            helper.assertTrue(Slapen.slaapt(guh), "counts as asleep");
            Dagritme.TEST_DAGDEEL.remove(guh.getUUID());
        });
    }

    /** Breakfast in bed: too early at night; in the morning it wakes, eats one knabbel and is "uitgerust" (once a day). */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void ontbijtOpBedWektInDeOchtend(GameTestHelper helper) {
        GuhEntity guh = guh(helper, new BlockPos(2, 1, 2));
        ServerPlayer p = speler(helper, new BlockPos(1, 1, 1));
        try {
            Dagritme.TEST_DAGDEEL.put(guh.getUUID(), Dagdeel.NACHT);
            helper.assertTrue(Slapen.slaapIn(guh), "sung asleep");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 3));
            helper.assertTrue(OntbijtOpBed.klik(guh, p, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "handled");
            helper.assertTrue(Slapen.slaapt(guh) && p.getMainHandItem().getCount() == 3, "at night: too early, nothing eaten, still asleep");
            Dagritme.TEST_DAGDEEL.put(guh.getUUID(), Dagdeel.OCHTEND);
            OntbijtOpBed.klik(guh, p, InteractionHand.MAIN_HAND);
            helper.assertFalse(Slapen.slaapt(guh), "awake for breakfast");
            helper.assertTrue(p.getMainHandItem().getCount() == 2, "one knabbel eaten");
            helper.assertTrue(guh.hasEffect(KamperenFeature.UITGERUST), "uitgerust");
            helper.assertTrue(guh.getPersistentData().contains(Slapen.SMAKKEN_OM), "it munches after its yawn");
            helper.assertTrue(OntbijtOpBed.klik(guh, p, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "awake: no breakfast in bed any more");
        } finally {
            Dagritme.TEST_DAGDEEL.remove(guh.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    /** The lamps click on and off (and give light only when on); the recipes and the tag are there. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void lampjesEnRecepten(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(1, 1, 1));
        try {
            BlockPos lamp = new BlockPos(3, 1, 3);
            helper.setBlock(lamp, SlaapzorgFeature.MAANLAMPJE_GUH.get());
            helper.assertTrue(helper.getBlockState(lamp).getValue(SlaapBlokken.Lampje.LIT) && helper.getBlockState(lamp).getLightEmission() > 0, "on: light");
            BlockPos abs = helper.absolutePos(lamp);
            helper.getBlockState(lamp).useWithoutItem(helper.getLevel(), p, new BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.NORTH, abs, false));
            helper.assertFalse(helper.getBlockState(lamp).getValue(SlaapBlokken.Lampje.LIT), "a click: off");
            helper.assertTrue(helper.getBlockState(lamp).getLightEmission() == 0, "off: dark");
            var recipes = helper.getLevel().getServer().getRecipeManager();
            for (var b : SlaapzorgFeature.ALLE) {
                String id = b.getId().getPath();
                helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(id))).isPresent(), "a recipe for " + id);
            }
            helper.assertTrue(HuisjeFeature.BEDJE.get().defaultBlockState().is(Slapen.SLAAPPLEKKEN), "the tag holds the guh bed");
        } finally {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    /** By day a tame guh near a krabpaal hops on top and sits there. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void tammeGuhHuptOpDeKrabpaal(GameTestHelper helper) {
        BlockPos paal = new BlockPos(3, 1, 3);
        helper.setBlock(paal, SlaapzorgFeature.KRABPAAL.get());
        ServerPlayer p = speler(helper, new BlockPos(1, 1, 1));
        GuhEntity guh = guh(helper, new BlockPos(1, 1, 3));
        guh.tame(p);
        KrabpaalGoal.TEST_ALTIJD.add(guh.getUUID());
        Dagritme.TEST_DAGDEEL.put(guh.getUUID(), Dagdeel.DAG);
        helper.succeedWhen(() -> {
            BlockPos top = helper.absolutePos(paal).above();
            helper.assertTrue(guh.blockPosition().equals(top) && guh.isInSittingPose(), "sitting on top of the krabpaal");
            KrabpaalGoal.TEST_ALTIJD.remove(guh.getUUID());
            Dagritme.TEST_DAGDEEL.remove(guh.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        });
    }
}
