package nl.juiced.guhs.feature.einde2.audio;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice audio: the two state machines of the slice. (1) The music area of a player: story structures only behind an
 * open veil, side places by their element, the margins, the forced area of the dev command. (2) The names of 6.1 (all
 * registered, the BPM table, the categories), a moment begun and stopped on the server, the credits book given once, the
 * timing of Miroh's lullaby.
 */
public class E2AudioGameTests {
    private static final String BATCH = "einde2_audio";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2AudioPlek(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BlockPos pos = p.blockPosition();
        BoundingBox doos = new BoundingBox(pos.getX() - 5, pos.getY() - 5, pos.getZ() - 5, pos.getX() + 5, pos.getY() + 5, pos.getZ() + 5);
        try {
            // the pure rule: the margins, the element of the side places, a closed story structure says nothing
            Map<String, BoundingBox> lucht = Map.of(Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, doos);
            helper.assertTrue(AudioPlek.gebied(lucht, pos, s -> true).equals(AudioNamen.LUCHT)
                    && AudioPlek.gebied(lucht, pos.east(5 + AudioPlek.RAND_XZ), s -> true).equals(AudioNamen.LUCHT)
                    && AudioPlek.gebied(lucht, pos.east(6 + AudioPlek.RAND_XZ), s -> true).isEmpty()
                    && AudioPlek.gebied(lucht, pos.above(6 + AudioPlek.RAND_Y), s -> true).isEmpty()
                    && AudioPlek.gebied(lucht, pos, s -> false).isEmpty(), "the box with its margins, and only when open");
            helper.assertTrue(AudioPlek.gebiedVan(Einde2Ids.KYOSHI_DORP).equals(AudioNamen.WATER)
                    && AudioPlek.gebiedVan(Einde2Ids.OMASHGUH).equals(AudioNamen.AARDE)
                    && AudioPlek.gebiedVan(Einde2Ids.VUURNATIE_HOOFDSTAD).equals(AudioNamen.VUUR)
                    && AudioPlek.gebiedVan(Einde2Ids.LEEGTEHAVEN) == null, "the areas of the places");
            // the real path for a player: a new player has the air temple behind its veil, so no music there
            AudioPlek.TEST_DOZEN.put(Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, doos);
            helper.assertTrue(AudioPlek.werkBij(p).isEmpty() && AudioPlek.gebiedNu(p).isEmpty(), "a closed story structure: no music");
            AudioPlek.TEST_DOZEN.clear();
            AudioPlek.TEST_DOZEN.put(Einde2Ids.WESTELIJKE_LUCHTTEMPEL, doos);
            helper.assertTrue(AudioPlek.werkBij(p).equals(AudioNamen.LUCHT), "the Western Air Temple: lucht");
            AudioPlek.TEST_DOZEN.clear();
            AudioPlek.TEST_DOZEN.put(Einde2Ids.LEEGTEHAVEN, doos);
            helper.assertTrue(AudioPlek.werkBij(p).isEmpty() && AudioPlek.gebiedNu(p).isEmpty(), "the harbour: no element, no music");
            AudioPlek.TEST_DOZEN.put(Einde2Ids.KYOSHI_DORP, doos);
            helper.assertTrue(AudioPlek.werkBij(p).equals(AudioNamen.WATER), "Kyoshi's island: water");
            // the dev command can force one, and let go again
            AudioPlek.forceer(p, AudioNamen.VUUR);
            helper.assertTrue(AudioPlek.gebiedNu(p).equals(AudioNamen.VUUR), "forced: vuur");
            AudioPlek.forceer(p, null);
            helper.assertTrue(AudioPlek.gebiedNu(p).equals(AudioNamen.WATER), "let go: the world again");
        } finally {
            AudioPlek.TEST_DOZEN.clear();
            AudioPlek.forceer(p, null);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2AudioNamenEnMoment(GameTestHelper helper) {
        List<String> alle = AudioNamen.alle();
        Set<String> uniek = new HashSet<>(alle);
        helper.assertTrue(alle.size() == 51 && uniek.size() == 51, "51 names in 6.1: " + alle.size());
        for (String naam : alle) {
            helper.assertTrue(E2Audio.holder(naam) != null && BuiltInRegistries.SOUND_EVENT.containsKey(Guhs.id("einde2_audio." + naam))
                    && E2Audio.geluid(naam).get().location().equals(Guhs.id("einde2_audio." + naam)), "registered: " + naam);
        }
        helper.assertTrue(alle.containsAll(E2Audio.BPM.keySet()) && E2Audio.BPM.get("deuntje.dansfeest") == 100, "the BPM table names real tunes");
        helper.assertTrue(AudioNamen.categorie("moment.golf") == SoundSource.MUSIC && AudioNamen.categorie("muziek.lucht_1") == SoundSource.MUSIC
                && AudioNamen.categorie("geest.klok") == SoundSource.AMBIENT && AudioNamen.lang("moment.lu_ten") && !AudioNamen.lang("stinger.kolen"),
                "the categories");
        helper.assertTrue(AudioNamen.muziekVan(AudioNamen.GEESTENWERELD).equals(List.of("muziek.geest_1", "muziek.geest_2"))
                && AudioNamen.muziekVan("").isEmpty(), "two pieces per area");
        // the lullaby: lines in order, inside the tune
        int vorige = -1;
        for (LuTenLied.Regel r : LuTenLied.REGELS) {
            helper.assertTrue(r.tick() > vorige && r.tick() + r.ticks() <= LuTenLied.DUUR, "the lullaby's line " + r.key());
            vorige = r.tick();
        }
        ServerPlayer p = speler(helper);
        try {
            E2Audio.moment(p, "golf");
            helper.assertTrue("moment.golf".equals(E2Audio.moment(p)), "the moment begins");
            E2Audio.moment(p, "bestaat_niet");
            helper.assertTrue("moment.golf".equals(E2Audio.moment(p)), "an unknown moment changes nothing");
            E2Audio.stopMoment(p, 40);
            helper.assertTrue(E2Audio.moment(p) == null, "stopped");
            // the credits book: once, with every page
            helper.assertTrue(Muziekboekje.geefEenmaal(p) && !Muziekboekje.geefEenmaal(p) && GuhQuests.saved(p).getBooleanOr(Muziekboekje.GEGEVEN, false),
                    "the book is given once");
            ItemStack boek = Muziekboekje.stack();
            var inhoud = boek.get(DataComponents.WRITTEN_BOOK_CONTENT);
            helper.assertTrue(inhoud != null && inhoud.pages().size() == Muziekboekje.PAGINAS && p.getInventory().contains(s -> s.has(DataComponents.CUSTOM_DATA)
                    && s.get(DataComponents.CUSTOM_DATA).copyTag().getBooleanOr(Muziekboekje.TAG, false)), "the book has its pages and is in the inventory");
        } finally {
            GuhQuests.saved(p).remove(Muziekboekje.GEGEVEN);
            E2Audio.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }
}
