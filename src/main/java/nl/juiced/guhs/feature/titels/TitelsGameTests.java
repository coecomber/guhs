package nl.juiced.guhs.feature.titels;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.feature.balto.BaltoVerhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.guhwaii.Ohana;
import nl.juiced.guhs.feature.hemel.HemelQuest;
import nl.juiced.guhs.feature.knuffeldal.Feestbuffet;
import nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang;
import nl.juiced.guhs.feature.timmerguh.TimmerguhVoortgang;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * The titles (1.2.6): every title has its texts and icon; a title is earned from the progress that was already there;
 * a player without titles has a plain name; without a choice the first earned title shows (what the player list showed
 * before 1.2.6); choosing one, another, a locked one and none; only one title shows at a time, in the player list, the
 * display name (above the head) and the chat name; a newly earned title is noticed; the payloads survive the network.
 */
public class TitelsGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "titels";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Titels.vergeet(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** Does this text (or a part of it) show this lang key? */
    static boolean noemt(Component c, String key) {
        if (c == null) {
            return false;
        }
        if (c.getContents() instanceof TranslatableContents t && t.getKey().equals(key)) {
            return true;
        }
        for (Component s : c.getSiblings()) {
            if (noemt(s, key)) {
                return true;
            }
        }
        return false;
    }

    /** How many titles this text shows. */
    static int aantalTitels(Component c) {
        int n = 0;
        for (Titels.Titel t : Titels.ALLE) {
            if (noemt(c, t.naamSleutel())) {
                n++;
            }
        }
        return n;
    }

    /** The three places show exactly this title (null: no title at all, a plain name). */
    private static void toont(GameTestHelper helper, ServerPlayer p, Titels.Titel t, String wat) {
        Component tab = p.getTabListDisplayName(), naam = p.getDisplayName(), chat = ChatType.bind(ChatType.CHAT, p).name();
        if (t == null) {
            helper.assertTrue(Titels.actief(p) == null, wat + ": no title shows");
            helper.assertTrue(tab == null, wat + ": the player list leaves the name alone, got " + tab);
            helper.assertTrue(aantalTitels(naam) == 0 && naam.getString().equals(p.getName().getString()), wat + ": a plain display name, got " + naam.getString());
            helper.assertTrue(aantalTitels(chat) == 0 && chat.getString().equals(p.getName().getString()), wat + ": a plain chat name, got " + chat.getString());
            return;
        }
        helper.assertTrue(Titels.actief(p) == t, wat + ": " + t.id() + " shows, got " + Titels.actief(p));
        helper.assertTrue(noemt(tab, t.naamSleutel()) && aantalTitels(tab) == 1, wat + ": the player list shows only " + t.id());
        helper.assertTrue(tab.getString().startsWith(p.getName().getString()), wat + ": the title comes after the name in the player list");
        helper.assertTrue(noemt(naam, t.naamSleutel()) && aantalTitels(naam) == 1, wat + ": the display name (above the head) shows only " + t.id());
        helper.assertTrue(naam.getString().startsWith(p.getName().getString()), wat + ": the title comes after the display name");
        helper.assertTrue(noemt(chat, t.naamSleutel()) && aantalTitels(chat) == 1, wat + ": the chat name shows only " + t.id());
    }

    private static Titels.Titel titel(String id) {
        return Titels.van(id);
    }

    // =====================================================================================================================

    /** Every title: a unique id, a name, a hint, an icon; the texts of the tab. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void titelsTekstenBestaan(GameTestHelper helper) {
        Language lang = Language.getInstance();
        Set<String> ids = new HashSet<>(), namen = new HashSet<>();
        // (bbq2: the eight built-in titles, the guhpixel ones, and the ones slices registered with Titels.registreer: ringdrager, ...)
        helper.assertTrue(Titels.ALLE.size() >= 8 + nl.juiced.guhs.feature.guhpixel.GuhpixelTitels.ALLE.size(),
                "at least the eight built-in titles and the guhpixel ones, got " + Titels.ALLE.size());
        for (Titels.Titel t : Titels.ALLE) {
            helper.assertTrue(ids.add(t.id()) && namen.add(t.naamSleutel()), "unique: " + t.id());
            helper.assertTrue(lang.has(t.naamSleutel()), "name of " + t.id());
            helper.assertTrue(lang.has("gui.guhs.titels.hint." + t.id()), "hint of " + t.id());
            helper.assertTrue(BuiltInRegistries.ITEM.getValue(Identifier.parse(t.icoon())) != Items.AIR, "icon of " + t.id() + ": " + t.icoon());
            helper.assertTrue(Titels.van(t.id()) == t, "found by id: " + t.id());
        }
        helper.assertTrue(Titels.ALLE.get(0).id().equals(Titels.HELD_VAN_NOMGUH) && Titels.ALLE.get(1).id().equals(Titels.KNUFFELBURGEMEESTER),
                "the two old titles come first (what shows without a choice)");
        helper.assertTrue(Titels.van("bestaat_niet") == null && Titels.van(null) == null && Titels.van(Titels.GEEN) == null, "unknown ids");
        for (String k : List.of("geen", "geen.tip", "uitleg", "gekozen", "kies", "tip.waar", "tip.voorbeeld", "tip.kies", "tip.weg", "tip.op_slot", "nieuw")) {
            helper.assertTrue(lang.has("gui.guhs.titels." + k), "text " + k);
        }
        helper.assertTrue(lang.has("gui.guhs.guhdex.tab.titels"), "the tab name");
        helper.succeed();
    }

    /** A player without titles: nothing earned, a plain name everywhere, and nothing can be chosen. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void titelsZonderTitelGewoneNaam(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Titels.kijk(p);
        helper.assertTrue(Titels.behaald(p).isEmpty(), "nothing earned");
        toont(helper, p, null, "a new player");
        for (Titels.Titel t : Titels.ALLE) {
            helper.assertTrue(!Titels.heeft(p, t) && !Titels.kies(p, t.id()), "can't choose the locked " + t.id());
        }
        helper.assertTrue(Titels.keuze(p).isEmpty(), "no choice was saved");
        toont(helper, p, null, "after trying the locked ones");
        TitelsPayloads.Stand stand = TitelsPayloads.stand(p);
        helper.assertTrue(stand.behaald().isEmpty() && stand.actief().isEmpty(), "the tab: everything locked");
        weg(helper, p);
        helper.succeed();
    }

    /** Each title is earned from the progress that was already there. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void titelsVerdiendUitVoortgang(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Map<String, Runnable> hoe = Map.of(
                Titels.HELD_VAN_NOMGUH, () -> GuhQuests.saved(p).putBoolean(BaltoVerhaal.HELD, true),
                Titels.KNUFFELBURGEMEESTER, () -> GuhQuests.saved(p).putBoolean(Feestbuffet.TITEL, true),
                Titels.VRIEND_VAN_GUHTWO, () -> MewtwoVoortgang.zetStap(p, MewtwoVoortgang.KLAAR),
                Titels.OHANA_GUH, () -> Ohana.zet(p, Ohana.KLAAR),
                Titels.WOLKENVRIEND, () -> HemelQuest.wakker(p),
                Titels.HUISJESBOUWER, () -> TimmerguhVoortgang.zet(p, TimmerguhVoortgang.KLAAR),
                Titels.OPPER_VADSER, () -> GuhQuests.saved(p).putInt(GuheindeGevecht.WINS, 1),
                Titels.GUHKENNER, () -> GuhWorldData.get(helper.getLevel().getServer()).player(p.getUUID()).seen.addAll(GuhDex.TELLEND));
        // (the guhpixel titles have their own tests; bbq2: a title a slice registered itself is tested by that slice, e.g. RingGameTests.ringVerhaal)
        helper.assertTrue(hoe.size() == 8 && Titels.ALLE.stream().filter(t -> hoe.containsKey(t.id())).count() == hoe.size(),
                "a way to earn every built-in title");
        int n = 0;
        // (the last one first: each new title is the only new one)
        for (int i = 8 - 1; i >= 0; i--) {
            Titels.Titel t = Titels.ALLE.get(i);
            if (!hoe.containsKey(t.id())) {
                continue;
            }
            helper.assertTrue(!Titels.heeft(p, t), t.id() + " isn't earned yet");
            hoe.get(t.id()).run();
            n++;
            helper.assertTrue(Titels.heeft(p, t), t.id() + " is earned");
            helper.assertTrue(Titels.behaald(p).size() == n, "only " + t.id() + " was added: " + Titels.ids(Titels.behaald(p)));
            Titels.kijk(p);
            toont(helper, p, t, "never chosen, " + t.id() + " is the first earned one");
        }
        // an almost full Guhdex is no full Guhdex; a timmerguh questline one step short neither
        var seen = GuhWorldData.get(helper.getLevel().getServer()).player(p.getUUID()).seen;
        seen.remove(GuhDex.TELLEND.get(0));
        TimmerguhVoortgang.zet(p, TimmerguhVoortgang.KLAAR - 1);
        helper.assertTrue(!Titels.heeft(p, titel(Titels.GUHKENNER)) && !Titels.heeft(p, titel(Titels.HUISJESBOUWER)), "one short: no title");
        seen.clear();
        weg(helper, p);
        helper.succeed();
    }

    /**
     * The old behaviour: who was Held van Nomguh before 1.2.6 (the flag, never chose anything) shows that title; with the
     * Knuffelburgemeester title too it stays Held van Nomguh (one title, not both); only Knuffelburgemeester shows that.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void titelsOudeTitelBlijftStaan(GameTestHelper helper) {
        ServerPlayer held = speler(helper), burgemeester = speler(helper);
        GuhQuests.saved(held).putBoolean(BaltoVerhaal.HELD, true);
        GuhQuests.saved(held).remove(Titels.BEKEND);   // (as after the update: never checked)
        Titels.kijk(held);
        helper.assertTrue(Titels.keuze(held).isEmpty(), "nothing was chosen for the player");
        toont(helper, held, titel(Titels.HELD_VAN_NOMGUH), "an old Held van Nomguh");
        helper.assertTrue(GuhQuests.saved(held).getStringOr(Titels.BEKEND, "").equals(Titels.HELD_VAN_NOMGUH), "the old title is known, without a message");
        GuhQuests.saved(held).putBoolean(Feestbuffet.TITEL, true);
        Titels.kijk(held);
        toont(helper, held, titel(Titels.HELD_VAN_NOMGUH), "both old titles: one shows");
        helper.assertTrue(Titels.behaald(held).size() == 2, "both are earned");

        GuhQuests.saved(burgemeester).putBoolean(Feestbuffet.TITEL, true);
        Titels.kijk(burgemeester);
        toont(helper, burgemeester, titel(Titels.KNUFFELBURGEMEESTER), "an old Knuffelburgemeester");
        weg(helper, held, burgemeester);
        helper.succeed();
    }

    /** Choosing: one title, another one, a locked one (refused), none; the choice is saved with the player. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void titelsKiezen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhQuests.saved(p).putBoolean(BaltoVerhaal.HELD, true);
        GuhQuests.saved(p).putBoolean(Feestbuffet.TITEL, true);
        GuhQuests.saved(p).putInt(GuheindeGevecht.WINS, 2);
        Titels.kijk(p);
        toont(helper, p, titel(Titels.HELD_VAN_NOMGUH), "before choosing");

        helper.assertTrue(Titels.kies(p, Titels.OPPER_VADSER), "choose Opper-vadser");
        toont(helper, p, titel(Titels.OPPER_VADSER), "chosen");
        helper.assertTrue(GuhQuests.saved(p).getStringOr(Titels.KEUZE, "").equals(Titels.OPPER_VADSER), "saved with the player");
        helper.assertTrue(TitelsPayloads.stand(p).actief().equals(Titels.OPPER_VADSER) && TitelsPayloads.stand(p).behaald().size() == 3, "the tab knows");

        helper.assertTrue(Titels.kies(p, Titels.KNUFFELBURGEMEESTER), "choose another");
        toont(helper, p, titel(Titels.KNUFFELBURGEMEESTER), "another one");

        helper.assertTrue(!Titels.kies(p, Titels.VRIEND_VAN_GUHTWO) && !Titels.kies(p, "bestaat_niet"), "a locked or unknown title is refused");
        toont(helper, p, titel(Titels.KNUFFELBURGEMEESTER), "after a refused choice");

        helper.assertTrue(Titels.kies(p, Titels.GEEN), "choose none");
        toont(helper, p, null, "no title");
        helper.assertTrue(Titels.behaald(p).size() == 3 && TitelsPayloads.stand(p).actief().isEmpty(), "still earned, none shows");
        Titels.kijk(p);
        toont(helper, p, null, "no title stays no title");
        // a title earned later doesn't push itself forward when you chose none
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.KLAAR);
        Titels.kijk(p);
        toont(helper, p, null, "a new title, still none chosen");
        helper.assertTrue(Titels.kies(p, Titels.VRIEND_VAN_GUHTWO), "now it can be chosen");
        toont(helper, p, titel(Titels.VRIEND_VAN_GUHTWO), "the new one");
        helper.assertTrue(Titels.kies(p, ""), "\"\" is none too");
        toont(helper, p, null, "none again");
        // 1.3.1: a saved choice for a title that doesn't exist any more (six guhpixel titles were removed): no title, no raw key
        for (String weg : List.of("lobby_dakhaas", "lobby_mvg", "among_onterecht", "among_kussenkampioen", "among_speurguh", "among_taakjesguh")) {
            helper.assertTrue(Titels.van(weg) == null, "removed in 1.3.1: " + weg);
            GuhQuests.saved(p).putString(Titels.KEUZE, weg);
            Titels.kijk(p);
            toont(helper, p, null, "a removed title was chosen (" + weg + ")");
            helper.assertTrue(TitelsPayloads.stand(p).actief().isEmpty() && !TitelsPayloads.stand(p).behaald().contains(weg), "the tab: none shows");
        }
        helper.assertTrue(Titels.kies(p, Titels.OPPER_VADSER), "and another title can be chosen again");
        toont(helper, p, titel(Titels.OPPER_VADSER), "after a removed title");
        weg(helper, p);
        helper.succeed();
    }

    /**
     * The regular check: a title earned while playing shows without anything else (never chosen) and is remembered as
     * announced; a chosen title that is taken away falls back to the first earned one.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void titelsNieuwVerdiendEnKwijt(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Titels.kijk(p);
        helper.assertTrue(GuhQuests.saved(p).contains(Titels.BEKEND) && GuhQuests.saved(p).getStringOr(Titels.BEKEND, "x").isEmpty(), "checked once, nothing known");
        toont(helper, p, null, "nothing yet");
        TimmerguhVoortgang.zet(p, TimmerguhVoortgang.KLAAR);
        Titels.kijk(p);
        toont(helper, p, titel(Titels.HUISJESBOUWER), "the check found the new title");
        helper.assertTrue(GuhQuests.saved(p).getStringOr(Titels.BEKEND, "").equals(Titels.HUISJESBOUWER), "announced once");
        GuhQuests.saved(p).putInt(GuheindeGevecht.WINS, 1);
        Titels.kijk(p);
        toont(helper, p, titel(Titels.HUISJESBOUWER), "a second title: the first earned one stays");
        helper.assertTrue(GuhQuests.saved(p).getStringOr(Titels.BEKEND, "").equals(Titels.HUISJESBOUWER + "," + Titels.OPPER_VADSER), "both announced");
        helper.assertTrue(Titels.kies(p, Titels.OPPER_VADSER), "choose the second");
        GuhQuests.saved(p).putInt(GuheindeGevecht.WINS, 0);
        Titels.kijk(p);
        toont(helper, p, titel(Titels.HUISJESBOUWER), "the chosen title is gone: the first earned one");
        TimmerguhVoortgang.zet(p, 0);
        Titels.kijk(p);
        toont(helper, p, null, "everything gone: a plain name");
        weg(helper, p);
        helper.succeed();
    }

    /** The Balto story's reward and the wipe refresh the names at once (no waiting for the check). */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void titelsBaltoVerhaalVerverst(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BaltoVerhaal.wis(p);
        toont(helper, p, null, "before the story");
        BaltoVerhaal.zet(p, BaltoVerhaal.AANGEKOMEN);
        BaltoVerhaal.beloon(p, null);
        toont(helper, p, titel(Titels.HELD_VAN_NOMGUH), "right after the story");
        BaltoVerhaal.wis(p);
        toont(helper, p, null, "after the wipe");
        weg(helper, p);
        helper.succeed();
    }

    /** The payloads survive the network; the list for the clients names every player with a title, and only those. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void titelsPayloads(GameTestHelper helper) {
        ServerPlayer met = speler(helper), zonder = speler(helper);
        GuhQuests.saved(met).putBoolean(Feestbuffet.TITEL, true);
        GuhQuests.saved(met).putInt(GuheindeGevecht.WINS, 1);
        Titels.kijk(met);
        Titels.kijk(zonder);
        TitelsPayloads.Stand stand = TitelsPayloads.stand(met);
        helper.assertTrue(stand.behaald().equals(List.of(Titels.KNUFFELBURGEMEESTER, Titels.OPPER_VADSER)) && stand.actief().equals(Titels.KNUFFELBURGEMEESTER),
                "the tab's data: " + stand);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        TitelsPayloads.Stand.STREAM_CODEC.encode(buf, stand);
        helper.assertTrue(TitelsPayloads.Stand.STREAM_CODEC.decode(buf).equals(stand), "guhs:titels arrives");
        TitelsPayloads.Kies.STREAM_CODEC.encode(buf, new TitelsPayloads.Kies(Titels.GEEN));
        helper.assertTrue(TitelsPayloads.Kies.STREAM_CODEC.decode(buf).id().equals(Titels.GEEN), "guhs:titels_kies arrives");

        Map<UUID, String> actief = Titels.actief(helper.getLevel().getServer()).titels();
        helper.assertTrue(Titels.KNUFFELBURGEMEESTER.equals(actief.get(met.getUUID())) && !actief.containsKey(zonder.getUUID()), "who shows what: " + actief);
        TitelsPayloads.Actief.STREAM_CODEC.encode(buf, new TitelsPayloads.Actief(actief));
        Map<UUID, String> na = TitelsPayloads.Actief.STREAM_CODEC.decode(buf).titels();
        helper.assertTrue(na.equals(actief), "guhs:titels_actief arrives");
        // the client side of it: the title of a player as the server told it (the name above the head)
        Titels.zetClientActief(na);
        helper.assertTrue(Titels.clientTitel(met.getUUID()) == titel(Titels.KNUFFELBURGEMEESTER) && Titels.clientTitel(zonder.getUUID()) == null, "the client's list");
        Titels.zetClientActief(Map.of());
        // after choosing, the list follows
        Titels.kies(met, Titels.OPPER_VADSER);
        helper.assertTrue(Titels.OPPER_VADSER.equals(Titels.actief(helper.getLevel().getServer()).titels().get(met.getUUID())), "the list follows the choice");
        Titels.kies(met, Titels.GEEN);
        helper.assertTrue(!Titels.actief(helper.getLevel().getServer()).titels().containsKey(met.getUUID()), "no title: not in the list");
        weg(helper, met, zonder);
        helper.succeed();
    }
}
