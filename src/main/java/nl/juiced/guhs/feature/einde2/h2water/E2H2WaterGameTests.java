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
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
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

    /** A mock player is not ticked by a connection: the story engine's locks (cards, scenes) end on the player's tick. */
    private static void tik(ServerPlayer p) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            E2H2Water.wis(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        // (E2H2Water.TEST stays: the tests of the batch run side by side, and only the chain test uses it)
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
            tik(p);
            tik(vriend);
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
        ServerPlayer stil = speler(helper, 2, 2), beweeg = speler(helper, 5, 2), spring = speler(helper, 8, 2);
        Geestreis.OVERAL = true;
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
     * fix3-h2 (playtest 2, item 2): the pink mist of the Geestenwereld closes exactly the paths that are not for now (at Koh's
     * step every way but the one to Koh, at the Hemelvijver's the way east opens), a wall pushes towards its open side, and
     * Koh faces the player squarely for the whole test (no looking round) and is let go afterwards. Item 3: the sneak for the
     * fish waits until the scene on the harbour told the player that the fish is aboard.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 100)
    public static void einde2H2WaterNevelEnKoh(GameTestHelper helper) {
        // the walls per step
        List<Geestnevel.Nevel> koh = Geestnevel.dicht(E2H2Water.KOH), vijver = Geestnevel.dicht(E2H2Water.HEMELVIJVER);
        helper.assertTrue(koh.containsAll(List.of(Geestnevel.LOTUS, Geestnevel.VIJVER, Geestnevel.THEE, Geestnevel.LOTUS_KOH)),
                "at Koh's step the paths north, east and south-east of the start islet are closed, and the Lotus path at Koh");
        helper.assertTrue(!vijver.contains(Geestnevel.VIJVER) && vijver.contains(Geestnevel.LOTUS) && vijver.contains(Geestnevel.THEE),
                "at the Hemelvijver's step the way east opens, the others stay closed");
        helper.assertTrue(Geestnevel.dicht(E2H2Water.MEDITEER).isEmpty() && Geestnevel.dicht(E2H2Water.VIS_HALEN).isEmpty()
                && Geestnevel.dicht(E2H2Water.STAPPEN).isEmpty(), "no walls at any other step, nor after the chapter");
        for (Geestnevel.Nevel n : Geestnevel.ALLE) {
            net.minecraft.world.phys.Vec3 m = n.midden(), uit = n.buiten(m);
            helper.assertTrue(n.binnen(m) && !n.binnen(uit) && n.terug().lengthSqr() > 0.9, n.id() + ": a box with an open side to push towards");
            helper.assertTrue(n.diepte(m) > 0 && n.diepte(uit) < 0, n.id() + ": the middle is inside, the spot at the open side outside");
        }
        // Koh faces the player during the test, and only then
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 2, 6);
        E2VerhaalDev.zetTot(p, E2Verhaal.WATER, E2H2Water.KOH);
        nl.juiced.guhs.feature.einde2.geestwezens.KohEntity k = nl.juiced.guhs.feature.einde2.geestwezens.E2GeestWezensFeature.KOH.get()
                .create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        BlockPos kp = helper.absolutePos(new BlockPos(8, 2, 2));
        k.snapTo(kp.getX() + 0.5, kp.getY(), kp.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(k);
        Geestreis.OVERAL = true;
        Geestreis.start(p, 0);
        helper.assertTrue(k.kijktStrak() && k.stil(), "the test begins: Koh stands still and looks at the player");
        k.tick();
        float naar = (float) Math.toDegrees(Math.atan2(p.getZ() - k.getZ(), p.getX() - k.getX())) - 90f;
        helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(k.getYRot() - naar)) < 1f
                && Math.abs(net.minecraft.util.Mth.wrapDegrees(k.getYHeadRot() - naar)) < 1f, "body and head turned to the player");
        Geestreis.faal(p, "beweeg");
        helper.assertTrue(!k.kijktStrak(), "after the test Koh may look round again");
        // the fish: the sneak waits for the scene on the harbour
        E2VerhaalDev.zetTot(p, E2Verhaal.WATER, E2H2Water.VIS_HALEN);
        nl.juiced.guhs.feature.verhaal.Cutscenes.vergeet(p, E2H2WaterScenes.VIS_AAN_BOORD.id());
        helper.assertTrue(E2H2Water.wachtOpVis(p, E2H2Water.VIS_HALEN) && !E2H2Water.wachtOpVis(p, E2H2Water.ONTSNAP),
                "the sneak for the fish waits for the scene that says the fish is aboard");
        k.discard();
        weg(helper, p);
        helper.succeed();
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
        // a knabbel lies two blocks ahead (south, inside the test room), at the height the boemerang flies
        BlockPos at = helper.absolutePos(new BlockPos(2, 3, 4));
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
            helper.assertTrue(GuhQuests.count(p, ModItems.KAAS_KNABBELS.get()) == 3, "it brought the knabbels it touched (the knabbel "
                    + (knabbel.isAlive() ? "still lies at " + knabbel.position() : "is gone") + ", the boemerang ended at " + b.position()
                    + ", the player stands at " + p.position() + ")");
            helper.assertTrue(GuhQuests.count(p, E2H2WaterFeature.BOEMERANG.get()) == 1, "the boemerang itself was never used up");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(20)).isEmpty(),
                    "nothing left lying about");
            weg(helper, p);
        });
    }

    /**
     * fix2-h2: every step after the arrival has a hint (a text, a resume spot), the steps whose place is not in view have
     * their own 3.2 s look scene, the two Geestenwereld steps point into the Geestenwereld but resume beside the pond in the
     * Guheinde, the step-start hint waits for its step (Pakguh -> the whip), and the storm and Koh show theirs themselves.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 60)
    public static void einde2H2WaterHints(GameTestHelper helper) {
        for (int stap = E2H2Water.PAKGUH; stap < E2H2Water.STAPPEN; stap++) {
            StapHint.Hint h = StapHint.hint(E2Verhaal.WATER, stap);
            helper.assertTrue(h != null && h.tekst() != null && h.tekst().startsWith(H2Hints.T) && h.bakenAan(), "step " + stap + ": a hint with a text and a marker");
            if (h.kijk() != null) {
                helper.assertTrue(h.kijk().id().startsWith("einde2_h2_water_kijk_") && h.kijk().duur() == StapHint.KIJK_DUUR,
                        "step " + stap + ": its own look scene of 3.2 s");
            }
            boolean geest = stap == E2H2Water.KOH || stap == E2H2Water.HEMELVIJVER;
            helper.assertTrue((h.doel().dim() == Geestenwereld.DIM) == geest && h.terugDoel().dim() == GuheindeFeature.GUHEINDE,
                    "step " + stap + ": the Geestenwereld only for Koh and the Hemelvijver; resume always in the Guheinde");
        }
        helper.assertTrue(StapHint.hint(E2Verhaal.WATER, E2H2Water.AANKOMST) == null, "the arrival: Appa's landing spot (kern's default)");
        helper.assertTrue(!StapHint.hint(E2Verhaal.WATER, E2H2Water.STORM).bijStartAan() && !StapHint.hint(E2Verhaal.WATER, E2H2Water.KOH).bijStartAan(),
                "the storm and Koh show their hint themselves (after the scene of Zuka, after the soul arrived)");
        helper.assertTrue(H2Hints.KIJK.length == 14, "fourteen look scenes");
        ServerPlayer p = speler(helper, 2, 2);
        E2VerhaalDev.zetTot(p, E2Verhaal.WATER, E2H2Water.PAKGUH);
        E2H2Water.naPakguh(p);
        helper.assertTrue(E2H2Water.lijn().stap(p) == E2H2Water.ZWEEP && (E2Verhaal.WATER + "/" + E2H2Water.ZWEEP).equals(StapHint.wacht(p.getUUID())),
                "after Pakguh the whip's hint waits for its turn");
        StapHint.vergeet(p.getUUID());
        weg(helper, p);
        helper.succeed();
    }
}
