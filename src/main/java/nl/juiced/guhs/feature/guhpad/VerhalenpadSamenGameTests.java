package nl.juiced.guhs.feature.guhpad;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.compat.FtbQuestsChapter;
import nl.juiced.guhs.feature.balto.BaltoVerhaal;
import nl.juiced.guhs.feature.barbecuether.Grillguh;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.gids.VerhalenVoortgang;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.GrootVerhaal;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.Wereld;
import nl.juiced.guhs.feature.guhwaii.Ohana;
import nl.juiced.guhs.feature.hemel.HemelQuest;
import nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang;
import nl.juiced.guhs.feature.oudescenes.OudeScene;
import nl.juiced.guhs.feature.oudescenes.OudeScenes;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ringknipoog.Knipogen;
import nl.juiced.guhs.feature.snuffel.Rang;
import nl.juiced.guhs.feature.snuffel.Snuffel;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.snuffelsteiger.Steiger;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Doelen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.taal.NlTekst;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Written at the merge of the "verhalenpad" branches into bbq2 (Het Guhpad, the old stories' scenes, Het Snuffeleiland:
 * kern, dock and village): the seams between them, which no branch could test because none held the others (batch
 * "verhalenpadsamen").
 * <ul>
 *     <li>the Guhpad's registry finds the REAL Verhaallijn {@code snuffeleiland} (no stand-in): seven big stories;</li>
 *     <li>the lock on the Knabbelring takes a player who really finished the first series of the Snuffeleiland
 *     ({@code Snuffel.rondAf}, the first and only sniff rank of this update), and not a player one step before it;</li>
 *     <li>"Mijn verhaal" points to a steigerhuisje for a player who misses that story, also when a later story stands
 *     nearer, and while the story's own line is followed;</li>
 *     <li>the quest book: the two sections of the Snuffeleiland stand in "Verhalen van de Guhmensie" (the chapter
 *     guhs_verhalen of the group Het Guhpad) and nowhere else;</li>
 *     <li>the scenes of the old stories next to the winks of the Knabbelring: every one of them is its own scene under a
 *     page of the Guhdex tab Verhalen that the Guhpad shows under a world, and the two "seen" marks never touch.</li>
 * </ul>
 */
public class VerhalenpadSamenGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "verhalenpadsamen";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Guhpad.vergeet(p.getUUID());
            GuhpadKompas.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean heeft(ServerPlayer p, String quest) {
        AdvancementHolder a = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + quest));
        return a != null && p.getAdvancements().getOrStartProgress(a).isDone();
    }

    private static List<String> ids(List<GrootVerhaal> verhalen) {
        return verhalen.stream().map(GrootVerhaal::id).toList();
    }

    private static List<String> sleutels(List<Guhpad.Eis> eisen) {
        return eisen.stream().map(Guhpad.Eis::sleutel).toList();
    }

    private static String bron(String path) throws IOException {
        try (InputStream in = FtbQuestsChapter.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** The four older big stories of the Guhmensie, really finished (each story's own progress, not an op's mark). */
    private static void vierOudeVerhalen(ServerPlayer p) {
        BaltoVerhaal.zet(p, BaltoVerhaal.KLAAR);
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.KLAAR);
        GuhQuests.saved(p).putBoolean(HemelQuest.HART, true);
        Ohana.zet(p, Ohana.KLAAR);
    }

    /** The Guhpad finds the Snuffeleiland by its real questline: seven big stories, five of them in the Guhmensie. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhalenpadsamenRegisterVindtSnuffeleiland(GameTestHelper helper) {
        Verhaallijn lijn = Verhaallijnen.van(GroteVerhalen.SNUFFELEILAND);
        helper.assertTrue(lijn != null && lijn == SnuffelFeature.LIJN && GroteVerhalen.SNUFFELEILAND.equals(lijn.groep()),
                "the Verhaallijn snuffeleiland of the kern is the one the Guhpad looks up");
        helper.assertTrue(GroteVerhalen.SNUFFEL.bestaat().getAsBoolean() && GroteVerhalen.SNUFFEL.isEr(), "the story is there by itself, without a stand-in");
        helper.assertTrue(GroteVerhalen.totaal() == 7 && ids(GroteVerhalen.alle()).equals(List.of("balto", "mewtwo", "hemel", "guhwaii", "snuffeleiland",
                "knabbelring", "guhrio")), "seven big stories: " + ids(GroteVerhalen.alle()));
        helper.assertTrue(ids(GroteVerhalen.van(Wereld.GUHMENSIE)).equals(List.of("balto", "mewtwo", "hemel", "guhwaii", "snuffeleiland")),
                "five of them in the Guhmensie");
        helper.assertTrue(GroteVerhalen.vanLijn(lijn.id()) == GroteVerhalen.SNUFFEL && GroteVerhalen.wereldVan(lijn.id()) == Wereld.GUHMENSIE,
                "its questline is shown under the Guhmensie");
        // where it begins: the dock's structure, which the game knows and the Superkompas lists under Verhalen
        helper.assertTrue(GroteVerhalen.SNUFFEL.structuren().equals(List.of(Steiger.STRUCTUUR)), "it begins at the steigerhuisje");
        helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .get(ResourceKey.create(Registries.STRUCTURE, Guhs.id(Steiger.STRUCTUUR))).isPresent(), "the structure guhs:steigerhuisje is in the datapack");
        helper.assertTrue(SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.id().equals("verhalen") && c.structures().contains(Steiger.STRUCTUUR)),
                "the Superkompas lists the steigerhuisje under Verhalen");
        // its texts and its advancement for the lock quest of the Guhbarbecuether
        helper.assertTrue("Het Snuffeleiland".equals(NlTekst.get(GroteVerhalen.SNUFFEL.naamSleutel())), "its name in the lists");
        for (String key : List.of("structure.guhs." + Steiger.STRUCTUUR, "gui.guhs.verhalen.kop." + lijn.groep())) {
            helper.assertTrue(NlTekst.has(key), "the text " + key);
        }
        helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Guhs.id("quest/guhpad_klaar_snuffeleiland")) != null,
                "the hidden advancement of the finished story");
        // the Guhdex tab Verhalen has its page, and the Guhpad marks it as a big story
        ServerPlayer p = speler(helper);
        helper.assertTrue(VerhalenVoortgang.alle(p).stream().anyMatch(v -> v.id().equals(lijn.id())), "the Guhdex has the page snuffeleiland");
        GuhpadPayloads.Stand stand = GuhpadPayloads.stand(p);
        helper.assertTrue(stand.totaal() == 7 && stand.gevolgd() == 0 && stand.isGroot(lijn.id()) && stand.verhalen(Wereld.GUHMENSIE).size() == 5,
                "what the client is told: 0 of 7, five stories in the Guhmensie");
        weg(helper, p);
        helper.succeed();
    }

    /**
     * Guhdalf's lock: the four older stories are not enough, the Snuffeleiland one step before its end is not enough, the
     * end of its first series ({@code Snuffel.rondAf}: the player is a Snuffelpup, the only rank there is now) opens it.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhalenpadsamenKnabbelringNaEersteSnuffelrang(GameTestHelper helper) {
        ServerPlayer p = speler(helper), ander = speler(helper);
        Verhaallijn snuffel = SnuffelFeature.LIJN;
        try {
            Grillguh.setStep(p, Grillguh.DONE);
            Grillguh.setStep(ander, Grillguh.DONE);
            vierOudeVerhalen(p);
            vierOudeVerhalen(ander);
            helper.assertTrue(!Guhpad.magKnabbelring(p) && !Ring.magBeginnen(p) && sleutels(Guhpad.ontbreektVoorKnabbelring(p))
                    .equals(List.of("gui.guhs.guhpad.verhaal.snuffeleiland")), "four stories followed: only the Snuffeleiland is missing");
            helper.assertTrue("Het Snuffeleiland".equals(NlTekst.tekst(Guhpad.lijst(Guhpad.ontbreektVoorKnabbelring(p)))), "Guhdalf names it");
            // on the way: the last step of the first series still open
            snuffel.begin(p);
            snuffel.zet(p, SnuffelFeature.STAP_SPOOR);
            helper.assertTrue(!Snuffel.klaar(p) && !GroteVerhalen.SNUFFEL.klaar(p) && !Guhpad.magKnabbelring(p) && !Ring.magBeginnen(p),
                    "at the last step (a trace of papa) the story is not finished yet");
            // the end of the first series
            helper.assertTrue(Snuffel.rondAf(p), "the first series ends");
            helper.assertTrue(Rang.HOOGSTE_NU == Rang.SNUFFELPUP && Rang.van(p) == Rang.SNUFFELPUP && Rang.van(p).nummer() == 1,
                    "the player is a Snuffelpup: the first rank, the only one this update has");
            helper.assertTrue(Snuffel.klaar(p) && GroteVerhalen.SNUFFEL.klaar(p) && Guhpad.ontbreektVoorKnabbelring(p).isEmpty() && Guhpad.magKnabbelring(p)
                    && Ring.magBeginnen(p), "the first sniff rank finished: Guhdalf may start the Knabbelring");
            helper.assertTrue(!Guhpad.magKnabbelring(ander) && !Ring.magBeginnen(ander) && !Snuffel.klaar(ander), "per player: the other one is still asked");
            // the counter, the statistic and the advancement the lock quest of the quest book waits for
            GuhpadEvents.kijk(p);
            GuhpadEvents.kijk(ander);
            helper.assertTrue(GroteVerhalen.gevolgd(p) == 5 && p.getStats().getValue(GuhpadEvents.statistiek()) == 5 && heeft(p, "guhpad_klaar_snuffeleiland"),
                    "five of seven followed, with the advancement of the Snuffeleiland");
            helper.assertTrue(GroteVerhalen.gevolgd(ander) == 4 && !heeft(ander, "guhpad_klaar_snuffeleiland"), "the other one: four, no advancement");
            // the grill portal still wants Guhdalf's party on top, as before
            helper.assertTrue(!Guhpad.magBarbecuether(p) && !Ring.magDoorPortaal(p) && sleutels(Guhpad.ontbreekt(p, Wereld.BARBECUETHER))
                    .equals(List.of(Guhpad.EIS_KNABBELFEEST)), "the Guhbarbecuether: only chapter 1 of the Knabbelring is left");
            Ring.lijn(1).zet(p, Ring.lijn(1).stappen());
            helper.assertTrue(Guhpad.magBarbecuether(p) && Ring.magDoorPortaal(p), "after the party the grill portal is open");
        } finally {
            Ring.wis(p);
            snuffel.wis(p);
            weg(helper, p, ander);
        }
        helper.succeed();
    }

    /** "Mijn verhaal" for a player who misses the Snuffeleiland: a steigerhuisje. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhalenpadsamenMijnVerhaalNaarSteiger(GameTestHelper helper) {
        ServerPlayer p = speler(helper), q = speler(helper);
        ResourceKey<Level> mensie = ModDimensions.GUHMENSION;
        BlockPos hier = p.blockPosition();
        Map<String, BlockPos> plekken = new HashMap<>(Map.of(Steiger.STRUCTUUR, hier.offset(0, 0, 200), "nomguh", hier.offset(50, 0, 0),
                "knabbelgouw", hier.offset(10, 0, 0)));
        var echteZoeker = GuhpadKompas.zoeker;
        GuhpadKompas.zoeker = (speler, structuur) -> plekken.get(structuur);
        Verhaallijn snuffel = SnuffelFeature.LIJN;
        try {
            // only the Snuffeleiland missing: the dock, although Guhdalf's camp is much nearer (his story is not open yet)
            vierOudeVerhalen(p);
            helper.assertTrue(ids(GuhpadKompas.kandidaten(p)).equals(List.of("snuffeleiland")), "the one story left to begin: " + ids(GuhpadKompas.kandidaten(p)));
            Doel d = GuhpadKompas.doel(p, mensie);
            helper.assertTrue(d != null && d.dim() == mensie && plekken.get(Steiger.STRUCTUUR).equals(d.plek())
                    && NlTekst.tekst(d.tekst()).equals(NlTekst.get("structure.guhs." + Steiger.STRUCTUUR)), "in the Guhmensie: the nearest steigerhuisje");
            GuhpadKompas.vergeet(p.getUUID());
            d = GuhpadKompas.doel(p, Level.OVERWORLD);
            helper.assertTrue(d != null && d.dim() == mensie && d.plek() == null && Steiger.STRUCTUUR.equals(d.structuur()),
                    "from another world: a steigerhuisje in the Guhmensie (the compass shows the portal)");
            d = Doelen.kompas(p);
            helper.assertTrue(Doelen.van(p) == null && d != null && d.dim() == mensie && Steiger.STRUCTUUR.equals(d.structuur()),
                    "the Superkompas option Mijn verhaal says the same without a followed questline");
            // two stories missing: the nearest of the two; with the dock nearer, the dock
            BaltoVerhaal.zet(q, BaltoVerhaal.KLAAR - 1);
            MewtwoVoortgang.zetStap(q, MewtwoVoortgang.KLAAR);
            GuhQuests.saved(q).putBoolean(HemelQuest.HART, true);
            Ohana.zet(q, Ohana.KLAAR);
            helper.assertTrue(ids(GuhpadKompas.kandidaten(q)).equals(List.of("balto", "snuffeleiland")), "Balto and the Snuffeleiland are left");
            plekken.put(Steiger.STRUCTUUR, hier.offset(0, 0, 20));
            GuhpadKompas.vergeet(q.getUUID());
            d = GuhpadKompas.doel(q, mensie);
            helper.assertTrue(d != null && plekken.get(Steiger.STRUCTUUR).equals(d.plek()), "the dock at 20 blocks goes before Nomguh at 50");
            // the story began at the dock and is followed: its own goal is the same steigerhuisje
            snuffel.begin(p);
            snuffel.zet(p, SnuffelFeature.STAP_UITVAREN);
            d = Doelen.kompas(p);
            helper.assertTrue(Verhaallijnen.gevolgd(p) == snuffel && d != null && d.dim() == mensie && Steiger.STRUCTUUR.equals(d.structuur()),
                    "while following the story at the dock (step 1): still a steigerhuisje");
            // finished: Guhdalf's camp is next
            Snuffel.rondAf(p);
            GuhpadKompas.vergeet(p.getUUID());
            d = GuhpadKompas.doel(p, mensie);
            helper.assertTrue(ids(GuhpadKompas.kandidaten(p)).equals(List.of("knabbelring")) && d != null && plekken.get("knabbelgouw").equals(d.plek()),
                    "the Snuffeleiland done: the Knabbelgouw");
        } finally {
            GuhpadKompas.zoeker = echteZoeker;
            snuffel.wis(p);
            weg(helper, p, q);
        }
        helper.succeed();
    }

    /** The quest book: both sections of the Snuffeleiland are in "Verhalen van de Guhmensie" (Het Guhpad), and only there. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhalenpadsamenQuestboek(GameTestHelper helper) {
        try {
            List<String> groepen = bron("ftbquests/index.txt").lines().filter(l -> l.startsWith("group ")).map(l -> l.substring(6).trim()).toList();
            String verhalen = bron("ftbquests/chapters/guhs_verhalen.json5"), nl = bron("ftbquests/lang/nl_nl/guhs_verhalen.json5");
            Matcher groep = Pattern.compile("(?m)^\\s*group: \"([0-9A-F]{16})\"").matcher(verhalen);
            helper.assertTrue(groepen.size() == 2 && groep.find() && groep.group(1).equals(groepen.get(1)), "guhs_verhalen is in the group Het Guhpad");
            helper.assertTrue(nl.contains("Verhalen van de Guhmensie"), "the chapter is called Verhalen van de Guhmensie");
            // the nine steps of the questline and the thirteen quests of the village: tasks on their hidden advancements
            for (int i = 1; i <= SnuffelFeature.STAPPEN; i++) {
                helper.assertTrue(verhalen.contains("advancement: \"guhs:quest/snuffeleiland_stap_" + i + "\""), "step " + i + " of the Snuffeleiland is a quest here");
            }
            for (String quest : List.of("snuffel_eerste_geur", "snuffel_dorp_maatje", "snuffel_dorp_daad_bakker", "snuffel_dorp_daad_visser", "snuffel_dorp_daad_juf",
                    "snuffel_dorp_daad_oma", "snuffel_dorp_daad_tuinder", "snuffel_dorp_daad_pup", "snuffel_dorp_boom", "snuffel_dorp_alle", "snuffel_diploma",
                    "snuffel_dorp_versperring", "snuffel_guhstation")) {
                helper.assertTrue(verhalen.contains("advancement: \"guhs:quest/" + quest + "\""), "the village quest on " + quest + " is here");
                helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Guhs.id("quest/" + quest)) != null, "the advancement " + quest);
            }
            // each section has its header picture, and the pictures ship
            Matcher kop = Pattern.compile("image: \"guhs:textures/ftbquests/guhs_verhalen/(kop_[a-z0-9_]*snuffel[a-z0-9_]*)\\.png\"").matcher(verhalen);
            Set<String> koppen = new HashSet<>();
            while (kop.find()) {
                koppen.add(kop.group(1));
                helper.assertTrue(FtbQuestsChapter.class.getClassLoader().getResource("assets/guhs/textures/ftbquests/guhs_verhalen/" + kop.group(1) + ".png") != null,
                        "the header picture " + kop.group(1));
            }
            helper.assertTrue(koppen.size() == 2, "two sections (Het Snuffeleiland, Snuffeldorp): " + koppen);
            // nowhere else, and the lock quest of the next chapter asks the story
            for (String c : FtbQuestsChapter.chapters()) {
                if (!c.equals("guhs_verhalen")) {
                    String ander = bron("ftbquests/chapters/" + c + ".json5");
                    helper.assertTrue(!ander.contains("guhs:quest/snuffeleiland_stap_") && !ander.contains("guhs:quest/snuffel_dorp_"), "no Snuffeleiland quest in " + c);
                }
            }
            helper.assertTrue(bron("ftbquests/chapters/guhs_pad_barbecuether.json5").contains("advancement: \"guhs:quest/guhpad_klaar_snuffeleiland\"")
                    && bron("ftbquests/lang/nl_nl/guhs_pad_barbecuether.json5").contains("\"Het Snuffeleiland\""), "the lock quest of the Guhbarbecuether asks Het Snuffeleiland");
            Matcher teller = Pattern.compile("stat: \"guhs:" + GuhpadFeature.STATISTIEK + "\",\\s*value: (\\d+)|value: (\\d+),\\s*stat: \"guhs:" + GuhpadFeature.STATISTIEK + "\"")
                    .matcher(bron("ftbquests/chapters/guhs_pad_echt.json5"));
            helper.assertTrue(teller.find() && "7".equals(teller.group(1) != null ? teller.group(1) : teller.group(2)), "the counter of Het echte Guheinde counts to 7");
        } catch (IOException e) {
            helper.fail("io: " + e);
        }
        helper.succeed();
    }

    /**
     * The six scenes of the old stories and the eight winks of the Knabbelring are fourteen scenes of their own; each hangs
     * under a page of the Guhdex tab Verhalen ("Opnieuw bekijken"), the old ones under the Guhmensie, the winks under the
     * Guhbarbecuether; and "seen" of the one never changes "seen" of the other.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void verhalenpadsamenOudeScenesEnKnipogen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            List<Cutscene> oud = OudeScenes.ALLE.stream().map(OudeScene::scene).toList(), knipogen = Knipogen.alle();
            List<String> alle = Cutscene.alle().stream().map(Cutscene::id).toList();
            helper.assertTrue(new HashSet<>(alle).size() == alle.size(), "no scene id is registered twice");
            List<String> paginas = VerhalenVoortgang.alle(p).stream().map(VerhaalStand::id).toList();
            List<Cutscene> samen = new ArrayList<>(oud);
            samen.addAll(knipogen);
            helper.assertTrue(oud.size() == 6 && knipogen.size() == 8 && samen.stream().map(Cutscene::id).distinct().count() == 14, "six old scenes, eight winks");
            for (Cutscene s : samen) {
                boolean oude = oud.contains(s);
                helper.assertTrue(alle.contains(s.id()) && Cutscene.van(s.id()) == s, s.id() + " is registered");
                helper.assertTrue(s.lijn() != null && paginas.contains(s.lijn()), s.id() + " hangs under a page of the tab Verhalen: " + s.lijn());
                helper.assertTrue(GroteVerhalen.wereldVan(s.lijn()) == (oude ? Wereld.GUHMENSIE : Wereld.BARBECUETHER),
                        s.id() + ": its page " + s.lijn() + " stands under " + GroteVerhalen.wereldVan(s.lijn()));
                helper.assertTrue(NlTekst.has(s.titelKey()), s.id() + " has a title for the list Opnieuw bekijken");
                helper.assertTrue(!Cutscenes.gezien(p, s.id()), s.id() + ": a new player saw nothing");
            }
            // past Balto's moment: the old scene is in the Guhdex, no wink is
            BaltoVerhaal.zet(p, BaltoVerhaal.KLAAR);
            OudeScenes.bijwerken(p);
            helper.assertTrue(Cutscenes.gezien(p, OudeScenes.BALTO.id()) && knipogen.stream().noneMatch(k -> Knipogen.gezien(p, k)),
                    "past Balto's moment: his old scene counts as seen, the winks do not");
            // the wink with Baltoguh at the council seen (the engine's own mark, as it writes it at a scene's end)
            CompoundTag gezien = new CompoundTag();
            gezien.putBoolean("Gezien", true);
            GuhQuests.saved(p).put("guhs_scene_" + Knipogen.BALTOGUH.id(), gezien);
            helper.assertTrue(Knipogen.gezien(p, Knipogen.BALTOGUH) && Cutscenes.gezien(p, OudeScenes.BALTO.id()), "both are in the list now");
            helper.assertTrue(OudeScenes.bijwerken(p) == 0, "the regular look of the old scenes has nothing to change");
            // forgetting the one leaves the other
            OudeScenes.vergeet(p, OudeScenes.BALTO);
            helper.assertTrue(!Cutscenes.gezien(p, OudeScenes.BALTO.id()) && Knipogen.gezien(p, Knipogen.BALTOGUH), "the old scene forgotten: the wink stays");
            helper.assertTrue(OudeScenes.bijwerken(p) == 1 && Cutscenes.gezien(p, OudeScenes.BALTO.id()), "and it comes back: the player is past its moment");
            Knipogen.wis(p);
            helper.assertTrue(!Knipogen.gezien(p, Knipogen.BALTOGUH) && Cutscenes.gezien(p, OudeScenes.BALTO.id()), "the winks forgotten: the old scene stays");
            // the scenes of the Snuffeleiland hang under its own page
            for (String id : List.of("snuffelsteiger_feest", "snuffelsteiger_overtocht", "snuffeldorp_wakker", "snuffeldorp_maatje", "snuffeldorp_spoor")) {
                Cutscene s = Cutscene.van(id);
                helper.assertTrue(s != null && GroteVerhalen.SNUFFELEILAND.equals(s.lijn()) && paginas.contains(s.lijn()), id + " hangs under the page snuffeleiland");
            }
        } finally {
            Knipogen.wis(p);
            weg(helper, p);
        }
        helper.succeed();
    }
}
