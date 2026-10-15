package nl.juiced.guhs.feature.einde2.h2water;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 h2_water: three focused tests of the chapter's own state machine (CONTRACT_E2_B 8: very few). The test server has
 * no Guheinde, so the chapter plays in a "copy" whose template 0,0,0 lies such that the arrival lands on the test room
 * ({@link E2H2Water#TEST}); there are no blocks of the Waterstam here, only its markers.
 */
public class E2H2WaterGameTests {
    private static final String BATCH = "einde2_h2_water";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setOnGround(true);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            E2H2Water.wis(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        E2H2Water.TEST = null;
    }

    /** The copy of the test: the marker aankomst_speler of the Waterstam lies on the test room's (2, 2, 2). */
    private static E2H2Water.Oord kopie(GameTestHelper helper) {
        Plekken.Plek a = Plekken.plek(E2H2Water.STRUCTUUR, "aankomst_speler");
        helper.assertTrue(a != null, "the Waterstam's markers are in the jar");
        E2H2Water.Oord o = new E2H2Water.Oord(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)).subtract(a.pos()));
        E2H2Water.TEST = o;
        return o;
    }

    /**
     * The chapter for one player: the narrator card and the arrival scene (mock players watch for two ticks), Pakguh's
     * scene, the five targets, three waves; a second player at Pakguh's step stays exactly where their own story is. Then
     * the end of the chapter: the flag geleerd, water learnt, the rewards once.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public static void einde2H2WaterKeten(GameTestHelper helper) {
        Verhaallijn lijn = E2H2Water.lijn();
        kopie(helper);
        ServerPlayer p = speler(helper, 2, 2), vriend = speler(helper, 3, 2);
        E2VerhaalDev.zetTot(p, E2Verhaal.WATER, E2H2Water.AANKOMST);
        E2VerhaalDev.zetTot(vriend, E2Verhaal.WATER, E2H2Water.PAKGUH);
        helper.assertTrue(lijn.stappen() == E2H2Water.STAPPEN && E2H2Water.STAPNAMEN.size() == E2H2Water.STAPPEN + 1 && lijn.aanDeBeurt(p)
                && E2Verhaal.lijn(E2Verhaal.WATER) == lijn && E2Verhaal.structuurVan(E2Verhaal.WATER).equals(Einde2Ids.NOORDELIJKE_WATERSTAM),
                "eighteen steps, registered as guhvatar_water");
        helper.assertTrue(E2H2Water.binnen(p) && E2H2Water.binnen(vriend), "both stand in the copy");
        final int[] fase = {0};
        helper.succeedWhen(() -> {
            E2H2Water.seconde(p);
            E2H2Water.seconde(vriend);
            switch (fase[0]) {
                case 0 -> {
                    helper.assertTrue(lijn.stap(p) == E2H2Water.PAKGUH, "card + arrival scene: step 1");
                    helper.assertTrue(E2H2Water.pakguh(p), "Pakguh's scene starts");
                    fase[0] = 1;
                    helper.fail("next: Pakguh's scene");
                }
                case 1 -> {
                    helper.assertTrue(lijn.stap(p) == E2H2Water.ZWEEP, "after Pakguh's scene: the whip");
                    for (int i = 1; i <= E2H2Water.DOELEN; i++) {
                        E2H2Water.doelGeraakt(p, i);
                        E2H2Water.doelGeraakt(p, i);   // (twice the same target counts once)
                    }
                    helper.assertTrue(lijn.stap(p) == E2H2Water.GOLFOEFENING, "five targets: the waves");
                    E2H2Water.golfGemaakt(p);
                    E2H2Water.golfGemaakt(vriend);    // (not their step: nothing)
                    E2H2Water.golfGemaakt(p);
                    helper.assertTrue(lijn.stap(p) == E2H2Water.GOLFOEFENING && lijn.teller(p, E2H2Water.T_GOLVEN) == 2, "two waves");
                    E2H2Water.golfGemaakt(p);
                    helper.assertTrue(lijn.stap(p) == E2H2Water.IJSBRUG, "three waves: the ice bridge");
                    helper.assertTrue(lijn.stap(vriend) == E2H2Water.PAKGUH && lijn.teller(vriend, E2H2Water.T_GOLVEN) == 0,
                            "the friend is still at Pakguh: nothing of the other's story moved theirs");
                    // the end of the chapter, once
                    E2VerhaalDev.zetTot(p, E2Verhaal.WATER, E2H2Water.STORM);
                    E2H2Water.klaar(p);
                    E2H2Water.klaar(p);
                    helper.assertTrue(lijn.klaar(p) && E2Verhaal.vlag(p, E2Verhaal.WATER, E2Verhaal.V_GELEERD)
                            && E2Verhaal.elementGeleerd(p, Einde2Ids.Element.WATER) && E2Verhaal.ontgrendeld(p, Einde2Ids.BA_SING_GUH),
                            "done: water learnt, Ba Sing Guh open");
                    helper.assertTrue(GuhQuests.count(p, E2H2WaterFeature.BOEMERANG.get()) == 1
                            && GuhQuests.count(p, ModItems.clothingItem(GuhClothes.WATERSTAM_PARKA)) == 1
                            && GuhQuests.count(p, ModItems.clothingItem(GuhClothes.BLAUWE_GEEST_MASKER)) == 1, "the rewards, once");
                    weg(helper, p, vriend);
                    fase[0] = 2;
                }
                default -> {
                }
            }
        });
    }

    /**
     * Koh's test: a player who stands still for the whole test passes (on to the Hemelvijver); a player who moves loses
     * their face for a while (nothing else: the step stays, no harm, no item lost) and may try again; a jump is a feeling too.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2H2WaterKoh(GameTestHelper helper) {
        Verhaallijn lijn = E2H2Water.lijn();
        kopie(helper);
        ServerPlayer stil = speler(helper, 2, 2), beweeg = speler(helper, 5, 2), spring = speler(helper, 8, 2);
        for (ServerPlayer p : List.of(stil, beweeg, spring)) {
            E2VerhaalDev.zetTot(p, E2Verhaal.WATER, E2H2Water.KOH);
            p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
            Geestreis.start(p, 0);
            helper.assertTrue(Geestreis.bezig(p), "the test runs");
        }
        Vec3 weg = beweeg.position().add(1.5, 0, 0);
        final int[] tik = {0};
        final String[] mis = {null};
        helper.succeedWhen(() -> {
            tik[0]++;
            for (ServerPlayer p : List.of(stil, beweeg, spring)) {
                Geestreis.tick(p);
            }
            if (tik[0] == 20) {
                beweeg.snapTo(weg.x, weg.y, weg.z);
            }
            if (tik[0] == 30) {
                Geestreis.emotie(spring, "spring");
            }
            if (tik[0] == 40) {
                if (Geestreis.bezig(beweeg) || lijn.stap(beweeg) != E2H2Water.KOH) {
                    mis[0] = "moved: should have failed and stayed at Koh's step";
                } else if (Geestreis.bezig(spring) || lijn.stap(spring) != E2H2Water.KOH) {
                    mis[0] = "jumped: should have failed and stayed at Koh's step";
                } else if (GuhQuests.count(beweeg, ModItems.KAAS_KNABBELS.get()) != 5 || beweeg.getHealth() < beweeg.getMaxHealth()) {
                    mis[0] = "failing must cost nothing";
                } else if (!Geestreis.bezig(stil)) {
                    mis[0] = "the still one should still be in the test";
                }
            }
            if (mis[0] != null) {
                helper.fail(mis[0]);   // (fails again every tick until the time-out reports it)
                return;
            }
            helper.assertTrue(lijn.stap(stil) == E2H2Water.HEMELVIJVER && !Geestreis.bezig(stil), "stood still: on to the Hemelvijver");
            helper.assertTrue(tik[0] >= Geestreis.PROEF_TICKS, "not before the test is over");
            weg(helper, stil, beweeg, spring);
        });
    }

    /**
     * The boemerang always comes back, with what it touched on the way, and is never used up; and the pure maths of the
     * chapter: the whip's aim, a guard's cone, the cells of the ice bridge.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public static void einde2H2WaterBoemerang(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 2, 2);
        p.setYRot(0f);
        p.setXRot(0f);
        p.getInventory().add(new ItemStack(E2H2WaterFeature.BOEMERANG.get()));
        // a knabbel lies four blocks ahead (south), at the height the boemerang flies
        BlockPos at = helper.absolutePos(new BlockPos(2, 3, 6));
        ItemEntity knabbel = new ItemEntity(level, at.getX() + 0.5, p.getEyeY() - 0.1, at.getZ() + 0.5, new ItemStack(ModItems.KAAS_KNABBELS.get(), 3));
        knabbel.setDeltaMovement(Vec3.ZERO);
        knabbel.setNoGravity(true);
        knabbel.setNoPickUpDelay();
        level.addFreshEntity(knabbel);
        BoemerangEntity b = new BoemerangEntity(level, p);
        b.setPos(p.getX(), p.getEyeY() - 0.1, p.getZ());
        b.shootFromRotation(p, 0f, 0f, 0.0f, 1.0f, 0.0f);
        level.addFreshEntity(b);
        // the pure maths
        Vec3 oog = new Vec3(0, 1.6, 0), zuid = new Vec3(0, 0, 1);
        helper.assertTrue(E2H2WaterKrachten.geraakt(oog, zuid, new Vec3(0.5, 1.8, 6)) && !E2H2WaterKrachten.geraakt(oog, zuid, new Vec3(4, 1.6, 4))
                && !E2H2WaterKrachten.geraakt(oog, zuid, new Vec3(0, 1.6, 12)), "the whip hits straight ahead within reach only");
        helper.assertTrue(Sluipen.inKegel(oog, 0f, new Vec3(0.5, 1.6, 4), Sluipen.ZICHT) && !Sluipen.inKegel(oog, 0f, new Vec3(0, 1.6, -4), Sluipen.ZICHT)
                && !Sluipen.inKegel(oog, 0f, new Vec3(0, 1.6, 5.5), Sluipen.ZICHT_SLUIPEND) && Sluipen.inKegel(oog, 90f, new Vec3(-4, 1.6, 0), Sluipen.ZICHT),
                "a guard sees ahead in its cone, not behind it, less far at a sneak");
        helper.assertTrue(E2H2WaterKrachten.brug(new BlockPos(150, 36, 87), new BlockPos(157, 36, 87)).size() == 18, "the ice bridge: 6 long, 3 wide");
        helper.succeedWhen(() -> {
            helper.assertTrue(!b.isAlive(), "the boemerang is back");
            helper.assertTrue(GuhQuests.count(p, ModItems.KAAS_KNABBELS.get()) == 3, "it brought the knabbels it touched");
            helper.assertTrue(GuhQuests.count(p, E2H2WaterFeature.BOEMERANG.get()) == 1, "the boemerang itself was never used up");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(20)).isEmpty(),
                    "nothing left lying about");
            weg(helper, p);
        });
    }
}
