package nl.juiced.guhs.gametest;

import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.barbecuether.GrillPortalBlock;
import nl.juiced.guhs.feature.barbecuether.Grillguh;
import nl.juiced.guhs.feature.ring.Cast;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingBeloning;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.ring.Smikagol;
import nl.juiced.guhs.feature.ring.SmikagolEntity;
import nl.juiced.guhs.feature.ringh1.Feest;
import nl.juiced.guhs.feature.ringh1.Gouw;
import nl.juiced.guhs.feature.ringh4.RingH4Feature;
import nl.juiced.guhs.feature.ringh5.RingH5Feature;
import nl.juiced.guhs.feature.ringh6.RingH6Feature;
import nl.juiced.guhs.feature.ringh6.Thuis;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.ModDimensions;

/**
 * bbq2 phase 3 (PHASE3 R13): the Knabbelring as ONE chain. Every chapter has its own tests, each starting from "the chapter
 * before is set done"; what none of them shows is that the chapters fit: that what chapter 1 hands over is what chapter 2
 * finds, and so on to the feast. Here one mock player goes from "never heard of it" to the daily party in one world, and
 * then a second player who starts when the first is done gets exactly the same.
 * <ul>
 *   <li>Chapter 1 is played for real, click by click, with the props a camp has (Guhdalf with the role of the Gouw, a
 *       Sam-guh at home, a grill portal): the gate (the Grillguh first), the card and both scenes, the chores, the ring,
 *       Sam-guh joins, the provisions, the walk to the portal. Its last step opens the portal lock and the next sluier.</li>
 *   <li>Chapters 2 to 6 are walked by their steps (their rooms are their own tests' business), and at every hand-over the
 *       thing itself is asked: the ring is still carried and nothing took it; Sam-guh still walks along; each chapter's
 *       structure opens exactly when the one before is done; Guhladriel herself hands out the three gifts (her real role)
 *       and they are still there in chapters 5 and 6; Smikagol the guide of chapter 5 is at the player's side in 6; the
 *       real end of chapter 6 ({@code Finale.naFrituur}, {@code Thuis.breng}, {@code Thuis.klaar}: called by name, so a
 *       rename breaks this test and not the story) takes the ring, leaves a piece of it, flies the player home, and "home"
 *       is the camp where they met Guhdalf in chapter 1 (the keys {@code guhs_ringh1_thuis*} that chapter 6 reads by
 *       name); the rewards of the whole story come once; the daily feast gives one treat a day.</li>
 * </ul>
 * Template ringh1_test_kamer (25 x 12 x 25, a lawn; you stand on helper y 3).
 */
public final class RingKetenGameTests {
    private static final String KAMER = "ringh1_test_kamer";

    private static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(x, 3, z));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setOnGround(true);
        // (the cards of the later chapters are their own tests' business: here nothing may pop up half-way)
        for (String kaart : Verteller.ids()) {
            if (kaart.startsWith("ring_h") && !kaart.equals("ring_h1")) {
                GuhQuests.saved(p).putBoolean("guhs_kaart_" + kaart, true);
            }
        }
        return p;
    }

    /**
     * One tick of this player's game: every chapter's own upkeep runs, as on a real server. The player's clock is set by
     * the test, one tick at a time (whatever the test server does to a mock player's own counter): the once-a-second jobs
     * of the chapters hang on it, each at its own moment of the second.
     */
    private static void tik(ServerPlayer p, int tijd) {
        p.tickCount = tijd;
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
    }

    /** A package-private step of a chapter, called by its name. */
    private static Object roep(String klasse, String methode, Class<?>[] soorten, Object... args) {
        try {
            Method m = Class.forName(klasse).getDeclaredMethod(methode, soorten);
            m.setAccessible(true);
            return m.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("the chain test calls " + klasse + "." + methode + " by name: it moved or changed", e);
        }
    }

    private static int tel(ServerPlayer p, net.minecraft.world.item.Item item) {
        return GuhQuests.count(p, item);
    }

    @GuhTest(template = KAMER, batch = "ringketen", timeoutTicks = 1200)
    public static void ringKetenVanGouwTotFeest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Ring.OVERAL = true;
        Vec3 bijGuhdalf = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(12, 3, 6)));
        GuhNpcEntity guhdalf = Cast.zet(level, GuhNpcEntity.Kind.GUHDALF, bijGuhdalf, 0f, Gouw.ROL);
        GuhNpcEntity guhladriel = Cast.zet(level, GuhNpcEntity.Kind.GUHLADRIEL, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(20, 3, 6))), 0f, RingH4Feature.STAD);
        GuhEntity samThuis = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        BlockPos samPlek = helper.absolutePos(new BlockPos(8, 3, 9));
        samThuis.setVariant(GuhVariant.SAM_GUH);
        samThuis.snapTo(samPlek.getX() + 0.5, samPlek.getY(), samPlek.getZ() + 0.5);
        VerhaalGuhs.markeer(samThuis, VerhaalGuh.SAM_GUH, samPlek);
        samThuis.getPersistentData().putString(Bezetting.TAG, Gouw.SAM_KAMP);
        level.addFreshEntity(samThuis);
        helper.assertTrue(guhdalf != null && guhladriel != null && Gouw.isSamThuis(samThuis) && NpcRollen.van(guhdalf) != null && NpcRollen.van(guhladriel) != null,
                "the props: Guhdalf with the role of the Gouw, Guhladriel with hers, a Sam-guh at home");
        BlockPos kist = helper.absolutePos(new BlockPos(14, 3, 6)), tafel = helper.absolutePos(new BlockPos(9, 3, 7)), krat = helper.absolutePos(new BlockPos(15, 3, 7));
        BlockPos portaal = helper.absolutePos(new BlockPos(20, 3, 20)), thuis = helper.absolutePos(new BlockPos(6, 3, 18));
        Thuis.testLevel = level;
        ServerPlayer eerste = speler(helper, 12, 10), tweede = speler(helper, 13, 10);
        nl.juiced.guhs.feature.guhpad.GuhpadGameTests.guhmensieGedaan(eerste);   // (guhpad: both followed the stories of the Guhmensie; that lock has its own tests)
        nl.juiced.guhs.feature.guhpad.GuhpadGameTests.guhmensieGedaan(tweede);
        ServerPlayer[] aanDeBeurt = {eerste};
        int[] fase = {0}, wacht = {0, 0}, klok = {0};
        helper.onEachTick(() -> {
            ServerPlayer p = aanDeBeurt[0];
            if (p == null) {
                return;
            }
            tik(p, ++klok[0]);
            Verhaallijn h1 = Ring.lijn(1);
            // (a phase that waits for the game takes a second or two; one that waits much longer says where it hangs)
            wacht[1] = wacht[0] == fase[0] ? wacht[1] + 1 : 0;
            wacht[0] = fase[0];
            helper.assertTrue(wacht[1] < 200, "the chain hangs: player " + (p == eerste ? 1 : 2) + ", phase " + fase[0] + ", step " + h1.stap(p) + " of chapter 1, begun "
                    + Ring.begonnen(p) + ", may begin " + Ring.magBeginnen(p) + ", watching " + Cutscenes.bezig(p) + ", ring " + Ring.heeft(p) + ", Sam-guh "
                    + (Sam.van(p) != null) + ", at " + p.blockPosition().subtract(helper.absolutePos(BlockPos.ZERO)).toShortString());
            switch (fase[0]) {
                case 0 -> {
                    // --- the gate: nothing starts, nothing opens, until the Grillguh's barbecue burns --------------------------
                    helper.assertTrue(!Ring.magBeginnen(p) && !Ring.begonnen(p) && !Ring.magDoorPortaal(p), "a new player: no story, no portal");
                    NpcRollen.van(guhdalf).talk(guhdalf, p);
                    helper.assertTrue(!h1.begonnen(p) && !Cutscenes.bezig(p), "Guhdalf sends them to the Grillguh first");
                    Grillguh.setStep(p, Grillguh.DONE);
                    fase[0] = 1;
                }
                case 1 -> {
                    if (Ring.begonnen(p)) {                               // (the story starts by itself within a second)
                        NpcRollen.van(guhdalf).talk(guhdalf, p);
                        helper.assertTrue(Cutscenes.bezig(p) && h1.stap(p) == 0, "the narrator card of chapter 1");
                        fase[0] = 2;
                    }
                }
                case 2 -> {
                    if (h1.stap(p) == 1 && !Cutscenes.bezig(p)) {         // (the card, then the scene of the arrival)
                        // what chapter 6 will read by name: this camp is home
                        Object gouw = roep("nl.juiced.guhs.feature.ringh6.Thuis", "gouw", new Class<?>[]{ServerPlayer.class, ServerLevel.class}, p, level);
                        helper.assertTrue(guhdalf.blockPosition().equals(gouw), "chapter 6 finds the camp of chapter 1 (the keys guhs_ringh1_thuis*): " + gouw);
                        Feest.vuurwerk(p, kist);
                        Feest.tafel(p, tafel);
                        for (VerhaalGuhs.Klik k : Sam.BIJ_KLIK) {
                            k.klik(samThuis, p, InteractionHand.MAIN_HAND);
                        }
                        helper.assertTrue(h1.stap(p) == 2, "the three chores: " + h1.stap(p));
                        NpcRollen.van(guhdalf).talk(guhdalf, p);
                        helper.assertTrue(Cutscenes.bezig(p) && !Ring.heeft(p), "the farewell party plays; the ring comes at its end");
                        fase[0] = 3;
                    }
                }
                case 3 -> {
                    if (h1.stap(p) == 3 && !Cutscenes.bezig(p)) {
                        helper.assertTrue(Ring.heeft(p) && Ring.kreeg(p) && tel(p, RingFeature.KNABBELRING.get()) == 1, "the ring, one");
                        for (VerhaalGuhs.Klik k : Sam.BIJ_KLIK) {
                            k.klik(samThuis, p, InteractionHand.MAIN_HAND);
                        }
                        helper.assertTrue(h1.stap(p) == 4 && Sam.looptMee(p) && Sam.van(p) != null && samThuis.isAlive(), "Sam-guh walks along; the one at home stays for the next player");
                        for (int soort = 0; soort < 3; soort++) {
                            Feest.proviand(p, krat, soort);
                        }
                        helper.assertTrue(h1.stap(p) == 5 && !Ring.magDoorPortaal(p) && GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, p) != null,
                                "provisions packed; one step to go and the portal is still shut");
                        helper.assertTrue(!Sluiers.open(p, Ring.STRUCTUREN.get(1)), "Guhvendel is still behind its sluier");
                        level.setBlock(portaal, BarbecuetherFeature.BARBECUETHER_PORTAAL.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        p.snapTo(portaal.getX() - 1.5, portaal.getY(), portaal.getZ() + 0.5);
                        fase[0] = 4;
                    }
                }
                case 4 -> {
                    if (h1.klaar(p)) {                                    // (standing at the portal with Sam-guh: within a second)
                        level.setBlock(portaal, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        helper.assertTrue(Ring.magDoorPortaal(p) && GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, p) == null && Ring.hoofdstuk(p) == 2,
                                "the last step of chapter 1 opens the grill portal");
                        deRest(helper, p, guhdalf, guhladriel, tafel, thuis);
                        if (p == eerste) {
                            // the second player starts when the first is done, at the same camp, with the same Sam-guh at home
                            helper.assertTrue(!Ring.begonnen(tweede) && !Ring.magDoorPortaal(tweede) && samThuis.isAlive() && guhdalf.isAlive(),
                                    "nothing of the first player's story rubbed off on the second");
                            aanDeBeurt[0] = tweede;
                            fase[0] = 0;
                        } else {
                            aanDeBeurt[0] = null;
                            ruimOp(helper, eerste, tweede);
                            helper.succeed();
                        }
                    }
                }
                default -> {
                }
            }
        });
    }

    /** Chapters 2 to 6, the feast and afterwards, in one go (nothing of it waits for a tick). */
    private static void deRest(GameTestHelper helper, ServerPlayer p, GuhNpcEntity guhdalf, GuhNpcEntity guhladriel, BlockPos tafel, BlockPos thuis) {
        ServerLevel level = helper.getLevel();
        // --- chapters 2 and 3: the ring is carried on, Sam-guh walks along, each place opens in its turn -------------------
        for (int n = 2; n <= 3; n++) {
            Verhaallijn l = Ring.lijn(n);
            helper.assertTrue(l.aanDeBeurt(p) && Ring.hoofdstuk(p) == n && Ring.bezigMet(p) == l && Sluiers.open(p, Ring.STRUCTUREN.get(n - 1))
                    && !Sluiers.open(p, Ring.STRUCTUREN.get(n)), "chapter " + n + " is next: its place is open, the one after it is not");
            l.begin(p);
            l.zet(p, l.stappen());
            helper.assertTrue(Ring.heeft(p) && tel(p, RingFeature.KNABBELRING.get()) == 1 && Sam.looptMee(p), "after chapter " + n + ": the ring and Sam-guh are still there");
        }
        // --- chapter 4: Guhladriel herself hands out the gifts (her role, step 5) ----------------------------------------------
        Verhaallijn h4 = Ring.lijn(4);
        helper.assertTrue(h4.aanDeBeurt(p) && !Sluiers.open(p, Ring.SAUSUMAN) && !Gaven.heeft(p, RingFeature.LICHTFLESJE.get()), "chapter 4 opens; no gifts, no tower yet");
        h4.begin(p);
        h4.zet(p, 5);
        NpcRollen.van(guhladriel).talk(guhladriel, p);
        helper.assertTrue(h4.stap(p) == 6 && Gaven.heeft(p, RingFeature.LICHTFLESJE.get()) && Gaven.heeft(p, RingFeature.ELFENMANTELTJE.get())
                && Gaven.heeft(p, RingFeature.ELFENTOUW.get()), "the three gifts of Guhladriel: " + h4.stap(p));
        h4.zet(p, h4.stappen());
        helper.assertTrue(Sluiers.open(p, Ring.SAUSUMAN) && Sluiers.open(p, Ring.STRUCTUREN.get(4)) && !Sluiers.open(p, Ring.STRUCTUREN.get(5)),
                "after chapter 4: the tower of Sausuman and the Roosterpoort open, the mountain does not");
        // --- chapter 5: Smikagol becomes the guide; the gifts are still in the pockets --------------------------------------------
        Verhaallijn h5 = Ring.lijn(5);
        h5.begin(p);
        SmikagolEntity gids = Smikagol.roep(p, null);
        helper.assertTrue(gids != null && Smikagol.isGids(p), "Smikagol guides this player");
        h5.zet(p, h5.stappen());
        helper.assertTrue(Ring.heeft(p) && Gaven.heeft(p, RingFeature.LICHTFLESJE.get()) && Gaven.heeft(p, RingFeature.ELFENTOUW.get()) && Sam.looptMee(p)
                && Smikagol.isGids(p) && Smikagol.van(p) == gids, "into chapter 6: the ring, the gifts, Sam-guh and Smikagol the guide");
        // --- chapter 6: the real end --------------------------------------------------------------------------------------------
        Verhaallijn h6 = Ring.lijn(6);
        helper.assertTrue(h6.aanDeBeurt(p) && Sluiers.open(p, Ring.STRUCTUREN.get(5)) && Ring.hoofdstuk(p) == 6 && !Ring.klaar(p), "the Frituurberg opens");
        h6.begin(p);
        h6.zet(p, 5);
        Class<?>[] speler = {ServerPlayer.class};
        roep("nl.juiced.guhs.feature.ringh6.Finale", "naFrituur", speler, p);
        helper.assertTrue(!Ring.heeft(p) && !Ring.kreeg(p) && tel(p, RingH6Feature.STUKJE.get()) == 1 && !Smikagol.isGids(p), "fried and shared: no ring, a piece to keep, no guide");
        Thuis.testPlek = thuis;
        roep("nl.juiced.guhs.feature.ringh6.Thuis", "breng", speler, p);
        helper.assertTrue(h6.stap(p) == 6 && p.position().distanceTo(Vec3.atBottomCenterOf(thuis)) < 0.6 && tel(p, RingH6Feature.STUKJE.get()) == 1, "flown home: step 6, the piece still there");
        helper.assertTrue(tel(p, RingH5Feature.OOG_VAN_SAUSRON_BEELDJE_ITEM.get()) == 0 && !RingBeloning.gegeven(p), "no reward before the feast");
        roep("nl.juiced.guhs.feature.ringh6.Thuis", "klaar", speler, p);
        Titels.Titel titel = Titels.van(RingFeature.TITEL);
        helper.assertTrue(Ring.klaar(p) && Ring.hoofdstuk(p) == 7 && RingBeloning.gegeven(p) && tel(p, RingH5Feature.OOG_VAN_SAUSRON_BEELDJE_ITEM.get()) == 1
                && tel(p, RingFeature.ELFENTOUW_HAAK_ITEM.get()) == RingBeloning.HAKEN && titel != null && titel.behaald().test(p),
                "the feast is over: the story is done, the rewards and the title are there");
        helper.assertTrue(!RingBeloning.geef(p) && tel(p, RingH5Feature.OOG_VAN_SAUSRON_BEELDJE_ITEM.get()) == 1, "the rewards come once");
        helper.assertTrue(VerhaalGuhs.magTemmen(p, VerhaalGuh.SAM_GUH) && Smikagol.heeftMaatje(p), "Sam-guh may come home for good, Smikagol is a buddy");
        // --- afterwards: one treat a day in the Gouw, from Guhdalf or from the table -------------------------------------------
        NpcRollen.van(guhdalf).talk(guhdalf, p);
        Feest.tafel(p, tafel);
        NpcRollen.van(guhdalf).talk(guhdalf, p);
        helper.assertTrue(tel(p, RingFeature.FEESTKNABBEL.get()) == 1, "one Feestknabbel a day: " + tel(p, RingFeature.FEESTKNABBEL.get()));
        helper.assertTrue(Ring.magDoorPortaal(p) && GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, p) == null, "and the grill portal stays open for good");
    }

    private static void ruimOp(GameTestHelper helper, ServerPlayer... spelers) {
        ServerLevel level = helper.getLevel();
        for (ServerPlayer p : spelers) {
            SmikagolEntity maatje = Smikagol.maatje(p);
            if (maatje != null) {
                maatje.discard();
            }
            Ring.wis(p);
            level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        Thuis.testLevel = null;
        Thuis.testPlek = null;
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(25, 30, 25).inflate(2);
        level.getEntitiesOfClass(Entity.class, kamer, e -> !(e instanceof ServerPlayer)).forEach(Entity::discard);
    }

    private RingKetenGameTests() {
    }
}
