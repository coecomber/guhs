package nl.juiced.guhs.feature.guhpad;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.Unpooled;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.compat.FtbQuestsChapter;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.balto.BaltoVerhaal;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.barbecuether.GrillPortalBlock;
import nl.juiced.guhs.feature.barbecuether.Grillguh;
import nl.juiced.guhs.feature.gids.VerhalenVoortgang;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.GrootVerhaal;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.Wereld;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhwaii.Ohana;
import nl.juiced.guhs.feature.hemel.HemelQuest;
import nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ringh1.Feest;
import nl.juiced.guhs.feature.ringh1.RingH1Feature;
import nl.juiced.guhs.feature.techbron.AangebrandeMika;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Doelen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.taal.NlTekst;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Het Guhpad: the one registry of the big stories (ids, worlds, how each story says "finished"); the lock on the
 * Knabbelring (closed, the list of what is missing, open); the lock on the grill portal and the lock on the portal of the
 * Knabbelkelder (closed, open, a player inside may stay, the way out is never blocked, only players are asked); the counter
 * "Verhalen gevolgd" with its statistic and the advancements for the quest book; "Mijn verhaal" points to the nearest
 * story that is not done; every text exists; the quest book has the chapter group "Het Guhpad" with its four chapters and
 * lock quests, no quest was lost in the move, and a chapter that is gone is taken away from a pack.
 * <p>
 * The Snuffeleiland story is another slice's: where it is not in this tree the tests let it count as "there" with a
 * stand-in ({@link GroteVerhalen#standIn}), so the five-story rule is tested either way.
 */
public class GuhpadGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "guhpad";

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

    /**
     * (also for the tests of other packages that walk a mock player past Guhdalf or through a grill portal) this player
     * has followed every big story of the Guhmensie, as far as the Guhpad is concerned.
     */
    public static void guhmensieGedaan(ServerPlayer p) {
        for (GrootVerhaal v : GroteVerhalen.van(Wereld.GUHMENSIE)) {
            GroteVerhalen.zetGedaan(p, v, true);
        }
    }

    /** Lets the Snuffeleiland count as "there" when its own slice is not in this tree; returns whether the test has to undo that. */
    private static boolean metSnuffeleiland() {
        if (GroteVerhalen.SNUFFEL.isEr()) {
            return false;
        }
        GroteVerhalen.standIn(GroteVerhalen.SNUFFEL, true);
        return true;
    }

    private static String key(Component c) {
        return c != null && c.getContents() instanceof TranslatableContents t ? t.getKey() : "";
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

    /** What chapter 1 of the Knabbelring does once a second for a player (the story starts by itself when it may). */
    private static void seconde(ServerPlayer p) {
        p.tickCount = Math.floorMod(7 - p.getId(), 20);
        nl.juiced.guhs.feature.ringh1.RingH1Events.onTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
    }

    // =====================================================================================================================
    // the registry
    // =====================================================================================================================

    /** The one registry: the seven big stories with their worlds, where every questline of the Guhdex belongs, no doubles. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void guhpadRegister(GameTestHelper helper) {
        helper.assertTrue(ids(GroteVerhalen.geregistreerd()).equals(List.of("balto", "mewtwo", "hemel", "guhwaii", "snuffeleiland", "knabbelring", "guhrio")),
                "the big stories, the worlds in order: " + ids(GroteVerhalen.geregistreerd()));
        boolean standIn = metSnuffeleiland();
        try {
            helper.assertTrue(ids(GroteVerhalen.van(Wereld.GUHMENSIE)).equals(List.of("balto", "mewtwo", "hemel", "guhwaii", "snuffeleiland")),
                    "the five of the Guhmensie");
            helper.assertTrue(ids(GroteVerhalen.van(Wereld.BARBECUETHER)).equals(List.of("knabbelring", "guhrio")), "the two of the Guhbarbecuether");
            helper.assertTrue(GroteVerhalen.van(Wereld.GUHEINDE).isEmpty() && GroteVerhalen.van(Wereld.ECHT).isEmpty() && GroteVerhalen.totaal() == 7,
                    "none in the Guheinde yet: seven in all");
        } finally {
            if (standIn) {
                GroteVerhalen.standIn(GroteVerhalen.SNUFFEL, false);
            }
        }
        // a story of another slice only counts when it is really there: the Verhaallijn with the fixed id
        boolean lijn = Verhaallijnen.van(GroteVerhalen.SNUFFELEILAND) != null;
        helper.assertTrue(GroteVerhalen.SNUFFEL.isEr() == lijn && GroteVerhalen.totaal() == (lijn ? 7 : 6), "the Snuffeleiland counts when its line is registered");
        if (GuhpadGameTests.class.getClassLoader().getResource("nl/juiced/guhs/feature/snuffel") != null) {
            helper.assertTrue(lijn, "the Snuffeleiland slice is in this tree: its Verhaallijn must be registered as \"" + GroteVerhalen.SNUFFELEILAND + "\"");
        }
        for (GrootVerhaal v : GroteVerhalen.geregistreerd()) {
            helper.assertTrue(NlTekst.has(v.naamSleutel()), "a name for " + v.id());
            helper.assertTrue(BuiltInRegistries.ITEM.getValue(Identifier.parse(v.icoon())) != Items.AIR, "the icon of " + v.id() + " is an item: " + v.icoon());
            helper.assertTrue(!v.structuren().isEmpty() && !v.lijnen().isEmpty() && GroteVerhalen.vanLijn(v.lijnen().get(0)) == v, v.id() + " has a place and a questline");
            helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Guhs.id("quest/guhpad_klaar_" + v.id())) != null, "the advancement of " + v.id());
            if (v.isEr()) {
                for (String s : v.structuren()) {
                    helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                            .get(ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, Guhs.id(s))).isPresent(), "structure " + s + " of " + v.id());
                }
            }
        }
        boolean gooit = false;
        try {
            GroteVerhalen.registreer(new GrootVerhaal("balto", Wereld.GUHEINDE, Wereld.GUHEINDE, "minecraft:stone", List.of("x"), List.of("x"), () -> true, p -> true));
        } catch (IllegalStateException e) {
            gooit = true;
        }
        helper.assertTrue(gooit && GroteVerhalen.geregistreerd().size() == 7, "a second story with the same id is a mistake");
        // every questline of the Guhdex tab Verhalen has its world
        Map<String, Wereld> waar = Map.of("balto", Wereld.GUHMENSIE, "timmerguh", Wereld.GUHMENSIE, "vadsig", Wereld.GUHMENSIE, "grillguh", Wereld.GUHMENSIE,
                "beroep_bouw", Wereld.GUHMENSIE, "ring_h3", Wereld.BARBECUETHER, "ring_sausuman", Wereld.BARBECUETHER, "guhrio_beloning", Wereld.BARBECUETHER,
                "techniek", Wereld.BARBECUETHER, "guheinde", Wereld.GUHEINDE);
        waar.forEach((lijn2, w) -> helper.assertTrue(GroteVerhalen.wereldVan(lijn2) == w, lijn2 + " belongs to " + w + ", not " + GroteVerhalen.wereldVan(lijn2)));
        for (String id : VerhalenVoortgang.ids()) {
            helper.assertTrue(GroteVerhalen.wereldVan(id) != Wereld.ECHT, id + " is not in the real Guheinde");
        }
        for (Verhaallijn l : Verhaallijnen.vanGroep("barbecue")) {
            helper.assertTrue(GroteVerhalen.wereldVan(l.id()) == Wereld.BARBECUETHER, "a building's questline of the Guhbarbecuether: " + l.id());
        }
        helper.assertTrue(Wereld.van(ModDimensions.GUHMENSION) == Wereld.GUHMENSIE && Wereld.van(BarbecuetherFeature.BARBECUETHER) == Wereld.BARBECUETHER
                && Wereld.van(GuheindeFeature.GUHEINDE) == Wereld.GUHEINDE && Wereld.van(Level.OVERWORLD) == null && Wereld.ECHT.dimensie() == null,
                "the worlds know their dimensions");
        helper.succeed();
    }

    /**
     * How each story says "finished", per player, from its own progress; the counter, the statistic and the advancements
     * follow; an op's mark counts too and never undoes a real finish.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void guhpadVerhaalKlaarEnTeller(GameTestHelper helper) {
        ServerPlayer p = speler(helper), ander = speler(helper);
        int m = GroteVerhalen.totaal();
        helper.assertTrue(GroteVerhalen.gevolgd(p) == 0 && m >= 6, "a new player followed nothing of " + m);
        GuhpadEvents.kijk(p);
        helper.assertTrue(!heeft(p, "guhpad_klaar_balto") && heeft(p, "guhpad_open_guhmensie") && !heeft(p, "guhpad_open_barbecuether")
                && p.getStats().getValue(GuhpadEvents.statistiek()) == 0, "nothing yet: only the Guhmensie is open");
        // Balto: its last step
        BaltoVerhaal.zet(p, BaltoVerhaal.KLAAR - 1);
        helper.assertTrue(!GroteVerhalen.BALTO.klaar(p), "Balto one step before the end is not done");
        BaltoVerhaal.zet(p, BaltoVerhaal.KLAAR);
        helper.assertTrue(GroteVerhalen.BALTO.klaar(p) && !GroteVerhalen.BALTO.klaar(ander) && GroteVerhalen.gevolgd(p) == 1, "Balto: done, for this player only");
        // Mewtwo, the hemelkapelletje, Lilo & Stitch
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.KLAAR - 1);
        helper.assertTrue(!GroteVerhalen.MEWTWO.klaar(p), "Mewtwo at the meal is not done");
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.KLAAR);
        HemelQuest.zetStap(p, 1);
        helper.assertTrue(GroteVerhalen.MEWTWO.klaar(p) && !GroteVerhalen.HEMEL.klaar(p), "Mewtwo done; the chapel only once the Knuffelhart beats");
        GuhQuests.saved(p).putBoolean(HemelQuest.HART, true);
        Ohana.zet(p, Ohana.KLAAR - 1);
        helper.assertTrue(GroteVerhalen.HEMEL.klaar(p) && !GroteVerhalen.GUHWAII.klaar(p), "the Knuffelhart beats; ohana is not a pet yet");
        Ohana.zet(p, Ohana.KLAAR);
        helper.assertTrue(GroteVerhalen.GUHWAII.klaar(p) && GroteVerhalen.gevolgd(p) == 4, "four of the Guhmensie");
        // the Knabbelring = chapter 6 done (ring_h6), Super Guhrio = the line "guhrio" done
        for (int n = 1; n <= 5; n++) {
            Ring.lijn(n).zet(p, Ring.lijn(n).stappen());
        }
        Verhaallijn h6 = Ring.lijn(6);
        h6.zet(p, h6.stappen() - 1);
        helper.assertTrue(!GroteVerhalen.KNABBELRING.klaar(p), "five chapters and a half is not the Knabbelring");
        h6.zet(p, h6.stappen());
        Verhaallijn guhrio = GuhrioKasteel.LIJN;
        guhrio.zet(p, guhrio.stappen() - 1);
        helper.assertTrue(GroteVerhalen.KNABBELRING.klaar(p) && Ring.klaar(p) && !GroteVerhalen.GUHRIO.klaar(p), "the Knabbelring is done, Super Guhrio not yet");
        guhrio.zet(p, guhrio.stappen());
        helper.assertTrue(GroteVerhalen.GUHRIO.klaar(p) && GroteVerhalen.gevolgd(p) == 6 && GroteVerhalen.gevolgd(ander) == 0, "six followed; the other player none");
        // the Snuffeleiland: its Verhaallijn's finished state (or, where that slice is not in this tree, not there at all)
        Verhaallijn snuffel = Verhaallijnen.van(GroteVerhalen.SNUFFELEILAND);
        if (snuffel != null) {
            helper.assertTrue(!GroteVerhalen.SNUFFEL.klaar(p), "the Snuffeleiland is not done yet");
            snuffel.zet(p, snuffel.stappen());
            helper.assertTrue(GroteVerhalen.SNUFFEL.klaar(p), "the Snuffeleiland: its line is done");
        } else {
            helper.assertTrue(!GroteVerhalen.SNUFFEL.klaar(p) && !GroteVerhalen.alle().contains(GroteVerhalen.SNUFFEL), "a story that is not there is never done and never asked");
        }
        helper.assertTrue(GroteVerhalen.gevolgd(p) == m, "all " + m + " followed");
        // what follows: the advancements for the quest book, the statistic, what the client is told
        GuhpadEvents.kijk(p);
        for (GrootVerhaal v : GroteVerhalen.alle()) {
            helper.assertTrue(heeft(p, "guhpad_klaar_" + v.id()) && !heeft(ander, "guhpad_klaar_" + v.id()), "the advancement of " + v.id());
        }
        helper.assertTrue(p.getStats().getValue(GuhpadEvents.statistiek()) == m, "the statistic Verhalen gevolgd is " + m);
        helper.assertTrue(heeft(p, "guhpad_open_barbecuether") && !heeft(p, "guhpad_open_guheinde") && !heeft(p, "guhpad_mika") && !heeft(p, "guhpad_echt"),
                "the Guhbarbecuether is open; the Guheinde still wants the Aangebrande Mika");
        GuhpadPayloads.Stand stand = GuhpadPayloads.stand(p);
        helper.assertTrue(stand.gevolgd() == m && stand.totaal() == m && stand.open(Wereld.GUHMENSIE) && stand.open(Wereld.BARBECUETHER)
                && !stand.open(Wereld.GUHEINDE) && !stand.open(Wereld.ECHT), "the stand for the Guhdex");
        helper.assertTrue(stand.ontbreekt(Wereld.GUHEINDE).size() == 1 && stand.ontbreekt(Wereld.GUHEINDE).get(0).sleutel().equals(Guhpad.EIS_MIKA)
                && stand.ontbreekt(Wereld.ECHT).size() == 1 && stand.ontbreekt(Wereld.ECHT).get(0).sleutel().equals(Guhpad.EIS_ECHT),
                "the Guheinde misses the Mika; the real Guheinde stays locked whatever you did");
        helper.assertTrue(stand.isGroot("balto") && stand.isGroot("ring_h4") && stand.isGroot("guhrio") && !stand.isGroot("timmerguh") && !stand.isGroot("grillguh")
                && !stand.isGroot("guhrio_beloning") && !stand.isGroot("ring_sausuman"), "the Guhdex knows which questlines are big stories");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        GuhpadPayloads.Stand.STREAM_CODEC.encode(buf, stand);
        helper.assertTrue(GuhpadPayloads.Stand.STREAM_CODEC.decode(buf).equals(stand), "guhs:guhpad_stand arrives");
        AangebrandeMika.zet(p);
        GuhpadEvents.kijk(p);
        helper.assertTrue(heeft(p, "guhpad_mika") && heeft(p, "guhpad_open_guheinde") && !heeft(p, "guhpad_open_echt") && !heeft(p, "guhpad_echt"),
                "the Mika beaten: the Guheinde opens; the real Guheinde never");
        // an op's mark: counts for the Guhpad, touches nothing of the story, and taking it away never undoes a real finish
        GroteVerhalen.zetGedaan(ander, GroteVerhalen.BALTO, true);
        helper.assertTrue(GroteVerhalen.BALTO.klaar(ander) && BaltoVerhaal.stap(ander) == 0 && GroteVerhalen.gevolgd(ander) == 1, "marked: done for the Guhpad only");
        GroteVerhalen.zetGedaan(ander, GroteVerhalen.BALTO, false);
        GroteVerhalen.zetGedaan(p, GroteVerhalen.BALTO, true);
        GroteVerhalen.zetGedaan(p, GroteVerhalen.BALTO, false);
        helper.assertTrue(!GroteVerhalen.BALTO.klaar(ander) && GroteVerhalen.BALTO.klaar(p), "the mark is gone; a real finish stays");
        weg(helper, p, ander);
        helper.succeed();
    }

    // =====================================================================================================================
    // the lock on the Knabbelring
    // =====================================================================================================================

    /** Guhdalf starts the Knabbelring only after ALL big stories of the Guhmensie; his refusal lists what is missing. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void guhpadKnabbelringSlot(GameTestHelper helper) {
        boolean standIn = false;
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        GuhNpcEntity guhdalf = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        guhdalf.setKind(GuhNpcEntity.Kind.GUHDALF);
        guhdalf.roleData.putString(nl.juiced.guhs.feature.verhaal.NpcRollen.PLEK, nl.juiced.guhs.feature.ringh1.Gouw.ROL);
        nl.juiced.guhs.feature.NpcRole rol = nl.juiced.guhs.feature.verhaal.NpcRollen.van(guhdalf);
        helper.assertTrue(rol != null, "Guhdalf of a camp has his role");
        guhdalf.snapTo(p.getX() + 1, p.getY(), p.getZ());
        level.addFreshEntity(guhdalf);
        Verhaallijn h1 = RingH1Feature.LIJN;
        try {
            standIn = metSnuffeleiland();
            Grillguh.setStep(p, Grillguh.DONE);
            // closed: nothing followed yet
            helper.assertTrue(!Guhpad.magKnabbelring(p) && !Ring.magBeginnen(p), "closed: no story of the Guhmensie followed");
            helper.assertTrue(sleutels(Guhpad.ontbreektVoorKnabbelring(p)).equals(List.of("gui.guhs.guhpad.verhaal.balto", "gui.guhs.guhpad.verhaal.mewtwo",
                    "gui.guhs.guhpad.verhaal.hemel", "gui.guhs.guhpad.verhaal.guhwaii", "gui.guhs.guhpad.verhaal.snuffeleiland")), "all five are missing, in order");
            String lijst = NlTekst.tekst(Guhpad.lijst(Guhpad.ontbreektVoorKnabbelring(p)));
            helper.assertTrue(lijst.equals("Baltoguh en Nomguh, Guhtwo en het kloon-eiland, Het Hemelkapelletje, Ohana op Guhwai'i en Het Snuffeleiland"),
                    "the list as a Dutch reader sees it: " + lijst);
            String nee = NlTekst.tekst(Component.translatable(Guhpad.GUHDALF_NEE, Guhpad.lijst(Guhpad.ontbreektVoorKnabbelring(p))));
            helper.assertTrue(nee.contains(lijst) && nee.contains("njeg") && !nee.contains("%s"), "Guhdalf's refusal names them, friendly: " + nee);
            helper.assertTrue(Guhpad.guhdalfWeigert(guhdalf, p), "Guhdalf says no");
            rol.talk(guhdalf, p);
            seconde(p);
            helper.assertTrue(!h1.begonnen(p) && h1.stap(p) == 0 && !Cutscenes.bezig(p), "a click on Guhdalf does not start the story, and it does not start by itself");
            // four of the five: still closed, he names the one
            for (GrootVerhaal v : List.of(GroteVerhalen.BALTO, GroteVerhalen.MEWTWO, GroteVerhalen.HEMEL, GroteVerhalen.SNUFFEL)) {
                GroteVerhalen.zetGedaan(p, v, true);
            }
            helper.assertTrue(!Guhpad.magKnabbelring(p) && !Ring.magBeginnen(p) && NlTekst.tekst(Guhpad.lijst(Guhpad.ontbreektVoorKnabbelring(p))).equals("Ohana op Guhwai'i"),
                    "four of five: still closed, one name left");
            rol.talk(guhdalf, p);
            helper.assertTrue(!h1.begonnen(p) && !Cutscenes.bezig(p), "still no");
            // the last one really finished: open
            Ohana.zet(p, Ohana.KLAAR);
            helper.assertTrue(Guhpad.magKnabbelring(p) && Ring.magBeginnen(p) && Guhpad.ontbreektVoorKnabbelring(p).isEmpty() && !Guhpad.guhdalfWeigert(guhdalf, p),
                    "all five: Guhdalf may start");
            seconde(p);
            helper.assertTrue(h1.begonnen(p), "the story begins by itself now (zoek Guhdalf)");
            // the Grillguh's barbecue is still asked, as before; and the stories alone are not enough for the portal
            ServerPlayer koud = speler(helper);
            guhmensieGedaan(koud);
            helper.assertTrue(Guhpad.magKnabbelring(koud) && !Ring.magBeginnen(koud) && !Guhpad.guhdalfWeigert(guhdalf, koud),
                    "every story but a cold barbecue: Guhdalf has no list to give, the old rule still says no");
            weg(helper, koud);
        } finally {
            if (standIn) {
                GroteVerhalen.standIn(GroteVerhalen.SNUFFEL, false);
            }
            guhdalf.discard();
            h1.wis(p);
            weg(helper, p);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // the lock on the Guhbarbecuether
    // =====================================================================================================================

    /** The grill portal: closed with the list, open after the stories and chapter 1; who is inside may stay; out always. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void guhpadGrillportaal(GameTestHelper helper) {
        boolean standIn = false;
        ServerLevel level = helper.getLevel();
        ServerPlayer nieuw = speler(helper), verhalen = speler(helper), verder = speler(helper), klaar = speler(helper);
        ResourceKey<Level> mensie = ModDimensions.GUHMENSION, bbq = BarbecuetherFeature.BARBECUETHER;
        try {
            standIn = metSnuffeleiland();
            guhmensieGedaan(verhalen);
            Ring.lijn(1).zet(verder, Ring.lijn(1).stappen());        // (was further before the update: chapter 1 done, the stories not)
            guhmensieGedaan(klaar);
            Ring.lijn(1).zet(klaar, Ring.lijn(1).stappen());
            // closed, with the list
            Component nee = GrillPortalBlock.slot(mensie, level, nieuw);
            helper.assertTrue(Guhpad.GRILL_DICHT.equals(key(nee)) && !Guhpad.magBarbecuether(nieuw), "closed for a new player, with our message");
            String tekst = NlTekst.tekst(nee);
            helper.assertTrue(tekst.contains("Baltoguh en Nomguh, Guhtwo en het kloon-eiland, Het Hemelkapelletje, Ohana op Guhwai'i en Het Snuffeleiland")
                    && tekst.contains("njeg") && !tekst.contains("%s"), "the portal lists the missing stories: " + tekst);
            helper.assertTrue(Guhpad.GRILL_DICHT.equals(key(GrillPortalBlock.slot(mensie, level, verder))) && !Guhpad.magBarbecuether(verder),
                    "everyone back to the path: chapter 1 done before the update, the stories not: closed");
            // the stories followed, Guhdalf's party not: the Knabbelring's own words
            helper.assertTrue(RingFeature.PORTAAL_DICHT.equals(key(GrillPortalBlock.slot(mensie, level, verhalen))) && !Guhpad.magBarbecuether(verhalen)
                    && sleutels(Guhpad.ontbreekt(verhalen, Wereld.BARBECUETHER)).equals(List.of(Guhpad.EIS_KNABBELFEEST)), "the stories done: only Guhdalf's party is left");
            // open
            helper.assertTrue(GrillPortalBlock.slot(mensie, level, klaar) == null && Guhpad.magBarbecuether(klaar) && Guhpad.open(klaar, Wereld.BARBECUETHER),
                    "open: the five stories and chapter 1");
            for (ServerPlayer p : List.of(nieuw, verhalen, verder, klaar)) {
                helper.assertTrue(Ring.magDoorPortaal(p) == Guhpad.magBarbecuether(p), "the Knabbelring's own gate of the portal says the same as the Guhpad");
            }
            // the way out is never blocked, and who is inside may stay: only a portal that leads IN from elsewhere is ever asked
            for (ServerPlayer p : List.of(nieuw, verder, verhalen)) {
                helper.assertTrue(GrillPortalBlock.slot(bbq, level, p) == null && Guhpad.slot(bbq, mensie, p) == null, "the way out of the Guhbarbecuether is open");
                helper.assertTrue(Guhpad.slot(bbq, bbq, p) == null && Guhpad.slot(mensie, mensie, p) == null, "a trip inside one world is nobody's business");
            }
            helper.assertTrue(!GrillPortalBlock.geweigerd(level, nieuw), "the lock is only asked in the Guhmensie");
            // nobody is moved, nothing of their story is touched: the regular look only reads
            Ring.lijn(2).zet(verder, 1);
            double x = verder.getX(), y = verder.getY(), z = verder.getZ();
            for (int i = 0; i < 5; i++) {
                GuhpadEvents.kijk(verder);
            }
            helper.assertTrue(verder.getX() == x && verder.getY() == y && verder.getZ() == z && verder.level() == level && verder.isAlive(), "nobody is moved");
            helper.assertTrue(Ring.lijn(1).klaar(verder) && Ring.lijn(2).stap(verder) == 1, "story progress is kept");
            // only players are asked
            Entity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            helper.assertTrue(GrillPortalBlock.slot(mensie, level, guh) == null && Guhpad.slot(mensie, bbq, guh) == null, "a guh walks through");
            nieuw.setGameMode(GameType.SPECTATOR);
            helper.assertTrue(GrillPortalBlock.slot(mensie, level, nieuw) == null, "a spectator passes");
        } finally {
            if (standIn) {
                GroteVerhalen.standIn(GroteVerhalen.SNUFFEL, false);
            }
            Ring.wis(verder);
            Ring.wis(klaar);
            weg(helper, nieuw, verhalen, verder, klaar);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // the lock on the Guheinde
    // =====================================================================================================================

    /** The portal of the Knabbelkelder: the Knabbelring, Super Guhrio and the Aangebrande Mika; inside you may stay, out always. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void guhpadGuheindeSlot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper), binnen = speler(helper);
        ResourceKey<Level> mensie = ModDimensions.GUHMENSION, einde = GuheindeFeature.GUHEINDE;
        // closed, with the list
        Component nee = Guhpad.guheindeSlot(mensie, p);
        helper.assertTrue(Guhpad.GUHEINDE_DICHT.equals(key(nee)) && !Guhpad.magGuheinde(p), "closed for a new player");
        String tekst = NlTekst.tekst(nee);
        helper.assertTrue(tekst.contains("In de ban van de Knabbelring, Super Guhrio en De Aangebrande Mika verslaan") && tekst.contains("njeg") && !tekst.contains("%s"),
                "the portal lists what is missing: " + tekst);
        helper.assertTrue(Guhpad.guheindeSlot(Level.OVERWORLD, p) != null && Guhpad.guheindeSlot(BarbecuetherFeature.BARBECUETHER, p) != null,
                "closed from wherever a portal to the Guheinde stands");
        // one thing at a time
        GroteVerhalen.zetGedaan(p, GroteVerhalen.KNABBELRING, true);
        helper.assertTrue(sleutels(Guhpad.ontbreekt(p, Wereld.GUHEINDE)).equals(List.of("gui.guhs.guhpad.verhaal.guhrio", Guhpad.EIS_MIKA)), "the Knabbelring done: two left");
        AangebrandeMika.zet(p);
        helper.assertTrue(!Guhpad.magGuheinde(p) && NlTekst.tekst(Guhpad.lijst(Guhpad.ontbreekt(p, Wereld.GUHEINDE))).equals("Super Guhrio"), "the Mika beaten: Super Guhrio left");
        helper.assertTrue(Guhpad.guheindeGeweigerd(level, p) && Guhpad.guheindeGeweigerd(level, p), "refused at the portal (told once per attempt)");
        GuhrioKasteel.LIJN.zet(p, GuhrioKasteel.LIJN.stappen());
        // open
        helper.assertTrue(Guhpad.magGuheinde(p) && Guhpad.guheindeSlot(mensie, p) == null && !Guhpad.guheindeGeweigerd(level, p) && Guhpad.open(p, Wereld.GUHEINDE),
                "open: the Knabbelring, Super Guhrio and the Aangebrande Mika");
        // the way out and staying inside, for a player who has done nothing of it (was in the Guheinde before the update)
        helper.assertTrue(!Guhpad.magGuheinde(binnen) && Guhpad.guheindeSlot(einde, binnen) == null && Guhpad.slot(einde, mensie, binnen) == null,
                "the terugportaal in the Guheinde is never locked");
        helper.assertTrue(Guhpad.slot(einde, einde, binnen) == null, "a Knabbelpoort inside the Guheinde is nobody's business");
        // the block itself: a locked player standing in it gets no portal trip; an open one does
        BlockPos portaal = helper.absolutePos(new BlockPos(3, 2, 3));
        level.setBlock(portaal, GuheindeFeature.GUHEINDE_PORTAAL.get().defaultBlockState(), 3);
        for (ServerPlayer wie : List.of(binnen, p)) {
            wie.snapTo(portaal.getX() + 0.5, portaal.getY() + 0.4, portaal.getZ() + 0.5);
            wie.setPortalCooldown(0);
            wie.portalProcess = null;
            level.getBlockState(portaal).entityInside(level, portaal, wie, net.minecraft.world.entity.InsideBlockEffectApplier.NOOP, true);
        }
        helper.assertTrue(binnen.portalProcess == null && p.portalProcess != null, "the portal block takes the open player and leaves the locked one");
        helper.assertTrue(GuheindeFeature.GUHEINDE_PORTAAL.get().getPortalDestination(level, binnen, portaal) == null, "and no destination for the locked one");
        level.setBlock(portaal, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        // only players are asked
        Entity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        helper.assertTrue(Guhpad.guheindeSlot(mensie, guh) == null && !Guhpad.guheindeGeweigerd(level, guh), "a guh walks through");
        binnen.setGameMode(GameType.SPECTATOR);
        helper.assertTrue(Guhpad.guheindeSlot(mensie, binnen) == null, "a spectator passes");
        GuhrioKasteel.LIJN.wis(p);
        weg(helper, p, binnen);
        helper.succeed();
    }

    /** Each world's list in order, the real Guheinde locked for everybody whatever they did. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void guhpadOntbreektLijst(GameTestHelper helper) {
        boolean standIn = false;
        ServerPlayer p = speler(helper);
        try {
            standIn = metSnuffeleiland();
            helper.assertTrue(Guhpad.eisen(p, Wereld.GUHMENSIE).isEmpty() && Guhpad.open(p, Wereld.GUHMENSIE), "the Guhmensie asks nothing");
            helper.assertTrue(sleutels(Guhpad.ontbreekt(p, Wereld.BARBECUETHER)).equals(List.of("gui.guhs.guhpad.verhaal.balto", "gui.guhs.guhpad.verhaal.mewtwo",
                    "gui.guhs.guhpad.verhaal.hemel", "gui.guhs.guhpad.verhaal.guhwaii", "gui.guhs.guhpad.verhaal.snuffeleiland", Guhpad.EIS_KNABBELFEEST)),
                    "the Guhbarbecuether: the five stories and Guhdalf's party");
            helper.assertTrue(sleutels(Guhpad.ontbreekt(p, Wereld.GUHEINDE)).equals(List.of("gui.guhs.guhpad.verhaal.knabbelring", "gui.guhs.guhpad.verhaal.guhrio",
                    Guhpad.EIS_MIKA)), "the Guheinde: the two stories and the Mika");
            helper.assertTrue(Guhpad.eisen(p, Wereld.ECHT).size() == 8 && sleutels(Guhpad.ontbreekt(p, Wereld.ECHT)).get(7).equals(Guhpad.EIS_ECHT),
                    "the real Guheinde: all seven and itself");
            for (Guhpad.Eis e : Guhpad.eisen(p, Wereld.ECHT)) {
                helper.assertTrue(NlTekst.has(e.sleutel()) && BuiltInRegistries.ITEM.getValue(Identifier.parse(e.icoon())) != Items.AIR, "a text and an icon for " + e.sleutel());
            }
            // an op marks one story: the list shrinks by exactly that one
            GroteVerhalen.zetGedaan(p, GroteVerhalen.MEWTWO, true);
            helper.assertTrue(NlTekst.tekst(Guhpad.lijst(Guhpad.ontbreektVoorKnabbelring(p))).equals("Baltoguh en Nomguh, Het Hemelkapelletje, Ohana op Guhwai'i en Het Snuffeleiland"),
                    "the list without the story that is done");
            helper.assertTrue(NlTekst.tekst(Guhpad.lijst(List.of())).isEmpty() && NlTekst.tekst(Guhpad.lijst(Guhpad.ontbreekt(p, Wereld.GUHEINDE).subList(0, 2)))
                    .equals("In de ban van de Knabbelring en Super Guhrio"), "none, and two with \"en\"");
            // everything there is, done: three worlds open, the last one not
            for (GrootVerhaal v : GroteVerhalen.alle()) {
                GroteVerhalen.zetGedaan(p, v, true);
            }
            Ring.lijn(1).zet(p, Ring.lijn(1).stappen());
            AangebrandeMika.zet(p);
            helper.assertTrue(Guhpad.open(p, Wereld.BARBECUETHER) && Guhpad.open(p, Wereld.GUHEINDE) && !Guhpad.open(p, Wereld.ECHT)
                    && sleutels(Guhpad.ontbreekt(p, Wereld.ECHT)).equals(List.of(Guhpad.EIS_ECHT)), "all followed: the real Guheinde stays locked");
        } finally {
            if (standIn) {
                GroteVerhalen.standIn(GroteVerhalen.SNUFFEL, false);
            }
            Ring.wis(p);
            weg(helper, p);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // "Mijn verhaal"
    // =====================================================================================================================

    /** Without a questline to follow, "Mijn verhaal" points to the nearest story the player has not done and may begin. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void guhpadKompas(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ResourceKey<Level> mensie = ModDimensions.GUHMENSION, bbq = BarbecuetherFeature.BARBECUETHER;
        BlockPos hier = p.blockPosition();
        Map<String, BlockPos> plekken = new HashMap<>(Map.of("nomguh", hier.offset(500, 0, 0), "kloon_eiland", hier.offset(0, 0, 120),
                "hemelkapelletje", hier.offset(-300, 0, 40), "guhwaii_ohana", hier.offset(90, 0, -90), "knabbelgouw", hier.offset(40, 0, 0)));
        var echteZoeker = GuhpadKompas.zoeker;
        GuhpadKompas.zoeker = (speler, structuur) -> plekken.get(structuur);
        try {
            helper.assertTrue(ids(GuhpadKompas.kandidaten(p)).equals(ids(GroteVerhalen.van(Wereld.GUHMENSIE))), "a new player: the stories of the Guhmensie, nothing later");
            Doel d = GuhpadKompas.doel(p, mensie);
            helper.assertTrue(d != null && d.dim() == mensie && plekken.get("kloon_eiland").equals(d.plek())
                    && NlTekst.tekst(d.tekst()).equals(NlTekst.get("structure.guhs.kloon_eiland")), "the nearest one: the kloon-eiland at 120 blocks");
            // the answer is kept for a while (a structure lookup is not free)...
            plekken.put("nomguh", hier.offset(10, 0, 0));
            helper.assertTrue(plekken.get("kloon_eiland").equals(GuhpadKompas.doel(p, mensie).plek()), "kept for a while");
            // ...until the stories change: Mewtwo done, and Nomguh is nearest now
            GroteVerhalen.zetGedaan(p, GroteVerhalen.MEWTWO, true);
            d = GuhpadKompas.doel(p, mensie);
            helper.assertTrue(plekken.get("nomguh").equals(d.plek()), "Mewtwo done: the next nearest, Nomguh");
            // from another dimension: a goal elsewhere (the compass then shows the portal last used)
            GuhpadKompas.vergeet(p.getUUID());
            d = GuhpadKompas.doel(p, Level.OVERWORLD);
            helper.assertTrue(d != null && d.dim() == mensie && d.plek() == null && "nomguh".equals(d.structuur()), "from the overworld: a story in the Guhmensie");
            helper.assertTrue(Doelen.van(p) == null && Doelen.kompas(p) != null && Doelen.kompas(p).dim() == mensie && Doelen.wijs(p) == null,
                    "Doelen.kompas falls back on it; no portal known in the test world, so nothing to point at");
            // every story of the Guhmensie done: Guhdalf's camp (the Knabbelring begins in the Guhmensie), not the castle yet
            guhmensieGedaan(p);
            GuhpadKompas.vergeet(p.getUUID());
            d = GuhpadKompas.doel(p, mensie);
            helper.assertTrue(ids(GuhpadKompas.kandidaten(p)).equals(List.of("knabbelring")) && plekken.get("knabbelgouw").equals(d.plek()),
                    "the Guhmensie done: the Knabbelgouw; Super Guhrio only once the grill portal is open");
            Ring.lijn(1).zet(p, Ring.lijn(1).stappen());
            helper.assertTrue(ids(GuhpadKompas.kandidaten(p)).equals(List.of("knabbelring", "guhrio")), "chapter 1 done: the castle is a goal too");
            GroteVerhalen.zetGedaan(p, GroteVerhalen.KNABBELRING, true);
            d = GuhpadKompas.doel(p, mensie);
            helper.assertTrue(d != null && d.dim() == bbq && GuhrioKasteel.STRUCTUUR.equals(d.structuur()), "only Super Guhrio left: in the Guhbarbecuether");
            plekken.put(GuhrioKasteel.STRUCTUUR, hier.offset(0, 0, 64));
            d = GuhpadKompas.doel(p, bbq);
            helper.assertTrue(d != null && hier.offset(0, 0, 64).equals(d.plek()), "and once there: the castle itself");
            GroteVerhalen.zetGedaan(p, GroteVerhalen.GUHRIO, true);
            helper.assertTrue(GuhpadKompas.kandidaten(p).isEmpty() && GuhpadKompas.doel(p, mensie) == null, "everything followed: nowhere to point");
        } finally {
            GuhpadKompas.zoeker = echteZoeker;
            Ring.wis(p);
            weg(helper, p);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // texts, the quest book
    // =====================================================================================================================

    /** Every text the Guhpad shows exists (Dutch), every hidden advancement is in the datapack, both pictures ship. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void guhpadTekstenBestaan(GameTestHelper helper) {
        List<String> keys = new java.util.ArrayList<>(List.of("gui.guhs.guhpad.titel", "gui.guhs.guhpad.uitleg", "gui.guhs.guhpad.teller", "gui.guhs.guhpad.teller.uitleg",
                "gui.guhs.guhpad.slot.open", "gui.guhs.guhpad.slot.dicht", "gui.guhs.guhpad.slot.echt", "gui.guhs.guhpad.op_slot", "gui.guhs.guhpad.open", "gui.guhs.guhpad.nog_nodig",
                "gui.guhs.guhpad.leeg", "gui.guhs.guhpad.hier", "gui.guhs.guhpad.klik_halte", "gui.guhs.guhpad.vouw_open", "gui.guhs.guhpad.vouw_dicht",
                "gui.guhs.guhpad.groot", "gui.guhs.guhpad.lijst.komma", "gui.guhs.guhpad.lijst.en", Guhpad.EIS_KNABBELFEEST, Guhpad.EIS_MIKA, Guhpad.EIS_ECHT,
                "gui.guhs.guhpad.echt.bekend", "gui.guhs.guhpad.echt.laag6", "gui.guhs.guhpad.echt.laag6.tekst", "gui.guhs.guhpad.echt.raadsel.1",
                "gui.guhs.guhpad.echt.raadsel.2", "gui.guhs.guhpad.echt.raadsel.3", "gui.guhs.guhpad.kompas.uitleg", "gui.guhs.guhpad.kompas.dichtstbij",
                "gui.guhs.guhpad.kompas.volgt", Guhpad.GUHDALF_NEE, Guhpad.GRILL_DICHT, Guhpad.GUHEINDE_DICHT, RingFeature.PORTAAL_DICHT,
                "stat.guhs." + GuhpadFeature.STATISTIEK, "structure.guhs.@doel"));
        for (Wereld w : Wereld.values()) {
            keys.add("gui.guhs.guhpad.wereld." + w.id());
            keys.add("gui.guhs.guhpad.wereld." + w.id() + ".kort");
            keys.add("gui.guhs.guhpad.wereld." + w.id() + ".uitleg");
            helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Guhs.id("quest/guhpad_open_" + w.id())) != null, "the advancement of " + w.id());
            helper.assertTrue(BuiltInRegistries.ITEM.getValue(Identifier.parse(w.icoon())) != Items.AIR, "the icon of " + w.id());
        }
        for (GrootVerhaal v : GroteVerhalen.geregistreerd()) {
            keys.add(v.naamSleutel());
        }
        for (String key : keys) {
            helper.assertTrue(NlTekst.has(key) && !NlTekst.get(key).isBlank() || key.startsWith("gui.guhs.guhpad.lijst."), "the text " + key);
        }
        for (String key : List.of(Guhpad.GUHDALF_NEE, Guhpad.GRILL_DICHT, Guhpad.GUHEINDE_DICHT, "gui.guhs.guhpad.nog_nodig", "gui.guhs.guhpad.kompas.volgt")) {
            helper.assertTrue(NlTekst.get(key).contains("%s"), key + " has room for its list");
        }
        helper.assertTrue(NlTekst.get("gui.guhs.guhpad.teller").chars().filter(c -> c == '%').count() == 2, "the counter: n van m");
        for (String quest : List.of("guhpad_mika", "guhpad_echt")) {
            helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Guhs.id("quest/" + quest)) != null, "the advancement " + quest);
        }
        for (String plaatje : List.of("padkaart", "echt")) {
            helper.assertTrue(GuhpadGameTests.class.getClassLoader().getResource("assets/guhs/textures/gui/guhpad/" + plaatje + ".png") != null, "the picture " + plaatje);
        }
        helper.assertTrue(BuiltInRegistries.CUSTOM_STAT.containsKey(Guhs.id(GuhpadFeature.STATISTIEK)), "the statistic is registered");
        helper.succeed();
    }

    private static String bron(String path) throws IOException {
        try (InputStream in = FtbQuestsChapter.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String groep(String chapter) {
        Matcher m = Pattern.compile("(?m)^\\s*group: \"([0-9A-F]{16})\"").matcher(chapter);
        return m.find() ? m.group(1) : "";
    }

    /**
     * The quest book: the chapter group "Het Guhpad" at the end with its four chapters in order, each starting with its lock
     * quest; the story quests moved with the ids they had; the counter counts the big stories of this game; the preview is
     * really locked; a chapter of ours that is gone is taken away from a pack, a pack's own edit is not.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void guhpadQuestboek(GameTestHelper helper) {
        try {
            String index = bron("ftbquests/index.txt");
            List<String> groepen = index.lines().filter(l -> l.startsWith("group ")).map(l -> l.substring(6).trim()).toList();
            List<String> hoofdstukken = FtbQuestsChapter.chapters();
            List<String> pad = List.of("guhs_verhalen", "guhs_pad_barbecuether", "guhs_guheinde", "guhs_pad_echt");
            helper.assertTrue(groepen.size() == 2 && !groepen.get(0).equals(groepen.get(1)), "two chapter groups: " + groepen);
            helper.assertTrue(hoofdstukken.subList(hoofdstukken.size() - 4, hoofdstukken.size()).equals(pad), "the four chapters of the Guhpad last, in order: " + hoofdstukken);
            helper.assertTrue(!hoofdstukken.contains("guhs_knabbelring") && !hoofdstukken.contains("guhs_guhrio") && bron("ftbquests/chapters/guhs_knabbelring.json5") == null,
                    "the two story chapters of the Guhbarbecuether are one chapter of the Guhpad now");
            helper.assertTrue(groep(bron("ftbquests/chapters/guhs_basis.json5")).equals(groepen.get(0)), "Guhs & basis is in the first group");
            Map<String, String> slot = Map.of("guhs_verhalen", "Het Guhpad begint hier", "guhs_pad_barbecuether", "Slotje: eerst de Guhmensie",
                    "guhs_guheinde", "Slotje: eerst de Guhbarbecuether", "guhs_pad_echt", "Verhalen gevolgd");
            for (int i = 0; i < pad.size(); i++) {
                String naam = pad.get(i), chapter = bron("ftbquests/chapters/" + naam + ".json5"), nl = bron("ftbquests/lang/nl_nl/" + naam + ".json5");
                helper.assertTrue(groep(chapter).equals(groepen.get(1)), naam + " is in the group Het Guhpad");
                helper.assertTrue(nl.contains("\"" + slot.get(naam) + "\"") && !nl.contains("Hoe kom je hier?"), naam + " starts with its lock quest: " + slot.get(naam));
                helper.assertTrue(Pattern.compile("chapter_subtitle\": \\[\\s*\"" + (i + 1) + "\\. ").matcher(nl).find(), naam + " is number " + (i + 1) + " of the path");
                helper.assertTrue(nl.contains("chapter_group." + groepen.get(1) + ".title\": \"&6Het Guhpad\"") == (i == 0), "the group's name comes with its first chapter");
            }
            // what each lock quest shows
            String mensie = bron("ftbquests/lang/nl_nl/guhs_verhalen.json5"), bbq = bron("ftbquests/lang/nl_nl/guhs_pad_barbecuether.json5"),
                    einde = bron("ftbquests/lang/nl_nl/guhs_guheinde.json5"), echt = bron("ftbquests/chapters/guhs_pad_echt.json5");
            for (GrootVerhaal v : GroteVerhalen.van(Wereld.GUHMENSIE)) {
                helper.assertTrue(bbq.contains("\"" + NlTekst.get(v.naamSleutel()) + "\"") && bron("ftbquests/chapters/guhs_pad_barbecuether.json5")
                        .contains("advancement: \"guhs:quest/guhpad_klaar_" + v.id() + "\""), "the lock quest of the Guhbarbecuether asks " + v.id());
            }
            helper.assertTrue(einde.contains("\"In de ban van de Knabbelring\"") && einde.contains("\"Super Guhrio\"") && einde.contains("\"De Aangebrande Mika verslaan\"")
                    && bron("ftbquests/chapters/guhs_guheinde.json5").contains("advancement: \"guhs:quest/guhpad_mika\""), "the lock quest of the Guheinde");
            helper.assertTrue(mensie.contains("Mijn verhaal") && bron("ftbquests/chapters/guhs_verhalen.json5").contains("dimension: \"guhs:guhmension\""),
                    "the first chapter is open: step into the Guhmensie");
            // the ONE counter: a stat task on the statistic, as many as there are big stories in this game
            Matcher teller = Pattern.compile("stat: \"guhs:" + GuhpadFeature.STATISTIEK + "\",\\s*value: (\\d+)|value: (\\d+),\\s*stat: \"guhs:" + GuhpadFeature.STATISTIEK + "\"")
                    .matcher(echt);
            helper.assertTrue(teller.find(), "the counter of Het echte Guheinde is a stat task");
            int m = Integer.parseInt(teller.group(1) != null ? teller.group(1) : teller.group(2));
            helper.assertTrue(m == GroteVerhalen.totaal(), "the quest book counts " + m + " big stories, the game " + GroteVerhalen.totaal());
            helper.assertTrue(echt.split("type: \"stat\"", -1).length == 2, "ONE counter");
            // the preview: every quest but the lock quest really hangs behind it, and nobody can do it
            Matcher slotId = Pattern.compile("\"quest\\.(475548[0-9A-F]{10})\\.title\": \"Verhalen gevolgd\"").matcher(bron("ftbquests/lang/nl_nl/guhs_pad_echt.json5"));
            helper.assertTrue(slotId.find(), "the lock quest of Het echte Guheinde");
            int quests = echt.split("(?m)^    \\{\\R      id: \"475548", -1).length - 1;
            int achterSlot = echt.split("dependencies: \\[\\s*\"" + slotId.group(1) + "\",?\\s*\\]", -1).length - 1;
            helper.assertTrue(quests == 9 && achterSlot == 8, "eight question marks behind the lock quest: " + quests + " quests, " + achterSlot + " locked");
            helper.assertTrue(echt.split("guhs:quest/guhpad_echt", -1).length - 1 == 9, "every one of them asks the advancement nobody gets");
            helper.assertTrue(bron("ftbquests/lang/nl_nl/guhs_pad_echt.json5").contains("Gaat pas open in &7het echte Guheinde"), "layer 6: only in the real Guheinde");
            // the Knabbelring is still told in order, in its new chapter; the stories of the Guhmensie and the Guheinde are still free
            String ringBoek = bron("ftbquests/chapters/guhs_pad_barbecuether.json5");
            helper.assertTrue(ringBoek.contains("progression_mode: \"linear\"") && !bron("ftbquests/chapters/guhs_verhalen.json5").contains("dependencies:")
                    && !bron("ftbquests/chapters/guhs_guheinde.json5").contains("dependencies:"), "only the Knabbelring is locked quest by quest");
            // a pack that has the old chapters: ours go, an edited one stays, the two groups are added in order
            Path quests2 = Files.createTempDirectory("guhs-guhpad");
            Files.createDirectories(quests2.resolve("chapters"));
            String oud = "{\n  guhs_chapter_version: 30,\n  filename: \"guhs_knabbelring\",\n  id: \"4755480000000001\",\n}\n";
            String bewerkt = "{\n  filename: \"guhs_guhrio\",\n  id: \"4755480000000002\",\n  x: 4.5,\n}\n";
            Files.writeString(quests2.resolve("chapters").resolve("guhs_knabbelring.json5"), oud);
            Files.writeString(quests2.resolve("chapters").resolve("guhs_guhrio.json5"), bewerkt);
            Files.writeString(quests2.resolve("guhs_chapters.txt"), "guhs_knabbelring 30 " + FtbQuestsChapter.fingerprint(oud) + "\nguhs_guhrio 30 0-0-0-0\n");
            helper.assertTrue(FtbQuestsChapter.installInto(quests2), "installs");
            helper.assertTrue(!Files.exists(quests2.resolve("chapters").resolve("guhs_knabbelring.json5")), "our old chapter that is gone is taken away");
            helper.assertTrue(Files.readString(quests2.resolve("chapters").resolve("guhs_guhrio.json5")).equals(bewerkt), "a chapter the pack changed stays");
            String ingeschreven = Files.readString(quests2.resolve("guhs_chapters.txt"));
            helper.assertTrue(!ingeschreven.contains("guhs_knabbelring ") && ingeschreven.contains("guhs_guhrio ") && ingeschreven.contains("guhs_pad_barbecuether "),
                    "the record follows");
            String g = Files.readString(quests2.resolve("chapter_groups.json5"));
            helper.assertTrue(g.contains(groepen.get(0)) && g.indexOf(groepen.get(1)) > g.indexOf(groepen.get(0)), "both groups, Het Guhpad after Guhs: " + g);
            helper.assertTrue(!FtbQuestsChapter.installInto(quests2) && Files.readString(quests2.resolve("chapter_groups.json5")).equals(g), "and not again");
        } catch (IOException e) {
            helper.fail("io: " + e);
        }
        helper.succeed();
    }
}
