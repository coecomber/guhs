package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import nl.juiced.guhs.feature.vadsrijk.gilden.Rang.Recht;
import nl.juiced.guhs.feature.vadsrijk.karakter.Karakter;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;

/**
 * U-GILDEN: everything that changes a bond's members (V1: "oprichten pas in zone 5; rekruteren/joinen vanaf zone 1").
 * Every action returns an {@link Uitslag} (ok + a Dutch line); permissions are checked here, so commands, the chat panel, the
 * NPCs and the tests all go through the same rules.
 * <ul>
 *   <li>{@link #maak}: at the Bondsklerk in Hoogvads (zone 5), costs {@link #PRIJS}, needs zone 4 done or level {@link #MIN_LEVEL}+;
 *   the founder becomes leider, the bond gets a plot in the Bondenvallei with a castle of level 1 (the tower).</li>
 *   <li>Joining (any level, any zone): an invite ({@link #nodigUit} -> {@link #accepteer}), or at a Bondenbord: {@link #meldAan}
 *   (an open bond takes you in at once, else an application an officer accepts with {@link #keurGoed}).</li>
 *   <li>New members start as rekruut; after {@link #AUTO_LID} bond xp contributed they become lid by themselves.</li>
 * </ul>
 */
public final class BondActies {
    public static long PRIJS = 10000;   // 1 goud
    public static final int MIN_LEVEL = 56, MAX_LEDEN = 50;
    public static final long AUTO_LID = 200, UITNODIGING_MS = 5 * 60_000L;
    /** The last main quest of zone 4 (its end = "zone 4 done"). */
    public static final String ZONE4_KLAAR = "z4_hq07_opstijgpier";
    static final Pattern NAAM = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 \\-]{1,18}[A-Za-z0-9]");
    static final Pattern TAG = Pattern.compile("[A-Za-z0-9]{2,4}");

    public record Uitslag(boolean ok, Component tekst) {
        static Uitslag ja(String key, Object... args) {
            return new Uitslag(true, Component.translatable(key, args).withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        static Uitslag nee(String key, Object... args) {
            return new Uitslag(false, Component.translatable(key, args).withStyle(ChatFormatting.RED));
        }
    }

    record Uitnodiging(int bond, String van, long tot) {
    }

    /** character key -> open invite */
    private static final Map<String, Uitnodiging> UITNODIGINGEN = new ConcurrentHashMap<>();

    private BondActies() {
    }

    static long nu() {
        return BondXp.KLOK.getAsLong();
    }

    static String karakterNaam(ServerPlayer p) {
        try {
            String n = Karakter.naam(p);
            return n == null ? "" : n;
        } catch (RuntimeException e) {
            return "";
        }
    }

    static Bond.Lid nieuwLid(ServerPlayer p, Rang r) {
        return new Bond.Lid(p.getGameProfile().name(), karakterNaam(p), r, nu());
    }

    /** May this player found a bond (level / zone 4)? */
    public static boolean magOprichten(ServerPlayer p) {
        return Spelers.data(p).level() >= MIN_LEVEL || NpcApi.questKlaar(p, ZONE4_KLAAR) || p.isCreative();
    }

    // ---- founding ------------------------------------------------------------------------------------------------------------

    /** Founds a bond. {@code bijKlerk}: the caller checked the player stands at the Bondsklerk (dev: true). */
    public static Uitslag maak(ServerPlayer p, String naam, String tag, boolean bijKlerk) {
        return maak(p, naam, tag, bijKlerk, false);
    }

    /** {@code dev}: no level rule, free. */
    public static Uitslag maak(ServerPlayer p, String naam, String tag, boolean bijKlerk, boolean dev) {
        MinecraftServer s = p.level().getServer();
        Bonden d = Bonden.get(s);
        naam = naam.trim().replaceAll(" +", " ");
        tag = tag.trim().toUpperCase(java.util.Locale.ROOT);
        if (!bijKlerk) {
            return Uitslag.nee("vadsrijk.gilden.fout.klerk");
        }
        if (Bonden.van(p) != null) {
            return Uitslag.nee("vadsrijk.gilden.fout.al_in_bond");
        }
        if (!dev && !magOprichten(p)) {
            return Uitslag.nee("vadsrijk.gilden.fout.te_vroeg", MIN_LEVEL);
        }
        if (!NAAM.matcher(naam).matches()) {
            return Uitslag.nee("vadsrijk.gilden.fout.naam");
        }
        if (!TAG.matcher(tag).matches()) {
            return Uitslag.nee("vadsrijk.gilden.fout.tag");
        }
        if (d.naamBezet(naam) || d.tagBezet(tag)) {
            return Uitslag.nee("vadsrijk.gilden.fout.bezet");
        }
        if (!dev && !p.isCreative() && !Spelers.neemMunten(p, PRIJS)) {
            return Uitslag.nee("vadsrijk.gilden.fout.munten", Spelers.munten(PRIJS));
        }
        Bond b = d.nieuw(naam, tag, nu());
        b.lagen.add(new Bond.Laag("bloem", DyeColor.WHITE));
        b.lagen.add(new Bond.Laag("rand", DyeColor.MAGENTA));
        d.zetLid(b, Bonden.sleutel(p), nieuwLid(p, Rang.LEIDER));
        UITNODIGINGEN.remove(Bonden.sleutel(p));
        BondXp.week(b);
        d.neemKavel(b);
        b.kasteel = 1;
        Kasteel.bouw(s, b);
        BondTags.ververs(s);
        s.getPlayerList().broadcastSystemMessage(Component.translatable("vadsrijk.gilden.opgericht.iedereen", p.getGameProfile().name(), b.naam, b.tag)
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return Uitslag.ja("vadsrijk.gilden.opgericht", b.naam);
    }

    // ---- invites ---------------------------------------------------------------------------------------------------------------

    public static Uitslag nodigUit(ServerPlayer van, ServerPlayer naar) {
        Bond b = Bonden.van(van);
        if (b == null) {
            return Uitslag.nee("vadsrijk.gilden.fout.geen_bond");
        }
        Bond.Lid l = b.lid(Bonden.sleutel(van));
        if (l == null || !l.rang.mag(Recht.UITNODIGEN)) {
            return Uitslag.nee("vadsrijk.gilden.fout.recht");
        }
        if (van == naar) {
            return Uitslag.nee("vadsrijk.gilden.fout.zelf");
        }
        if (Bonden.van(naar) != null) {
            return Uitslag.nee("vadsrijk.gilden.fout.ander_al", naar.getGameProfile().name());
        }
        if (b.leden.size() >= MAX_LEDEN) {
            return Uitslag.nee("vadsrijk.gilden.fout.vol", MAX_LEDEN);
        }
        UITNODIGINGEN.put(Bonden.sleutel(naar), new Uitnodiging(b.id, van.getGameProfile().name(), nu() + UITNODIGING_MS));
        BondPaneel.uitnodiging(naar, b, van.getGameProfile().name());
        return Uitslag.ja("vadsrijk.gilden.uitgenodigd", naar.getGameProfile().name());
    }

    @Nullable
    public static Bond uitnodigingVan(ServerPlayer p) {
        Uitnodiging u = UITNODIGINGEN.get(Bonden.sleutel(p));
        if (u == null || u.tot() < nu()) {
            return null;
        }
        return Bonden.get(p.level().getServer()).bond(u.bond());
    }

    public static Uitslag accepteer(ServerPlayer p) {
        Uitnodiging u = UITNODIGINGEN.remove(Bonden.sleutel(p));
        if (u == null || u.tot() < nu()) {
            return Uitslag.nee("vadsrijk.gilden.fout.geen_uitnodiging");
        }
        Bond b = Bonden.get(p.level().getServer()).bond(u.bond());
        if (b == null) {
            return Uitslag.nee("vadsrijk.gilden.fout.geen_uitnodiging");
        }
        return treedToe(p, b);
    }

    public static Uitslag weiger(ServerPlayer p) {
        return UITNODIGINGEN.remove(Bonden.sleutel(p)) == null ? Uitslag.nee("vadsrijk.gilden.fout.geen_uitnodiging")
                : Uitslag.ja("vadsrijk.gilden.geweigerd");
    }

    static Uitslag treedToe(ServerPlayer p, Bond b) {
        MinecraftServer s = p.level().getServer();
        if (Bonden.van(p) != null) {
            return Uitslag.nee("vadsrijk.gilden.fout.al_in_bond");
        }
        if (b.leden.size() >= MAX_LEDEN) {
            return Uitslag.nee("vadsrijk.gilden.fout.vol", MAX_LEDEN);
        }
        b.aanvragen.remove(Bonden.sleutel(p));
        Bonden.get(s).zetLid(b, Bonden.sleutel(p), nieuwLid(p, Rang.REKRUUT));
        BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.nieuw_lid", p.getGameProfile().name()).withStyle(ChatFormatting.AQUA));
        BondTags.ververs(s);
        return Uitslag.ja("vadsrijk.gilden.toegetreden", b.naam);
    }

    // ---- the board: applying -----------------------------------------------------------------------------------------------------

    /** Apply to (or, for an open bond, join) a recruiting bond. */
    public static Uitslag meldAan(ServerPlayer p, Bond b) {
        if (Bonden.van(p) != null) {
            return Uitslag.nee("vadsrijk.gilden.fout.al_in_bond");
        }
        if (!b.werft) {
            return Uitslag.nee("vadsrijk.gilden.fout.werft_niet");
        }
        int lv = Spelers.data(p).level();
        if (lv < b.minLevel) {
            return Uitslag.nee("vadsrijk.gilden.fout.min_level", b.minLevel);
        }
        if (b.vrij) {
            return treedToe(p, b);
        }
        String k = Bonden.sleutel(p);
        if (b.aanvragen.containsKey(k)) {
            return Uitslag.nee("vadsrijk.gilden.fout.al_aangemeld");
        }
        if (b.aanvragen.size() >= 30) {
            return Uitslag.nee("vadsrijk.gilden.fout.vol", 30);
        }
        String kar = karakterNaam(p);
        b.aanvragen.put(k, p.getGameProfile().name() + (kar.isEmpty() ? "" : " (" + kar + ")") + " lv " + lv);
        MinecraftServer s = p.level().getServer();
        Bonden.get(s).setDirty();
        for (ServerPlayer o : BondChat.online(s, b)) {
            Bond.Lid l = b.lid(Bonden.sleutel(o));
            if (l != null && l.rang.mag(Recht.WERVEN)) {
                BondPaneel.aanvraag(o, b, k, b.aanvragen.get(k));
            }
        }
        return Uitslag.ja("vadsrijk.gilden.aangemeld", b.naam);
    }

    public static Uitslag keurGoed(ServerPlayer officier, String sleutel, boolean ja) {
        Bond b = Bonden.van(officier);
        Bond.Lid l = b == null ? null : b.lid(Bonden.sleutel(officier));
        if (l == null || !l.rang.mag(Recht.WERVEN)) {
            return Uitslag.nee("vadsrijk.gilden.fout.recht");
        }
        String wie = b.aanvragen.remove(sleutel);
        MinecraftServer s = officier.level().getServer();
        Bonden.get(s).setDirty();
        if (wie == null) {
            return Uitslag.nee("vadsrijk.gilden.fout.geen_aanvraag");
        }
        if (!ja) {
            return Uitslag.ja("vadsrijk.gilden.afgewezen", wie);
        }
        Karakter.Sleutel ks = Karakter.Sleutel.van(sleutel);
        ServerPlayer p = ks == null ? null : s.getPlayerList().getPlayer(ks.speler());
        if (p != null && Bonden.sleutel(p).equals(sleutel)) {
            Uitslag u = treedToe(p, b);
            if (u.ok()) {
                p.sendSystemMessage(u.tekst());
            }
            return u.ok() ? Uitslag.ja("vadsrijk.gilden.goedgekeurd", wie) : u;
        }
        // (offline or playing another character: takes them in by key; the name stays what the application said)
        if (Bonden.get(s).vanSleutel(sleutel) != null) {
            return Uitslag.nee("vadsrijk.gilden.fout.ander_al", wie);
        }
        String naam = wie.split(" ")[0];
        Bonden.get(s).zetLid(b, sleutel, new Bond.Lid(naam, "", Rang.REKRUUT, nu()));
        BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.nieuw_lid", naam).withStyle(ChatFormatting.AQUA));
        return Uitslag.ja("vadsrijk.gilden.goedgekeurd", wie);
    }

    // ---- leaving, kicking, ranks ---------------------------------------------------------------------------------------------------

    public static Uitslag verlaat(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        Bond b = Bonden.van(p);
        if (b == null) {
            return Uitslag.nee("vadsrijk.gilden.fout.geen_bond");
        }
        String k = Bonden.sleutel(p);
        Bond.Lid l = b.lid(k);
        if (l != null && l.rang == Rang.LEIDER && b.leden.size() > 1) {
            return Uitslag.nee("vadsrijk.gilden.fout.leider_eerst");
        }
        if (b.leden.size() <= 1) {
            return hefOp(p);
        }
        Bonden.get(s).haalWeg(b, k);
        BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.vertrokken", p.getGameProfile().name()).withStyle(ChatFormatting.GRAY));
        BondTags.ververs(s);
        return Uitslag.ja("vadsrijk.gilden.verlaten", b.naam);
    }

    /** Find a member key by player name (or "name/slot"); null: none. */
    @Nullable
    public static String zoekLid(Bond b, String naam) {
        for (Map.Entry<String, Bond.Lid> e : b.leden.entrySet()) {
            if (e.getValue().naam.equalsIgnoreCase(naam) || e.getKey().equals(naam)
                    || (e.getValue().naam + "/" + e.getValue().karakter).equalsIgnoreCase(naam)) {
                return e.getKey();
            }
        }
        return null;
    }

    public static Uitslag verwijder(ServerPlayer door, String naam) {
        MinecraftServer s = door.level().getServer();
        Bond b = Bonden.van(door);
        Bond.Lid ik = b == null ? null : b.lid(Bonden.sleutel(door));
        if (ik == null || !ik.rang.mag(Recht.VERWIJDEREN)) {
            return Uitslag.nee("vadsrijk.gilden.fout.recht");
        }
        String k = zoekLid(b, naam);
        Bond.Lid hij = k == null ? null : b.lid(k);
        if (hij == null) {
            return Uitslag.nee("vadsrijk.gilden.fout.geen_lid", naam);
        }
        if (!ik.rang.boven(hij.rang)) {
            return Uitslag.nee("vadsrijk.gilden.fout.rang_hoog");
        }
        Bonden.get(s).haalWeg(b, k);
        BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.verwijderd", hij.naam, door.getGameProfile().name()).withStyle(ChatFormatting.GRAY));
        Karakter.Sleutel ks = Karakter.Sleutel.van(k);
        ServerPlayer p = ks == null ? null : s.getPlayerList().getPlayer(ks.speler());
        if (p != null && Bonden.sleutel(p).equals(k)) {
            p.sendSystemMessage(Component.translatable("vadsrijk.gilden.jij_verwijderd", b.naam).withStyle(ChatFormatting.RED));
        }
        BondTags.ververs(s);
        return Uitslag.ja("vadsrijk.gilden.verwijderd_ok", hij.naam);
    }

    /** Promote ({@code op} = true) or demote a member one step. */
    public static Uitslag rang(ServerPlayer door, String naam, boolean op) {
        MinecraftServer s = door.level().getServer();
        Bond b = Bonden.van(door);
        Bond.Lid ik = b == null ? null : b.lid(Bonden.sleutel(door));
        if (ik == null || !ik.rang.mag(Recht.BEVORDEREN)) {
            return Uitslag.nee("vadsrijk.gilden.fout.recht");
        }
        String k = zoekLid(b, naam);
        Bond.Lid hij = k == null ? null : b.lid(k);
        if (hij == null) {
            return Uitslag.nee("vadsrijk.gilden.fout.geen_lid", naam);
        }
        if (!ik.rang.boven(hij.rang)) {
            return Uitslag.nee("vadsrijk.gilden.fout.rang_hoog");
        }
        Rang nieuw = op ? hij.rang.hoger() : hij.rang.lager();
        if (nieuw == null || (op && !ik.rang.boven(nieuw))) {
            return Uitslag.nee("vadsrijk.gilden.fout.rang_grens");
        }
        hij.rang = nieuw;
        Bonden.get(s).setDirty();
        BondChat.meld(s, b, Component.translatable(op ? "vadsrijk.gilden.bevorderd" : "vadsrijk.gilden.gedegradeerd", hij.naam,
                Component.translatable(nieuw.naamKey())).withStyle(ChatFormatting.AQUA));
        return Uitslag.ja("vadsrijk.gilden.rang_ok", hij.naam, Component.translatable(nieuw.naamKey()));
    }

    public static Uitslag draagOver(ServerPlayer leider, String naam) {
        MinecraftServer s = leider.level().getServer();
        Bond b = Bonden.van(leider);
        Bond.Lid ik = b == null ? null : b.lid(Bonden.sleutel(leider));
        if (ik == null || !ik.rang.mag(Recht.OPHEFFEN)) {
            return Uitslag.nee("vadsrijk.gilden.fout.recht");
        }
        String k = zoekLid(b, naam);
        Bond.Lid hij = k == null ? null : b.lid(k);
        if (hij == null || hij == ik) {
            return Uitslag.nee("vadsrijk.gilden.fout.geen_lid", naam);
        }
        hij.rang = Rang.LEIDER;
        ik.rang = Rang.OFFICIER;
        Bonden.get(s).setDirty();
        BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.nieuwe_leider", hij.naam).withStyle(ChatFormatting.GOLD));
        return Uitslag.ja("vadsrijk.gilden.overgedragen", hij.naam);
    }

    /**
     * Disband: only the leader, only when the bank is empty (nothing is ever lost); the kas goes to the leader, the depot materials
     * back to the leader (materiaaltas / inventory), the plot is freed (the castle is cleared).
     */
    public static Uitslag hefOp(ServerPlayer leider) {
        MinecraftServer s = leider.level().getServer();
        Bond b = Bonden.van(leider);
        Bond.Lid ik = b == null ? null : b.lid(Bonden.sleutel(leider));
        if (ik == null || !ik.rang.mag(Recht.OPHEFFEN)) {
            return Uitslag.nee("vadsrijk.gilden.fout.recht");
        }
        if (!b.bankLeeg(s.registryAccess())) {
            return Uitslag.nee("vadsrijk.gilden.fout.bank_vol");
        }
        BondBank.sluitAlles(s, b);
        if (b.kas > 0) {
            Spelers.geefMunten(leider, b.kas);
            b.kas = 0;
        }
        Kasteel.geefDepotTerug(leider, b);
        Kasteel.ruimOp(s, b);
        BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.opgeheven", b.naam).withStyle(ChatFormatting.GRAY));
        Bonden.get(s).hefOp(b);
        BondTags.ververs(s);
        return Uitslag.ja("vadsrijk.gilden.opgeheven_ok", b.naam);
    }

    // ---- settings ------------------------------------------------------------------------------------------------------------------

    static Uitslag mag(ServerPlayer p, Recht r) {
        Bond b = Bonden.van(p);
        Bond.Lid l = b == null ? null : b.lid(Bonden.sleutel(p));
        if (l == null) {
            return Uitslag.nee("vadsrijk.gilden.fout.geen_bond");
        }
        return l.rang.mag(r) ? Uitslag.ja("") : Uitslag.nee("vadsrijk.gilden.fout.recht");
    }

    public static Uitslag werving(ServerPlayer p, @Nullable Boolean werft, @Nullable Boolean vrij, @Nullable Integer minLevel, @Nullable String tekst) {
        Uitslag m = mag(p, Recht.WERVEN);
        if (!m.ok()) {
            return m;
        }
        Bond b = Bonden.van(p);
        if (werft != null) {
            b.werft = werft;
        }
        if (vrij != null) {
            b.vrij = vrij;
        }
        if (minLevel != null) {
            b.minLevel = Math.max(1, Math.min(60, minLevel));
        }
        if (tekst != null) {
            String t = tekst.replaceAll("[^\\p{L}\\p{N} .,!?'\\-:()]", "").trim();
            b.werfTekst = t.length() > 60 ? t.substring(0, 60) : t;
        }
        Bonden.get(p.level().getServer()).setDirty();
        return Uitslag.ja("vadsrijk.gilden.werving_ok");
    }

    public static Uitslag banierBasis(ServerPlayer p, DyeColor c) {
        Uitslag m = mag(p, Recht.BANIER);
        if (!m.ok()) {
            return m;
        }
        Bond b = Bonden.van(p);
        b.basis = c;
        return banierKlaar(p, b);
    }

    /** Set layer i (0..2) to a pattern + colour; pattern "" removes the layer. */
    public static Uitslag banierLaag(ServerPlayer p, int i, String patroon, DyeColor c) {
        Uitslag m = mag(p, Recht.BANIER);
        if (!m.ok()) {
            return m;
        }
        Bond b = Bonden.van(p);
        if (patroon.isEmpty()) {
            if (i < b.lagen.size()) {
                b.lagen.remove(i);
            }
            return banierKlaar(p, b);
        }
        if (Banier.vanilla(patroon) == null || i < 0 || i >= Banier.MAX_LAGEN) {
            return Uitslag.nee("vadsrijk.gilden.fout.patroon");
        }
        Bond.Laag l = new Bond.Laag(patroon, c);
        if (i < b.lagen.size()) {
            b.lagen.set(i, l);
        } else {
            b.lagen.add(l);
        }
        return banierKlaar(p, b);
    }

    private static Uitslag banierKlaar(ServerPlayer p, Bond b) {
        MinecraftServer s = p.level().getServer();
        Bonden.get(s).setDirty();
        Kasteel.kleurBanieren(s, b);
        BondTags.ververs(s);
        return Uitslag.ja("vadsrijk.gilden.banier_ok");
    }

    /** Coins into the kas (anyone in the bond). */
    public static Uitslag kasGeef(ServerPlayer p, long brons) {
        Bond b = Bonden.van(p);
        if (b == null) {
            return Uitslag.nee("vadsrijk.gilden.fout.geen_bond");
        }
        if (brons <= 0 || !Spelers.neemMunten(p, brons)) {
            return Uitslag.nee("vadsrijk.gilden.fout.munten", Spelers.munten(Math.max(0, brons)));
        }
        b.kas += brons;
        MinecraftServer s = p.level().getServer();
        Bonden.get(s).setDirty();
        BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.kas_gift", p.getGameProfile().name(), Spelers.munten(brons)).withStyle(ChatFormatting.GOLD));
        return Uitslag.ja("vadsrijk.gilden.kas_ok", Spelers.munten(brons));
    }

    /** Dev/tests: forget the open invites. */
    static void wisUitnodigingen() {
        UITNODIGINGEN.clear();
    }
}
