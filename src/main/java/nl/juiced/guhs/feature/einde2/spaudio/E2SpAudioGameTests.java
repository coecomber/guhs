package nl.juiced.guhs.feature.einde2.spaudio;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * einde2 "Sponsguh", slice sp_audio (CONTRACT_E2_C 8.3): (1) every name registered, the BPM table, the categories, the three
 * songs (lines in order inside the song, {@link SpAudio#lied} puts them in a scene as captions that never wait and records
 * the start tick); (2) the music-area rule (closed sluier = silence, the loopgraaf inside Schelpenstad) and the moment and
 * tune state of a player (a tune ends when the area changes, an unknown moment changes nothing).
 */
public class E2SpAudioGameTests {
    private static final String BATCH = "einde2_sp_audio";
    private static final String EMPTY = "empty";

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2SpAudioNamenEnLiederen(GameTestHelper helper) {
        Set<String> uniek = new HashSet<>(SpAudio.NAMEN);
        helper.assertTrue(SpAudio.NAMEN.size() == 28 && uniek.size() == 28, "28 names in 6.1: " + SpAudio.NAMEN.size());
        for (String naam : SpAudio.NAMEN) {
            helper.assertTrue(SpAudio.holder(naam) != null && BuiltInRegistries.SOUND_EVENT.containsKey(Guhs.id("einde2_sp_audio." + naam))
                    && SpAudio.geluid(naam).get().location().equals(Guhs.id("einde2_sp_audio." + naam)), "registered: " + naam);
        }
        helper.assertTrue(SpAudio.DEUNTJES.containsAll(SpAudio.BPM.keySet()) && SpAudio.BPM.get("deuntje.trommel") == 112
                && SpAudio.BPM.get("deuntje.kroeg") == 96, "the BPM table");
        helper.assertTrue(SpAudio.categorie("moment.plan_z") == SoundSource.MUSIC && SpAudio.categorie("muziek.loopgraaf") == SoundSource.MUSIC
                && SpAudio.categorie("stinger.lach") == SoundSource.RECORDS, "the categories");
        helper.assertTrue(SpAudio.LIEDEREN.keySet().equals(SpLiederen.ALLE.keySet()), "three songs");
        for (SpLiederen.Lied l : SpLiederen.ALLE.values()) {
            helper.assertTrue(l.scene().equals(SpAudio.LIEDEREN.get(l.naam())) && SpAudio.MOMENTEN.contains("moment." + l.naam()),
                    "the song's scene and moment: " + l.naam());
            int vorige = -1;
            for (SpLiederen.Regel r : l.regels()) {
                helper.assertTrue(r.tick() > vorige && r.tick() + r.ticks() <= l.duur() && r.ticks() >= 10, "line " + r.key() + " of " + l.naam());
                vorige = r.tick();
            }
            // into a scene: every line a caption that runs on (no pause), from vanaf on; the start tick is remembered
            SpLiederen.Start was = SpLiederen.IN_SCENE.get(l.scene());
            try {
                Cutscene.Builder b = Cutscene.maak(l.scene()).duur(40 + l.duur() + 20);
                int lengte = SpAudio.lied(b, l.naam(), 40);
                Cutscene c = b.bouw();
                helper.assertTrue(lengte == l.duur() && c.zinnen().size() == l.regels().size() && c.pauzes().length == 0
                        && c.zinnen().get(0).t() == 40 && c.zinnen().stream().allMatch(Cutscene.Zeg::lied), "lied() in a scene: " + l.naam());
                SpLiederen.Start s = SpLiederen.inScene(l.scene());
                helper.assertTrue(s != null && s.lied().equals(l.naam()) && s.vanaf() == 40, "the start tick: " + l.naam());
            } finally {
                if (was == null) {
                    SpLiederen.IN_SCENE.remove(l.scene());
                } else {
                    SpLiederen.IN_SCENE.put(l.scene(), was);
                }
            }
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2SpAudioPlekEnMoment(GameTestHelper helper) {
        // the pure area rule
        helper.assertTrue(SpAudioPlek.gebied(true, false, false, true).equals(SpAudioPlek.BIKINIBODEM)
                && SpAudioPlek.gebied(false, true, false, true).equals(SpAudioPlek.SCHELPENSTAD)
                && SpAudioPlek.gebied(false, true, true, true).equals(SpAudioPlek.LOOPGRAAF)
                && SpAudioPlek.gebied(true, false, false, false).isEmpty() && SpAudioPlek.gebied(false, true, true, false).isEmpty()
                && SpAudioPlek.gebied(false, false, false, true).isEmpty(), "the area rule (and silence behind a closed sluier)");
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        try {
            helper.assertTrue(SpAudioPlek.gebied(p).isEmpty(), "the test level is no Guheinde: no area");
            SpAudio.moment(p, "plan_z");
            helper.assertTrue("moment.plan_z".equals(SpAudio.moment(p)), "the moment begins");
            SpAudio.moment(p, "bestaat_niet");
            helper.assertTrue("moment.plan_z".equals(SpAudio.moment(p)), "an unknown moment changes nothing");
            SpAudio.moment(p, "zoete_zege");
            helper.assertTrue("moment.zoete_zege".equals(SpAudio.moment(p)), "a song is a moment");
            SpAudio.stopMoment(p, 40);
            helper.assertTrue(SpAudio.moment(p) == null, "stopped");
            // a tune through stinger: the player's tune; a change of area ends it; stopDeuntje ends it
            SpAudio.stinger(p, "deuntje.trommel");
            helper.assertTrue("deuntje.trommel".equals(SpAudio.deuntje(p)), "the tune plays");
            SpAudio.stinger(p, "stinger.lach");
            helper.assertTrue("deuntje.trommel".equals(SpAudio.deuntje(p)), "a stinger leaves the tune alone");
            SpAudioPlek.forceer(p, SpAudioPlek.BIKINIBODEM);
            helper.assertTrue(SpAudio.deuntje(p) == null && SpAudioPlek.gebiedNu(p).equals(SpAudioPlek.BIKINIBODEM), "another area: the tune ends");
            SpAudio.stinger(p, "deuntje.bakken");
            SpAudio.stopDeuntje(p);
            helper.assertTrue(SpAudio.deuntje(p) == null, "stopDeuntje");
            SpAudioPlek.forceer(p, null);
            helper.assertTrue(SpAudioPlek.gebiedNu(p).isEmpty(), "back to what the world says");
        } finally {
            SpAudioPlek.forceer(p, null);
            SpAudio.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }
}
