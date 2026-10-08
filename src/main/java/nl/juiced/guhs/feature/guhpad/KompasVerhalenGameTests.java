package nl.juiced.guhs.feature.guhpad;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.Wereld;
import nl.juiced.guhs.feature.guhpad.KompasVerhalen.Blok;
import nl.juiced.guhs.feature.guhpad.KompasVerhalen.Groep;
import nl.juiced.guhs.feature.guhpad.KompasVerhalen.Plek;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.snuffelsteiger.Steiger;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * The Superkompas tab Verhalen (1.4.1, {@link KompasVerhalen}): the split by world in path order with GroteVerhalen as the
 * one source, the locks of the Guhdex, the Knabbelring's place per chapter, and the spoiler rule ("???" until the player's
 * story reached the place; such a place can not be chosen).
 */
public class KompasVerhalenGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "kompasverhalen";
    /** The Knabbelring's places in story order; the chapter whose reaching opens each (0: never hidden). */
    private static final List<String> RING = List.of("knabbelgouw", "guhvendel", "knabbelmoria", "guhladriel_boomstad", "zwarte_roosterpoort",
            "frituurberg", "sausuman_toren");

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        Ring.wis(p);
        Guhpad.vergeet(p.getUUID());
        GuhpadKompas.vergeet(p.getUUID());
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static Groep groep(List<Groep> groepen, Wereld w) {
        return groepen.stream().filter(g -> g.wereld() == w).findFirst().orElseThrow();
    }

    private static List<String> ids(List<Plek> plekken) {
        return plekken.stream().map(Plek::structuur).toList();
    }

    private static List<String> geheim(Groep g) {
        return g.plekken().stream().filter(Plek::geheim).map(Plek::structuur).toList();
    }

    /** The four worlds in path order; every story's places under its own world; the older places stay; everything can be looked for. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void kompasverhalenIndeling(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            List<Groep> groepen = KompasVerhalen.groepen(p);
            helper.assertTrue(groepen.stream().map(Groep::wereld).toList().equals(List.of(Wereld.values())), "the four worlds in path order");
            // the Guhmensie: one entry per story in the order of GroteVerhalen, then the other places of the old tab
            Groep mensie = groep(groepen, Wereld.GUHMENSIE);
            List<String> verwacht = new ArrayList<>(List.of("nomguh", "kloon_eiland", "hemelkapelletje", "guhwaii_ohana"));
            if (GroteVerhalen.SNUFFEL.isEr()) {
                verwacht.add(Steiger.STRUCTUUR);
            }
            verwacht.addAll(List.of("guhwaii_surfstrand", "knuffeldal_stadje"));
            if (!GroteVerhalen.SNUFFEL.isEr()) {
                verwacht.add(Steiger.STRUCTUUR);
            }
            helper.assertTrue(mensie.blokken().size() == 1 && mensie.blokken().get(0).kop() == null && ids(mensie.plekken()).equals(verwacht),
                    "the Guhmensie: " + ids(mensie.plekken()));
            helper.assertTrue(!GroteVerhalen.SNUFFEL.isEr() || mensie.plekken().stream().anyMatch(x -> x.structuur().equals(Steiger.STRUCTUUR)
                    && GroteVerhalen.SNUFFELEILAND.equals(x.verhaal())), "Het Snuffeleiland is an entry of its own: its Steigerhuisje");
            helper.assertTrue("balto".equals(mensie.plekken().get(0).verhaal()) && mensie.plekken().get(verwacht.indexOf("knuffeldal_stadje")).verhaal() == null,
                    "a story place knows its story, another place has none");
            // the Guhbarbecuether: the Knabbelring under its own heading with a place per chapter and the Toren, then the castle
            Groep bbq = groep(groepen, Wereld.BARBECUETHER);
            helper.assertTrue(bbq.blokken().size() == 2, "the Guhbarbecuether: two blocks: " + bbq.blokken());
            Blok ring = bbq.blokken().get(0), rest = bbq.blokken().get(1);
            helper.assertTrue("knabbelring".equals(ring.kop()) && ids(ring.plekken()).equals(RING), "the Knabbelring: " + ids(ring.plekken()));
            helper.assertTrue(rest.kop() == null && ids(rest.plekken()).equals(List.of("guhrio_kasteel")) && "guhrio".equals(rest.plekken().get(0).verhaal()),
                    "Super Guhrio: the castle");
            // the Guheinde: what is there now; the real Guheinde: only its locked line
            helper.assertTrue(ids(groep(groepen, Wereld.GUHEINDE).plekken()).equals(List.of("knabbelkelder", "guheinde_terugpoort")), "the Guheinde");
            Groep echt = groep(groepen, Wereld.ECHT);
            helper.assertTrue(echt.opSlot() && echt.plekken().isEmpty(), "Het echte Guheinde: locked, nothing in it");
            // one source: every place of every big story of GroteVerhalen is listed once, under the story's own world
            List<String> alles = new ArrayList<>();
            for (Groep g : groepen) {
                for (Plek plek : g.plekken()) {
                    helper.assertTrue(!alles.contains(plek.structuur()), "listed once: " + plek.structuur());
                    alles.add(plek.structuur());
                    helper.assertTrue(plek.verhaal() == null || GroteVerhalen.van(plek.verhaal()).wereld() == g.wereld(),
                            plek.structuur() + " under the world of its story");
                }
            }
            for (GroteVerhalen.GrootVerhaal v : GroteVerhalen.alle()) {
                helper.assertTrue(alles.containsAll(v.plekken()) && v.plekken().containsAll(v.structuren()), "every place of " + v.id());
            }
            // every place is a real structure with a name, and the Superkompas may be set to it (the fixed tab keeps its shape)
            var structures = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
            int tab = -1;
            for (int i = 0; i < SuperkompasItem.CATEGORIES.size(); i++) {
                if (SuperkompasItem.CATEGORIES.get(i).id().equals(KompasVerhalen.TAB)) {
                    tab = i;
                }
            }
            helper.assertTrue(tab >= 0, "the tab Verhalen");
            for (String s : alles) {
                helper.assertTrue(structures.containsKey(Guhs.id(s)), "structure exists: " + s);
                helper.assertTrue(Language.getInstance().has("structure.guhs." + s) && Language.getInstance().has("structure.guhs." + s + ".tooltip"),
                        "the name and the description of " + s);
                helper.assertTrue(SuperkompasItem.allowed(s) && SuperkompasItem.categoryOf(s) >= 0, "the Superkompas may look for " + s);
            }
            helper.assertTrue(SuperkompasItem.categoryOf("knabbelmoria") == tab && !SuperkompasItem.CATEGORIES.get(tab).structures().contains("knabbelmoria"),
                    "a chapter place opens the tab Verhalen without being in the fixed list");
            helper.assertTrue(!SuperkompasItem.allowed("bestaat_niet") && SuperkompasItem.categoryOf("bestaat_niet") == -1, "nothing else became allowed");
            for (String key : List.of("gui.guhs.guhpad.kompas.verhaal", "gui.guhs.guhpad.kompas.geheim", "gui.guhs.guhpad.kompas.geheim.uitleg",
                    "gui.guhs.guhpad.kompas.vouw_open", "gui.guhs.guhpad.kompas.vouw_dicht", "gui.guhs.guhpad.nog_nodig", "gui.guhs.guhpad.op_slot")) {
                helper.assertTrue(Language.getInstance().has(key), "the text " + key);
            }
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    /** The locks are the Guhdex's: a world the player may not go into is locked and says what is missing, until it opens. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void kompasverhalenSloten(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            List<Groep> groepen = KompasVerhalen.groepen(p);
            GuhpadPayloads.Stand stand = GuhpadPayloads.stand(p);
            for (Groep g : groepen) {
                helper.assertTrue(g.opSlot() == !Guhpad.open(p, g.wereld()) && g.opSlot() == !stand.open(g.wereld()),
                        "a new player: the lock of " + g.wereld() + " is the Guhdex's");
                helper.assertTrue(g.mist().stream().map(GuhpadPayloads.Eis::sleutel).toList()
                        .equals(Guhpad.ontbreekt(p, g.wereld()).stream().map(Guhpad.Eis::sleutel).toList()), "and so is what it still asks");
            }
            helper.assertTrue(!groep(groepen, Wereld.GUHMENSIE).opSlot() && groep(groepen, Wereld.BARBECUETHER).opSlot()
                    && groep(groepen, Wereld.GUHEINDE).opSlot(), "a new player: only the Guhmensie is open");
            // a locked world still shows its places by name where that is no spoiler (the Knabbelgouw, the castle)
            helper.assertTrue(!groep(groepen, Wereld.BARBECUETHER).plekken().get(0).geheim()
                    && geheim(groep(groepen, Wereld.BARBECUETHER)).equals(RING.subList(1, RING.size())), "locked, the Knabbelgouw and the castle by name");
            // before the first sync nothing shows as locked (and nothing is missing)
            for (Groep g : KompasVerhalen.groepen(GuhpadPayloads.Stand.LEEG, s -> false)) {
                helper.assertTrue(!g.opSlot() && g.mist().isEmpty(), "nothing known yet: no lock on " + g.wereld());
            }
            // the stories of the Guhmensie and Guhdalf's party: the Guhbarbecuether opens, the Guheinde stays locked
            GuhpadGameTests.guhmensieGedaan(p);
            helper.assertTrue(groep(KompasVerhalen.groepen(p), Wereld.BARBECUETHER).opSlot(), "the stories alone: still locked (Guhdalf's party)");
            Ring.lijn(1).zet(p, Ring.lijn(1).stappen());
            groepen = KompasVerhalen.groepen(p);
            helper.assertTrue(!groep(groepen, Wereld.BARBECUETHER).opSlot() && groep(groepen, Wereld.BARBECUETHER).mist().isEmpty()
                    && groep(groepen, Wereld.GUHEINDE).opSlot() && groep(groepen, Wereld.ECHT).opSlot(), "the Guhbarbecuether is open now");
            for (Groep g : groepen) {
                helper.assertTrue(g.opSlot() == !Guhpad.open(p, g.wereld()), "still the Guhdex's lock: " + g.wereld());
            }
        } finally {
            for (GroteVerhalen.GrootVerhaal v : GroteVerhalen.alle()) {
                GroteVerhalen.zetGedaan(p, v, false);
            }
            weg(helper, p);
        }
        helper.succeed();
    }

    /** The spoiler rule: a chapter place is "???" and can not be chosen until the player's own story reached it; then it is a normal entry. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void kompasverhalenGeheim(GameTestHelper helper) {
        ServerPlayer p = speler(helper), ander = speler(helper);
        try {
            Groep bbq = groep(KompasVerhalen.groepen(p), Wereld.BARBECUETHER);
            helper.assertTrue(geheim(bbq).equals(RING.subList(1, RING.size())), "a new player: only the Knabbelgouw shows: " + geheim(bbq));
            helper.assertTrue(KompasVerhalen.magKiezen(p, "knabbelgouw") && KompasVerhalen.magKiezen(p, SuperkompasItem.DOEL)
                    && KompasVerhalen.magKiezen(p, "guhrio_kasteel") && KompasVerhalen.magKiezen(p, "nomguh"), "what shows can be chosen");
            for (String s : RING.subList(1, RING.size())) {
                helper.assertTrue(!KompasVerhalen.magKiezen(p, s) && SuperkompasItem.allowed(s), "??? can not be chosen: " + s);
            }
            // chapter by chapter: the place of chapter n opens when chapter n - 1 is done, the Toren after chapter 4
            for (int klaar = 1; klaar <= 5; klaar++) {
                Ring.lijn(klaar).zet(p, Ring.lijn(klaar).stappen());
                List<String> open = new ArrayList<>(RING.subList(0, klaar + 1));
                if (klaar >= 4) {
                    open.add("sausuman_toren");
                }
                bbq = groep(KompasVerhalen.groepen(p), Wereld.BARBECUETHER);
                for (Plek plek : bbq.blokken().get(0).plekken()) {
                    boolean zichtbaar = open.contains(plek.structuur());
                    helper.assertTrue(plek.geheim() == !zichtbaar && KompasVerhalen.magKiezen(p, plek.structuur()) == zichtbaar,
                            "chapter " + klaar + " done: " + plek.structuur() + (zichtbaar ? " is a normal entry" : " is still ???"));
                }
                helper.assertTrue(ids(bbq.blokken().get(0).plekken()).equals(RING), "the order never changes");
            }
            helper.assertTrue(geheim(bbq).isEmpty(), "chapter 5 done: nothing is a secret any more");
            // per player: somebody else's progress opens nothing
            helper.assertTrue(geheim(groep(KompasVerhalen.groepen(ander), Wereld.BARBECUETHER)).equals(RING.subList(1, RING.size()))
                    && !KompasVerhalen.magKiezen(ander, "frituurberg"), "the other player still sees ???");
        } finally {
            weg(helper, p);
            weg(helper, ander);
        }
        helper.succeed();
    }
}
