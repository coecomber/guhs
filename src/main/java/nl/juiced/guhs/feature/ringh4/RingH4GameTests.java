package nl.juiced.guhs.feature.ringh4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Cast;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.spiesburcht.BurchtStructure;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-h4): chapter 4 of the Knabbelring, server side. The test server has no Guhbarbecuether and the city is 192
 * blocks long, so the tests put down a try-out copy ({@link Boomstad#test}) whose spots lie in the test room
 * {@code ringh4_test_kamer} (tools/features/ring_h4_bouw.py test_template: a floor of 25 x 17, the mirror on a pedestal at
 * (4, 3, 4), a Rustvuurtje at (12, 2, 4), a strip of kaassaus along z 12..14 for the boat). Mock players get no packets and
 * are not ticked: the tests post their tick themselves (the story engine's lock ends a scene on it).
 */
public final class RingH4GameTests {
    private static final String KAMER = "ringh4_test_kamer";
    private static final BlockPos SPIEGEL = new BlockPos(4, 3, 4), VUUR = new BlockPos(12, 2, 4);

    private static ServerPlayer speler(GameTestHelper helper, int x, int z, int hoofdstukkenKlaar) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        zet(helper, p, x, z);
        Ring.OVERAL = true;
        Ring.lijn(1).begin(p);
        for (int n = 1; n <= hoofdstukkenKlaar; n++) {
            Verhaallijn l = Ring.lijn(n);
            l.zet(p, l.stappen());
        }
        return p;
    }

    private static void zet(GameTestHelper helper, ServerPlayer p, double x, double z) {
        Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 2, z + 0.5));
        p.snapTo(at.x, at.y, at.z);
        p.setOnGround(true);
    }

    private static void tik(ServerPlayer p) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
    }

    /** The try-out copy of this test room. */
    private static Boomstad.Kopie kopie(GameTestHelper helper) {
        Map<String, Vec3> plekken = new HashMap<>();
        Object[][] spots = {{"poort", 2, 2}, {"leguhlas_poort", 4, 2}, {"zaal", 8, 2}, {"gast", 12, 6}, {"dal", 6, 6}, {"steiger", 3, 10}, {"aanleg", 21, 10},
                {"leguhlas_steiger", 2, 9}, {"gimguh_steiger", 2, 11}};
        for (Object[] s : spots) {
            plekken.put((String) s[0], helper.absoluteVec(new Vec3((int) s[1] + 0.5, 2, (int) s[2] + 0.5)));
        }
        plekken.put("gast_vuur", helper.absoluteVec(new Vec3(VUUR.getX() + 0.5, VUUR.getY(), VUUR.getZ() + 0.5)));
        plekken.put("spiegel", helper.absoluteVec(new Vec3(SPIEGEL.getX() + 0.5, SPIEGEL.getY(), SPIEGEL.getZ() + 0.5)));
        plekken.put("boot", helper.absoluteVec(new Vec3(4.5, 1.72, 13.5)));
        List<Vec3> route = List.of(helper.absoluteVec(new Vec3(4.5, 1.72, 13.5)), helper.absoluteVec(new Vec3(12.5, 1.72, 13.0)),
                helper.absoluteVec(new Vec3(20.5, 1.72, 13.5)));
        return Boomstad.test(helper.getLevel(), helper.absolutePos(new BlockPos(12, 1, 8)), plekken, route);
    }

    private static GuhNpcEntity npc(GameTestHelper helper, Boomstad.Kopie kopie, GuhNpcEntity.Kind kind, String plek, String rol) {
        return Cast.zet(helper.getLevel(), kind, kopie.punt(plek), 0f, rol);
    }

    private static ElfenbootjeEntity boot(GameTestHelper helper, Vec3 plek, int soort) {
        ElfenbootjeEntity b = RingH4Feature.ELFENBOOTJE.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        b.setSoort(soort);
        b.snapTo(plek.x, plek.y, plek.z, -90f, 0f);
        helper.getLevel().addFreshEntity(b);
        return b;
    }

    private static void weg(GameTestHelper helper, Boomstad.Kopie kopie, ServerPlayer... ps) {
        Boomstad.testWeg(kopie.anker());
        for (ServerPlayer p : ps) {
            if (p.isPassenger()) {
                p.stopRiding();
            }
            Ring.wis(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        for (Entity e : helper.getLevel().getEntitiesOfClass(Entity.class, new net.minecraft.world.phys.AABB(kopie.anker()).inflate(20),
                e -> e instanceof ElfenbootjeEntity || e instanceof GuhNpcEntity)) {
            e.discard();
        }
    }

    /** What the chapter is made of: the structure and its tiles, the spots, the boat's path, the line, the card, the scene, the sluier. */
    @GuhTest(template = KAMER, batch = "ringh4")
    public static void ringh4DeBoomstadBestaat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Structure structure = Kopieen.structuur(level, Boomstad.STRUCTUUR);
        helper.assertTrue(structure instanceof BoomstadStructure, "guhs:guhladriel_boomstad has our own placement");
        BurchtStructure burcht = (BurchtStructure) structure;
        helper.assertTrue(burcht.anchor().equals(Plekken.ANKER), "the anchor of the structure is the anchor of plekken.json: " + burcht.anchor());
        int tegels = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                tegels += level.getStructureManager().get(burcht.tile(i, j)).isPresent() ? 1 : 0;
            }
        }
        helper.assertTrue(tegels >= 12, "the city is saved as tiles: " + tegels);
        helper.assertTrue(structure.terrainAdaptation() == net.minecraft.world.level.levelgen.structure.TerrainAdjustment.BEARD_THIN,
                "the cave floor meets the edges of the build");
        // exactly one copy per world: a guaranteed set, no random spread
        var sets = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET);
        helper.assertTrue(sets.getValue(nl.juiced.guhs.Guhs.id(Boomstad.STRUCTUUR + "_gegarandeerd")) != null
                && sets.getValue(nl.juiced.guhs.Guhs.id(Boomstad.STRUCTUUR)) == null, "one guaranteed copy, nothing else");
        // the spots and the path
        for (String plek : List.of("poort", "leguhlas_poort", "zaal", "gast", "gast_vuur", "spiegel", "dal", "steiger", "aanleg", "boot", "boot_terug", "uitgang")) {
            helper.assertTrue(Plekken.heeft(plek), "plekken.json has '" + plek + "'");
            Vec3 p = Plekken.punt(plek);
            helper.assertTrue(p.x >= 0 && p.x < Plekken.GROOTTE.getX() && p.z >= 0 && p.z < Plekken.GROOTTE.getZ() && p.y >= 0 && p.y < Plekken.GROOTTE.getY(),
                    plek + " lies inside the build: " + p);
        }
        helper.assertTrue(Plekken.ROUTE.size() >= 10, "the boat has a path");
        for (int i = 1; i < Plekken.ROUTE.size(); i++) {
            helper.assertTrue(Plekken.ROUTE.get(i).distanceTo(Plekken.ROUTE.get(i - 1)) < 6, "no jump in the boat's path at point " + i);
        }
        helper.assertTrue(Plekken.ROUTE.get(0).distanceTo(Plekken.punt("boot")) < 0.1, "the trip starts where the guide boat lies");
        helper.assertTrue(Plekken.ROUTE.get(Plekken.ROUTE.size() - 1).distanceTo(Plekken.punt("aanleg").add(0.5, 0, 0.5)) < 14, "and ends at the landing");
        // a copy turns its spots like the game turns the blocks of its tiles
        BlockPos anker = helper.absolutePos(new BlockPos(12, 1, 8));
        for (Rotation draai : Rotation.values()) {
            Boomstad.Kopie k = new Boomstad.Kopie(anker, draai, null, null);
            BlockPos lokaal = Plekken.blok("spiegel");
            BlockPos verwacht = StructureTemplate.transform(lokaal, Mirror.NONE, draai, Plekken.ANKER).subtract(Plekken.ANKER).offset(anker);
            helper.assertTrue(k.blok("spiegel").equals(verwacht), draai + ": the mirror is at " + k.blok("spiegel") + ", its block lands at " + verwacht);
            helper.assertTrue(k.route(true).get(0).distanceTo(k.route(false).get(Plekken.ROUTE.size() - 1)) < 1e-6, "back up is the same path the other way");
        }
        // the story
        Verhaallijn lijn = RingH4Feature.LIJN;
        helper.assertTrue(lijn == Ring.lijn(4) && lijn.stappen() == 7 && "ring_h3".equals(lijn.na()), "the questline ring_h4: 7 steps, after ring_h3");
        helper.assertTrue(Verteller.van(RingH4Feature.KAART) != null && Verteller.van(RingH4Feature.KAART).regels() == 4, "the narrator card");
        Cutscene scene = Cutscene.van(Spiegel.SCENE_ID);
        helper.assertTrue(scene == Spiegel.SCENE && scene.duur() == 1100, "the mirror scene is registered");
        helper.assertTrue(Sluiers.structuren().contains(Boomstad.STRUCTUUR), "the city lies behind Guhdalfs sluier");
        ServerPlayer p = speler(helper, 2, 2, 2), verder = speler(helper, 3, 2, 3);
        helper.assertTrue(!Sluiers.open(p, Boomstad.STRUCTUUR) && Sluiers.open(verder, Boomstad.STRUCTUUR), "closed until chapter 3 is done");
        helper.assertTrue(helper.getBlockState(SPIEGEL).is(RingH4Feature.SPIEGEL.get()), "the test room has the mirror");
        Ring.wis(p);
        Ring.wis(verder);
        level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        level.removePlayerImmediately(verder, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /**
     * The whole chapter for one player, from the gate to the landing, with a friend alongside whose own story is not here
     * yet: the friend solves nothing and gets nothing.
     */
    @GuhTest(template = KAMER, batch = "ringh4_verhaal", timeoutTicks = 1200)
    public static void ringh4VanPoortTotAanleg(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Boomstad.Kopie kopie = kopie(helper);
        ServerPlayer p = speler(helper, 14, 2, 3), vriend = speler(helper, 14, 3, 2);
        Verhaallijn lijn = RingH4Feature.LIJN;
        GuhNpcEntity leguhlas = npc(helper, kopie, GuhNpcEntity.Kind.LEGUHLAS, "leguhlas_poort", RingH4Feature.POORT);
        GuhNpcEntity guhladriel = npc(helper, kopie, GuhNpcEntity.Kind.GUHLADRIEL, "zaal", RingH4Feature.STAD);
        GuhNpcEntity steigerguh = npc(helper, kopie, GuhNpcEntity.Kind.LEGUHLAS, "leguhlas_steiger", RingH4Feature.STEIGER);
        ElfenbootjeEntity gidsboot = boot(helper, kopie.punt("boot"), ElfenbootjeEntity.GIDS);
        helper.assertTrue(lijn.aanDeBeurt(p) && lijn.stap(p) == 0 && !lijn.aanDeBeurt(vriend), "chapter 4 is p's, not the friend's");
        // far from the gate nothing happens
        RingH4Events.tik(p, true);
        helper.assertTrue(lijn.stap(p) == 0 && !Cutscenes.bezig(p), "not at the gate yet");
        zet(helper, p, 2, 2);
        zet(helper, vriend, 2, 3);
        RingH4Events.tik(vriend, true);
        NpcRollen.van(leguhlas).talk(leguhlas, vriend);
        NpcRollen.van(guhladriel).talk(guhladriel, vriend);
        Spiegel.kijk(vriend, helper.absolutePos(SPIEGEL));
        helper.assertTrue(lijn.stap(vriend) == 0 && !lijn.begonnen(vriend) && !Cutscenes.bezig(vriend) && !Vaart.stapIn(vriend, gidsboot),
                "a friend who is not this far gets small talk and no boat");
        RingH4Events.tik(p, true);
        helper.assertTrue(Cutscenes.bezig(p), "at the gate: the narrator card of the chapter");
        int[] fase = {0};
        ElfenbootjeEntity[] rit = {null};
        helper.onEachTick(() -> {
            tik(p);
            int stap = lijn.stap(p);
            switch (fase[0]) {
                case 0 -> {
                    if (stap == 1 && !Cutscenes.bezig(p)) {
                        helper.assertTrue(Verteller.gezien(p, RingH4Feature.KAART), "the card was read");
                        NpcRollen.van(guhladriel).talk(guhladriel, p);       // (she takes whoever ran past Leguhlas: 1 -> 3)
                        helper.assertTrue(lijn.stap(p) == 3, "Guhladriel's welcome: on to the rest, also for whoever skipped Leguhlas (" + lijn.stap(p) + ")");
                        NpcRollen.van(steigerguh).talk(steigerguh, p);
                        helper.assertTrue(!p.isPassenger(), "Leguhlas on the quay does not sail before the gifts");
                        zet(helper, p, VUUR.getX() + 1, VUUR.getZ());
                        fase[0] = 1;
                    }
                }
                case 1 -> {
                    RingH4Events.tik(p, true);                               // (waits for the after-care of the card)
                    if (stap == 4) {
                        helper.assertTrue(GuhQuests.saved(p).contains("guhs_ring_h4_stap") || lijn.stap(p) == 4, "rested");
                        NpcRollen.van(guhladriel).talk(guhladriel, p);
                        helper.assertTrue(lijn.stap(p) == 4 && !Gaven.heeft(p, RingFeature.LICHTFLESJE.get()), "no gifts before the mirror");
                        Spiegel.kijk(p, helper.absolutePos(SPIEGEL));
                        helper.assertTrue(Cutscenes.bezig(p) && lijn.stap(p) == 4, "the mirror scene plays; the step waits for its end");
                        fase[0] = 2;
                    }
                }
                case 2 -> {
                    if (stap == 5 && !Cutscenes.bezig(p)) {
                        helper.assertTrue(Cutscenes.gezien(p, Spiegel.SCENE_ID), "the scene was seen");
                        // PHASE3 R07: a second click on the mirror never locks the player into the 55 seconds again
                        Spiegel.kijk(p, helper.absolutePos(SPIEGEL));
                        helper.assertTrue(!Cutscenes.bezig(p) && lijn.stap(p) == 5, "the mirror is calm: no second scene, the Guhdex has the replay");
                        helper.assertTrue(!Vaart.stapIn(p, gidsboot), "no boat before the gifts");
                        NpcRollen.van(guhladriel).talk(guhladriel, p);
                        helper.assertTrue(lijn.stap(p) == 6 && Gaven.heeft(p, RingFeature.LICHTFLESJE.get()) && Gaven.heeft(p, RingFeature.ELFENMANTELTJE.get())
                                && Gaven.heeft(p, RingFeature.ELFENTOUW.get()), "the three gifts of Guhladriel");
                        // a gift that got lost comes back
                        p.getInventory().clearContent();
                        NpcRollen.van(guhladriel).talk(guhladriel, p);
                        helper.assertTrue(Gaven.heeft(p, RingFeature.ELFENTOUW.get()) && GuhQuests.count(p, RingFeature.ELFENTOUW.get()) == 1, "a lost gift is given again, once");
                        zet(helper, p, 3, 10);
                        fase[0] = 3;
                    }
                }
                case 3 -> {
                    if (Duwtje.mag(p)) {                                      // (the after-care of the scene is over)
                        NpcRollen.van(steigerguh).talk(steigerguh, p);       // "stap maar in"
                        helper.assertTrue(p.getVehicle() instanceof ElfenbootjeEntity b && b.soort() == ElfenbootjeEntity.RIT, "Leguhlas lets p into a trip boat");
                        rit[0] = (ElfenbootjeEntity) p.getVehicle();
                        helper.assertTrue(gidsboot.isWeg() && !gidsboot.isPickable() && steigerguh.isInvisible() && rit[0].bemand() && !rit[0].vaart(),
                                "the moored boat and the guides on the quay are out of sight; the trip waits for a friend");
                        fase[0] = 4;
                    }
                }
                case 4 -> {
                    if (rit[0].vaart() && rit[0].fractie() > 0.3) {
                        p.setShiftKeyDown(true);
                        p.stopRiding();
                        p.setShiftKeyDown(false);
                        helper.assertTrue(p.getVehicle() == rit[0], "nobody gets out of a sailing boat");
                        helper.assertTrue(lijn.stap(p) == 6, "not done before the landing");
                        fase[0] = 5;
                    }
                }
                default -> {
                    if (lijn.klaar(p)) {
                        helper.assertTrue(!p.isPassenger() && p.position().distanceTo(kopie.punt("aanleg")) < 2.5, "put ashore at the landing: " + p.position());
                        helper.assertTrue(rit[0].isRemoved() && !gidsboot.isWeg() && !steigerguh.isInvisible(), "the trip is gone, the boat and the guides are back");
                        helper.assertTrue(Ring.hoofdstuk(p) == 5 && Ring.rustpunt(p) != null, "chapter 5 is next; the landing is the rest point");
                        helper.assertTrue(p.getHealth() == p.getMaxHealth(), "nothing in the chapter hurts");
                        weg(helper, kopie, p, vriend);
                        helper.succeed();
                    }
                }
            }
        });
    }

    /** The boats: who may sail where, a second passenger, logging out half-way, the bank reached without a boat, the blessing. */
    @GuhTest(template = KAMER, batch = "ringh4_boot", timeoutTicks = 600)
    public static void ringh4BootjesEnZegen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Boomstad.Kopie kopie = kopie(helper);
        ServerPlayer p = speler(helper, 3, 10, 3), maat = speler(helper, 4, 10, 4), zwemmer = speler(helper, 5, 10, 3);
        Verhaallijn lijn = RingH4Feature.LIJN;
        lijn.zet(p, 6);
        lijn.zet(zwemmer, 6);
        ElfenbootjeEntity gidsboot = boot(helper, kopie.punt("boot"), ElfenbootjeEntity.GIDS);
        ElfenbootjeEntity deco = boot(helper, kopie.punt("boot").add(0, 0, -1.5), ElfenbootjeEntity.DECO);
        ElfenbootjeEntity terug = boot(helper, kopie.route(false).get(2), ElfenbootjeEntity.TERUG);
        helper.assertTrue(!Vaart.stapIn(p, deco) && !p.isPassenger(), "a boat to look at");
        helper.assertTrue(!Vaart.stapIn(p, terug) && Vaart.stapIn(maat, terug), "the boat back is for whoever finished the chapter");
        ElfenbootjeEntity omhoog = (ElfenbootjeEntity) maat.getVehicle();
        helper.assertTrue(omhoog.terug && !omhoog.bemand() && omhoog.pad().get(0).distanceTo(kopie.route(false).get(2)) < 0.1, "it sails the path the other way, without guides");
        omhoog.eindig(false);
        helper.assertTrue(!maat.isPassenger() && omhoog.isRemoved() && !terug.isWeg(), "a trip that is broken off: ashore, the moored boat is back");
        // p gets in, the friend who is done comes along in the same boat
        helper.assertTrue(Vaart.stapIn(p, gidsboot) && p.getVehicle() instanceof ElfenbootjeEntity, "p sits in a trip boat");
        ElfenbootjeEntity rit = (ElfenbootjeEntity) p.getVehicle();
        helper.assertTrue(!Vaart.stapIn(maat, gidsboot) && Vaart.stapIn(maat, rit) && maat.getVehicle() == rit && rit.getPassengers().size() == 2,
                "a second player joins the waiting trip");
        helper.assertTrue(!Vaart.stapIn(zwemmer, rit), "two seats");
        // the fall blessing: only inside the city's box
        LivingFallEvent buiten = new LivingFallEvent(zwemmer, 20, 1f);
        RingH4Events.onFall(buiten);
        helper.assertTrue(buiten.getDamageMultiplier() == 1f, "outside the city a fall is a fall");
        BlockPos hoek = helper.absolutePos(BlockPos.ZERO);
        Sluiers.zetPlek(level, Boomstad.STRUCTUUR, new BoundingBox(hoek.getX(), hoek.getY(), hoek.getZ(), hoek.getX() + 24, hoek.getY() + 7, hoek.getZ() + 16));
        LivingFallEvent binnen = new LivingFallEvent(zwemmer, 20, 1f);
        RingH4Events.onFall(binnen);
        Sluiers.wisPlekken(level, Boomstad.STRUCTUUR);
        helper.assertTrue(binnen.getDamageMultiplier() == 0f, "Guhladriel's blessing: no fall damage in the tree city");
        // whoever reaches the far bank without the boat is done too
        zet(helper, zwemmer, 21, 10);
        RingH4Events.tik(zwemmer, true);
        helper.assertTrue(lijn.klaar(zwemmer), "the boat is the way, not a lock");
        int[] fase = {0};
        helper.onEachTick(() -> {
            if (fase[0] == 0 && rit.vaart() && rit.fractie() > 0.25) {
                // p logs out half-way: the trip ends for both, back on the jetty they left from, nothing is lost
                RingH4Events.onLogout(new PlayerEvent.PlayerLoggedOutEvent(p));
                helper.assertTrue(rit.isRemoved() && !p.isPassenger() && !maat.isPassenger() && !gidsboot.isWeg(), "logged out: the trip is over");
                helper.assertTrue(p.position().distanceTo(kopie.punt("steiger")) < 2.5 && lijn.stap(p) == 6, "back on the jetty, still at step 6: " + p.position());
                helper.assertTrue(Vaart.stapIn(p, gidsboot), "and the boat takes p again");
                fase[0] = 1;
            } else if (fase[0] == 1 && lijn.klaar(p)) {
                helper.assertTrue(p.position().distanceTo(kopie.punt("aanleg")) < 2.5, "the second trip arrives");
                weg(helper, kopie, p, maat, zwemmer);
                helper.succeed();
            }
        });
    }

    private RingH4GameTests() {
    }
}
