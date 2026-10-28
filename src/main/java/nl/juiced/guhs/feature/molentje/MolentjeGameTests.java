package nl.juiced.guhs.feature.molentje;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.feature.droomwezens.DroomNpc;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the Maanmolentje (batch "molentje"): the cog puzzle (shuffled = never solved, always solvable; a click turns
 * the next cog along), the sails turning only with all cogs up AND three blowing Blubs, and the Zandguh's bag of droomzand
 * (once per run) with Snoes' piece. A mill is built in the test garden and registered like the island's one.
 */
public class MolentjeGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "molentje";

    record Opstelling(Molen.Plekken p, ServerLevel level) {
    }

    private static Opstelling bouw(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<BlockPos> tandwielen = new ArrayList<>(), kussentjes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            BlockPos rel = new BlockPos(1 + i, 2, 1);
            helper.setBlock(rel, MolentjeFeature.TANDWIEL.get().defaultBlockState().setValue(MolenBlokken.Tandwiel.FACING, Direction.SOUTH));
            tandwielen.add(helper.absolutePos(rel));
        }
        for (int i = 0; i < 3; i++) {
            BlockPos rel = new BlockPos(1 + 2 * i, 1, 4);
            helper.setBlock(rel, MolentjeFeature.KUSSENTJE.get().defaultBlockState());
            kussentjes.add(helper.absolutePos(rel));
        }
        BlockPos wieken = new BlockPos(6, 3, 1);
        helper.setBlock(wieken, MolentjeFeature.WIEKEN.get().defaultBlockState());
        Molen.Plekken p = Molen.van(tandwielen, kussentjes, helper.absolutePos(wieken), Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 1, 3))));
        Molen.EXTRA.add(p);
        return new Opstelling(p, level);
    }

    private static void klaar(Opstelling o) {
        Molen.reset(o.level(), o.p());
        Molen.EXTRA.remove(o.p());
    }

    /** Solve it the way a player would: from the left, click each cog until it points up (the last one only turns itself). */
    private static int los(ServerLevel level, Molen.Plekken p) {
        int kliks = 0;
        for (int i = 0; i < p.tandwielen().size(); i++) {
            while (Molen.standen(level, p)[i] != 0) {
                Molen.klik(level, p, i);
                kliks++;
            }
        }
        return kliks;
    }

    private static BlubGuhEntity blubOp(Opstelling o, int i) {
        BlubGuhEntity b = BlubFeature.BLUBGUH.get().create(o.level(), EntitySpawnReason.EVENT);
        BlockPos k = o.p().kussentjes().get(i);
        b.snapTo(k.getX() + 0.5, k.getY() + 0.25, k.getZ() + 0.5, 0, 0);
        b.finalizeSpawn(o.level(), o.level().getCurrentDifficultyAt(k), EntitySpawnReason.EVENT, null);
        o.level().addFreshEntity(b);
        Molen.stuur(b, i);
        return b;
    }

    /** A click turns that cog and the next one along; shuffling never leaves it solved, and it can always be solved. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void tandwielPuzzel(GameTestHelper helper) {
        Opstelling o = bouw(helper);
        try {
            Molen.klik(o.level(), o.p(), 1);
            int[] s = Molen.standen(o.level(), o.p());
            helper.assertTrue(s[0] == 0 && s[1] == 1 && s[2] == 1 && s[3] == 0, "a click turns its cog and the next: " + java.util.Arrays.toString(s));
            Molen.klik(o.level(), o.p(), 3);
            helper.assertTrue(Molen.standen(o.level(), o.p())[3] == 1, "the last cog turns only itself");
            for (int keer = 0; keer < 12; keer++) {
                Molen.schud(o.level(), o.p());
                helper.assertFalse(Molen.opgelost(o.level(), o.p()), "shuffled is never solved");
                int kliks = los(o.level(), o.p());
                helper.assertTrue(Molen.opgelost(o.level(), o.p()) && kliks > 0, "a shuffled puzzle can be solved");
            }
        } finally {
            klaar(o);
        }
        helper.succeed();
    }

    /** The sails turn only with every cog up AND three Blubs blowing; then the Zandguh gives droomzand once and Snoes' piece. */
    @SuppressWarnings("removal")
    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 200)
    public static void molenDraaitEnZandguhMaalt(GameTestHelper helper) {
        Opstelling o = bouw(helper);
        ServerLevel level = o.level();
        Molen.Plekken p = o.p();
        try {
            Molen.stap(level, p);                                   // the first look of a new mill shuffles it
            Molen.schud(level, p);
            helper.assertFalse(Molen.opgelost(level, p), "the mill starts shuffled");
            blubOp(o, 0);
            blubOp(o, 1);
            los(level, p);
            Molen.stap(level, p);
            helper.assertFalse(Molen.loopt(level, p), "two Blubs are not enough");
            blubOp(o, 2);
            Molen.stap(level, p);
            helper.assertTrue(Molen.loopt(level, p), "three Blubs and every cog up: the sails turn");
            helper.assertTrue(level.getBlockState(p.wieken()).getValue(MolenBlokken.Wieken.DRAAIT), "the sails' block turns too");

            ServerPlayer sp = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
            sp.setGameMode(GameType.SURVIVAL);
            sp.getInventory().clearContent();
            DroomVoortgang.reset(sp);
            GuhNpcEntity zandguh = DroomNpc.spawn(level, p.zandguh(), 0, GuhNpcEntity.Kind.ZANDGUH);
            helper.assertTrue(Molen.praatZandguh(sp, zandguh), "the Zandguh talks at the mill");
            int zand = sp.getInventory().countItem(DroomblokkenFeature.DROOMZAND.get());
            helper.assertTrue(zand == 3, "a bag of 3 droomzand, got " + zand);
            helper.assertTrue(DroomVoortgang.heeft(sp, DroomVoortgang.Stukje.DROOMZAND), "Snoes' piece: the droomzand");
            Molen.praatZandguh(sp, zandguh);
            helper.assertTrue(sp.getInventory().countItem(DroomblokkenFeature.DROOMZAND.get()) == 3, "only one bag per run");
            zandguh.discard();

            Molen.reset(level, p);
            helper.assertFalse(Molen.loopt(level, p), "after the run the sails stop");
            helper.assertTrue(Molen.blubsVan(level, p).isEmpty(), "and the Blubs hop off");
            helper.assertFalse(Molen.opgelost(level, p), "and the cogs are shuffled again");
        } finally {
            klaar(o);
        }
        helper.succeed();
    }
}
