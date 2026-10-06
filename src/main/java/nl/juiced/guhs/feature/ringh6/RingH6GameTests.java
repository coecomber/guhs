package nl.juiced.guhs.feature.ringh6;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.Negen;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingBeloning;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.ring.Smikagol;
import nl.juiced.guhs.feature.ring.SmikagolEntity;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-h6): chapter 6, server side, on the small test mountain (template ringh6_test_berg: the same named spots as the
 * real Frituurberg on a floor of 25 x 17, tools/features/ring_h6_bouw.py test_berg). Mock players get no packets and are not
 * ticked by the server: the tests call the chapter's once-a-second upkeep themselves and post the player tick event (that is
 * what ends a cutscene a mock player "watches").
 */
public final class RingH6GameTests {
    private static final String BERG = "ringh6_test_berg", BATCH = "ringh6";

    private static ServerPlayer speler(GameTestHelper helper, Berg.Kopie berg, String plek) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        zet(p, berg, plek);
        Ring.OVERAL = true;
        Negen.OVERAL = true;
        return p;
    }

    private static void zet(ServerPlayer p, Berg.Kopie berg, String plek) {
        Vec3 daar = berg.midden(plek);
        p.snapTo(daar.x, daar.y, daar.z);
        p.setOnGround(true);
        p.setDeltaMovement(Vec3.ZERO);
    }

    /** The test mountain of this test: the template's (0, 0, 0) is the test's (0, 1, 0). */
    private static Berg.Kopie berg(GameTestHelper helper) {
        Berg.wisTest(helper.getLevel());
        return Berg.zetTest(helper.getLevel(), Berg.test(), helper.absolutePos(new BlockPos(0, 1, 0)));
    }

    /** The story up to this chapter is done, the ring and the gifts are in the pockets, the narrator card was read. */
    private static void opStap(ServerPlayer p, int stap) {
        Ring.lijn(1).begin(p);
        for (int n = 1; n <= 5; n++) {
            Verhaallijn l = Ring.lijn(n);
            l.zet(p, l.stappen());
        }
        Ring.geef(p);
        Gaven.geef(p);
        GuhQuests.saved(p).putBoolean("guhs_kaart_" + Klim.KAART, true);
        RingH6Feature.LIJN.begin(p);
        RingH6Feature.LIJN.zet(p, stap);
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Ring.wis(p);
            Klim.vergeet(p.getUUID());
            Kolen.vergeet(p.getUUID());
            Berg.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        Berg.wisTest(helper.getLevel());
        Thuis.testLevel = null;
        Thuis.testPlek = null;
    }

    private static void tik(ServerPlayer p) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
    }

    /** The mountain's spots (both files), a turned copy, the questline's shape, the scenes and the card. */
    @GuhTest(template = BERG, batch = BATCH)
    public static void ringh6BergEnLijn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (Berg.Gegevens g : List.of(Berg.echt(), Berg.test())) {
            for (String plek : List.of("kamp", "kamp_vuur", "spleet", "rand", "frituur")) {
                helper.assertTrue(g.plekken().containsKey(plek), "spot " + plek);
            }
            for (int n = 1; n <= 3; n++) {
                for (String soort : List.of("richel", "vuur", "slot", "kooi", "haak", "start", "boven")) {
                    helper.assertTrue(g.plekken().containsKey(soort + "_" + n), "spot " + soort + "_" + n);
                }
            }
            helper.assertTrue(g.routes().getOrDefault("sam", List.of()).size() >= 4, "Sam-guh's route");
        }
        Berg.Gegevens echt = Berg.echt();
        helper.assertTrue(echt.plek("rand").getY() > echt.plek("richel_3").getY() && echt.plek("richel_3").getY() > echt.plek("richel_2").getY()
                && echt.plek("richel_2").getY() > echt.plek("richel_1").getY() && echt.plek("richel_1").getY() > echt.plek("kamp").getY(), "the climb goes up");
        helper.assertTrue(echt.routes().get("sam").size() >= 12, "the real route is a road");
        // a turned copy: a point of the template lies in the block the template's block lands in, whichever way it is turned
        BlockPos anker = helper.absolutePos(new BlockPos(12, 2, 8));
        for (Rotation draai : Rotation.values()) {
            Berg.Kopie k = new Berg.Kopie(level, echt, anker, draai);
            for (String plek : List.of("rand", "slot_2", "haak_3", "kamp")) {
                BlockPos lokaal = echt.plek(plek);
                BlockPos wereld = anker.offset(StructureTemplate.transform(lokaal.subtract(echt.anker()), Mirror.NONE, draai, BlockPos.ZERO));
                helper.assertTrue(k.wereld(plek).equals(wereld), plek + " turned " + draai);
                helper.assertTrue(BlockPos.containing(k.wereld(Vec3.atCenterOf(lokaal))).equals(wereld), "a point in " + plek + " turned " + draai);
                helper.assertTrue(k.binnen(Vec3.atCenterOf(wereld), 0), plek + " lies in the box, turned " + draai);
            }
            helper.assertTrue(!k.binnen(Vec3.atCenterOf(k.wereld(new BlockPos(-9, 10, 48))), 4), "outside the box");
        }
        Berg.Kopie test = berg(helper);
        helper.assertTrue(level.getBlockState(test.wereld("slot_2")).getBlock() instanceof KooislotBlock
                && level.getBlockState(test.wereld("slot_2")).getValue(KooislotBlock.NR) == 2, "the test mountain has its locks");
        helper.assertTrue(Berg.bij(level, test.wereld("kamp")) == test && Berg.bij(level, test.wereld("kamp").above(40)) == null, "found by a spot on it");
        // the questline, the scenes, the card
        Verhaallijn lijn = RingH6Feature.LIJN;
        helper.assertTrue(lijn == Ring.lijn(6) && lijn.stappen() == 7 && "ring_h5".equals(lijn.na()) && lijn.doelregel(), "the questline of chapter 6");
        helper.assertTrue(Finale.FRITUUR != null && Finale.FRITUUR.duur() > 1000 && Cutscene.van("ringh6_vlucht") == Finale.VLUCHT
                && Cutscene.van("ringh6_feest") == Finale.FEEST, "the three scenes");
        helper.assertTrue(Verteller.van(Klim.KAART) != null && Verteller.van(Klim.KAART).regels() == 4, "the narrator card");
        Berg.wisTest(level);
        helper.succeed();
    }

    /** The climb, step by step: the card, the camp, the cages in order, each player their own, the ring's weight. */
    @GuhTest(template = BERG, batch = BATCH + "_klim", timeoutTicks = 300)
    public static void ringh6KlimEnKooien(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Berg.Kopie berg = berg(helper);
        ServerPlayer p = speler(helper, berg, "kamp"), vriend = speler(helper, berg, "kamp"), klaar = speler(helper, berg, "kamp");
        Verhaallijn lijn = RingH6Feature.LIJN;
        opStap(p, 0);
        GuhQuests.saved(p).remove("guhs_kaart_" + Klim.KAART);
        opStap(vriend, 0);
        opStap(klaar, 7);
        helper.assertTrue(lijn.klaar(klaar) && !lijn.klaar(p), "three players: one done, two at the start");
        // the chapter opens with its card; then the camp
        Klim.seconde(p);
        helper.assertTrue(Cutscenes.bezig(p) && lijn.begonnen(p) && lijn.stap(p) == 0, "the narrator card shows first");
        helper.onEachTick(() -> tik(p));
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(Verteller.gezien(p, Klim.KAART) && !Cutscenes.bezig(p), "the card was read");
            Klim.seconde(p);
            helper.assertTrue(lijn.stap(p) == 1 && lijn.stap(vriend) == 0, "at the camp fire: step 1, for this player only");
            GuhEntity sam = Sam.van(p);
            SmikagolEntity smikagol = Smikagol.van(p);
            helper.assertTrue(sam != null && smikagol != null && Zicht.magZien(p, smikagol) && !Zicht.magZien(vriend, smikagol), "Sam-guh and Smikagol are there");
            Klim.seconde(p);
            helper.assertTrue(Math.abs(Ring.zwaarte(p) - Klim.ZWAAR_VOET) < 0.01, "at the foot the ring is light: " + Ring.zwaarte(p));
            // the cages: in order, only your own
            BlockPos slot1 = berg.wereld("slot_1"), slot2 = berg.wereld("slot_2"), slot3 = berg.wereld("slot_3");
            Klim.slot(p, slot2, 2);
            helper.assertTrue(lijn.stap(p) == 1, "cage 2 before cage 1: locked");
            Klim.slot(klaar, slot1, 1);
            helper.assertTrue(lijn.stap(p) == 1 && lijn.stap(vriend) == 0, "somebody who is done opens nothing for anybody");
            GekooideRookguhEntity gekooid = GekooideRookguhEntity.maak(level, berg.midden("kooi_1"), 1);
            level.addFreshEntity(gekooid);
            helper.assertTrue(Zicht.magZien(p, gekooid) && Zicht.magZien(vriend, gekooid) && !Zicht.magZien(klaar, gekooid), "the caged Rookguhje: for who still has to free it");
            Klim.slot(p, slot1, 1);
            helper.assertTrue(lijn.stap(p) == 2 && lijn.stap(vriend) == 0, "the lock of cage 1: step 2");
            helper.assertTrue(!Zicht.magZien(p, gekooid) && Zicht.magZien(vriend, gekooid), "gone for who freed it, still there for the friend");
            List<GekooideRookguhEntity> vrij = level.getEntitiesOfClass(GekooideRookguhEntity.class, new AABB(slot1).inflate(6), GekooideRookguhEntity::isVrij);
            helper.assertTrue(vrij.size() == 1 && p.getUUID().equals(Zicht.eigenaar(vrij.get(0))) && !vrij.get(0).shouldBeSaved(), "their own Rookguhje flies off");
            Klim.slot(p, slot1, 1);
            helper.assertTrue(lijn.stap(p) == 2, "a lock opens once");
            // a friend walks to the camp by clicking the first lock straight away
            Klim.slot(vriend, slot1, 1);
            helper.assertTrue(lijn.stap(vriend) == 2 && lijn.stap(p) == 2, "the friend frees their own, later");
            Klim.slot(p, slot3, 3);
            helper.assertTrue(lijn.stap(p) == 2, "cage 3 waits for cage 2");
            Klim.slot(p, slot2, 2);
            Klim.slot(p, slot3, 3);
            helper.assertTrue(lijn.stap(p) == 4, "all three free: Sam-guh's turn");
            Klim.seconde(p);
            helper.assertTrue(Ring.zwaarte(p) == 1.0, "the ring is as heavy as it gets");
            // without a rope the chapter hands one out again
            lijn.wis(vriend);
            opStap(vriend, 2);
            vriend.getInventory().clearContent();
            Ring.geef(vriend);
            Klim.seconde(vriend);
            helper.assertTrue(Gaven.heeft(vriend, RingFeature.ELFENTOUW.get()), "a lost Elfentouw comes back on the rope stage");
            gekooid.discard();
            vrij.get(0).discard();
            weg(helper, p, vriend, klaar);
            helper.succeed();
        });
    }

    /** A falling coal shoves and never hurts; nothing on the mountain hurts; a dip in the frituur puts back at the rest point. */
    @GuhTest(template = BERG, batch = BATCH + "_kool", timeoutTicks = 300)
    public static void ringh6KoolEnVeilig(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Berg.Kopie berg = berg(helper);
        ServerPlayer p = speler(helper, berg, "start_2");
        opStap(p, 1);
        Klim.seconde(p);
        helper.assertTrue(Berg.van(p) == berg, "the player is on the mountain");
        float leven = p.getHealth();
        // fire, the frituur and falls don't hurt here; off the mountain they do
        p.hurtServer(level, level.damageSources().lava(), 4f);
        p.hurtServer(level, level.damageSources().fall(), 6f);
        p.hurtServer(level, level.damageSources().hotFloor(), 1f);
        helper.assertTrue(p.getHealth() == leven, "no damage from fire, frituur or a fall on the mountain");
        // back at the rest point after a dip
        Vec3 rust = berg.midden("richel_1");
        Rustpunten.zet(p, Ring.RUST, level.dimension(), rust, 0f);
        Klim.red(p, berg, Klim.FRITUUR);
        helper.assertTrue(p.position().distanceTo(rust) < 0.5 && p.getHealth() == leven && Ring.heeft(p), "fished out: at the rest point, unharmed, the ring still there");
        // a visitor without a rest point here lands at the camp
        ServerPlayer gast = speler(helper, berg, "rand");
        Klim.seconde(gast);
        Klim.red(gast, berg, Klim.FRITUUR);
        helper.assertTrue(gast.position().distanceTo(berg.midden("kamp")) < 0.5, "a visitor is put at the camp");
        // a coal
        zet(p, berg, "start_2");
        helper.assertTrue(Kolen.opDeFlank(p, berg), "on the flank coals fall");
        zet(gast, berg, "kamp_vuur");
        helper.assertTrue(!Kolen.opDeFlank(gast, berg), "at a Rustvuurtje no coals fall");
        ValkoolEntity kool = Kolen.laatVallen(level, p.position(), p);
        helper.assertTrue(kool != null && kool.getY() > p.getY() + 4, "a coal starts high above its target");
        helper.succeedWhen(() -> {
            helper.assertTrue(Kolen.geraaktAantal(p) == 1, "the coal reaches the player");
            helper.assertTrue(kool.isRemoved(), "and is gone");
            helper.assertTrue(p.getHealth() == leven && p.getRemainingFireTicks() <= 0, "a shove, no damage, no fire");
            helper.assertTrue(p.getDeltaMovement().horizontalDistance() > 0.3, "shoved: " + p.getDeltaMovement());
            Berg.wisTest(level);
            p.hurtServer(level, level.damageSources().fall(), 2f);
            helper.assertTrue(p.getHealth() < leven, "(off the mountain a fall hurts as always)");
            weg(helper, p, gast);
        });
    }

    /** Smikagol grabs at the ring: a shove, nothing more; a flash of the Lichtflesje stops him for a while. */
    @GuhTest(template = BERG, batch = BATCH + "_smikagol")
    public static void ringh6Smikagol(GameTestHelper helper) {
        Berg.Kopie berg = berg(helper);
        ServerPlayer p = speler(helper, berg, "start_1");
        opStap(p, 2);
        Klim.seconde(p);
        SmikagolEntity s = Smikagol.van(p);
        helper.assertTrue(s != null, "Smikagol is called on the rope stage");
        Vec3 naast = p.position().add(1.2, 0, 0);
        s.snapTo(naast.x, naast.y, naast.z, 0f, 0f);
        float leven = p.getHealth();
        Klim.zetGraaiNu(p, true);
        Klim.tik(p);
        helper.assertTrue(p.getDeltaMovement().x < -0.3 && p.getHealth() == leven && Ring.heeft(p), "the grab: a shove away from him, the ring stays");
        // far away he misses
        p.setDeltaMovement(Vec3.ZERO);
        zet(p, berg, "start_3");
        Klim.zetGraaiNu(p, true);
        Klim.tik(p);
        helper.assertTrue(p.getDeltaMovement().lengthSqr() < 1e-6, "out of reach: he misses");
        // the Lichtflesje
        Vec3 bij = p.position().add(1.2, 0, 0);
        s.snapTo(bij.x, bij.y, bij.z, 0f, 0f);
        helper.assertTrue(!Klim.verblind(p), "(not blinded yet)");
        Klim.licht(p, p.position());
        helper.assertTrue(Klim.verblind(p), "a flash near him: blinded");
        Klim.seconde(p);
        Klim.tik(p);
        helper.assertTrue(p.getDeltaMovement().lengthSqr() < 1e-6, "blinded: no grabbing");
        weg(helper, p);
        helper.succeed();
    }

    /** Step 4: a click on Sam-guh and he carries his player to the Frituurspleet. */
    @GuhTest(template = BERG, batch = BATCH + "_sam", timeoutTicks = 900)
    public static void ringh6SamDraagt(GameTestHelper helper) {
        Berg.Kopie berg = berg(helper);
        ServerPlayer p = speler(helper, berg, "richel_1");
        Verhaallijn lijn = RingH6Feature.LIJN;
        opStap(p, 2);
        Sam.roep(p);
        GuhEntity sam = Sam.van(p);
        helper.assertTrue(sam != null, "Sam-guh walks along");
        Klim.seconde(p);
        // the ring is heavy: on the rope stage he does not carry, and does not walk off towards the middle of the mountain
        Ring.zetZwaarte(p, 0.9, 200);
        helper.assertTrue(Klim.samKlik(sam, p, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && p.getVehicle() == null, "not on the rope stage");
        Ring.zetZwaarte(p, 0.1, 200);
        helper.assertTrue(Klim.samKlik(sam, p, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "a light ring: his usual talk (ring-kern's)");
        lijn.zet(p, 4);
        Klim.seconde(p);
        helper.assertTrue(Klim.samKlik(sam, p, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && p.getVehicle() == sam && Sam.doet(sam).equals(Sam.DRAAGT),
                "step 4: on his back");
        helper.assertTrue(lijn.stap(p) == 4, "not there yet");
        int[] tel = {0};
        helper.onEachTick(() -> {
            if (++tel[0] % 20 == 0 && !p.isRemoved()) {
                Klim.seconde(p);                              // (the run-up when a mob's path finder gives up is part of the ride)
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(lijn.stap(p) == 5, "Sam-guh arrives at the cleft: step 5");
            helper.assertTrue(p.getVehicle() == null, "and puts his player down");
            helper.assertTrue(sam.position().distanceTo(berg.midden("rand")) < 4, "at the balcony");
            weg(helper, p);
        });
    }

    /** The end: the ring is fried and gone, a piece to keep, the flight home, the feast, and ring-kern's rewards. */
    @GuhTest(template = BERG, batch = BATCH + "_einde", timeoutTicks = 600)
    public static void ringh6FinaleEnFeest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Berg.Kopie berg = berg(helper);
        ServerPlayer p = speler(helper, berg, "spleet");
        Verhaallijn lijn = RingH6Feature.LIJN;
        opStap(p, 4);
        Thuis.testLevel = level;
        Thuis.testPlek = berg.wereld("kamp");
        // walking to the cleft yourself counts too
        Klim.seconde(p);
        helper.assertTrue(lijn.stap(p) == 5, "at the cleft on foot: step 5");
        Klim.seconde(p);
        helper.assertTrue(!Cutscenes.bezig(p) && Ring.heeft(p), "not on the balcony yet: nothing happens");
        zet(p, berg, "rand");
        Doel doel = lijn.doel(p);
        helper.assertTrue(doel != null && Berg.STRUCTUUR.equals(doel.structuur()), "the goal is the mountain");
        Klim.seconde(p);
        helper.assertTrue(Cutscenes.bezig(p), "on the balcony: the finale plays");
        int[] tel = {0};
        helper.onEachTick(() -> {
            tik(p);
            p.setOnGround(true);
            if (++tel[0] % 5 == 0) {
                Klim.seconde(p);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(lijn.vlag(p, Finale.GEFRITUURD) && !Ring.heeft(p) && !Ring.kreeg(p), "the ring is fried and gone for good");
            helper.assertTrue(GuhQuests.count(p, RingH6Feature.STUKJE.get()) == 1, "a piece of the fried ring, once");
            helper.assertTrue(lijn.klaar(p), "the flight, the feast: the questline is done (step " + lijn.stap(p) + ")");
            helper.assertTrue(Thuis.thuis(p) != null && Thuis.thuis(p).equals(Thuis.testPlek), "home was remembered");
            helper.assertTrue(Cutscenes.gezien(p, "ringh6_frituur") && Cutscenes.gezien(p, "ringh6_vlucht") && Cutscenes.gezien(p, "ringh6_feest"), "all three scenes seen");
            helper.assertTrue(Ring.klaar(p) && RingBeloning.gegeven(p) && Smikagol.heeftMaatje(p), "ring-kern gave the rewards of the story");
            if (Smikagol.maatje(p) != null) {
                Smikagol.maatje(p).discard();
            }
            weg(helper, p);
        });
    }

    /** Home: the feast looks for flat open ground; the goal of step 6 is the landing spot. */
    @GuhTest(template = BERG, batch = BATCH)
    public static void ringh6Thuis(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Berg.Kopie berg = berg(helper);
        ServerPlayer p = speler(helper, berg, "kamp");
        opStap(p, 6);
        BlockPos kamp = berg.wereld("kamp");
        BlockPos plek = Thuis.feestplek(level, kamp);
        helper.assertTrue(level.getBlockState(plek.below()).blocksMotion() && !level.getBlockState(plek).blocksMotion() && plek.closerThan(kamp, 30),
                "the feast stands on open ground nearby: " + plek);
        BlockPos land = Thuis.landing(level, kamp);
        helper.assertTrue(level.getBlockState(land.below()).blocksMotion() && !level.getBlockState(land).blocksMotion(), "a landing spot to stand on: " + land);
        helper.assertTrue(Berg.STRUCTUUR.equals(Ring.STRUCTUREN.get(5)), "(ring-kern's name of the mountain)");
        Thuis.testLevel = level;
        Thuis.testPlek = kamp;
        GuhQuests.saved(p).putLong(Thuis.THUIS, kamp.asLong());
        Doel doel = RingH6Feature.LIJN.doel(p);
        helper.assertTrue(doel != null && kamp.equals(doel.plek()) && doel.dim() == level.dimension(), "step 6 points at home");
        weg(helper, p);
        helper.succeed();
    }

    private RingH6GameTests() {
    }
}
