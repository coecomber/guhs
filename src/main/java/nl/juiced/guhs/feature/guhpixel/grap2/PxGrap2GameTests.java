package nl.juiced.guhs.feature.guhpixel.grap2;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.guhpixel.Arena;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.Films;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxTest;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the guhpixel slice "grap2" (batch px_grap2; run with {@code -Pgt=px_grap2}): the rules of the Guhmon
 * battle (every move, the failing Nap, the full belly, both outcomes, the other guh's choices), a whole visit to the gym
 * (the leenguh, a copy of an own guh, refused input, the badges, a replay, "kwijt"), a whole episode of Boer zoekt Guh
 * (the order of the steps, refused input, the keepsakes, a replay), two players at once in their own arenas, both
 * templates, the Badgedoosje, and the Guhdex section. The game test server has no guhpixel dimension: every test marks
 * its own box ({@link PxTest#gebied}) and stamps its arenas there.
 */
public class PxGrap2GameTests {
    private static final String BATCH = "px_grap2";
    private static final String KLEIN = "px_test_16", GROOT = "px_test_48", REUS = "px_test_96";
    private static final GuhmonGevecht.Zet V = GuhmonGevecht.Zet.VADSEN, N = GuhmonGevecht.Zet.NJEG, K = GuhmonGevecht.Zet.KNABBEL,
            D = GuhmonGevecht.Zet.DUTJE;
    private static final int IK = GuhmonGevecht.SPELER, HIJ = GuhmonGevecht.TEGEN;

    private static int tel(ServerPlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static CompoundTag zaad(long seed) {
        CompoundTag t = new CompoundTag();
        t.putLong("Seed", seed);
        return t;
    }

    // =====================================================================================================================
    // the battle rules (no world needed)
    // =====================================================================================================================

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void guhmonRegels(GameTestHelper helper) {
        GuhmonGevecht g = new GuhmonGevecht();
        helper.assertTrue(g.slaap(IK) == 0 && g.slaap(HIJ) == GuhmonGevecht.START_TEGEN && g.uitkomst() == GuhmonGevecht.Uitkomst.BEZIG,
                "the start: 0 against a drowsy " + GuhmonGevecht.START_TEGEN);
        List<GuhmonGevecht.Regel> r = g.speel(V, V);
        helper.assertTrue(g.slaap(IK) == 25 && g.slaap(HIJ) == 55 && r.size() == 2 && r.get(0).sleutel().equals("vadsen") && r.get(0).wie() == IK
                && r.get(0).slaapSpeler() == 25 && r.get(0).slaapTegen() == 30 && r.get(1).wie() == HIJ, "Vadsen: +25 own sleep, the player first");
        g.speel(N, V);
        helper.assertTrue(g.slaap(IK) == 25 && g.slaap(HIJ) == 65, "Njeg: -15 for the other (55 - 15 + 25), got " + g.slaap(HIJ));
        // a Nap fails when the last thing the other did was Njeg
        g = new GuhmonGevecht();
        r = g.speel(D, N);
        helper.assertTrue(g.slaap(IK) == 25 && r.get(0).sleutel().equals("dutje") && g.dutjeMislukt(IK), "Dutje +40, then the other's Njeg -15");
        r = g.speel(D, V);
        helper.assertTrue(g.slaap(IK) == 25 && r.get(0).sleutel().equals("dutje_mislukt") && !g.dutjeMislukt(IK), "the Nap after a Njeg fails; the Njeg is used up");
        g.speel(D, V);
        helper.assertTrue(g.slaap(IK) == 65, "and the next Nap works again");
        // the player's Njeg makes the other's Nap fail in the same round (the Njegbadge)
        g = new GuhmonGevecht();
        r = g.speel(N, D);
        helper.assertTrue(g.slaap(HIJ) == 15 && r.get(1).sleutel().equals("dutje_mislukt") && g.njegBlokte(), "Njeg, then the other naps: it fails");
        helper.assertTrue(g.slaap(IK) == 0 && g.slaap(HIJ) >= 0, "nothing goes below zero");
        // the full belly: +10 now and +10 at the start of the own next two moves
        g = new GuhmonGevecht();
        g.speel(K, N);
        helper.assertTrue(g.slaap(IK) == 0 && g.maag(IK) == 2, "Knabbel +10 (and the other's Njeg -15 never goes below 0)");
        r = g.speel(V, V);
        helper.assertTrue(r.get(0).sleutel().equals("buikje") && g.slaap(IK) == 35 && g.maag(IK) == 1, "turn 1 after: +10, then the move");
        g.speel(V, V);
        helper.assertTrue(g.slaap(IK) == 70 && g.maag(IK) == 0, "turn 2 after: +10 again");
        r = g.speel(N, N);
        helper.assertTrue(!r.get(0).sleutel().equals("buikje") && g.slaap(IK) == 55, "and then the belly is empty");
        // whoever sleeps first wins: the player moves first, the other does not move any more
        g = new GuhmonGevecht();
        g.speel(D, N);
        g.speel(V, V);
        g.speel(D, N);
        helper.assertTrue(g.slaap(IK) == 75 && g.slaap(HIJ) == 55, "75 against 55 by now, got " + g.slaap(IK) + " / " + g.slaap(HIJ));
        r = g.speel(V, D);
        helper.assertTrue(g.uitkomst() == GuhmonGevecht.Uitkomst.GEWONNEN && r.get(r.size() - 1).sleutel().equals("gewonnen")
                && r.stream().noneMatch(x -> x.wie() == HIJ && !x.sleutel().equals("gewonnen") && !x.sleutel().equals("slaapt")),
                "at 100 the player's guh sleeps and wins; the other gets no move");
        helper.assertTrue(g.speel(V, V).isEmpty() && g.speel(D, RandomSource.create(1)).isEmpty(), "a finished battle takes no more moves");
        // losing: the other guh reaches 100 first
        g = new GuhmonGevecht();
        g.speel(N, V);
        g.speel(N, D);
        helper.assertTrue(g.slaap(HIJ) == 25 && g.njegBlokte(), "40 - 15, and the Nap failed");
        g.speel(K, D);
        r = g.speel(K, D);
        helper.assertTrue(r.get(1).sleutel().equals("knabbel_vol") && g.uitkomst() == GuhmonGevecht.Uitkomst.VERLOREN && r.get(r.size() - 1).sleutel().equals("verloren"), "the other sleeps first: lost");
        // the Knabbelbadge: asleep with a full belly (through it, or by a move while it is full)
        g = new GuhmonGevecht();
        g.speel(D, V);
        g.speel(D, V);
        g.speel(K, K);
        r = g.speel(V, V);
        helper.assertTrue(g.uitkomst() == GuhmonGevecht.Uitkomst.GEWONNEN && g.volBuikje() && r.get(0).sleutel().equals("buikje") && r.size() == 3,
                "90 + the full belly = asleep before the move");
        g = new GuhmonGevecht();
        g.speel(D, V);
        g.speel(D, V);
        helper.assertTrue(!g.volBuikje() && g.slaap(IK) == 80, "no belly yet");
        g.speel(V, V);
        helper.assertTrue(g.uitkomst() == GuhmonGevecht.Uitkomst.GEWONNEN && !g.volBuikje(), "a win without a snack is no Knabbelbadge");
        // the other guh's own choices: a snack first, and over many battles both outcomes happen
        int winst = 0, verlies = 0, alleenDutje = 0;
        for (int seed = 0; seed < 300; seed++) {
            RandomSource random = RandomSource.create(seed);
            GuhmonGevecht slim = new GuhmonGevecht();
            int ronden = 0;
            while (slim.uitkomst() == GuhmonGevecht.Uitkomst.BEZIG && ronden++ < 200) {
                List<GuhmonGevecht.Regel> regels = slim.speel(slim.dutjeMislukt(IK) ? V : D, random);
                if (ronden == 1) {
                    helper.assertTrue(regels.get(regels.size() - 1).sleutel().equals("knabbel"), "the other guh opens with a snack");
                }
            }
            if (slim.uitkomst() == GuhmonGevecht.Uitkomst.GEWONNEN) {
                winst++;
            } else {
                verlies++;
            }
            GuhmonGevecht dom = new GuhmonGevecht();
            ronden = 0;
            while (dom.uitkomst() == GuhmonGevecht.Uitkomst.BEZIG && ronden++ < 200) {
                dom.speel(D, random);
            }
            if (dom.uitkomst() == GuhmonGevecht.Uitkomst.GEWONNEN) {
                alleenDutje++;
            }
        }
        helper.assertTrue(winst >= 225 && verlies >= 5, "paying attention wins most battles, not all: " + winst + " of 300");
        helper.assertTrue(alleenDutje >= 90 && alleenDutje <= 220, "only napping wins about half: " + alleenDutje + " of 300");
        helper.succeed();
    }

    // =====================================================================================================================
    // a visit to the gym
    // =====================================================================================================================

    private static void vrijePlek(GameTestHelper helper, Arena a, Vec3 lokaal, String wat) {
        BlockPos voet = BlockPos.containing(a.wereld(lokaal));
        ServerLevel level = a.level();
        helper.assertTrue(!level.getBlockState(voet.below()).isAir() && level.getBlockState(voet).getCollisionShape(level, voet).isEmpty()
                && level.getBlockState(voet.above()).getCollisionShape(level, voet.above()).isEmpty(), wat + ": a floor and room to stand at " + voet.toShortString());
    }

    @GuhTest(template = GROOT, batch = BATCH, timeoutTicks = 300)
    public static void guhmonBezoek(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        p.getInventory().setItem(3, new ItemStack(Blocks.COBBLESTONE, 7));
        Sessie sessie = Sessies.startOp(level, helper.absolutePos(new BlockPos(2, 2, 2)), Guhmon.SPEL, List.of(p), zaad(4));
        helper.assertTrue(sessie instanceof GuhmonSessie, "the gym opens");
        GuhmonSessie s = (GuhmonSessie) sessie;
        Arena a = s.arena();
        for (Vec3 plek : List.of(Guhmon.START, Guhmon.VAK, Guhmon.MIJN, Guhmon.TEGEN, Guhmon.LEIDER, Guhmon.TEGEN_WACHT)) {
            vrijePlek(helper, a, plek, "the gym");
        }
        for (Vec3 plek : Guhmon.PUBLIEK) {
            vrijePlek(helper, a, plek, "the audience");
        }
        List<StandInGuh> guhs = level.getEntitiesOfClass(StandInGuh.class, a.doos());
        helper.assertTrue(guhs.size() == 7 && guhs.stream().filter(StandInGuh::slaapt).count() == 6, "Snurkel and six sleeping guhs in the stands: " + guhs.size());
        helper.assertTrue(s.leider() != null && s.leider().getKind() == GuhNpcEntity.Kind.GUHMON_GYMLEIDER
                && level.getEntitiesOfClass(GuhEntity.class, a.doos(), e -> e.getType() == ModEntities.GUH.get()).isEmpty(), "the gym leader; never a real guh");
        helper.assertTrue(Grappen.stap(p, Guhmon.ID) == 1 && s.fase() == GuhmonSessie.Fase.AANKOMST && p.getInventory().getItem(3).isEmpty(), "step 1, own things in the safe");
        // nothing works out of order
        helper.assertTrue(s.zet(p, 0).isEmpty() && !s.kies(p, Guhmon.LEENGUH) && !s.verder(p) && !s.opnieuw(p), "no move, pick or badge before the challenge");
        Guhmon.ROL.talk(s.leider(), p);
        helper.assertTrue(s.fase() == GuhmonSessie.Fase.KIEZEN && Grappen.stap(p, Guhmon.ID) == 2, "a click on the gym leader: pick a guh");
        ListTag lijst = s.kiesStand(p).getListOrEmpty("Guhs");
        helper.assertTrue(lijst.size() == 1 && Guhmon.LEENGUH_ID.equals(lijst.getCompoundOrEmpty(0).read("Id", UUIDUtil.CODEC).orElse(null)),
                "no tamed guhs: the leenguh is the only choice");
        helper.assertTrue(!s.kies(p, UUID.randomUUID().toString()) && !s.kies(p, "njeg") && s.mijn() == null, "a guh that is not yours is refused");
        helper.assertTrue(s.kies(p, Guhmon.LEENGUH) && s.leenguh() && s.mijn() != null && s.fase() == GuhmonSessie.Fase.GEVECHT
                && Grappen.stap(p, Guhmon.ID) == 3, "the leenguh steps forward, the battle begins");
        helper.assertTrue(s.mijn().position().distanceTo(a.wereld(Guhmon.MIJN)) < 0.1 && !s.kies(p, Guhmon.LEENGUH), "on its spot; no second pick");
        helper.assertTrue(s.zet(p, 7).isEmpty() && s.zet(p, -1).isEmpty() && !s.verder(p), "a move that does not exist, a badge before the win");
        CompoundTag stand = s.stand(true, List.of());
        helper.assertTrue(stand.getIntOr("SlaapTegen", 0) == GuhmonGevecht.START_TEGEN && stand.getIntOr("Uitkomst", 9) == 0 && stand.contains("TegenLooks"),
                "what the battle screen gets");
        int ronden = Grap2Slice.speelUit(s, p);
        helper.assertTrue(s.gevecht().uitkomst() == GuhmonGevecht.Uitkomst.GEWONNEN && ronden >= 2 && s.mijn().slaapt(), "won after " + ronden + " rounds: your guh sleeps");
        helper.assertTrue(s.zet(p, 0).isEmpty() && !s.opnieuw(p) && !Grappen.isKlaar(p, Guhmon.ID), "the battle is over; nothing is paid inside the game");
        helper.assertTrue(s.verder(p) && !s.verder(p) && s.fase() == GuhmonSessie.Fase.GEWONNEN && Grappen.stap(p, Guhmon.ID) == 4, "the badge speech, once");
        helper.runAfterDelay(GuhmonSessie.UITLOOP + 5, () -> {
            helper.assertTrue(Sessies.van(p) == null && s.isGestopt(), "the visit ended by itself");
            helper.assertTrue(p.getInventory().getItem(3).is(Blocks.COBBLESTONE.asItem()) && p.getInventory().getItem(3).getCount() == 7, "own things back");
            helper.assertTrue(Grappen.isKlaar(p, Guhmon.ID) && Muntjes.saldo(p) == 100 && Films.heeft(p, Guhmon.ID) && Guhmon.gewonnen(p) == 1,
                    "the first win: 100 muntjes, the film");
            helper.assertTrue(Guhmon.heeft(p, Guhmon.Badge.DUTJES) && tel(p, Grap2Slice.BADGEDOOS_ITEM.get()) == 1 && tel(p, Grap2Slice.BADGE_DUTJES.get()) == 1
                    && KledingUnlocks.heeft(p, GuhClothes.GUHMON_TRAINERSPET), "the keepsakes: the case, the Dutjesbadge, the cap");
            helper.assertTrue(level.getEntitiesOfClass(StandInGuh.class, a.doos()).isEmpty() && level.getEntitiesOfClass(Display.class, a.doos()).isEmpty(),
                    "the gym is tidied up");
            // "kwijt, njeg": only what the player does not carry
            Guhmon.kwijt(p);
            helper.assertTrue(tel(p, Grap2Slice.BADGEDOOS_ITEM.get()) == 1 && tel(p, Grap2Slice.BADGE_DUTJES.get()) == 1, "nothing lost: nothing given");
            p.getInventory().clearContent();
            Guhmon.kwijt(p);
            helper.assertTrue(tel(p, Grap2Slice.BADGEDOOS_ITEM.get()) == 1 && tel(p, Grap2Slice.BADGE_DUTJES.get()) == 1
                    && tel(p, Grap2Slice.BADGE_NJEG.get()) == 0, "lost: the case and the earned badge again, no badge that was not earned");
            // a replay pays nothing and gives no second case
            GuhmonSessie weer = (GuhmonSessie) Sessies.startOp(level, helper.absolutePos(new BlockPos(2, 2, 2)), Guhmon.SPEL, List.of(p), zaad(9));
            helper.assertTrue(weer != null, "again");
            Grap2Slice.speelUit(weer, p);
            helper.assertTrue(weer.verder(p), "won again");
            weer.klaar(p);
            helper.assertTrue(Muntjes.saldo(p) == 100 && Grappen.keren(p, Guhmon.ID) == 2 && Guhmon.gewonnen(p) == 2 && tel(p, Grap2Slice.BADGEDOOS_ITEM.get()) == 1
                    && tel(p, Grap2Slice.BADGE_DUTJES.get()) == 1, "a replay: no muntjes, no second case, no second badge");
            PxTest.klaar(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = GROOT, batch = BATCH)
    public static void guhmonEigenGuhEnBadges(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper), ander = PxTest.speler(helper);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(40, 2, 40));
        guh.tame(p);
        guh.setCustomName(Component.literal("Vadsje"));
        GuhVolger.zet(guh, PlekSoort.WERELD, "");
        GuhEntity vreemd = helper.spawn(ModEntities.GUH.get(), new BlockPos(42, 2, 40));
        vreemd.tame(ander);
        GuhVolger.zet(vreemd, PlekSoort.WERELD, "");
        Vec3 thuis = guh.position();
        GuhmonSessie s = (GuhmonSessie) Sessies.startOp(level, helper.absolutePos(new BlockPos(2, 2, 2)), Guhmon.SPEL, List.of(p), zaad(2));
        helper.assertTrue(s != null, "the gym opens");
        s.daagUit(p);
        ListTag lijst = s.kiesStand(p).getListOrEmpty("Guhs");
        helper.assertTrue(lijst.size() == 2 && guh.getUUID().equals(lijst.getCompoundOrEmpty(0).read("Id", UUIDUtil.CODEC).orElse(null))
                && lijst.getCompoundOrEmpty(1).getBooleanOr("Leenguh", false), "the picker: the own guh, then the leenguh");
        helper.assertTrue(!s.kies(p, vreemd.getUUID().toString()), "somebody else's guh is refused");
        helper.assertTrue(s.kies(p, guh.getUUID().toString()) && !s.leenguh() && s.mijn() != null && s.mijn().getName().getString().equals("Vadsje"),
                "a copy of Vadsje stands in the gym");
        helper.assertTrue(guh.isAlive() && guh.position().distanceTo(thuis) < 0.5 && s.mijn().getUUID() != guh.getUUID()
                && s.mijn().getType() != ModEntities.GUH.get(), "the real guh stays home: the copy is a stand-in");
        // lose on purpose (only Njeg against a guh that keeps chonking), then try again: losing costs nothing
        int ronden = 0;
        while (s.gevecht().uitkomst() == GuhmonGevecht.Uitkomst.BEZIG && ronden++ < 40) {
            s.zet(p, N, V);
        }
        helper.assertTrue(s.gevecht().uitkomst() == GuhmonGevecht.Uitkomst.VERLOREN && Guhmon.verloren(p) == 1 && !s.verder(p), "lost: no badge");
        helper.assertTrue(s.opnieuw(p) && s.gevecht().uitkomst() == GuhmonGevecht.Uitkomst.BEZIG && s.gevecht().ronde() == 0 && !s.mijn().slaapt(),
                "again: a fresh battle with the same guh");
        Grap2Slice.speelUit(s, p);
        helper.assertTrue(s.verder(p), "won");
        s.klaar(p);
        helper.assertTrue(Grappen.isKlaar(p, Guhmon.ID) && Guhmon.heeft(p, Guhmon.Badge.DUTJES) && Muntjes.saldo(p) == 100 && Muntjes.saldo(ander) == 0,
                "the win counts for this player only");
        // the other two badges: only the first time each, and all three grant the hidden advancement's flag
        helper.assertTrue(!Guhmon.verdien(p, Guhmon.Badge.DUTJES) && Guhmon.verdien(p, Guhmon.Badge.NJEG) && !Guhmon.verdien(p, Guhmon.Badge.NJEG)
                && Guhmon.verdien(p, Guhmon.Badge.KNABBEL) && Guhmon.badges(p) == 7 && tel(p, Grap2Slice.BADGE_NJEG.get()) == 1, "each badge once");
        helper.assertTrue(Guhmon.badges(ander) == 0 && !Grappen.isKlaar(ander, Guhmon.ID), "the other player has nothing");
        ListTag rijen = GidsBlad.stand(p).getListOrEmpty("Rijen");
        helper.assertTrue(rijen.stream().filter(r -> ((CompoundTag) r).getStringOr("T", "").equals(GidsBlad.PLAATJE)).count() >= 3
                && rijen.stream().filter(r -> ((CompoundTag) r).getStringOr("T", "").equals(GidsBlad.STAP)).count() >= 9, "the Guhdex: both games and the badges");
        guh.discard();
        vreemd.discard();
        PxTest.klaar(helper, p, ander);
        helper.succeed();
    }

    // =====================================================================================================================
    // an episode of Boer zoekt Guh
    // =====================================================================================================================

    @GuhTest(template = GROOT, batch = BATCH, timeoutTicks = 300)
    public static void bzgAflevering(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        p.getInventory().setItem(0, new ItemStack(Blocks.DIRT, 3));
        BzgSessie s = (BzgSessie) Sessies.startOp(level, helper.absolutePos(new BlockPos(2, 2, 2)), Bzg.SPEL, List.of(p), new CompoundTag());
        helper.assertTrue(s != null, "the studio opens");
        Arena a = s.arena();
        for (Vec3 plek : List.of(Bzg.START, Bzg.GUHVON, Bzg.BOER)) {
            vrijePlek(helper, a, plek, "the studio");
        }
        for (Vec3[] rij : List.of(Bzg.BANK, Bzg.HOOI, Bzg.SAMEN)) {
            for (Vec3 plek : rij) {
                vrijePlek(helper, a, plek, "a candidate's spot");
            }
        }
        BlockPos bus = a.wereld(Bzg.BRIEVENBUS.getX(), Bzg.BRIEVENBUS.getY(), Bzg.BRIEVENBUS.getZ());
        BlockState busState = level.getBlockState(bus);
        helper.assertTrue(busState.is(Grap2Slice.BRIEVENBUS.get()) && s.magGebruiken(p, bus, busState)
                && !s.magGebruiken(p, bus.below(), level.getBlockState(bus.below())), "the mailbox is the one block the show lets you use");
        helper.assertTrue(s.kandidaten().size() == 3 && s.guhvon() != null && s.boer() != null && s.kandidaten().stream().noneMatch(StandInGuh::slaapt)
                && s.kandidaten().get(0).getName().getString().length() > 0, "Guhvon, Boer Guhrrit and three candidates on the sofa");
        helper.assertTrue(s.fase() == BzgSessie.Fase.INTRO && Grappen.stap(p, Bzg.ID) == 1, "step 1");
        // out of order: nothing counts
        Bzg.doe(p, Grap2Payloads.GELEZEN, 0);
        s.klikGuh(p, s.kandidaten().get(0));
        s.praatBoer(p);
        helper.assertTrue(s.gelezenMasker() == 0 && s.ingestoptMasker() == 0 && !s.keuze(p, 1) && !s.aftitelingKlaar(p) && s.fase() == BzgSessie.Fase.INTRO,
                "letters, tucking in and choosing do nothing before the intro");
        // step 1: the intro (the talking screen reports "read to the end")
        Bzg.GUHVON_ROL.talk(s.guhvon(), p);
        helper.assertTrue(BzgSessie.SCENE_INTRO.equals(Praat.lopend(p)), "Guhvon's intro is on screen");
        Praat.antwoord(p, s.guhvon(), -1);
        helper.assertTrue(s.fase() == BzgSessie.Fase.BRIEVEN && Grappen.stap(p, Bzg.ID) == 2, "step 2: the letters");
        // step 2: all three letters, in any order; the logeerweek starts when the letters are put away
        busState.useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(bus), Direction.UP, bus, false));
        helper.assertTrue(s.brievenStand().getBooleanOr("Telt", false), "the mailbox gives the letters");
        Bzg.doe(p, Grap2Payloads.GELEZEN, 2);
        Bzg.doe(p, Grap2Payloads.GELEZEN, 7);
        Bzg.doe(p, Grap2Payloads.GELEZEN, -1);
        Bzg.doe(p, Grap2Payloads.BRIEVEN_DICHT, 0);
        helper.assertTrue(s.gelezenMasker() == 4 && s.fase() == BzgSessie.Fase.BRIEVEN, "one letter is not enough; a letter that does not exist is ignored");
        Bzg.doe(p, Grap2Payloads.GELEZEN, 0);
        Bzg.doe(p, Grap2Payloads.GELEZEN, 1);
        helper.assertTrue(s.gelezenMasker() == BzgSessie.ALLE_BRIEVEN && s.fase() == BzgSessie.Fase.BRIEVEN, "all read, the screen still open");
        Bzg.doe(p, Grap2Payloads.BRIEVEN_DICHT, 0);
        helper.assertTrue(s.fase() == BzgSessie.Fase.LOGEREN && Grappen.stap(p, Bzg.ID) == 3 && s.kandidaten().stream().allMatch(StandInGuh::slaapt)
                && s.kandidaten().get(1).position().distanceTo(a.wereld(Bzg.HOOI[1])) < 0.1, "step 3: everybody sleeps in the hay");
        // step 3: tuck in all four, each once
        s.praatBoer(p);
        s.praatBoer(p);
        helper.assertTrue(s.ingestoptMasker() == BzgSessie.BOER_BIT && Bzg.ingestopt(p) == 1, "the farmer is tucked in once");
        helper.assertTrue(!s.keuze(p, 0) && s.fase() == BzgSessie.Fase.LOGEREN, "no choice while they sleep");
        for (StandInGuh g : s.kandidaten()) {
            Grap2Slice.klik(g, p);
        }
        helper.assertTrue(s.ingestoptMasker() == BzgSessie.ALLEN_INGESTOPT && Bzg.ingestopt(p) == 4 && s.fase() == BzgSessie.Fase.KEUZE
                && Grappen.stap(p, Bzg.ID) == 4 && level.getEntitiesOfClass(Display.BlockDisplay.class, a.doos()).size() == 4, "four blankets: step 4, the choice");
        // step 4: whatever the advice, he cannot choose
        helper.assertTrue(!s.keuze(p, 9) && !s.keuze(p, -1), "an answer that does not exist");
        Bzg.BOER_ROL.talk(s.boer(), p);
        helper.assertTrue(BzgSessie.SCENE_KEUZE.equals(Praat.lopend(p)), "the farmer asks");
        Praat.antwoord(p, s.boer(), 1);
        helper.assertTrue(s.fase() == BzgSessie.Fase.SAMEN && s.keuze() == 1 && BzgSessie.SCENE_SAMEN.equals(Praat.lopend(p)) && !s.keuze(p, 2),
                "he cannot choose; the last scene plays, one answer only");
        helper.assertTrue(!Grappen.isKlaar(p, Bzg.ID), "not done before the scene is read");
        Praat.antwoord(p, s.boer(), -1);
        helper.assertTrue(s.fase() == BzgSessie.Fase.AFTITELING && Grappen.stap(p, Bzg.ID) == 5 && s.kandidaten().stream().allMatch(StandInGuh::slaapt)
                && s.kandidaten().get(0).position().distanceTo(a.wereld(Bzg.SAMEN[0])) < 0.1
                && s.kandidaten().stream().allMatch(g -> g.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.BZG_STROHOED), "step 5: they nap together, in straw hats");
        helper.assertTrue(!s.aftitelingKlaar(p) && Sessies.van(p) == s, "the credits cannot be skipped in the first two seconds");
        helper.runAfterDelay(BzgSessie.AFTITELING_MIN + 5, () -> {
            Bzg.doe(p, Grap2Payloads.AFTITELING_KLAAR, 0);
            helper.assertTrue(Sessies.van(p) == null && s.isGestopt(), "the credits are over: back to the lobby");
            helper.assertTrue(p.getInventory().getItem(0).is(Blocks.DIRT.asItem()) && p.getInventory().getItem(0).getCount() == 3, "own things back");
            helper.assertTrue(Grappen.isKlaar(p, Bzg.ID) && Muntjes.saldo(p) == 100 && Films.heeft(p, Bzg.ID) && tel(p, Grap2Slice.INGELIJSTE_BRIEF_ITEM.get()) == 1
                    && KledingUnlocks.heeft(p, GuhClothes.BZG_STROHOED) && KledingUnlocks.heeft(p, GuhClothes.BZG_OVERALL), "100 muntjes, the framed letter, the outfit");
            helper.assertTrue(level.getEntitiesOfClass(StandInGuh.class, a.doos()).isEmpty() && level.getEntitiesOfClass(GuhNpcEntity.class, a.doos()).isEmpty()
                    && level.getEntitiesOfClass(Display.class, a.doos()).isEmpty(), "the studio is tidied up");
            // a replay (walked through with the dev helper): no muntjes, no second letter
            p.getInventory().clearContent();
            BzgSessie weer = (BzgSessie) Sessies.startOp(level, helper.absolutePos(new BlockPos(2, 2, 2)), Bzg.SPEL, List.of(p), new CompoundTag());
            helper.assertTrue(weer != null, "again");
            Grap2Slice.spoelDoor(weer, p, 5);
            helper.assertTrue(weer.fase() == BzgSessie.Fase.SAMEN && weer.keuze() == 3, "walked on to the last scene");
            weer.samenKlaar(p);
            weer.klaar(p);
            helper.assertTrue(Muntjes.saldo(p) == 100 && Grappen.keren(p, Bzg.ID) == 2 && tel(p, Grap2Slice.INGELIJSTE_BRIEF_ITEM.get()) == 0, "a replay pays nothing");
            Bzg.kwijt(p);
            Bzg.kwijt(p);
            helper.assertTrue(tel(p, Grap2Slice.INGELIJSTE_BRIEF_ITEM.get()) == 1, "kwijt: one framed letter again, never two");
            PxTest.klaar(helper, p);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // unlimited players: everybody their own arena and their own progress
    // =====================================================================================================================

    @GuhTest(template = REUS, batch = BATCH)
    public static void tweeSpelersTegelijk(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer a = PxTest.speler(helper), b = PxTest.speler(helper);
        GuhmonSessie ga = (GuhmonSessie) Sessies.startOp(level, helper.absolutePos(new BlockPos(2, 2, 2)), Guhmon.SPEL, List.of(a), zaad(1));
        GuhmonSessie gb = (GuhmonSessie) Sessies.startOp(level, helper.absolutePos(new BlockPos(50, 2, 2)), Guhmon.SPEL, List.of(b), zaad(1));
        helper.assertTrue(ga != null && gb != null && ga.arena() != gb.arena() && Sessies.aantalSpelers(Guhmon.ID) >= 2, "two gyms at once");   // (other tests of the batch run gyms too)
        helper.assertTrue(Sessies.weigering(Guhmon.SPEL, List.of(a, b)) != null, "one player per gym");
        Grap2Slice.speelUit(ga, a);
        helper.assertTrue(ga.gevecht().uitkomst() == GuhmonGevecht.Uitkomst.GEWONNEN && gb.fase() == GuhmonSessie.Fase.AANKOMST && gb.gevecht() == null
                && gb.mijn() == null, "a battle in one gym leaves the other gym alone");
        helper.assertTrue(gb.zet(a, 0).isEmpty() && !gb.kies(a, Guhmon.LEENGUH) && !ga.verder(b), "nobody plays in somebody else's gym");
        Grap2Slice.klik(ga.mijn(), b);
        helper.assertTrue(gb.fase() == GuhmonSessie.Fase.AANKOMST, "a click on another gym's guh does nothing for you");
        helper.assertTrue(ga.verder(a), "a's badge");
        ga.klaar(a);
        helper.assertTrue(Grappen.isKlaar(a, Guhmon.ID) && !Grappen.isKlaar(b, Guhmon.ID) && Sessies.van(b) == gb && !gb.isGestopt(), "a is done, b plays on");
        gb.klaar(b);
        helper.assertTrue(!Grappen.isKlaar(b, Guhmon.ID) && Muntjes.saldo(b) == 0, "leaving without a win gives nothing");
        // the same for the show
        BzgSessie ba = (BzgSessie) Sessies.startOp(level, helper.absolutePos(new BlockPos(2, 2, 46)), Bzg.SPEL, List.of(a), new CompoundTag());
        BzgSessie bb = (BzgSessie) Sessies.startOp(level, helper.absolutePos(new BlockPos(50, 2, 46)), Bzg.SPEL, List.of(b), new CompoundTag());
        helper.assertTrue(ba != null && bb != null && Sessies.aantalSpelers(Bzg.ID) >= 2, "two studios at once");
        Grap2Slice.spoelDoor(ba, a, 4);
        helper.assertTrue(ba.fase() == BzgSessie.Fase.KEUZE && bb.fase() == BzgSessie.Fase.INTRO && bb.gelezenMasker() == 0 && bb.ingestoptMasker() == 0,
                "one show runs on, the other still waits for its intro");
        ba.klikGuh(b, ba.kandidaten().get(0));
        bb.klikGuh(a, bb.kandidaten().get(0));
        Grap2Slice.klik(ba.kandidaten().get(0), b);
        helper.assertTrue(bb.ingestoptMasker() == 0 && Bzg.ingestopt(b) == 0, "nobody tucks in another show's guhs");
        ba.stop();
        bb.stop();
        helper.assertTrue(!Grappen.isKlaar(a, Bzg.ID) && !Grappen.isKlaar(b, Bzg.ID) && Sessies.van(a) == null, "a stopped show pays nothing");
        PxTest.klaar(helper, a, b);
        helper.succeed();
    }

    // =====================================================================================================================
    // templates and blocks
    // =====================================================================================================================

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void templatesKloppen(GameTestHelper helper) {
        StructureTemplate gym = helper.getLevel().getStructureManager().get(Guhmon.ARENA.template()).orElse(null);
        StructureTemplate studio = helper.getLevel().getStructureManager().get(Bzg.ARENA.template()).orElse(null);
        helper.assertTrue(gym != null && gym.getSize().equals(Guhmon.MAAT), "the gym template is " + Guhmon.MAAT.toShortString());
        helper.assertTrue(studio != null && studio.getSize().equals(Bzg.MAAT), "the studio template is " + Bzg.MAAT.toShortString());
        List<StructureTemplate.StructureBlockInfo> bussen = studio.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Grap2Slice.BRIEVENBUS.get());
        helper.assertTrue(bussen.size() == 1 && bussen.get(0).pos().equals(Bzg.BRIEVENBUS), "one mailbox, where Bzg.BRIEVENBUS says");
        for (Block nooit : List.of(Blocks.WATER, Blocks.LAVA, Blocks.SAND, Blocks.GRAVEL)) {
            helper.assertTrue(gym.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), nooit).isEmpty()
                    && studio.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), nooit).isEmpty(), "nothing that flows or falls: " + nooit);
        }
        helper.assertTrue(Guhmon.ARENA.startLokaal().equals(Guhmon.START) && Bzg.ARENA.startLokaal().equals(Bzg.START)
                && Sessies.soort(Guhmon.ID) == Guhmon.SPEL && Sessies.soort(Bzg.ID) == Bzg.SPEL && Grappen.van(Guhmon.ID).stappen() == 4
                && Grappen.van(Bzg.ID).stappen() == 5, "both games are registered: 4 and 5 steps");
        helper.succeed();
    }

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void badgedoosEnBrievenbus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        BlockPos rel = new BlockPos(4, 2, 4), pos = helper.absolutePos(rel);
        helper.setBlock(rel, Grap2Slice.BADGEDOOS.get());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        helper.assertTrue(BadgeDoosBlock.aantal(level.getBlockState(pos)) == 0, "an empty case");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Grap2Slice.BADGE_NJEG.get()));
        level.getBlockState(pos).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(level.getBlockState(pos).getValue(BadgeDoosBlock.NJEG) && !level.getBlockState(pos).getValue(BadgeDoosBlock.DUTJES)
                && p.getMainHandItem().isEmpty(), "the Njegbadge goes into the case");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Grap2Slice.BADGE_NJEG.get()));
        level.getBlockState(pos).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(p.getMainHandItem().getCount() == 1 && BadgeDoosBlock.aantal(level.getBlockState(pos)) == 1, "a second one of the same stays in the hand");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Grap2Slice.BADGE_DUTJES.get()));
        level.getBlockState(pos).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, hit);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        level.getBlockState(pos).useWithoutItem(level, p, hit);
        helper.assertTrue(BadgeDoosBlock.aantal(level.getBlockState(pos)) == 2, "two badges in the case");
        List<ItemStack> buit = Block.getDrops(level.getBlockState(pos), level, pos, null);
        helper.assertTrue(buit.size() == 3 && buit.stream().anyMatch(s -> s.is(Grap2Slice.BADGEDOOS_ITEM.get()))
                && buit.stream().anyMatch(s -> s.is(Grap2Slice.BADGE_NJEG.get())) && buit.stream().anyMatch(s -> s.is(Grap2Slice.BADGE_DUTJES.get()))
                && buit.stream().noneMatch(s -> s.is(Grap2Slice.BADGE_KNABBEL.get())), "breaking it gives the case and its badges back: " + buit);
        // a mailbox at home: no show, no letters, no crash; a framed letter needs a wall
        BlockPos busRel = new BlockPos(8, 2, 8), bus = helper.absolutePos(busRel);
        helper.setBlock(busRel, Grap2Slice.BRIEVENBUS.get());
        level.getBlockState(bus).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(bus), Direction.UP, bus, false));
        helper.assertTrue(Sessies.van(p) == null, "a mailbox at home starts nothing");
        helper.assertTrue(!Grap2Slice.INGELIJSTE_BRIEF.get().defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(12, 3, 12))),
                "a framed letter does not hang in the air");
        helper.setBlock(new BlockPos(12, 3, 13), Blocks.STONE);
        helper.assertTrue(Grap2Slice.INGELIJSTE_BRIEF.get().defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(12, 3, 12))),
                "it hangs on a wall (facing north: the wall is to the south)");
        Bzg.leesIngelijst(p);
        PxTest.klaar(helper, p);
        helper.succeed();
    }
}
