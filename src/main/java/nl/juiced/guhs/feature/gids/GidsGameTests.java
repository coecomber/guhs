package nl.juiced.guhs.feature.gids;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.Highscores;

/**
 * De gids (2.9): the Superkompas tabs (icons, subheadings, every game in the Minigames tab, Knus and Barbecue unchanged),
 * the data of the Guhdex's Minigames and Kleding tabs (every Highscores row in exactly one building, every piece in exactly
 * one source group, hair apart), the texts, and the two explorer advancements.
 */
public class GidsGameTests {
    private static final String EMPTY = "empty";

    private static ServerPlayer player(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    /** The Superkompas: at most 14 icon tabs with a real icon, known ids kept, every structure exists, every game in Minigames. */
    @GuhTest(template = EMPTY)
    public static void gidsSuperkompasTabs(GameTestHelper helper) {
        var cats = SuperkompasItem.CATEGORIES;
        helper.assertTrue(cats.size() <= 14, "fits on one row of icon tabs: " + cats.size());
        Set<String> ids = new HashSet<>();
        var structures = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (SuperkompasItem.Category c : cats) {
            helper.assertTrue(ids.add(c.id()), "tab once: " + c.id());
            helper.assertTrue(!c.icoon().isEmpty(), "an icon for " + c.id());
            helper.assertTrue(!c.structures().isEmpty(), "places in " + c.id());
            Set<String> inTab = new HashSet<>();
            for (String s : c.structures()) {
                helper.assertTrue(structures.containsKey(Guhs.id(s)), "structure exists: " + s);
                helper.assertTrue(inTab.add(s), "once per tab: " + s);
            }
        }
        // the ids the other features rely on stay, Knus exactly as 2.8 made it
        SuperkompasItem.Category knus = cats.stream().filter(c -> c.id().equals("knus")).findFirst().orElseThrow();
        helper.assertTrue(knus.structures().equals(List.of("knuffeldal_stadje", "guhboerderij", "guh_sterrenwacht", "ballonfestival", "kampeerplekje",
                "knuffelbad")), "knus unchanged: " + knus.structures());
        helper.assertTrue(ids.contains("barbecue") && cats.stream().filter(c -> c.id().equals("avontuur")).findFirst().orElseThrow().structures()
                .contains("kaasknabbel_nest"), "barbecue stays, the nest stays in avontuur");
        // one Minigames tab with the subheadings, and every minigame building of the Guhdex in it
        SuperkompasItem.Category mg = cats.stream().filter(c -> c.id().equals("minigames")).findFirst().orElseThrow();
        helper.assertTrue(mg.kopjes().stream().map(SuperkompasItem.Kopje::id).toList().equals(List.of("klassiekers", "knuffeldal", "grote_guhspelen",
                "verhalen")), "the subheadings (3.0: + verhalen): " + mg.kopjes());
        SuperkompasItem.Category verhalen = cats.stream().filter(c -> c.id().equals("verhalen")).findFirst().orElseThrow();
        helper.assertTrue(verhalen.structures().equals(List.of("nomguh", "kloon_eiland", "hemelkapelletje", "guhwaii_ohana", "guhwaii_capsule",
                "guhwaii_surfstrand", "knuffeldal_stadje")), "3.0: the Verhalen tab: " + verhalen.structures());
        for (SpelGroepen.Groep g : SpelGroepen.alle()) {
            if (g.structuur() != null) {
                helper.assertTrue(mg.structures().contains(g.structuur()), "the Minigames tab has " + g.structuur() + " (" + g.id() + ")");
            }
        }
        SuperkompasItem.Kopje groot = mg.kopjes().get(2);
        helper.assertTrue(groot.structures().equals(List.of("sjoelhuisje", "guhdoolhof", "knabbelkatapult", "knabbelspelen", "elfguhjestocht", "guh_circuit")),
                "De Grote Guhspelen: " + groot.structures());
        for (String s : groot.structures()) {
            helper.assertTrue(SuperkompasItem.allowed(s), "the compass may look for " + s);
        }
        helper.assertTrue(SuperkompasItem.categoryOf("sjoelhuisje") == cats.indexOf(mg) && SuperkompasItem.categoryOf("nergens") == -1
                && SuperkompasItem.categoryOf(null) == -1, "the tab of a place");
        helper.assertTrue(GidsData.groepVanStructuur("guhdoolhof") == SpelGroepen.van("doolhof") && GidsData.groepVanStructuur("guh_kasteel") == null,
                "a place's game");
        helper.succeed();
    }

    /** The Minigames tab: every Highscores row sits in exactly one building, in the order of its group, with a short label. */
    @GuhTest(template = EMPTY)
    public static void gidsMinigamesRijen(GameTestHelper helper) {
        Map<String, String> waar = new HashMap<>();
        for (SpelGroepen.Groep g : SpelGroepen.alle()) {
            List<Highscores.Game> rijen = GidsData.rijen(g);
            helper.assertTrue(rijen.size() == g.spellen().size(), "every row of " + g.id() + " exists");
            for (int i = 0; i < rijen.size(); i++) {
                helper.assertTrue(rijen.get(i).id().equals(g.spellen().get(i)), "in order: " + g.id());
                String before = waar.put(rijen.get(i).id(), g.id());
                helper.assertTrue(before == null, rijen.get(i).id() + " in " + before + " and " + g.id());
            }
        }
        for (Highscores.Game game : Highscores.GAMES) {
            helper.assertTrue(waar.containsKey(game.id()), "the Minigames tab shows row " + game.id());
        }
        helper.assertTrue(GidsData.rijLabel("doolhof_lastig").equals("gui.guhs.gids.rij.doolhof_lastig"), "label key");
        helper.assertTrue(waar.get("disco_boogie").equals("disco") && waar.get("race_lap_lastig").equals("race") && waar.get("elfguhjestocht").equals("elftocht")
                && waar.get("glijbaan_grote_plons").equals("knuffelbad"), "rows in the right building");
        helper.succeed();
    }

    /** The Kleding tab: every unlockable piece in exactly one group (per source, kinds in order), the hairstyles apart, counts. */
    @GuhTest(template = EMPTY)
    public static void gidsKledingGroepen(GameTestHelper helper) {
        List<GidsData.KledingGroep> groepen = GidsData.kledingGroepen();
        Set<GuhClothes> gezien = new HashSet<>();
        GidsData.Soort vorige = null;
        Set<String> bronnen = new HashSet<>();
        for (GidsData.KledingGroep g : groepen) {
            helper.assertTrue(!g.stukken().isEmpty(), "no empty groups: " + g.bron());
            helper.assertTrue(bronnen.add(g.bron()), "a source once: " + g.bron());
            helper.assertTrue(vorige == null || g.soort().ordinal() >= vorige.ordinal(), "kinds in order at " + g.bron());
            vorige = g.soort();
            helper.assertTrue(g.soort() == GidsData.soort(g.bron()), "kind of " + g.bron());
            for (GuhClothes c : g.stukken()) {
                helper.assertTrue(gezien.add(c), "a piece once: " + c.id());
                helper.assertTrue(c.slot != GuhClothes.Slot.HAAR, "no hairstyles among the unlocks: " + c.id());
                String bron = KledingBronnen.bron(c);
                helper.assertTrue(g.bron().equals(bron == null ? GidsData.ZONDER_BRON : bron), c.id() + " in its own source");
            }
        }
        helper.assertTrue(gezien.size() == GidsData.ontgrendelbare().size(), "every unlockable piece: " + gezien.size());
        helper.assertTrue(!GidsData.kapsels().isEmpty() && GidsData.kapsels().stream().allMatch(c -> c.slot == GuhClothes.Slot.HAAR)
                && GidsData.kapsels().size() + GidsData.ontgrendelbare().size() == GuhClothes.values().length, "the hairstyles apart");
        helper.assertTrue(GidsData.soort("loot_kasteel") == GidsData.Soort.SCHATKISTEN && GidsData.soort("beroep_politie") == GidsData.Soort.BEROEPEN
                && GidsData.soort("kleermaker") == GidsData.Soort.WINKELS && GidsData.soort("sjoelen") == GidsData.Soort.GROTE_GUHSPELEN
                && GidsData.soort("golf") == GidsData.Soort.KLASSIEKERS && GidsData.soort("knuffelbad") == GidsData.Soort.KNUFFELDAL_SPELLETJES
                && GidsData.soort("sterrenwacht") == GidsData.Soort.PLEKKEN, "kinds");
        // a group's count follows your unlocks
        ServerPlayer player = player(helper);
        GuhClothes piece = GidsData.ontgrendelbare().get(0);
        helper.assertTrue(!KledingUnlocks.heeft(player, piece), "locked at first");
        KledingUnlocks.ontgrendel(player, piece);
        helper.assertTrue(KledingUnlocks.heeft(player, piece) && KledingUnlocks.alle(player).size() == 1, "one unlocked");
        leave(helper, player);
        helper.succeed();
    }

    /** Every text the gids screens show exists (the server's en_us: Dutch, like everything). */
    @GuhTest(template = EMPTY)
    public static void gidsTekstenBestaan(GameTestHelper helper) {
        Language lang = Language.getInstance();
        helper.assertTrue(lang.has("gui.guhs.guhdex.tab.minigames") && lang.has("gui.guhs.guhdex.tab.kleding"), "the tab names");
        for (Highscores.Game game : Highscores.GAMES) {
            helper.assertTrue(lang.has(GidsData.rijLabel(game.id())), "a label for row " + game.id());
        }
        for (GidsData.Soort s : GidsData.Soort.values()) {
            helper.assertTrue(lang.has("gui.guhs.gids.soort." + s.id()), "kind " + s.id());
        }
        for (SuperkompasItem.Category c : SuperkompasItem.CATEGORIES) {
            helper.assertTrue(lang.has("gui.guhs.superkompas." + c.id()) && lang.has("gui.guhs.superkompas." + c.id() + ".tooltip"), "tab " + c.id());
            for (SuperkompasItem.Kopje k : c.kopjes()) {
                helper.assertTrue(k.id() == null || lang.has("gui.guhs.superkompas.kopje." + k.id()), "subheading " + k.id());
            }
            for (String s : c.structures()) {
                helper.assertTrue(lang.has("structure.guhs." + s), "the name of " + s);
            }
        }
        for (String bron : KledingBronnen.bronnen()) {
            helper.assertTrue(lang.has("gui.guhs.kledingbron." + bron), "source " + bron);
        }
        helper.assertTrue(!lang.getOrDefault("gui.guhs.gids.kleding.uitleg").toLowerCase(java.util.Locale.ROOT).contains("te vads"), "never te vads");
        helper.succeed();
    }

    /** The explorer advancements: all six new buildings -> Guhspelen-ontdekker; every building -> Vahoege wereldreiziger. */
    @GuhTest(template = EMPTY)
    public static void gidsOntdekkerAdvancements(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Guhs.id(GidsFeature.ONTDEKKER)) != null
                && helper.getLevel().getServer().getAdvancements().get(Guhs.id(GidsFeature.WERELDREIZIGER)) != null, "both advancements exist");
        GidsFeature.kijk(player);
        helper.assertTrue(!GidsFeature.heeft(player, GidsFeature.ONTDEKKER), "nothing visited yet");
        List<SpelGroepen.Groep> groot = GidsFeature.nodig(true);
        helper.assertTrue(groot.size() == 6, "six new buildings: " + groot.size());
        for (int i = 0; i < groot.size() - 1; i++) {
            SpelGroepen.bezoek(player, groot.get(i).id());
        }
        GidsFeature.kijk(player);
        helper.assertTrue(!GidsFeature.heeft(player, GidsFeature.ONTDEKKER), "five of six isn't enough");
        SpelGroepen.bezoek(player, groot.get(groot.size() - 1).id());
        GidsFeature.kijk(player);
        helper.assertTrue(GidsFeature.heeft(player, GidsFeature.ONTDEKKER) && !GidsFeature.heeft(player, GidsFeature.WERELDREIZIGER), "ontdekker!");
        for (SpelGroepen.Groep g : GidsFeature.nodig(false)) {
            SpelGroepen.bezoek(player, g.id());
        }
        GidsFeature.kijk(player);
        helper.assertTrue(GidsFeature.heeft(player, GidsFeature.WERELDREIZIGER), "wereldreiziger!");
        leave(helper, player);
        helper.succeed();
    }
}
