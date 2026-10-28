package nl.juiced.guhs.feature.herinnering;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Game tests of the Herinneringseiland (batch "herinnering"). The game test server has no datapack dimensions, so these test
 * what works without the dream: the island template and its json, the three bubbles and their films, the last bubble (the
 * stars) opening after the other two, and Snoes' piece.
 */
public class HerinneringGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "herinnering";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        DroomVoortgang.reset(p);
        for (HerinneringBellen.Bel b : HerinneringBellen.BELLEN) {
            GuhQuests.saved(p).remove("guhs_scene_" + b.scene().id());
        }
        HerinneringBellen.vergeet(p);
        return p;
    }

    /** As Cutscenes does when a film was watched to the end. */
    static void gezien(ServerPlayer p, HerinneringBellen.Bel b) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Gezien", true);
        GuhQuests.saved(p).put("guhs_scene_" + b.scene().id(), t);
    }

    /** The template exists, fits part A's box, and the json points at it with an arrival and a steiger. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void eilandTemplateEnJson(GameTestHelper helper) {
        var template = helper.getLevel().getStructureManager().get(Guhs.id("dromen/eiland_herinnering")).orElse(null);
        helper.assertTrue(template != null, "template dromen/eiland_herinnering exists");
        Vec3i maat = template.getSize(), max = DroomEilanden.MAX_MAAT;
        helper.assertTrue(maat.getX() <= max.getX() && maat.getY() <= max.getY() && maat.getZ() <= max.getZ(), "fits the island box: " + maat);
        DroomEilanden.Opzet o = DroomEilanden.opzet(DroomEilanden.HERINNERING);
        helper.assertTrue(o.versie() >= 2 && o.template() != null && o.template().equals(Guhs.id("dromen/eiland_herinnering")),
                "the json is read: " + o);
        helper.assertTrue(o.steiger() != null && o.steiger().horizontalDistance() > 25, "a steiger beyond the island's rim");
        helper.succeed();
    }

    /** Three bubbles with three registered films, each found at its own middle; the stars are the last. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void drieBellenDrieFilmpjes(GameTestHelper helper) {
        Set<String> ids = new HashSet<>();
        for (HerinneringBellen.Bel b : HerinneringBellen.BELLEN) {
            helper.assertTrue(Cutscene.van(b.scene().id()) == b.scene(), "film registered: " + b.scene().id());
            helper.assertTrue(HerinneringBellen.bij(b.midden(), 0) == b, "the bubble at its own middle: " + b.id());
            helper.assertTrue(b.scene().duur() >= 300, "a real little film: " + b.id());
            ids.add(b.id());
        }
        helper.assertTrue(ids.size() == 3 && HerinneringScenes.ALLE.size() == 3, "three bubbles, three films");
        helper.assertTrue(HerinneringBellen.LAATSTE.scene() == HerinneringScenes.STERREN, "the stars are the last film");
        helper.assertTrue(HerinneringBellen.bij(DroomEilanden.wereld(DroomEilanden.HERINNERING, new Vec3(0.5, 30, 0.5)), 1) == null,
                "no bubble high in the sky");
        helper.succeed();
    }

    /** The last bubble (the stars) stays shut until the other two films were seen; then it gives the piece, once. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void sterrenNaDeAndereTwee(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        helper.assertTrue(HerinneringBellen.open(p, HerinneringBellen.BELLEN.get(0)), "the knabbel is open from the start");
        helper.assertTrue(HerinneringBellen.open(p, HerinneringBellen.BELLEN.get(1)), "the Blub is open from the start");
        helper.assertFalse(HerinneringBellen.open(p, HerinneringBellen.LAATSTE), "the stars start shut");
        gezien(p, HerinneringBellen.BELLEN.get(0));
        helper.assertFalse(HerinneringBellen.open(p, HerinneringBellen.LAATSTE), "one seen: still shut");
        HerinneringBellen.na(p, HerinneringBellen.BELLEN.get(0));
        helper.assertFalse(DroomVoortgang.heeft(p, DroomVoortgang.Stukje.HERINNERING), "not the piece after the first film");
        gezien(p, HerinneringBellen.BELLEN.get(1));
        helper.assertTrue(HerinneringBellen.open(p, HerinneringBellen.LAATSTE), "two seen: the stars open");
        HerinneringBellen.na(p, HerinneringBellen.LAATSTE);
        helper.assertTrue(DroomVoortgang.heeft(p, DroomVoortgang.Stukje.HERINNERING), "the last film gives the piece");
        helper.assertFalse(HerinneringBellen.stukje(p), "only once");
        DroomVoortgang.reset(p);
        DroomVoortgang.zetFinale(p);
        helper.assertFalse(HerinneringBellen.stukje(p), "after the finale the story is over: no piece");
        DroomVoortgang.reset(p);
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }
}
