package nl.juiced.guhs.feature.snoesverhaal;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.blub.BlubSnoes;
import nl.juiced.guhs.feature.dromen.DroomSlapen;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.dromen.DroomVoortgang.Stukje;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.weer.WeerUnlocks;
import nl.juiced.guhs.feature.weer.Weertje;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of Snoes' story (batch "snoesverhaal"). The game test server has no datapack dimensions, so these test what
 * works without the dream: the prologue (Snoes appears, three talks tuck him in), a piece gives its weerpotje once, the
 * finale's rewards, and the finale scene's script.
 */
public class SnoesGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "snoesverhaal";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        DroomVoortgang.reset(p);
        SnoesVerhaal.vergeet(p);
        WeerUnlocks.wis(p);
        return p;
    }

    /** Snoes appears at a spot; three talks at bedtime tuck him in. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void proloogInstoppen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BlockPos bed = helper.absolutePos(new BlockPos(3, 1, 3));
        GuhNpcEntity snoes = SnoesVerhaal.verschijn(p, bed);
        helper.assertTrue(snoes != null && snoes.getKind() == GuhNpcEntity.Kind.SNOES, "Snoes appears");
        helper.assertTrue(SnoesVerhaal.fase(p) == SnoesVerhaal.Fase.BIJ_BED, "phase BIJ_BED");
        helper.assertTrue(SnoesVerhaal.snoesThuis(p) == snoes, "the player's Snoes is found back");
        boolean oud = DroomSlapen.altijd;
        DroomSlapen.altijd = true;
        try {
            SnoesVerhaal.praatSnoes(p, snoes);
            SnoesVerhaal.praatSnoes(p, snoes);
            helper.assertTrue(SnoesVerhaal.stap(snoes) == 2 && SnoesVerhaal.fase(p) == SnoesVerhaal.Fase.BIJ_BED, "two talks: still awake");
            SnoesVerhaal.praatSnoes(p, snoes);
            helper.assertTrue(SnoesVerhaal.fase(p) == SnoesVerhaal.Fase.INGESTOPT, "third talk: tucked in");
        } finally {
            DroomSlapen.altijd = oud;
        }
        SnoesVerhaal.vergeet(p);
        helper.assertTrue(!snoes.isAlive() && SnoesVerhaal.fase(p) == SnoesVerhaal.Fase.NIETS, "forgotten: Snoes gone");
        helper.succeed();
    }

    /** (1.8.0) The player sleeps at another bed while the story runs: Snoes moves there (one Snoes, same step). */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void snoesVerhuistMee(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BlockPos bed1 = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos bed2 = helper.absolutePos(new BlockPos(9, 1, 9));
        GuhNpcEntity eerste = SnoesVerhaal.verschijn(p, bed1);
        helper.assertTrue(eerste != null, "Snoes appears at the first bed");
        SnoesVerhaal.praatSnoes(p, eerste);
        GuhNpcEntity tweede = SnoesVerhaal.verhuis(p, bed2);
        helper.assertTrue(tweede != null && !eerste.isAlive(), "a far bed: the old Snoes goes, a new one comes");
        helper.assertTrue(SnoesVerhaal.snoesThuis(p) == tweede && tweede.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(bed2)) < 9,
                "the new Snoes is the player's, beside the new bed");
        helper.assertTrue(SnoesVerhaal.stap(tweede) == 1 && SnoesVerhaal.fase(p) == SnoesVerhaal.Fase.BIJ_BED, "same step of the story");
        helper.assertTrue(SnoesVerhaal.verhuis(p, bed2) == null, "the same bed again: he stays");
        SnoesVerhaal.vergeet(p);
        helper.succeed();
    }

    /** Each piece gives its island's weerpotje once (and the story is on). */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void stukjeGeeftWeerpotje(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        helper.assertTrue(DroomVoortgang.geef(p, Stukje.LIEDJE), "new piece");
        helper.assertTrue(SnoesVerhaal.fase(p) == SnoesVerhaal.Fase.DROOMT, "the story is on");
        helper.assertTrue(!WeerUnlocks.potjeEenmalig(p, Weertje.PLUISJES), "the pluisjes potje was already given");
        helper.assertTrue(DroomBronnen.weertjeVoor(Stukje.BAD) == Weertje.BELLEN && DroomBronnen.weertjeVoor(Stukje.STER) == Weertje.STERREN
                && DroomBronnen.weertjeVoor(Stukje.DROOMZAND) == Weertje.KNUSMIST && DroomBronnen.weertjeVoor(Stukje.HERINNERING) == Weertje.HARTJES,
                "every island its weertje");
        helper.assertTrue(DroomBronnen.fontein(p) && !DroomBronnen.fontein(p), "the fountain gives its potje once");
        helper.succeed();
    }

    /** After the finale: Snoes helped (wild Blubs), the regenboog potje, phase KLAAR. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void finaleBeloning(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        for (Stukje s : Stukje.values()) {
            DroomVoortgang.geef(p, s);
        }
        SnoesVerhaal.naFinale(p);
        helper.assertTrue(DroomVoortgang.finaleKlaar(p) && BlubSnoes.heeft(p), "finale done, Snoes helped");
        helper.assertTrue(SnoesVerhaal.fase(p) == SnoesVerhaal.Fase.KLAAR && SnoesVerhaal.klaar(p), "phase KLAAR");
        helper.assertTrue(!WeerUnlocks.potjeEenmalig(p, Weertje.REGENBOOG), "the regenboog potje was given");
        DroomVoortgang.reset(p);
        helper.assertTrue(SnoesVerhaal.fase(p) == SnoesVerhaal.Fase.NIETS && !BlubSnoes.heeft(p), "reset forgets the story");
        helper.succeed();
    }

    /** The finale scene is registered and has its cast, its lines and the lullaby. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void finaleScene(GameTestHelper helper) {
        Cutscene s = Cutscene.van(SnoesScenes.FINALE_ID);
        helper.assertTrue(s != null && s == SnoesScenes.FINALE, "registered");
        helper.assertTrue(s.acteur("snoes") != null && s.acteur("oma") != null && s.acteur("gids") != null, "the cast");
        helper.assertTrue(s.zinnen().size() >= 12 && s.duur() >= 600, "lines and length");
        helper.succeed();
    }

    /** Playtest: Bubbels on your head (and down again) or picked up never makes a second Bubbels. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void bubbelsNooitDubbel(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        p.snapTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 3.5)));
        DroomVoortgang.geef(p, Stukje.LIEDJE);                       // (the story is on: DROOMT)
        nl.juiced.guhs.feature.blub.BlubGuhEntity gids = SnoesVerhaal.roepGids(p);
        helper.assertTrue(gids != null && aantalGidsen(helper) == 1, "one Bubbels");
        nl.juiced.guhs.feature.piep.Schouder.zet(p, gids);
        helper.assertTrue(SnoesVerhaal.draagtGids(p) && SnoesVerhaal.gids(p) == null, "Bubbels on the head");
        helper.assertTrue(SnoesVerhaal.roepGids(p) == null && aantalGidsen(helper) == 0, "on the head: no second Bubbels");
        nl.juiced.guhs.feature.piep.Schouder.eraf(p, p.position());
        helper.assertTrue(SnoesVerhaal.roepGids(p) != null && aantalGidsen(helper) == 1, "down again: still one Bubbels");
        nl.juiced.guhs.feature.blub.BlubGuhEntity weer = SnoesVerhaal.gids(p);
        nl.juiced.guhs.feature.piep.PiepDierItem.pakOp(weer, p);
        helper.assertTrue(SnoesVerhaal.draagtGids(p) && SnoesVerhaal.roepGids(p) == null && aantalGidsen(helper) == 0,
                "picked up: no second Bubbels");
        SnoesVerhaal.vergeet(p);
        helper.succeed();
    }

    private static int aantalGidsen(GameTestHelper helper) {
        return helper.getLevel().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(nl.juiced.guhs.feature.blub.BlubGuhEntity.class),
                b -> b.isAlive() && b.entityTags().contains(SnoesVerhaal.TAG_GIDS)).size();
    }
}
