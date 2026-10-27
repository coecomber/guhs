package nl.juiced.guhs.feature.blub;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.piep.PiepInstelling;
import nl.juiced.guhs.feature.piep.Schouder;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the Blub-guh (1.8.0 test build): the spawn dice (bell around 35, the soort and tint chances, the size range),
 * babyblubjes between their parents, taming + the pick-up round trip keeps every die (and the strikje and name), it can't be
 * hurt (but /kill works), and the menu buttons (strikje, volgen, a trick, the lullaby, hupsen, verstoppertje, the shoulder).
 * Template landdiertjes_test_tuin (24 x 24 grass).
 */
public class BlubGameTests {
    private static final String TUIN = "landdiertjes_test_tuin";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static BlubGuhEntity blub(GameTestHelper helper, BlockPos at, BlubTrekking.Waarden w) {
        BlubGuhEntity b = helper.spawn(BlubFeature.BLUBGUH.get(), at);
        b.zetWaarden(w);
        return b;
    }

    static void tem(GameTestHelper helper, BlubGuhEntity b, ServerPlayer p) {
        for (int i = 0; i < 80 && !b.isTame(); i++) {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
            b.mobInteract(p, InteractionHand.MAIN_HAND);
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(b.isTame() && b.isOwnedBy(p), "tamed with kaasknabbels (TIJDELIJK)");
    }

    @GuhTest(template = TUIN, batch = "blub")
    public static void blubTrekkingVerdeling(GameTestHelper helper) {
        RandomSource r = RandomSource.create(1808L);
        int n = 6000;
        long som = 0;
        int[] soorten = new int[BlubTrekking.Soort.values().length];
        int normaal = 0, laag = 0, hoog = 0;
        for (int i = 0; i < n; i++) {
            BlubTrekking.Waarden w = BlubTrekking.trek(r);
            helper.assertTrue(w.meter() >= 0 && w.meter() <= 100, "meter 0..100");
            helper.assertTrue(w.grootte() >= BlubTrekking.GROOTTE_MIN && w.grootte() <= BlubTrekking.GROOTTE_MAX, "size 85..115%");
            helper.assertTrue(w.tint() >= 0 && w.tint() <= 2, "tint 0..2");
            som += w.meter();
            soorten[w.soort().ordinal()]++;
            normaal += w.tint() == 0 ? 1 : 0;
            laag += w.meter() < 10 ? 1 : 0;
            hoog += w.meter() > 80 ? 1 : 0;
        }
        double gem = som / (double) n;
        helper.assertTrue(gem > 33 && gem < 37, "the meter is a bell around 35, got " + gem);
        helper.assertTrue(laag > hoog, "more Blub than guh: " + laag + " low vs " + hoog + " high");
        for (BlubTrekking.Soort s : BlubTrekking.Soort.values()) {
            double kans = soorten[s.ordinal()] * 100.0 / n;
            helper.assertTrue(Math.abs(kans - s.kans) < 2.5, s.id() + ": about " + s.kans + "%, got " + kans);
        }
        helper.assertTrue(Math.abs(normaal * 100.0 / n - 60) < 3, "the normal tint about 60%");
        helper.assertTrue(BlubTrekking.stap(0) == 0 && BlubTrekking.stap(1) == 1 && BlubTrekking.stap(20) == 2 && BlubTrekking.stap(59) == 3
                && BlubTrekking.stap(60) == 4 && BlubTrekking.stap(99) == 5 && BlubTrekking.stap(100) == 6, "the stages of the meter");
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = "blub")
    public static void blubBabyTussenOuders(GameTestHelper helper) {
        RandomSource r = RandomSource.create(42L);
        BlubTrekking.Waarden a = new BlubTrekking.Waarden(12, BlubTrekking.Soort.WEIDE, 1, 0.9f);
        BlubTrekking.Waarden b = new BlubTrekking.Waarden(70, BlubTrekking.Soort.ZWAM, 2, 1.1f);
        boolean metA = false, metB = false;
        for (int i = 0; i < 400; i++) {
            BlubTrekking.Waarden k = BlubTrekking.erf(a, b, r);
            helper.assertTrue(k.meter() >= 12 && k.meter() <= 70, "meter between the parents");
            helper.assertTrue(k.grootte() >= 0.9f && k.grootte() <= 1.1f, "size between the parents");
            helper.assertTrue(k.soort() == a.soort() || k.soort() == b.soort(), "soort of a parent");
            helper.assertTrue(k.tint() == a.tint() || k.tint() == b.tint(), "tint of a parent");
            metA |= k.soort() == a.soort();
            metB |= k.soort() == b.soort();
        }
        helper.assertTrue(metA && metB, "both parents' soort happen");
        // a real pair: the baby is the owner's, a baby, between its parents
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        BlubGuhEntity ma = blub(helper, new BlockPos(6, 2, 6), a);
        BlubGuhEntity pa = blub(helper, new BlockPos(7, 2, 6), b);
        tem(helper, ma, p);
        tem(helper, pa, p);
        BlubGuhEntity baby = (BlubGuhEntity) ma.getBreedOffspring(helper.getLevel(), pa);
        helper.assertTrue(baby != null && baby.isTame() && baby.isOwnedBy(p), "the baby is yours");
        helper.assertTrue(baby.meter() >= 12 && baby.meter() <= 70, "the baby's meter is between its parents'");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 4));
        ma.mobInteract(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(ma.isInLove(), "a kaasknabbel makes a tame Blub fall in love");
        helper.succeed();
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    @GuhTest(template = TUIN, batch = "blub")
    public static void blubTemmenOppakkenHoudtAlles(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        BlubGuhEntity b = blub(helper, new BlockPos(6, 2, 6), new BlubTrekking.Waarden(100, BlubTrekking.Soort.RAVIJN, 2, 1.12f));
        helper.assertTrue("vol".equals(b.titel()), "exactly 100 is a Volle Guh");
        tem(helper, b, p);
        b.setStrik(6);
        b.setCustomName(Component.literal("Bubbels"));
        p.setShiftKeyDown(true);
        b.mobInteract(p, InteractionHand.MAIN_HAND);
        p.setShiftKeyDown(false);
        helper.assertTrue(b.isRemoved(), "picked up (sneak + empty hand)");
        ItemStack item = ItemStack.EMPTY;
        for (ItemStack st : p.getInventory().getNonEquipmentItems()) {
            if (st.is(BlubFeature.BLUBGUH_ITEM.get())) {
                item = st;
            }
        }
        helper.assertTrue(!item.isEmpty(), "the Blub-guh item is in the pockets");
        BlubGuhEntity terug = PiepDierItem.zetNeer(item, level, helper.absolutePos(new BlockPos(9, 2, 9)).getCenter(), 0f, BlubFeature.BLUBGUH.get());
        helper.assertTrue(terug != null && terug.isTame() && terug.isOwnedBy(p), "put down, still yours");
        helper.assertTrue(terug.meter() == 100 && terug.blubSoort() == BlubTrekking.Soort.RAVIJN && terug.tint() == 2
                && Math.abs(terug.grootte() - 1.12f) < 0.001f, "meter, soort, tint and size are kept");
        helper.assertTrue(terug.strik() == 6 && "Bubbels".equals(terug.getCustomName().getString()), "strikje and name are kept");
        // like the real Blub: no damage (a fright), but /kill works
        float hp = terug.getHealth();
        helper.assertFalse(terug.hurtServer(level, level.damageSources().playerAttack(p), 5f), "a hit does nothing");
        helper.assertTrue(terug.getHealth() == hp && terug.emotie() == BlubGuhEntity.PLAT, "no damage, ears flat");
        helper.assertFalse(terug.isPushable(), "it can't be pushed");
        terug.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
        helper.assertTrue(terug.isDeadOrDying(), "/kill still works");
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = "blub", timeoutTicks = 320)
    public static void blubMenuKnoppen(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(11, 2, 11));
        BlubGuhEntity b = blub(helper, new BlockPos(12, 2, 12), new BlubTrekking.Waarden(35, BlubTrekking.Soort.BLAUW, 0, 1f));
        tem(helper, b, p);
        helper.assertTrue(BlubMenu.doe(p, b, BlubMenu.Actie.STRIK, 9, "") && b.strik() == 9, "strikje colour");
        helper.assertTrue(BlubMenu.doe(p, b, BlubMenu.Actie.STRIK, -1, "") && b.strik() == -1, "no strikje");
        boolean volgt = b.aan(PiepInstelling.VOLGEN);
        BlubMenu.doe(p, b, BlubMenu.Actie.WISSEL, PiepInstelling.VOLGEN.ordinal(), "");
        helper.assertTrue(b.aan(PiepInstelling.VOLGEN) != volgt, "volgen switches");
        BlubMenu.doe(p, b, BlubMenu.Actie.TRUC, 0, "");
        helper.assertTrue(b.isOrderedToSit(), "trick 'zit': it sits");
        BlubMenu.doe(p, b, BlubMenu.Actie.TRUC, 0, "");
        helper.assertFalse(b.isOrderedToSit(), "trick 'zit' again: up");
        helper.assertTrue(BlubMenu.doe(p, b, BlubMenu.Actie.SLAAPLIEDJE, 0, ""), "the lullaby");
        helper.assertTrue(BlubMenu.doe(p, b, BlubMenu.Actie.HUPSEN, 0, "") && b.isBezig(), "hupsen: busy hopping");
        helper.assertFalse(BlubMenu.doe(p, b, BlubMenu.Actie.SCHOUDER, 0, ""), "busy: no other buttons");
        helper.runAfterDelay(240, () -> {
            helper.assertFalse(b.isBezig(), "done hopping");
            helper.assertTrue(b.hupsRecord() >= 6, "a hop record");
            helper.assertTrue(BlubMenu.doe(p, b, BlubMenu.Actie.VERSTOPPERTJE, 0, "") && b.verstopt(), "verstoppertje: hidden");
            b.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertFalse(b.verstopt(), "found by clicking it");
            helper.assertTrue(BlubMenu.doe(p, b, BlubMenu.Actie.SCHOUDER, 0, "") && Schouder.heeft(p) && b.isRemoved(), "onto the shoulder");
            var eraf = Schouder.eraf(p, p.position());
            helper.assertTrue(eraf instanceof BlubGuhEntity e && e.meter() == 35, "off the shoulder, the same Blub");
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            helper.succeed();
        });
    }
}
