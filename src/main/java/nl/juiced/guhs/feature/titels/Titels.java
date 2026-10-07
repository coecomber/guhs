package nl.juiced.guhs.feature.titels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.balto.BaltoVerhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.guhwaii.Ohana;
import nl.juiced.guhs.feature.hemel.HemelQuest;
import nl.juiced.guhs.feature.knuffeldal.Burgemeester;
import nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang;
import nl.juiced.guhs.feature.timmerguh.TimmerguhVoortgang;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * The titles (1.2.6): big achievements give a title, and a player picks ONE of the titles they have earned (or none) to
 * show behind their name: in the player list (PlayerEvent.TabListNameFormat), above their head and in chat (both through
 * the display name, PlayerEvent.NameFormat; see {@link TitelsEvents}).
 * <p>
 * Whether a title is earned is read from the progress that was already there (the flags of the two old titles, the
 * stories' steps, the Guhdex, the Opper-Mika wins), so whoever earned something before 1.2.6 has its title at once.
 * The choice is saved per player ({@link #KEUZE} in GuhQuests.saved). Without a choice the first earned title of
 * {@link #ALLE} shows: exactly what the player list showed before (Held van Nomguh, else Knuffelburgemeester).
 * Resources: the texts in tools/features/taal.py (OVERIG); the Guhdex tab is client.GidsTitelsTab.
 */
public final class Titels {
    /** A title: its id, its colour behind a name, the item that draws it in the Guhdex, and who has earned it. */
    public record Titel(String id, String naamSleutel, ChatFormatting kleur, String icoon, Predicate<ServerPlayer> behaald) {
        public MutableComponent naam() {
            return Component.translatable(naamSleutel);
        }

        /** How to earn it (shown while it is locked): a hint, no spoilers. */
        public MutableComponent hint() {
            return Component.translatable("gui.guhs.titels.hint." + id);
        }
    }

    /** The choice in GuhQuests.saved: a title id, {@link #GEEN}, or absent (never chosen: the first earned one shows). */
    public static final String KEUZE = "guhs_titel";
    public static final String GEEN = "geen";
    /** The earned titles the player has been told about (comma separated ids); absent: never checked yet. */
    public static final String BEKEND = "guhs_titels_bekend";

    public static final String HELD_VAN_NOMGUH = "held_van_nomguh", KNUFFELBURGEMEESTER = "knuffelburgemeester",
            VRIEND_VAN_GUHTWO = "vriend_van_guhtwo", OHANA_GUH = "ohana_guh", WOLKENVRIEND = "wolkenvriend",
            HUISJESBOUWER = "huisjesbouwer", OPPER_VADSER = "opper_vadser", GUHKENNER = "guhkenner";

    /** Every title, in the order of the Guhdex tab (the two old ones first: see the class comment). */
    public static final List<Titel> ALLE = java.util.stream.Stream.concat(java.util.stream.Stream.of(
            new Titel(HELD_VAN_NOMGUH, "gui.guhs.balto.titel", ChatFormatting.AQUA, "guhs:baltoguh_beeldje", BaltoVerhaal::isHeld),
            new Titel(KNUFFELBURGEMEESTER, "gui.guhs.knuffeldal.titel", ChatFormatting.LIGHT_PURPLE, "guhs:knus_oorkonde",
                    Burgemeester::isKnuffelburgemeester),
            new Titel(VRIEND_VAN_GUHTWO, "gui.guhs.titels.naam." + VRIEND_VAN_GUHTWO, ChatFormatting.DARK_PURPLE, "guhs:mewtwo_labnotitie",
                    p -> MewtwoVoortgang.stap(p) >= MewtwoVoortgang.KLAAR),
            new Titel(OHANA_GUH, "gui.guhs.titels.naam." + OHANA_GUH, ChatFormatting.DARK_AQUA, "guhs:kokosnoot",
                    p -> Ohana.stap(p) >= Ohana.KLAAR),
            new Titel(WOLKENVRIEND, "gui.guhs.titels.naam." + WOLKENVRIEND, ChatFormatting.YELLOW, "guhs:pluisveertje", HemelQuest::klopt),
            new Titel(HUISJESBOUWER, "gui.guhs.titels.naam." + HUISJESBOUWER, ChatFormatting.GOLD, "guhs:timmerguh_bouwboekje",
                    p -> TimmerguhVoortgang.stap(p) >= TimmerguhVoortgang.KLAAR),
            new Titel(OPPER_VADSER, "gui.guhs.titels.naam." + OPPER_VADSER, ChatFormatting.RED, "guhs:knabbelkroon",
                    p -> GuhQuests.saved(p).getIntOr(GuheindeGevecht.WINS, 0) > 0),
            new Titel(GUHKENNER, "gui.guhs.titels.naam." + GUHKENNER, ChatFormatting.GREEN, "guhs:guhdex",
                    p -> GuhDex.vol(GuhWorldData.get(p.level().getServer()).player(p.getUUID()).seen))),
            nl.juiced.guhs.feature.guhpixel.GuhpixelTitels.ALLE.stream()).toList();   // (guhpixel: the titles of its slices come last)

    /** Server: the title each online player showed last (to notice changes; "" = none). */
    private static final Map<UUID, String> LAATST = new ConcurrentHashMap<>();
    /** Client: the titles of the players on the server (from guhs:titels_actief), for the names above their heads. */
    private static volatile Map<UUID, String> clientActief = Map.of();

    @Nullable
    public static Titel van(@Nullable String id) {
        for (Titel t : ALLE) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }

    public static boolean heeft(ServerPlayer p, Titel t) {
        return t.behaald().test(p);
    }

    /** The titles this player has earned, in the order of {@link #ALLE}. */
    public static List<Titel> behaald(ServerPlayer p) {
        return ALLE.stream().filter(t -> heeft(p, t)).toList();
    }

    /** What the player chose: a title id, {@link #GEEN}, or "" (never chosen). */
    public static String keuze(ServerPlayer p) {
        return GuhQuests.saved(p).getStringOr(KEUZE, "");
    }

    /**
     * The title this player shows (null: none): the chosen one when it is earned; nothing when they chose "geen titel";
     * (1.3.1) nothing either when the chosen title doesn't exist any more; else (never chosen, or the chosen one isn't theirs
     * any more) the first earned one.
     */
    @Nullable
    public static Titel actief(ServerPlayer p) {
        String keuze = keuze(p);
        if (GEEN.equals(keuze)) {
            return null;
        }
        Titel gekozen = van(keuze);
        if (gekozen != null && heeft(p, gekozen)) {
            return gekozen;
        }
        if (gekozen == null && !keuze.isEmpty()) {
            return null;   // (1.3.1) the chosen title doesn't exist any more (it was removed): no title, not some other one
        }
        for (Titel t : ALLE) {
            if (heeft(p, t)) {
                return t;
            }
        }
        return null;
    }

    /**
     * The player picks a title (an id of {@link #ALLE}) or none ({@link #GEEN} or ""). Only an earned title can be
     * picked. True = done; the names are refreshed.
     */
    public static boolean kies(ServerPlayer p, @Nullable String id) {
        if (id == null || id.isEmpty() || GEEN.equals(id)) {
            GuhQuests.saved(p).putString(KEUZE, GEEN);
        } else {
            Titel t = van(id);
            if (t == null || !heeft(p, t)) {
                return false;
            }
            GuhQuests.saved(p).putString(KEUZE, t.id());
        }
        ververs(p);
        return true;
    }

    /** A name with a title behind it: "Juiced ✿ Held van Nomguh" (the flower and the title in the title's colour). */
    public static Component metTitel(Component naam, Titel t) {
        return naam.copy().append(Component.literal(" ✿ ").withStyle(t.kleur())).append(t.naam().withStyle(t.kleur()));
    }

    // =====================================================================================================================
    // keeping the names up to date (server)
    // =====================================================================================================================

    /** Recomputes this player's names (player list, display name) and tells every client whose title is what. */
    public static void ververs(ServerPlayer p) {
        Titel t = actief(p);
        LAATST.put(p.getUUID(), t == null ? "" : t.id());
        p.refreshDisplayName();
        p.refreshTabListName();
        MinecraftServer server = p.level().getServer();
        if (server != null) {
            TitelsPayloads.Actief actief = actief(server);
            for (ServerPlayer ander : server.getPlayerList().getPlayers()) {
                nl.juiced.guhs.network.ModNetworking.sendTo(ander, actief);
            }
        }
    }

    /** The titles of every online player (for the clients: the names above the heads). */
    static TitelsPayloads.Actief actief(MinecraftServer server) {
        Map<UUID, String> out = new LinkedHashMap<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Titel t = actief(p);
            if (t != null) {
                out.put(p.getUUID(), t.id());
            }
        }
        return new TitelsPayloads.Actief(out);
    }

    /**
     * The regular check (TitelsEvents, every {@link TitelsEvents#CHECK_TICKS} ticks, and at login): a newly earned title is
     * announced to the player, and when the title that shows changed (earned, lost, chosen) the names are refreshed.
     */
    public static void kijk(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        List<Titel> behaald = behaald(p);
        if (!saved.contains(BEKEND)) {
            // the first check ever (a new player, or the update to 1.2.6): no list of messages for what was earned before
            saved.putString(BEKEND, String.join(",", ids(behaald)));
        } else if (!behaald.isEmpty()) {
            Set<String> bekend = new LinkedHashSet<>(Arrays.asList(saved.getStringOr(BEKEND, "").split(",")));
            bekend.remove("");
            boolean nieuw = false;
            for (Titel t : behaald) {
                if (bekend.add(t.id())) {
                    nieuw = true;
                    p.sendSystemMessage(Component.translatable("gui.guhs.titels.nieuw", t.naam().withStyle(t.kleur(), ChatFormatting.BOLD))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            if (nieuw) {
                saved.putString(BEKEND, String.join(",", bekend));
                TitelsPayloads.sync(p);
            }
        }
        Titel t = actief(p);
        if (!(t == null ? "" : t.id()).equals(LAATST.get(p.getUUID()))) {
            ververs(p);
        }
    }

    static void vergeet(ServerPlayer p) {
        LAATST.remove(p.getUUID());
    }

    // =====================================================================================================================
    // client: the titles of the other players
    // =====================================================================================================================

    /** (Client) the title of this player as the server told it, or null. */
    @Nullable
    public static Titel clientTitel(UUID speler) {
        return van(clientActief.get(speler));
    }

    /** (Client) the server's list of who shows which title. */
    public static void zetClientActief(Map<UUID, String> actief) {
        clientActief = Map.copyOf(actief);
    }

    /** The ids of these titles. */
    public static List<String> ids(List<Titel> titels) {
        List<String> out = new ArrayList<>();
        for (Titel t : titels) {
            out.add(t.id());
        }
        return out;
    }

    private Titels() {
    }
}
