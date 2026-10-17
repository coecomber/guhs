package nl.juiced.guhs.feature.einde2.spd1stad;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaalDev;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature;
import nl.juiced.guhs.feature.einde2.zwevers.KruimelkwalEntity;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 "Sponsguh", slice sp_d1_stad (CONTRACT_E2_C 8.3): the two state machines with a mock player, without scenes
 * ({@code D1Stad.PROEF}) and without the build (a route of its own).
 * <ul>
 *   <li>{@code spD1StadKwallen}: step 5's halves (sleutel 5 / 5_b), a wild jelly counts once per try, the count is saved,
 *       eight = the rewards once, the flags gone, step 6;</li>
 *   <li>{@code spD1StadExamen}: the gates in order (the bridge reported once, the finish at the end), eight bumps are fine and
 *       the ninth starts the try again at the first gate, the finish moves the line on to step 7; the scenes are within their
 *       lengths, every step has a hint.</li>
 * </ul>
 */
public class E2SpD1StadGameTests {
    private static final String BATCH = "einde2_sp_d1_stad";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static void opruimen(GameTestHelper helper, ServerPlayer p) {
        D1Stad.PROEF = false;
        Examen.PROEF_ROUTE = null;
        Kwallen.vergeet(p.getUUID());
        Examen.vergeet(p.getUUID());
        StapHint.vergeet(p.getUUID());
        SpVerhaalDev.wis(p);
        for (String l : E2Verhaal.HOOFDSTUKKEN) {
            E2VerhaalDev.wis(p, l);
        }
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static int tel(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void spD1StadKwallen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        D1Stad.PROEF = true;
        try {
            helper.assertTrue(SpStappen.isEcht(SpVerhaal.RECEPT, D1Stad.KWALLEN) && SpStappen.isEcht(SpVerhaal.RECEPT, D1Stad.EXAMEN),
                    "steps 5 and 6 are this slice's");
            SpVerhaalDev.zetTot(p, SpVerhaal.RECEPT, D1Stad.KWALLEN);
            var l = D1Stad.lijn();
            helper.assertTrue(D1Stad.aanZet(p, D1Stad.KWALLEN) && l.sleutel(p).equals("5"), "step 5, first half (to Patguh): " + l.sleutel(p));
            // the scene patguh (no scene in a test): the net, the second half
            Kwallen.begin(p);
            helper.assertTrue(l.vlag(p, D1Stad.V_NET) && l.sleutel(p).equals("5_b") && tel(p, E2SpD1StadFeature.KWALLENNET.get()) == 1,
                    "after Patguh: the net, sleutel 5_b");
            Kwallen.begin(p);
            helper.assertTrue(tel(p, E2SpD1StadFeature.KWALLENNET.get()) == 1, "never a second net");
            // a wild jelly: once per try
            KruimelkwalEntity kwal = E2ZweversFeature.KRUIMELKWAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            helper.assertTrue(kwal != null, "a kwal");
            Vec3 bij = Vec3.atBottomCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2)));
            kwal.snapTo(bij.x, bij.y, bij.z, 0f, 0f);
            helper.getLevel().addFreshEntity(kwal);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(E2SpD1StadFeature.KWALLENNET.get()));
            helper.assertTrue(Kwallen.tik(p, kwal) && Kwallen.gevangen(p) == 1, "a tap with the net: one in the jar");
            helper.assertTrue(Kwallen.tik(p, kwal) && Kwallen.gevangen(p) == 1, "the same wild jelly again: still one");
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            KruimelkwalEntity kwal2 = E2ZweversFeature.KRUIMELKWAL.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            kwal2.snapTo(bij.x + 1, bij.y, bij.z, 0f, 0f);
            helper.getLevel().addFreshEntity(kwal2);
            helper.assertTrue(!Kwallen.tik(p, kwal2) && Kwallen.gevangen(p) == 1, "an empty hand catches nothing");
            kwal.discard();
            kwal2.discard();
            // seven more; the count is in the line (leaving the field loses nothing)
            for (int i = 2; i < Kwallen.NODIG; i++) {
                Kwallen.tel(p);
            }
            helper.assertTrue(Kwallen.gevangen(p) == Kwallen.NODIG - 1 && l.stap(p) == D1Stad.KWALLEN, "seven: still step 5");
            Kwallen.tel(p);
            helper.assertTrue(l.stap(p) == D1Stad.EXAMEN && !l.vlag(p, D1Stad.V_NET) && l.teller(p, D1Stad.T_KWALLEN) == 0,
                    "eight: step 6, the step's flag and counter gone");
            helper.assertTrue(tel(p, E2SponsguhFeature.KWALLENJAM.get()) == 3 && tel(p, ModItems.clothingItem(GuhClothes.SPONSGUH_BLOEMETJESBROEK)) == 1,
                    "the rewards: three kwallenjam and the bloemetjesbroek");
            helper.assertTrue(l.sleutel(p).equals("6"), "step 6, first half (to juf Pufguh)");
        } finally {
            opruimen(helper, p);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void spD1StadExamen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        D1Stad.PROEF = true;
        try {
            // a straight course of its own: 14 points, 10 blocks apart
            List<Vec3> route = new ArrayList<>();
            for (int i = 0; i < 14; i++) {
                route.add(new Vec3(i * 10.0, 0, 0));
            }
            Examen.Rit r = new Examen.Rit();
            helper.assertTrue(Examen.poort(r, new Vec3(1, 0, 0), route) == Examen.Uitslag.NIETS && r.poort() == Examen.EERSTE, "not at a gate");
            helper.assertTrue(Examen.poort(r, new Vec3(30, 0, 0), route) == Examen.Uitslag.NIETS, "gate 3 before gate 1 does not count");
            List<Examen.Uitslag> uit = new ArrayList<>();
            for (int i = 1; i <= Examen.FINISH; i++) {
                uit.add(Examen.poort(r, new Vec3(i * 10.0 + 2.0, 0, 1.0), route));
            }
            helper.assertTrue(uit.get(Examen.BRUG - 2) == Examen.Uitslag.BRUG && uit.get(uit.size() - 1) == Examen.Uitslag.FINISH
                    && uit.stream().filter(u -> u == Examen.Uitslag.BRUG).count() == 1, "the gates in order, the bridge once, the finish last: " + uit);
            Examen.Rit b = new Examen.Rit();
            b.poort = 7;
            for (int i = 0; i < Examen.MAX_GERAAKT; i++) {
                helper.assertTrue(Examen.botsing(b) == Examen.Uitslag.POORT, "bump " + (i + 1) + " is fine");
            }
            helper.assertTrue(b.poort() == 7 && Examen.botsing(b) == Examen.Uitslag.OPNIEUW && b.poort() == Examen.EERSTE && b.geraakt() == 0,
                    "the ninth bump: a new try from the first gate");
            // the line: halves, the finish moves on
            SpVerhaalDev.zetTot(p, SpVerhaal.RECEPT, D1Stad.EXAMEN);
            var l = D1Stad.lijn();
            helper.assertTrue(l.sleutel(p).equals("6"), "step 6, first half");
            Examen.begin(p);
            helper.assertTrue(l.vlag(p, D1Stad.V_EXAMEN) && l.sleutel(p).equals("6_b"), "after juf Pufguh: sleutel 6_b");
            Examen.finish(p, null, Examen.spel(p));
            helper.assertTrue(l.stap(p) == SpVerhaal.R_PLAN_Z && !l.vlag(p, D1Stad.V_EXAMEN) && !l.vlag(p, D1Stad.V_GESLAAGD),
                    "through the finish: step 7 (Plan Z), the flags gone");
            // every step has a hint; the scenes are within their lengths
            helper.assertTrue(StapHint.hint(SpVerhaal.RECEPT, D1Stad.KWALLEN) != null && StapHint.hint(SpVerhaal.RECEPT, D1Stad.EXAMEN) != null,
                    "a hint per step");
            for (String naam : D1StadScenes.NAMEN) {
                Cutscene s = D1StadScenes.van(naam);
                int[] grens = D1StadScenes.LENGTES.get(naam);
                helper.assertTrue(s != null && grens != null && s.duur() >= grens[0] * 20 && s.duur() <= grens[1] * 20,
                        "scene " + naam + " is " + (s == null ? "missing" : s.duur() / 20.0 + " s"));
            }
        } finally {
            opruimen(helper, p);
        }
        helper.succeed();
    }
}
