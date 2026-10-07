package nl.juiced.guhs.feature.ringh5;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Cast;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.KnekelRuiterEntity;
import nl.juiced.guhs.feature.ring.Negen;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ring.Smikagol;
import nl.juiced.guhs.feature.ring.SmikagolEntity;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-h5): chapter 5 of the Knabbelring, server side (mock players get no packets and are not ticked: the tests call
 * the chapter's once-a-second look themselves). Template ringh5_test_kamer (tools/features/ring_h5_bouw.py): a floor of
 * 33 x 33, a Rustvuurtje at (3, 2, 3), a wall with a roof to hide behind (stand at (15, 2, 20): the wall is at z 21).
 * <p>
 * The test server has no Guhbarbecuether, so the valley is a {@link Terrein} put down by hand: the same code then works with
 * the spots of {@link Plekken}, shifted so that the part a test needs lies in the room.
 */
public final class RingH5GameTests {
    private static final String KAMER = "ringh5_test_kamer";
    /** The room's Rustvuurtje stands at (3, 2, 3); a player's rest point next to it. */
    private static final BlockPos RUST = new BlockPos(5, 2, 4);
    /** Where a player hides behind the room's wall, and a spot in the open. */
    private static final BlockPos SCHUIL = new BlockPos(15, 2, 20), OPEN = new BlockPos(10, 2, 10);
    /** The test Eye hangs here: beyond the wall, high up. */
    private static final BlockPos OOG = new BlockPos(16, 11, 30);

    private static ServerPlayer speler(GameTestHelper helper, BlockPos plek) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Praat.vergeet(p);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        zet(helper, p, plek);
        Ring.OVERAL = true;
        Negen.OVERAL = true;
        return p;
    }

    private static void zet(GameTestHelper helper, ServerPlayer p, BlockPos plek) {
        BlockPos at = helper.absolutePos(plek);
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setOnGround(true);
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static void naar(ServerPlayer p, Vec3 plek) {
        p.snapTo(plek.x, plek.y, plek.z);
        p.setOnGround(true);
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Ring.doeOm(p, false);
            Gaven.stopRots(p);
            SmikagolEntity maatje = Smikagol.maatje(p);
            if (maatje != null) {
                maatje.discard();   // (the buddy of a player the test let finish the whole story)
            }
            Ring.wis(p);
            Hoofdstuk.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** The story up to (and with) this chapter is done for this player. */
    private static void klaarTot(ServerPlayer p, int hoofdstuk) {
        Ring.lijn(1).begin(p);
        for (int n = 1; n <= hoofdstuk; n++) {
            Verhaallijn l = Ring.lijn(n);
            l.zet(p, l.stappen());
        }
    }

    private static Vec3 rustpunt(GameTestHelper helper, ServerPlayer p) {
        Vec3 rust = Vec3.atBottomCenterOf(helper.absolutePos(RUST));
        Rustpunten.zet(p, Ring.RUST, helper.getLevel().dimension(), rust, 0f);
        return rust;
    }

    /** A valley whose build block {@code lokaal} is the room block {@code kamer} (not turned). */
    private static Terrein terrein(GameTestHelper helper, BlockPos lokaal, BlockPos kamer) {
        return new Terrein(helper.getLevel().dimension(), helper.absolutePos(kamer).subtract(lokaal), Rotation.NONE);
    }

    /** Het Asveld over the room: the build block (28, 7, 21) is the room's corner (0, 2, 0), so zone A covers the room. */
    private static Terrein veld(GameTestHelper helper) {
        return terrein(helper, new BlockPos(28, 7, 21), new BlockPos(0, 2, 0));
    }

    private static Vec3 oogpunt(GameTestHelper helper) {
        return Vec3.atBottomCenterOf(helper.absolutePos(OOG)).add(OogEntity.PUPIL);
    }

    private static boolean bij(ServerPlayer p, Vec3 plek) {
        return p.position().distanceTo(plek) < 0.6;
    }

    /** Crouch still under the Elfenmanteltje until the player is a rock (the game's own player tick does it). */
    private static void wordRots(ServerPlayer p) {
        if (!Gaven.heeft(p, RingFeature.ELFENMANTELTJE.get())) {
            p.getInventory().add(new ItemStack(RingFeature.ELFENMANTELTJE.get()));
        }
        p.setShiftKeyDown(true);
        p.setOnGround(true);
        for (int i = 0; i <= Gaven.ROTS_NA + 2 && !Gaven.isRots(p); i++) {
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
        }
    }

    /** A Terrein turns the spots of the build like the game turns a template, and finds the copy of a real structure start. */
    @GuhTest(template = KAMER, batch = "ringh5_terrein")
    public static void ringh5Terrein(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Terrein.wisAlles();
        BlockPos nul = helper.absolutePos(new BlockPos(3, 2, 5));
        BlockPos lokaal = Plekken.VUUR_HOLTE;
        for (Rotation draai : Rotation.values()) {
            Terrein t = new Terrein(level.dimension(), nul, draai);
            BlockPos wereld = nul.offset(StructureTemplate.transform(lokaal, Mirror.NONE, draai, BlockPos.ZERO));
            helper.assertTrue(t.wereld(lokaal).equals(wereld), draai + ": a block of the build is where a template block would be");
            Vec3 midden = t.midden(lokaal);
            helper.assertTrue(midden.distanceTo(Vec3.atBottomCenterOf(wereld)) < 1e-6, draai + ": you stand in the middle of that block");
            Vec3 terug = t.lokaal(midden);
            helper.assertTrue(terug.distanceTo(new Vec3(lokaal.getX() + 0.5, lokaal.getY(), lokaal.getZ() + 0.5)) < 1e-6, draai + ": and back again");
            helper.assertTrue(t.in(Plekken.ZONE_B, t.midden(Plekken.BLIK_B.get(1))) && !t.in(Plekken.ZONE_B, t.midden(Plekken.VUUR_KAMP)),
                    draai + ": a box of the build holds what stands in it");
            helper.assertTrue(t.doos(Plekken.ROOK).contains(t.midden(Plekken.ROOK.get(0).offset(2, 1, 3))), draai + ": a box in the world");
            // "east in the build" points where the build's +x went
            Vec3 oost = t.midden(lokaal.east(4)).subtract(midden).normalize();
            helper.assertTrue(Vec3.directionFromRotation(0, t.yaw(-90f)).distanceTo(oost) < 1e-4, draai + ": a direction of the build");
            // the makers of the inhabitants work the copy out from one spot and the way it is turned
            OogEntity oog = Hoofdstuk.maakOog(level, t.midden(Plekken.OOG), draai);
            helper.assertTrue(oog != null && oog.terrein() != null && oog.terrein().nul().equals(nul) && oog.terrein().draai() == draai,
                    draai + ": the Eye knows its valley");
            helper.assertTrue(oog.kijkpunt().distanceTo(t.punt(Plekken.OOG_KIJK)) < 1e-6, draai + ": its sight starts at its pupil");
            RoosterwachterEntity wachter = Hoofdstuk.maakWachter(level, Plekken.WACHTER_1, t.midden(Plekken.WACHTER_1), draai);
            helper.assertTrue(wachter != null && Vec3.directionFromRotation(0, wachter.basisYaw()).distanceTo(oost) < 1e-4, draai + ": a guard looks down the lane");
            KnekelRuiterEntity ruiter = Hoofdstuk.maakRuiter(level, Plekken.RUITER_1, t.midden(Plekken.RUITER_1.get(0)), draai);
            helper.assertTrue(ruiter != null && ruiter.route().equals(List.of(t.wereld(Plekken.RUITER_1.get(0)), t.wereld(Plekken.RUITER_1.get(1)))),
                    draai + ": a rider's round lies in the lane");
        }
        // the real thing: the structure of the datapack, its nine tiles, the anchor of the burcht
        BlockPos kamp = helper.absolutePos(new BlockPos(4, 2, 4));
        StructureStart start = PoortProef.bouw(level, Plekken.VUUR_KAMP, kamp, false);
        try {
            helper.assertTrue(start.getPieces().size() == 9, "the valley is nine tiles, found " + start.getPieces().size());
            Terrein t = Terrein.van(level, start);
            helper.assertTrue(t != null && t.draai() == Rotation.NONE && t.wereld(Plekken.VUUR_KAMP).equals(kamp), "the copy's Terrein puts the camp fire at the camp fire");
            helper.assertTrue(kamp.equals(Kopieen.wereld(start, null, Plekken.VUUR_KAMP)), "the same spot Bezetting would use");
            Terrein gevonden = Terrein.bij(level, kamp);
            helper.assertTrue(gevonden != null && gevonden.nul().equals(t.nul()), "a player at the camp is in this copy");
            helper.assertTrue(Terrein.bij(level, kamp.offset(0, 0, -400)) == null, "and far away in none");
        } finally {
            Kopieen.testWissen(level);
            Terrein.wisAlles();
        }
        helper.succeed();
    }

    /**
     * The gaze of the Eye: whoever stands in its light with a free line to the Eye is put back on their rest point, unharmed;
     * behind a wall or as a rock under the Elfenmanteltje you are not seen; the worn ring draws it whatever stands in
     * between; who is done with the story is left alone.
     */
    @GuhTest(template = KAMER, batch = "ringh5_blik")
    public static void ringh5Blik(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Terrein.wisAlles();
        Terrein t = veld(helper);
        Vec3 oog = oogpunt(helper);
        Blik blik = new Blik();
        ServerPlayer p = speler(helper, OPEN);
        klaarTot(p, 4);
        helper.assertTrue(t.in(Plekken.ZONE_A, p.position()) && t.in(Plekken.DOMEIN, p.position()), "the room lies in het Asveld");
        Vec3 rust = rustpunt(helper, p);
        float leven = p.getHealth();
        // in the open, in the light: a warning first, caught after GENADE ticks
        helper.assertTrue(Blik.vrijZicht(level, oog, p, p), "the Eye has a free line to a player in the open");
        blik.zet(p.position(), 200);
        for (int i = 1; i < Blik.GENADE; i++) {
            helper.assertTrue(blik.tick(level, t, oog, p) == Blik.ZOEKT && blik.gezien(p) == i && !bij(p, rust), "tick " + i + " in the light: not caught yet");
        }
        blik.tick(level, t, oog, p);
        helper.assertTrue(bij(p, rust) && blik.gezien(p) == 0 && blik.neemBetrapt() == 1, "after " + Blik.GENADE + " ticks: back at the rest point");
        helper.assertTrue(p.getHealth() == leven && p.fallDistance == 0, "unharmed");
        // at your own rest point you are safe, also in the light
        blik.zet(p.position(), 200);
        for (int i = 0; i < 40; i++) {
            blik.tick(level, t, oog, p);
        }
        helper.assertTrue(Blik.bijRustpunt(p) && blik.gezien(p) == 0 && blik.neemBetrapt() == 0, "the light on your own rest point: nothing happens");
        // behind the wall: in the light, but the Eye has no line
        zet(helper, p, SCHUIL);
        helper.assertTrue(!Blik.vrijZicht(level, oog, p, p), "the wall and its roof stand between the Eye and the hiding place");
        blik.zet(p.position(), 200);
        for (int i = 0; i < 40; i++) {
            blik.tick(level, t, oog, p);
        }
        helper.assertTrue(!bij(p, rust) && blik.gezien(p) == 0, "hidden: not seen");
        // a rock in the open is not seen; standing up, it is
        zet(helper, p, OPEN);
        wordRots(p);
        helper.assertTrue(Gaven.isRots(p) && !Ring.oogZiet(p), "crouching still under the cloak: a rock");
        blik.zet(p.position(), 200);
        for (int i = 0; i < 40; i++) {
            blik.tick(level, t, oog, p);
        }
        helper.assertTrue(!bij(p, rust) && blik.gezien(p) == 0, "the Eye looks past a rock");
        p.setShiftKeyDown(false);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
        helper.assertTrue(!Gaven.isRots(p), "standing up: a player again");
        for (int i = 0; i < Blik.GENADE; i++) {
            blik.tick(level, t, oog, p);
        }
        helper.assertTrue(bij(p, rust), "and seen at once");
        // the ring: the light homes in from wherever it is, through the wall; FOCUS ticks and it has you
        zet(helper, p, SCHUIL);
        blik.zet(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(30, 2, 2))), 0);
        Ring.geef(p);
        helper.assertTrue(Ring.doeOm(p, true), "the ring is on");
        for (int i = 1; i < Blik.FOCUS; i++) {
            helper.assertTrue(blik.tick(level, t, oog, p) == Blik.ZOEKT && blik.focus(p) == i && !bij(p, rust), "tick " + i + " with the ring on: the Eye searches");
        }
        double ver = blik.plek().subtract(p.position()).multiply(1, 0, 1).length();
        helper.assertTrue(ver < 1.0, "by now the light is all but on the bearer (through the wall and its roof), " + ver + " away");
        blik.tick(level, t, oog, p);
        helper.assertTrue(bij(p, rust) && !Ring.om(p) && blik.focus(p) == 0, "after " + Blik.FOCUS + " ticks: caught, the ring is off");
        // ring on and off in time: the count runs down, nobody is caught
        zet(helper, p, SCHUIL);
        Ring.doeOm(p, true);
        for (int i = 0; i < Blik.FOCUS - 10; i++) {
            blik.tick(level, t, oog, p);
        }
        Ring.doeOm(p, false);
        for (int i = 0; i < Blik.FOCUS; i++) {
            blik.tick(level, t, oog, p);
        }
        helper.assertTrue(!bij(p, rust) && blik.focus(p) == 0, "taken off in time behind the wall: not caught");
        // where it looks: the zone somebody is in, else its idle round
        zet(helper, p, new BlockPos(2, 2, 30));     // (build z 51: de Kale Vlakte)
        helper.assertTrue(t.in(Plekken.ZONE_B, p.position()) && !t.in(Plekken.ZONE_A, p.position()), "the far end of the room is de Kale Vlakte");
        Blik tweede = new Blik();
        for (int i = 0; i < 20; i++) {
            tweede.tick(level, t, oog, p);
        }
        helper.assertTrue(tweede.zone().equals("b"), "somebody on de Kale Vlakte: the light works that zone, not " + tweede.zone());
        zet(helper, p, new BlockPos(30, 2, 2));
        for (int i = 0; i < 20; i++) {
            tweede.tick(level, t, oog, p);
        }
        helper.assertTrue(tweede.zone().equals("a"), "somebody in het Asveld: the light goes there, not " + tweede.zone());
        // done with the whole story: the Eye had its piece of ring and sleeps
        klaarTot(p, 6);
        zet(helper, p, OPEN);
        Blik derde = new Blik();
        derde.zet(p.position(), 0);
        helper.assertTrue(derde.tick(level, t, oog, p) == Blik.SLAAPT && derde.plek() == null, "nobody on the trip around: the Eye sleeps");
        weg(helper, p);
        helper.succeed();
    }

    /** The Eye itself: it wakes for a player on the trip, its light is a display that glides over the field, and it catches. */
    @GuhTest(template = KAMER, batch = "ringh5_oog", timeoutTicks = 900)
    public static void ringh5Oog(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Terrein.wisAlles();
        Terrein t = veld(helper);
        // a player on the line the light runs along in het Asveld (build (41, 7, 26) is the room's (13, 2, 5))
        ServerPlayer p = speler(helper, new BlockPos(13, 2, 5));
        klaarTot(p, 4);
        Vec3 rust = rustpunt(helper, p);
        float leven = p.getHealth();
        OogEntity oog = RingH5Feature.OOG_VAN_SAUSRON.get().create(level, EntitySpawnReason.TRIGGERED);
        Vec3 plek = Vec3.atBottomCenterOf(helper.absolutePos(OOG));
        oog.snapTo(plek.x, plek.y, plek.z, OogEntity.YAW, 0f);
        oog.zetTerrein(t);
        level.addFreshEntity(oog);
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(80);
        boolean[] lichtGezien = {false};
        helper.onEachTick(() -> {
            if (!lichtGezien[0] && oog.blikPlek() != null) {
                List<Display.BlockDisplay> lichten = level.getEntitiesOfClass(Display.BlockDisplay.class, kamer, e -> e.entityTags().contains(OogEntity.BLIK_TAG));
                lichtGezien[0] = lichten.size() == 1 && oog.staat() != Blik.SLAAPT && lichten.get(0).position().distanceTo(oog.blikPlek()) < 3;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(lichtGezien[0], "the Eye is awake and its light lies on the ground as one display");
            helper.assertTrue(bij(p, rust), "the light came by: back at the rest point");
            helper.assertTrue(p.getHealth() == leven, "unharmed");
            helper.assertTrue(oog.position().distanceTo(plek) < 0.01, "the Eye hangs where it hung");
            oog.discard();
            helper.assertTrue(level.getEntitiesOfClass(Display.BlockDisplay.class, kamer, e -> e.isAlive() && e.entityTags().contains(OogEntity.BLIK_TAG)).isEmpty(),
                    "its light goes with it");
            weg(helper, p);
        });
    }

    /** A Roosterwachter sends whoever he sees back to their rest point; a ring bearer and a rock he does not see. */
    @GuhTest(template = KAMER, batch = "ringh5_wachter", timeoutTicks = 400)
    public static void ringh5Wachter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Terrein.wisAlles();
        Vec3 post = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(10, 2, 10)));
        RoosterwachterEntity w = RingH5Feature.ROOSTERWACHTER.get().create(level, EntitySpawnReason.TRIGGERED);
        w.snapTo(post.x, post.y, post.z, 0f, 0f);
        w.zetPost(-90f);                                           // (he looks east: +x)
        w.setYHeadRot(-90f);
        level.addFreshEntity(w);
        ServerPlayer p = speler(helper, new BlockPos(14, 2, 10)), drager = speler(helper, new BlockPos(13, 2, 11)), achter = speler(helper, new BlockPos(6, 2, 10));
        ServerPlayer rots = speler(helper, new BlockPos(13, 2, 9)), klaar = speler(helper, new BlockPos(12, 2, 10));
        klaarTot(p, 4);
        klaarTot(drager, 4);
        klaarTot(achter, 4);
        klaarTot(rots, 4);
        klaarTot(klaar, 6);
        Vec3 rust = rustpunt(helper, p);
        for (ServerPlayer q : List.of(drager, achter, rots, klaar)) {
            rustpunt(helper, q);
        }
        Ring.geef(drager);
        Ring.doeOm(drager, true);
        wordRots(rots);
        helper.assertTrue(w.ziet(p), "a player in front of him: seen");
        helper.assertTrue(!w.ziet(drager) && Ring.onzichtbaarVoorMikas(drager), "who wears the ring: a Mika looks right through them");
        helper.assertTrue(!w.ziet(achter), "behind his back: not seen");
        helper.assertTrue(Gaven.isRots(rots) && !w.ziet(rots), "a rock: not seen");
        helper.assertTrue(!w.ziet(klaar), "who finished the story: left alone");
        Vec3 drPlek = drager.position(), achterPlek = achter.position(), rotsPlek = rots.position(), klaarPlek = klaar.position();
        float leven = p.getHealth();
        helper.succeedWhen(() -> {
            helper.assertTrue(bij(p, rust), "seen for " + RoosterwachterEntity.GENADE + " ticks: back at the rest point");
            helper.assertTrue(p.getHealth() == leven, "unharmed");
            helper.assertTrue(bij(drager, drPlek) && bij(achter, achterPlek) && bij(rots, rotsPlek) && bij(klaar, klaarPlek), "the others stand where they stood");
            helper.assertTrue(w.isAlive() && w.position().distanceTo(post) < 0.3, "the guard keeps his post");
            w.discard();
            weg(helper, p, drager, achter, rots, klaar);
        });
    }

    /**
     * The ten steps for one player, from the camp to the fire behind the wall: the card, Boromika's scene, Smikagol becomes
     * the guide, the scene on the ridge, the fields, the smoke (the Lichtflesje), the side door (bars only for who may not
     * pass yet) and the scene of Guhdalf de Witte. A friend who is done solves nothing and is stopped by nothing.
     */
    @GuhTest(template = KAMER, batch = "ringh5_stappen", timeoutTicks = 1600)
    public static void ringh5Stappen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // the camp in the room: the build block (40, 7, 2) is the room's (2, 2, 6)
        Terrein kamp = terrein(helper, new BlockPos(40, 7, 2), new BlockPos(2, 2, 6));
        // the side door in the room: the build block (4, 7, 62) is the room's (0, 2, 0)
        Terrein deur = terrein(helper, new BlockPos(4, 7, 62), new BlockPos(0, 2, 0));
        Terrein.wisAlles();
        Terrein.test(kamp);
        Verhaallijn lijn = RingH5Feature.LIJN;
        ServerPlayer p = speler(helper, new BlockPos(30, 2, 30)), vriend = speler(helper, new BlockPos(31, 2, 30));
        klaarTot(p, 4);
        klaarTot(vriend, 5);
        rustpunt(helper, p);
        Ring.geef(p);
        Gaven.geef(p);
        helper.assertTrue(lijn.stappen() == Hoofdstuk.STAPPEN && Hoofdstuk.bezig(p) && !Hoofdstuk.bezig(vriend), "the chapter is p's; the friend is done with it");
        // outside the camp nothing happens
        Hoofdstuk.seconde(p);
        helper.assertTrue(lijn.stap(p) == Hoofdstuk.AANKOMEN && !Verteller.gezien(p, Scenes.KAART), "not at the camp yet");
        // where the compass points: inside the valley at the camp, outside it at the way in with the least rock in front
        helper.assertTrue(Hoofdstuk.inBouw(kamp, p.position()) && kamp.wereld(Plekken.VUUR_KAMP).equals(Hoofdstuk.doel(p, Hoofdstuk.AANKOMEN).plek()),
                "in the valley the goal of step 0 is the camp fire");
        naar(p, kamp.midden(Plekken.INGANGEN.get(0).offset(2, 0, -5)));
        Hoofdstuk.seconde(p);
        helper.assertTrue(!Hoofdstuk.inBouw(kamp, p.position()) && Hoofdstuk.hier(p) != null, "just outside the mouth: at the valley, not in it");
        helper.assertTrue(kamp.wereld(Plekken.INGANGEN.get(0)).equals(Hoofdstuk.doel(p, Hoofdstuk.AANKOMEN).plek()), "outside it the goal is a way in: the mouth, the nearest");
        BlockPos voorDeMond = kamp.wereld(Plekken.INGANGEN.get(0).offset(0, 1, -1));
        helper.assertTrue(Hoofdstuk.rotsVoor(level, kamp, 0) == 0, "the mouth is open");
        level.setBlock(voorDeMond, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(Hoofdstuk.rotsVoor(level, kamp, 0) == 1, "a block of rock right in front of the mouth is counted");
        level.setBlock(voorDeMond, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        // the ways on behind the wall are measured the same way, each in its own direction (here the east tunnel: to +x)
        Terrein verder = terrein(helper, Plekken.UITGANGEN.get(2), OPEN);
        BlockPos voorDeGang = verder.wereld(Plekken.UITGANGEN.get(2).offset(1, 1, 0));
        helper.assertTrue(Plekken.UITGANGEN.size() == 3 && Hoofdstuk.rotsAchter(level, verder, 2) == 0, "the east way on is open");
        level.setBlock(voorDeGang, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(Hoofdstuk.rotsAchter(level, verder, 2) == 1, "a block of rock right outside a way on is counted");
        helper.assertTrue(Hoofdstuk.minste(0, 0, 0) == 0 && Hoofdstuk.minste(12, 12, 0) == 2 && Hoofdstuk.minste(3, 1, 1) == 1,
                "Smikagol names the way on with the least rock behind it, the nearest when it makes no difference");
        helper.assertTrue(Hoofdstuk.verder(0, 0, 0).equals("0") && Hoofdstuk.verder(12, 3, 12).equals("1") && Hoofdstuk.verder(12, 12, 12).equals("dicht")
                && nl.juiced.guhs.taal.NlTekst.has("quest.guhs.ringh5.smikagol.verder.dicht"), "with every way on in solid rock he sends the player back the way they came");
        level.setBlock(voorDeGang, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        zet(helper, p, new BlockPos(30, 2, 30));
        GuhNpcEntity boromika = Cast.zet(level, GuhNpcEntity.Kind.BOROMIKA, kamp.midden(Plekken.BOROMIKA), 120f, RingH5Feature.BOROMIKA_PLEK);
        Map<String, Vec3> onthoud = new HashMap<>();
        int[] fase = {0};
        helper.onEachTick(() -> {
            if (fase[0] >= 100) {
                return;
            }
            if (Cutscenes.bezig(p)) {
                // (a mock player is not ticked by the server: its own tick is what ends the two ticks it "watches")
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
                return;
            }
            if (!Duwtje.mag(p)) {
                return;   // (the 40 ticks of after-care that follow a card or a scene)
            }
            int stap = lijn.stap(p);
            switch (fase[0]) {
                case 0 -> {
                    // into the camp: the narrator card
                    naar(p, kamp.midden(Plekken.VUUR_KAMP.offset(1, 0, 2)));
                    Hoofdstuk.seconde(p);
                    helper.assertTrue(Cutscenes.bezig(p), "arriving at the camp shows the narrator card");
                    fase[0] = 1;
                }
                case 1 -> {
                    helper.assertTrue(stap == Hoofdstuk.BOROMIKA && Verteller.gezien(p, Scenes.KAART), "after the card: step 1");
                    // the friend clicks Boromika: small talk, no scene; then p: his moment
                    NpcRollen.van(boromika).talk(boromika, vriend);
                    helper.assertTrue(!Cutscenes.bezig(vriend), "Boromika's scene is not the friend's");
                    NpcRollen.van(boromika).talk(boromika, p);
                    helper.assertTrue(Cutscenes.bezig(p), "Boromika's moment plays for the player whose step it is");
                    fase[0] = 2;
                }
                case 2 -> {
                    helper.assertTrue(stap == Hoofdstuk.SMIKAGOL && Cutscenes.gezien(p, Scenes.BOROMIKA.id()), "after the scene: step 2");
                    helper.assertTrue(!Smikagol.isGids(p), "no guide yet");
                    Hoofdstuk.seconde(p);
                    SmikagolEntity s = Smikagol.van(p);
                    helper.assertTrue(s != null && Smikagol.isGids(p) && s.leidt() && s.position().distanceTo(kamp.midden(Plekken.SMIKAGOL_KAMP)) < 1.5,
                            "Smikagol sits at the provisions");
                    helper.assertTrue(Zicht.magZien(p, s) && !Zicht.magZien(vriend, s), "only his own player sees him");
                    // "no": nothing; "yes": he swears and becomes the guide
                    Hoofdstuk.praatSmikagol(p);
                    Praat.antwoord(p, s, Hoofdstuk.NEE);
                    Praat.antwoord(p, s, -1);   // (the screen closes)
                    helper.assertTrue(lijn.stap(p) == Hoofdstuk.SMIKAGOL, "'paws off' leaves the step where it is");
                    helper.assertTrue(Hoofdstuk.klikSmikagol(s, p), "a click on him asks again");
                    Praat.antwoord(p, s, Hoofdstuk.JA);
                    Praat.antwoord(p, s, -1);
                    helper.assertTrue(lijn.stap(p) == Hoofdstuk.UITKIJK && Duwtje.mag(p), "'show us the way': step 3");
                    fase[0] = 3;
                }
                case 3 -> {
                    Hoofdstuk.seconde(p);
                    SmikagolEntity s = Smikagol.van(p);
                    helper.assertTrue(s != null && s.leidt(), "the guide leads the way up the ridge");
                    naar(p, kamp.midden(Plekken.UITKIJK));
                    Hoofdstuk.seconde(p);
                    helper.assertTrue(Cutscenes.bezig(p), "on the crest: the scene of the Eye");
                    fase[0] = 4;
                }
                case 4 -> {
                    helper.assertTrue(stap == Hoofdstuk.ASVELD && Cutscenes.gezien(p, Scenes.OOG.id()), "after the scene: step 4");
                    // (from here on the valley is the stand-in with the side door in the room; the guide stays home)
                    Smikagol.stuurWeg(p);
                    Hoofdstuk.vergeet(p.getUUID());
                    Terrein.wisAlles();
                    Terrein.test(deur);
                    // the fields: a step is reached at its fire, in order
                    naar(p, deur.midden(Plekken.VUUR_HOLTE.offset(1, 0, 0)));
                    Hoofdstuk.seconde(p);
                    helper.assertTrue(lijn.stap(p) == Hoofdstuk.ASVELD, "the fire of the Holte is not this step's");
                    naar(p, deur.midden(Plekken.VUUR_SLAKKENHUT.offset(1, 0, 0)));
                    Hoofdstuk.seconde(p);
                    helper.assertTrue(lijn.stap(p) == Hoofdstuk.VLAKTE, "at the Slakkenhut: step 5");
                    naar(p, deur.midden(Plekken.VUUR_HOLTE.offset(1, 0, 0)));
                    Hoofdstuk.seconde(p);
                    helper.assertTrue(lijn.stap(p) == Hoofdstuk.LAAN, "at the Holte: step 6");
                    // the smoke pushes back until a Lichtflesje flashed at it
                    BlockPos rook = Plekken.ROOK.get(0).offset(2, 0, 3);
                    naar(p, deur.midden(rook));
                    p.setDeltaMovement(Vec3.ZERO);
                    Hoofdstuk.snel(p);
                    Vec3 terug = deur.midden(Plekken.ROOK_TERUG).subtract(p.position()).multiply(1, 0, 1).normalize();
                    helper.assertTrue(p.getDeltaMovement().multiply(1, 0, 1).length() > 0.1 && p.getDeltaMovement().multiply(1, 0, 1).normalize().dot(terug) > 0.9,
                            "in the smoke: a shove back to where you came from");
                    Gaven.flits(p);
                    helper.assertTrue(lijn.vlag(p, Hoofdstuk.ROOK_WEG), "the Lichtflesje blows the smoke away");
                    p.setDeltaMovement(Vec3.ZERO);
                    Hoofdstuk.snel(p);
                    helper.assertTrue(p.getDeltaMovement().lengthSqr() < 1e-6, "and the way is free");
                    naar(p, deur.midden(Plekken.SCHEDELPAAL.offset(0, 0, 2)));
                    Hoofdstuk.seconde(p);
                    helper.assertTrue(lijn.stap(p) == Hoofdstuk.WACHTHEK, "at the post of skulls: step 7");
                    naar(p, deur.midden(Plekken.VUUR_POORTJE.offset(1, 0, 1)));
                    Hoofdstuk.seconde(p);
                    helper.assertTrue(lijn.stap(p) == Hoofdstuk.POORTJE, "at the fire behind het Wachthek: step 8");
                    // the side door: bars for p, none for the friend; in the tunnel p is pushed back, the friend is not
                    Map<BlockPos, BlockState> bars = new HashMap<>();
                    Hoofdstuk.deurSchijn(p, bars);
                    helper.assertTrue(bars.size() == 9 && bars.values().stream().allMatch(b -> b.getBlock() instanceof IronBarsBlock),
                            "the side door looks barred to who may not pass yet: " + bars.size());
                    helper.assertTrue(bars.keySet().stream().allMatch(pos -> level.getBlockState(pos).isAir()), "while it really is air");
                    Map<BlockPos, BlockState> voorVriend = new HashMap<>();
                    Hoofdstuk.deurSchijn(vriend, voorVriend);
                    helper.assertTrue(voorVriend.isEmpty(), "the friend sees an open door");
                    Vec3 inTunnel = deur.midden(Plekken.TUNNEL.get(0).offset(1, 0, 1));
                    naar(p, inTunnel);
                    p.setDeltaMovement(Vec3.ZERO);
                    Hoofdstuk.snel(p);
                    helper.assertTrue(p.getDeltaMovement().multiply(1, 0, 1).length() > 0.1, "in the doorway: a shove back");
                    naar(vriend, inTunnel);
                    vriend.setDeltaMovement(Vec3.ZERO);
                    Hoofdstuk.seconde(vriend);
                    Hoofdstuk.snel(vriend);
                    helper.assertTrue(vriend.getDeltaMovement().lengthSqr() < 1e-6, "nothing stops the friend");
                    p.setDeltaMovement(Vec3.ZERO);
                    // in front of the door: Guhdalf de Witte
                    naar(p, deur.midden(Plekken.DEUR_SCENE));
                    Hoofdstuk.seconde(p);
                    helper.assertTrue(Cutscenes.bezig(p), "at the locked door: the scene of Guhdalf de Witte");
                    fase[0] = 5;
                }
                case 5 -> {
                    helper.assertTrue(stap == Hoofdstuk.ACHTER && Cutscenes.gezien(p, Scenes.GUHDALF.id()), "after the scene: step 9");
                    Map<BlockPos, BlockState> bars = new HashMap<>();
                    Hoofdstuk.deurSchijn(p, bars);
                    helper.assertTrue(bars.isEmpty(), "the door is open for p now");
                    naar(p, deur.midden(Plekken.TUNNEL.get(0).offset(1, 0, 3)));
                    p.setDeltaMovement(Vec3.ZERO);
                    Hoofdstuk.snel(p);
                    helper.assertTrue(p.getDeltaMovement().lengthSqr() < 1e-6, "and the tunnel lets them through");
                    onthoud.put("potjes", new Vec3(GuhQuests.count(p, RingFeature.STOOFPOTJE.get()), 0, 0));
                    naar(p, deur.midden(Plekken.VUUR_ACHTER.offset(1, 0, 1)));
                    Hoofdstuk.seconde(p);
                    helper.assertTrue(lijn.klaar(p) && Ring.hoofdstuk(p) == 6, "at the last fire: the chapter is done, chapter 6 is next");
                    helper.assertTrue(GuhQuests.count(p, RingFeature.STOOFPOTJE.get()) == (int) onthoud.get("potjes").x + 2, "two stoofpotjes for the climb, once");
                    helper.assertTrue(!Hoofdstuk.bezig(p) && Hoofdstuk.hier(p) == null, "the chapter lets go of the player");
                    fase[0] = 100;
                }
                default -> {
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(fase[0] == 100, "the chapter was played to its end (at phase " + fase[0] + ", step " + lijn.stap(p) + ")");
            boromika.discard();
            Terrein.wisAlles();
            weg(helper, p, vriend);
        });
    }

    /**
     * Nothing in the valley burns a player: sauce that welled up near a player of the chapter is filled in again, and fire
     * and sauce do no damage inside a copy (outside it they do what they always do).
     */
    @GuhTest(template = KAMER, batch = "ringh5_saus")
    public static void ringh5Saus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Terrein.wisAlles();
        Terrein t = veld(helper);
        ServerPlayer p = speler(helper, OPEN);
        klaarTot(p, 4);
        BlockPos plas = helper.absolutePos(OPEN.offset(3, 0, 1)), inVloer = helper.absolutePos(OPEN.offset(-2, -1, 2)), ver = helper.absolutePos(OPEN.offset(16, 0, 0));
        for (BlockPos pos : List.of(plas, inVloer, ver)) {
            level.setBlock(pos, nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get().defaultBlockState(), 2);
        }
        helper.assertTrue(Hoofdstuk.dempSaus(p, t) == 2, "the two springs near the player are filled in");
        helper.assertTrue(level.getBlockState(plas).isAir(), "above the floor: air again");
        helper.assertTrue(level.getBlockState(inVloer).is(nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature.HOUTSKOOLSTEEN.get()), "in the floor: rock again");
        helper.assertTrue(!level.getFluidState(ver).isEmpty(), "far from any player nothing is touched");
        level.setBlock(ver, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(inVloer, helper.getLevel().getBlockState(inVloer.north()), 2);
        // fire and sauce do no damage in a copy that is known, and do outside it
        // (a player whose client never said "loaded" can't be hurt at all)
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        float leven = p.getHealth();
        p.hurtServer(level, level.damageSources().lava(), 4f);
        helper.assertTrue(p.getHealth() < leven, "outside any valley sauce burns as ever");
        p.setHealth(p.getMaxHealth());
        p.invulnerableTime = 0;
        Terrein.test(t);
        helper.assertTrue(Terrein.kent(level, p.position()), "the stand-in valley is known now");
        p.igniteForSeconds(5);
        p.hurtServer(level, level.damageSources().lava(), 4f);
        helper.assertTrue(p.getHealth() == p.getMaxHealth(), "inside the valley nothing burns a player");
        helper.assertTrue(!p.isOnFire(), "and the flames are out");
        p.invulnerableTime = 0;
        p.hurtServer(level, level.damageSources().generic(), 1f);
        helper.assertTrue(p.getHealth() < p.getMaxHealth(), "(other damage is not this chapter's business)");
        Terrein.wisAlles();
        weg(helper, p);
        helper.succeed();
    }

    private RingH5GameTests() {
    }
}
