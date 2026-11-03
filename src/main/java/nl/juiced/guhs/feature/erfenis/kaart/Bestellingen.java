package nl.juiced.guhs.feature.erfenis.kaart;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.erfenis.ErfenisDeco;
import nl.juiced.guhs.feature.erfenis.ErfenisEconomie;
import nl.juiced.guhs.network.ModNetworking;

/**
 * De Erfenis: ORDERS from the neighbour villages (Hay Day style; ontwerp rounds 2 + 16, balans.md 3.4, 4, 5).
 * <ul>
 *   <li>A player's neighbours = the {@value #BUREN} NPC villages nearest to their base (for the first player: their own three;
 *   a later player: their own village and the two nearest others). Lower / equal / higher = that village's Hoeve level
 *   against the player's.</li>
 *   <li>The number of carts = the Marktplein level ({@link #karren}); every neighbour has 1-3 of them (the cart goes to the
 *   neighbour with the fewest). A cart: drives over the world map with an order ({@link Bestelling#HEEN}), waits at your
 *   village until you deliver or refuse (never expires), drives home, rests for the Marktplein's wait, comes again.</li>
 *   <li>Every day one gold star order (x4, a guaranteed special deco piece).</li>
 *   <li>Delivering takes the goods ({@code ErfenisEconomie.neem}) and pays Guhlden ({@code geef}), may give a deco piece
 *   (3/8/15%), and fires the moment {@code bestelling_geleverd:<totaal>}.</li>
 * </ul>
 * The rules are pure ({@link #bijwerken} on a {@link BestelData.Speler} with an {@link Omgeving}), so the gametests can drive
 * them; the rest is the server side (the screen: {@link #open}).
 */
public final class Bestellingen {
    public static final int BUREN = 3, MAX_PER_BUUR = 3;
    /** Carts per Marktplein level 0..10 (balans 3.4: 3/4/5/6/7 at balans L1..L5 = our L2/4/6/8/10; none yet: 2). */
    static final int[] KARREN = {2, 3, 3, 3, 4, 4, 5, 5, 6, 6, 7};
    /** Minutes before a cart that came home goes out again, per Marktplein level 0..10 (balans 3.4: 10/10/15/20/25). */
    static final int[] WACHT_MIN = {10, 10, 10, 10, 10, 12, 15, 17, 20, 22, 25};
    /** Travel minutes per tier (balans 4): {min, max}. */
    static final int[][] REIS_MIN = {{2, 2}, {2, 2}, {3, 4}, {4, 6}, {5, 7}, {6, 8}};
    static final long SNEL_MS = 10_000L;
    /** The special deco pieces of deco.md with source "Bestelling": id, from Hoeve level, only from a higher village. */
    record Deco(String id, int vanaf, boolean alleenHoger) {
    }
    static final List<Deco> DECO = List.of(new Deco("mika_schrik", 4, false), new Deco("knabbelbessenstruik", 5, false),
            new Deco("de_eerste_knabbel", 5, true), new Deco("kaassausfonteintje", 7, false));
    /** The star order's own piece from H9 (deco.md #46: only via the star order). */
    static final String STER_DECO = "mammoetje_van_heg";

    /** A neighbour village as an order needs it. */
    public record Buur(UUID id, String naam, int kleur, int hoeve) {
    }

    /** What the order rules need from the world (the server implements it with KaartData + the core; the tests with a fake). */
    public interface Omgeving {
        List<Buur> buren();

        int hoeve();

        int marktplein();

        /** Whether the player can already make this good (an order from a higher village may ask one of the next tier). */
        boolean kanMaken(BestelGoed g);
    }

    private Bestellingen() {
    }

    public static int karren(int marktplein) {
        return KARREN[Math.max(0, Math.min(10, marktplein))];
    }

    public static int tier(int hoeve) {
        return (Math.max(1, Math.min(10, hoeve)) + 1) / 2;
    }

    static long wachtMs(int marktplein, boolean snel) {
        return snel ? SNEL_MS : WACHT_MIN[Math.max(0, Math.min(10, marktplein))] * 60_000L;
    }

    static long reisMs(int tier, Random rng, boolean snel) {
        int[] r = REIS_MIN[Math.max(1, Math.min(5, tier))];
        return snel ? SNEL_MS : (r[0] * 60L + rng.nextInt((r[1] - r[0]) * 60 + 1)) * 1000L;
    }

    /** The real day (server time zone) of a moment: "once per day" for the star order and helping. */
    public static long dag(long nu) {
        return LocalDate.ofInstant(Instant.ofEpochMilli(nu), ZoneId.systemDefault()).toEpochDay();
    }

    // =====================================================================================================================
    // the rules
    // =====================================================================================================================

    /**
     * Brings one player's carts up to date (also after hours offline: every phase that ended is passed, in order). The first
     * time (no order ever) the carts are there at once, so a new player (the tutorial) has an order straight away.
     * True: something changed.
     */
    public static boolean bijwerken(BestelData.Speler s, Omgeving o, long seed, UUID speler, long nu, boolean snel) {
        boolean veranderd = false;
        List<Buur> buren = o.buren();
        int doel = karren(o.marktplein());
        boolean eersteKeer = s.teller == 0;
        while (s.karren.size() < doel) {
            Bestelling b = new Bestelling();
            b.fase = Bestelling.WACHT;
            b.start = nu;
            b.eind = nu;
            s.karren.add(b);
            veranderd = true;
        }
        for (int i = s.karren.size() - 1; i >= 0 && s.karren.size() > doel; i--) {   // (a smaller Marktplein: a reset base)
            if (s.karren.get(i).fase == Bestelling.WACHT) {
                s.karren.remove(i);
                veranderd = true;
            }
        }
        for (Bestelling b : s.karren) {
            veranderd |= stap(s, b, o, buren, seed, speler, nu, snel, eersteKeer);
        }
        // the daily star order
        long vandaag = dag(nu);
        if (s.ster != null) {
            veranderd |= stap(s, s.ster, o, buren, seed, speler, nu, snel, eersteKeer);
            if (s.ster.fase == Bestelling.WACHT && !s.ster.geweigerd && s.ster.eind == Long.MIN_VALUE) {
                s.ster = null;   // (delivered and home again: the next one comes tomorrow)
                s.sterDag = vandaag;
            }
        }
        if (s.ster == null && s.sterDag != vandaag && !buren.isEmpty()) {
            Bestelling b = new Bestelling();
            b.ster = true;
            b.fase = Bestelling.WACHT;
            b.start = nu;
            b.eind = nu;
            s.ster = b;
            s.sterDag = vandaag;
            veranderd = true;
            stap(s, b, o, buren, seed, speler, nu, snel, eersteKeer);
        }
        return veranderd;
    }

    /** Passes every phase of this cart that has ended. */
    private static boolean stap(BestelData.Speler s, Bestelling b, Omgeving o, List<Buur> buren, long seed, UUID speler, long nu,
            boolean snel, boolean meteen) {
        boolean veranderd = false;
        for (int veilig = 0; veilig < 20 && b.fase != Bestelling.OPEN && nu >= b.eind; veilig++) {
            veranderd = true;
            switch (b.fase) {
                case Bestelling.HEEN -> b.fase = Bestelling.OPEN;
                case Bestelling.TERUG -> {
                    if (b.ster && !b.geweigerd) {   // a delivered star order: done for today
                        b.fase = Bestelling.WACHT;
                        b.eind = Long.MIN_VALUE;
                        return true;
                    }
                    b.fase = Bestelling.WACHT;
                    b.start = b.eind;
                    b.eind = b.start + wachtMs(o.marktplein(), snel);
                }
                case Bestelling.WACHT -> {
                    Buur buur = kiesBuur(s, b, buren, new Random(seed ^ speler.getLeastSignificantBits() ^ s.teller * 0x9E3779B97F4A7C15L));
                    if (buur == null) {   // no neighbour (yet) or all full: try again in a minute
                        b.start = nu;
                        b.eind = nu + 60_000L;
                        return veranderd;
                    }
                    Random rng = new Random(seed * 31 + speler.getMostSignificantBits() ^ (s.teller++) * 0x2545F4914F6CDD1DL);
                    int tier = tier(o.hoeve());
                    b.maak(rng, buur.id(), tier, Integer.signum(buur.hoeve() - o.hoeve()), b.ster, o::kanMaken);
                    if (b.ster) {
                        b.deco = sterDeco(o.hoeve(), rng);
                    }
                    long van = Math.max(b.eind, nu - 24 * 3_600_000L);
                    if (meteen) {   // the very first carts are there at once
                        b.fase = Bestelling.OPEN;
                        b.start = nu;
                        b.eind = nu;
                    } else {
                        b.fase = Bestelling.HEEN;
                        b.start = van;
                        b.eind = van + reisMs(tier, rng, snel);
                    }
                }
                default -> {
                    return veranderd;
                }
            }
        }
        return veranderd;
    }

    /** The neighbour with the fewest carts (ties at random), at most {@value #MAX_PER_BUUR} each; null: none. */
    @Nullable
    static Buur kiesBuur(BestelData.Speler s, Bestelling zelf, List<Buur> buren, Random rng) {
        List<Buur> beste = new ArrayList<>();
        int minst = Integer.MAX_VALUE;
        for (Buur b : buren) {
            int n = 0;
            for (Bestelling k : s.karren) {
                n += k != zelf && k.fase != Bestelling.WACHT && k.dorp.equals(b.id()) ? 1 : 0;
            }
            if (n >= MAX_PER_BUUR) {
                continue;
            }
            if (n < minst) {
                minst = n;
                beste.clear();
            }
            if (n == minst) {
                beste.add(b);
            }
        }
        return beste.isEmpty() ? null : beste.get(rng.nextInt(beste.size()));
    }

    /** The star order's guaranteed piece: from what the player's Hoeve level allows (below H4 the Mika-schrik anyway). */
    static String sterDeco(int hoeve, Random rng) {
        List<String> l = new ArrayList<>();
        for (Deco d : DECO) {
            if (hoeve >= d.vanaf()) {
                l.add(d.id());
            }
        }
        if (hoeve >= 9) {
            l.add(STER_DECO);
            l.add(STER_DECO);   // (its own piece a bit more often)
        }
        return l.isEmpty() ? DECO.get(0).id() : l.get(rng.nextInt(l.size()));
    }

    /** The deco piece a normal order gives on delivery (by its chance), or null. */
    @Nullable
    static String rolDeco(Bestelling b, int hoeve, Random rng) {
        if (b.ster) {
            return b.deco.isEmpty() ? null : b.deco;
        }
        if (rng.nextInt(100) >= b.decoKans()) {
            return null;
        }
        List<String> l = new ArrayList<>();
        for (Deco d : DECO) {
            if (hoeve >= d.vanaf() && (!d.alleenHoger() || b.relatie > 0)) {
                l.add(d.id());
            }
        }
        return l.isEmpty() ? null : l.get(rng.nextInt(l.size()));
    }

    /** The cart in slot {@code slot} (-1 = the star cart), or null. */
    @Nullable
    public static Bestelling kar(BestelData.Speler s, int slot) {
        return slot == -1 ? s.ster : slot >= 0 && slot < s.karren.size() ? s.karren.get(slot) : null;
    }

    /** Refuse: the cart drives home empty and comes back after the normal wait with something else. */
    public static boolean weiger(BestelData.Speler s, int slot, Omgeving o, long nu, boolean snel) {
        Bestelling b = kar(s, slot);
        if (b == null || b.fase != Bestelling.OPEN) {
            return false;
        }
        b.geweigerd = true;
        b.fase = Bestelling.TERUG;
        b.start = nu;
        b.eind = nu + reisMs(tier(o.hoeve()), new Random(nu), snel);
        return true;
    }

    /**
     * Gives the first open (or arriving) order without help +20% ("Handje helpen"). False: no such order.
     */
    public static boolean geefHulp(BestelData.Speler s, String helper) {
        List<Bestelling> l = new ArrayList<>(s.karren);
        if (s.ster != null) {
            l.add(s.ster);
        }
        l.sort(Comparator.comparingInt((Bestelling b) -> b.fase == Bestelling.OPEN ? 0 : 1).thenComparingLong(b -> b.eind));
        for (Bestelling b : l) {
            if ((b.fase == Bestelling.OPEN || b.fase == Bestelling.HEEN) && b.hulp.isEmpty()) {
                b.hulp = helper;
                return true;
            }
        }
        return false;
    }

    // =====================================================================================================================
    // the server side
    // =====================================================================================================================

    /** The server's {@link Omgeving} of a player. */
    static Omgeving omgeving(MinecraftServer server, UUID speler) {
        KaartBron bron = KaartFeature.BRON;
        return new Omgeving() {
            private List<Buur> buren;

            @Override
            public List<Buur> buren() {
                if (buren == null) {
                    buren = Bestellingen.buren(server, speler);
                }
                return buren;
            }

            @Override
            public int hoeve() {
                return bron.hoeveLevel(server, speler);
            }

            @Override
            public int marktplein() {
                return bron.marktpleinLevel(server, speler);
            }

            @Override
            public boolean kanMaken(BestelGoed g) {
                return ErfenisEconomie.heeft(server, speler, g.id) > 0;
            }
        };
    }

    /** The {@value #BUREN} NPC villages nearest to the player's base. */
    public static List<Buur> buren(MinecraftServer server, UUID speler) {
        KaartDorp eigen = null;
        for (KaartDorp d : KaartFeature.BRON.spelerDorpen(server, System.currentTimeMillis())) {
            if (d.id().equals(speler)) {
                eigen = d;
            }
        }
        if (eigen == null) {
            return List.of();
        }
        int cx = eigen.cx(), cz = eigen.cz();
        List<NpcDorp> l = new ArrayList<>(KaartData.get(server).npcs());
        l.sort(Comparator.comparingDouble((NpcDorp d) -> (double) (d.cx - cx) * (d.cx - cx) + (double) (d.cz - cz) * (d.cz - cz))
                .thenComparing(d -> d.id));
        List<Buur> buren = new ArrayList<>();
        for (int i = 0; i < Math.min(BUREN, l.size()); i++) {
            NpcDorp d = l.get(i);
            buren.add(new Buur(d.id, d.naam, d.kleur, d.hoeveLevel()));
        }
        return buren;
    }

    /** Brings this player's carts up to date (server). */
    public static BestelData.Speler bij(MinecraftServer server, UUID speler) {
        BestelData data = BestelData.get(server);
        BestelData.Speler s = data.van(speler);
        if (data.snel) {   // (dev: carts that were already on a long trip arrive soon too)
            long max = System.currentTimeMillis() + SNEL_MS;
            for (Bestelling b : s.ster == null ? s.karren : java.util.stream.Stream.concat(s.karren.stream(), java.util.stream.Stream.of(s.ster)).toList()) {
                if (b.fase != Bestelling.OPEN && b.eind > max) {
                    b.eind = max;
                }
            }
        }
        if (bijwerken(s, omgeving(server, speler), KaartData.get(server).seed, speler, System.currentTimeMillis(), data.snel)) {
            data.setDirty();
        }
        return s;
    }

    /** Every second: the carts of everybody online (offline players catch up when they come). */
    static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (KaartFeature.BRON.hoeveLevel(server, p.getUUID()) >= 1 && heeftBase(server, p.getUUID())) {
                bij(server, p.getUUID());
            }
        }
    }

    private static boolean heeftBase(MinecraftServer server, UUID speler) {
        for (KaartDorp d : KaartFeature.BRON.spelerDorpen(server, System.currentTimeMillis())) {
            if (d.id().equals(speler)) {
                return true;
            }
        }
        return false;
    }

    /** Opens the orders screen for this player (from the map, or kern's Marktplein button). */
    public static void open(ServerPlayer p) {
        stuur(p, true);
    }

    /** Sends the orders screen's data ({@code open}: open it; false: refresh it if it is open). */
    public static void stuur(ServerPlayer p, boolean open) {
        MinecraftServer server = p.level().getServer();
        KaartFeature.BRON.eigenDorp(p);   // (a player without a base gets one: the map does the same)
        BestelData.Speler s = bij(server, p.getUUID());
        ModNetworking.sendTo(p, new KaartPayloads.BestelLijst(lijst(server, p.getUUID(), s), open));
    }

    /** How many orders this player delivered in total (Hoeve goals, FTB). */
    public static int geleverd(MinecraftServer server, UUID speler) {
        BestelData.Speler s = BestelData.get(server).bestaand(speler);
        return s == null ? 0 : s.geleverd;
    }

    /** The orders screen's data. */
    static CompoundTag lijst(MinecraftServer server, UUID speler, BestelData.Speler s) {
        CompoundTag t = new CompoundTag();
        t.putLong("Nu", System.currentTimeMillis());
        int markt = KaartFeature.BRON.marktpleinLevel(server, speler);
        t.putInt("Marktplein", markt);
        t.putInt("Geleverd", s.geleverd);
        t.putInt("Stempels", s.stempels);
        ListTag l = new ListTag();
        if (s.ster != null && s.ster.fase != Bestelling.WACHT) {
            l.add(kaart(server, speler, s.ster, -1));
        }
        for (int i = 0; i < s.karren.size(); i++) {
            l.add(kaart(server, speler, s.karren.get(i), i));
        }
        t.put("Karren", l);
        return t;
    }

    private static CompoundTag kaart(MinecraftServer server, UUID speler, Bestelling b, int slot) {
        CompoundTag c = b.save();
        c.putInt("Slot", slot);
        NpcDorp d = KaartData.get(server).npc(b.dorp);
        c.putString("DorpNaam", d == null ? "?" : d.naam);
        c.putInt("Kleur", d == null ? 0xFFF29BB8 : d.kleur);
        c.putInt("Uitbetaling", b.uitbetaling());
        c.putInt("DecoKans", b.decoKans());
        CompoundTag heeft = new CompoundTag();
        for (Bestelling.Regel r : b.regels) {
            heeft.putInt(r.goed(), ErfenisEconomie.heeft(server, speler, r.goed()));
        }
        c.put("Heeft", heeft);
        return c;
    }

    /** The map's per-viewer extras: the viewer's carts, neighbours, and who they helped today. */
    static CompoundTag kaartExtra(MinecraftServer server, UUID speler) {
        CompoundTag t = new CompoundTag();
        BestelData data = BestelData.get(server);
        BestelData.Speler s = bij(server, speler);
        ListTag k = new ListTag();
        List<Bestelling> alle = new ArrayList<>(s.karren);
        if (s.ster != null) {
            alle.add(s.ster);
        }
        for (Bestelling b : alle) {
            if (b.fase == Bestelling.WACHT) {
                continue;
            }
            CompoundTag c = new CompoundTag();
            c.store("Dorp", UUIDUtil.CODEC, b.dorp);
            c.putInt("Fase", b.fase);
            c.putLong("Start", b.start);
            c.putLong("Eind", b.eind);
            c.putBoolean("Ster", b.ster);
            k.add(c);
        }
        t.put("Karren", k);
        ListTag buren = new ListTag();
        for (Buur b : buren(server, speler)) {
            CompoundTag c = new CompoundTag();
            c.store("Id", UUIDUtil.CODEC, b.id());
            buren.add(c);
        }
        t.put("Buren", buren);
        Handje.vandaag(s, System.currentTimeMillis());
        ListTag g = new ListTag();
        for (UUID u : s.geholpen) {
            CompoundTag c = new CompoundTag();
            c.store("Id", UUIDUtil.CODEC, u);
            g.add(c);
        }
        t.put("Geholpen", g);
        t.putInt("HulpOver", Math.max(0, Handje.BELOOND_PER_DAG - s.hulpBeloond));
        data.setDirty();
        return t;
    }

    /** Deliver the order in this slot: goods out, Guhlden (and maybe a deco piece) in. */
    static void lever(ServerPlayer p, int slot) {
        MinecraftServer server = p.level().getServer();
        UUID id = p.getUUID();
        BestelData data = BestelData.get(server);
        BestelData.Speler s = bij(server, id);
        Bestelling b = kar(s, slot);
        if (b == null || b.fase != Bestelling.OPEN) {
            p.sendSystemMessage(Component.translatable("erfenis.kaart.bestel.niet_open"));
            stuur(p, false);
            return;
        }
        for (Bestelling.Regel r : b.regels) {
            int heeft = ErfenisEconomie.heeft(server, id, r.goed());
            if (heeft < r.n()) {
                p.sendSystemMessage(Component.translatable("erfenis.kaart.bestel.tekort", r.n() - heeft,
                        Component.translatable(BestelGoed.van(r.goed()) == null ? r.goed() : BestelGoed.van(r.goed()).naamKey())));
                stuur(p, false);
                return;
            }
        }
        for (Bestelling.Regel r : b.regels) {
            ErfenisEconomie.neem(server, id, r.goed(), r.n());
        }
        int geld = b.uitbetaling();
        ErfenisEconomie.geef(server, id, "guhlden", geld);
        int hoeve = KaartFeature.BRON.hoeveLevel(server, id);
        String deco = rolDeco(b, hoeve, new Random(System.nanoTime()));
        if (deco != null) {
            ErfenisDeco.geef(server, id, deco);
        }
        s.geleverd++;
        NpcDorp d = KaartData.get(server).npc(b.dorp);
        String naam = d == null ? "?" : d.naam;
        b.fase = Bestelling.TERUG;
        b.geweigerd = false;
        b.start = System.currentTimeMillis();
        b.eind = b.start + reisMs(tier(hoeve), new Random(b.start), data.snel);
        data.setDirty();
        p.sendSystemMessage(Component.translatable(b.ster ? "erfenis.kaart.bestel.geleverd_ster" : "erfenis.kaart.bestel.geleverd", naam, geld));
        if (deco != null) {
            p.sendSystemMessage(Component.translatable("erfenis.kaart.bestel.cadeau", naam, Component.translatable("erfenis.kaart.deco." + deco)));
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.PLAYERS, 0.8f, 1.3f);
        ErfenisEconomie.bestellingGeleverd(server, id);   // kern counts (Hoeve goals) and fires bestelling_geleverd:<totaal>
        stuur(p, false);
    }

    static void weiger(ServerPlayer p, int slot) {
        MinecraftServer server = p.level().getServer();
        BestelData data = BestelData.get(server);
        BestelData.Speler s = bij(server, p.getUUID());
        if (weiger(s, slot, omgeving(server, p.getUUID()), System.currentTimeMillis(), data.snel)) {
            data.setDirty();
            p.sendSystemMessage(Component.translatable("erfenis.kaart.bestel.geweigerd"));
        }
        stuur(p, false);
    }
}
