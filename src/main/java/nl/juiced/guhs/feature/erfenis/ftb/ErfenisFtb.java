package nl.juiced.guhs.feature.erfenis.ftb;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.erfenis.Base;
import nl.juiced.guhs.feature.erfenis.Bouwkeet;
import nl.juiced.guhs.feature.erfenis.Erfenis;
import nl.juiced.guhs.feature.erfenis.ErfenisData;
import nl.juiced.guhs.feature.erfenis.Gebouw;
import nl.juiced.guhs.feature.erfenis.GebouwType;
import nl.juiced.guhs.feature.erfenis.Goed;
import nl.juiced.guhs.feature.erfenis.LeverkistPost;
import nl.juiced.guhs.feature.erfenis.kaart.BestelData;
import nl.juiced.guhs.feature.erfenis.kaart.Bestelling;
import nl.juiced.guhs.feature.erfenis.koppeling.Makelaarsbrief;
import nl.juiced.guhs.feature.erfenis.sherlock.SherlockData;
import nl.juiced.guhs.feature.erfenis.sherlock.Voortgang;
import nl.juiced.guhs.feature.erfenis.sherlock.Zaak;
import nl.juiced.guhs.feature.erfenis.sherlock.Zaken;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Ticks off the quests of the chapter "Een opknapper!" from the village state. Safe to call as often as you like: an
 * advancement that is done stays done, a statistic only goes up. What counts:
 * <ul>
 *   <li>buildings: the highest built level of a type ({@link Base#besteLevel}); the Hoeve, residents, plukjes, kavels and
 *   Knusheid are statistics;</li>
 *   <li>"made" (meel, brood, planken...): every rise of storage + building buffers of that good since the last look
 *   ({@link Teller}, saved per player: the village's own production, nothing else puts goods in);</li>
 *   <li>"sent home": the Leverkist allowance of the capped goods (it only drops when something goes home), plus post that is
 *   still waiting for a Leverkist;</li>
 *   <li>orders, the star order, a higher village, Handje helpen: kaart's {@link BestelData} (a delivered cart drives home for
 *   minutes, so a look every second sees it);</li>
 *   <li>Sherlock: {@link Voortgang#isOpgelost} per case.</li>
 * </ul>
 */
public final class ErfenisFtb {
    /** The custom statistics (same names as STATS in tools/features/erfenis_ftb.py). */
    public static final List<String> STATS = List.of("erfenis_plukjes", "erfenis_bewoners", "erfenis_bestellingen", "erfenis_hoeve",
            "erfenis_kavels", "erfenis_planken", "erfenis_baksteen", "erfenis_naar_huis", "erfenis_burenstempels", "erfenis_ansichtkaarten",
            "erfenis_knusheid");
    /** A building that is built (level >= 1) ticks off this quest. */
    static final Map<GebouwType, String> GEBOUWD = new EnumMap<>(GebouwType.class);
    /** A good that has been made at least once ticks off this quest. */
    static final Map<Goed, String> GEMAAKT = new EnumMap<>(Goed.class);
    /** The deco pieces a neighbour village sends after an order (kaart.Bestellingen.DECO + STER_DECO). */
    static final Set<String> BEDANKJES = Set.of("mika_schrik", "knabbelbessenstruik", "de_eerste_knabbel", "kaassausfonteintje", "mammoetje_van_heg")
            .stream().map(nl.juiced.guhs.feature.erfenis.ErfenisDeco::vorm).collect(java.util.stream.Collectors.toUnmodifiableSet());   // (stored under the vormen ids)
    /** What goes home and counts as "bouwspul" for the quest Bouwpakket (all capped). */
    static final List<Goed> BOUWSPUL = List.of(Goed.STAM, Goed.KLEI, Goed.ZAND, Goed.GLASSCHERF, Goed.BOUWBLOK);

    static {
        GEBOUWD.put(GebouwType.KIPPENHOK, "erfenis_kippenhok");
        GEBOUWD.put(GebouwType.TIMMERSCHUUR, "erfenis_timmer");
        GEBOUWD.put(GebouwType.SCHAAPSKOOI, "erfenis_schaap");
        GEBOUWD.put(GebouwType.KONIJNENREN, "erfenis_konijn");
        GEBOUWD.put(GebouwType.EENDENVIJVER, "erfenis_eend");
        GEBOUWD.put(GebouwType.KOEIENSTAL, "erfenis_koe");
        GEBOUWD.put(GebouwType.BIJENHOF, "erfenis_bij");
        GEBOUWD.put(GebouwType.MAMMOETWEI, "erfenis_mammoet");
        GEBOUWD.put(GebouwType.AKKER, "erfenis_akker");
        GEBOUWD.put(GebouwType.BOOMGAARD, "erfenis_boomgaard");
        GEBOUWD.put(GebouwType.GROEVE, "erfenis_groeve");
        GEBOUWD.put(GebouwType.SPEELTUIN, "erfenis_speeltuin");
        GEBOUWD.put(GebouwType.DORPSCAFE, "erfenis_cafe");
        GEBOUWD.put(GebouwType.MUZIEKTENT, "erfenis_feest");
        GEBOUWD.put(GebouwType.DETECTIVEBUREAU, "erfenis_detective");
        GEMAAKT.put(Goed.MEEL, "erfenis_molen");
        GEMAAKT.put(Goed.ROZEMEEL, "erfenis_molen");
        GEMAAKT.put(Goed.BROOD, "erfenis_brood");
        GEMAAKT.put(Goed.KOEK, "erfenis_koek");
        GEMAAKT.put(Goed.WORTELCAKE, "erfenis_koek");
        GEMAAKT.put(Goed.KNABBELKOEK, "erfenis_koek");
        GEMAAKT.put(Goed.STOF, "erfenis_stof");
        GEMAAKT.put(Goed.KNUFFELSTOF, "erfenis_stof");
        GEMAAKT.put(Goed.KAAS, "erfenis_kaas");
        GEMAAKT.put(Goed.KAARS, "erfenis_kaars");
        GEMAAKT.put(Goed.LANTAARN, "erfenis_kaars");
        GEMAAKT.put(Goed.REUZENWOL, "erfenis_reuzenwol");
    }

    private ErfenisFtb() {
    }

    static void tick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (s.getTickCount() % 20 != 11) {
            return;
        }
        for (ServerPlayer p : List.copyOf(s.getPlayerList().getPlayers())) {
            kijk(p);
        }
    }

    static void opLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            kijk(p);
        }
    }

    /** The client opened the overview ("overzicht") or the world map ("kaart"). */
    static void geopend(ServerPlayer p, String wat) {
        if (wat.equals("overzicht") || wat.equals("kaart")) {
            GuhAdvancements.grant(p, "erfenis_" + wat);
        }
    }

    /** Looks at everything of this player and ticks off what is done. */
    public static void kijk(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        if (s == null) {
            return;
        }
        UUID id = p.getUUID();
        if (Makelaarsbrief.gekocht(s, id)) {
            GuhAdvancements.grant(p, "erfenis_koop");
        }
        Base onder = Erfenis.onder(p);
        if (onder != null && !id.equals(onder.eigenaar)) {
            GuhAdvancements.grant(p, "erfenis_bezoek");
        }
        bestellingen(p, s, id);
        sherlock(p, s, id);
        if (LeverkistPost.wacht(s, id) > 0) {
            GuhAdvancements.grant(p, "erfenis_naar_huis");
        }
        Base b = ErfenisData.get(s).van(id);
        if (b == null || b.npc) {
            return;
        }
        // statistics
        zet(p, "erfenis_plukjes", b.opgeruimdTotaal());
        zet(p, "erfenis_bewoners", b.bewoners);
        zet(p, "erfenis_hoeve", b.hoeveLevel());
        zet(p, "erfenis_kavels", b.kavelsGekocht());
        zet(p, "erfenis_knusheid", b.knusheid());
        zet(p, "erfenis_bestellingen", b.bestellingen);
        zet(p, "erfenis_ansichtkaarten", (int) b.vondsten.stream().filter(v -> v.startsWith("ansichtkaart")).distinct().count());
        // buildings
        for (Map.Entry<GebouwType, String> e : GEBOUWD.entrySet()) {
            if (b.besteLevel(e.getKey()) >= 1) {
                GuhAdvancements.grant(p, e.getValue());
            }
        }
        if (b.besteLevel(GebouwType.BOSJE) >= 1 && b.besteLevel(GebouwType.HOUTZAGERIJ) >= 1) {
            GuhAdvancements.grant(p, "erfenis_bosje");
        }
        if (b.besteLevel(GebouwType.ZANDKUIL) >= 1 && b.besteLevel(GebouwType.GLASBLAZERIJ) >= 1) {
            GuhAdvancements.grant(p, "erfenis_glas");
        }
        if (b.besteLevel(GebouwType.SCHAAPSKOOI) >= 7 && b.hoeveLevel() >= 7) {   // (Productie: wolkenschaapjes from L7 with Hoeve 7)
            GuhAdvancements.grant(p, "erfenis_wolkenschaap");
        }
        for (Gebouw g : b.gebouwen) {
            if (g.type == GebouwType.KAARSENMAKERIJ && g.kleur > 0) {
                GuhAdvancements.grant(p, "erfenis_kleurglas");
            }
            if (g.type == GebouwType.BOUWBLOKKENWERKPLAATS && g.level >= 1
                    && (g.selectie.size() > 1 || g.selectie.size() == 1 && !g.selectie.get(0).equals(Bouwkeet.STANDAARD))) {
                GuhAdvancements.grant(p, "erfenis_werkplaats");
            }
        }
        // the link with home
        if (!b.loket.isEmpty()) {
            GuhAdvancements.grant(p, "erfenis_loket");
        }
        if (b.loket.keySet().stream().anyMatch(k -> k.endsWith("_wool") && !k.equals("minecraft:white_wool"))) {
            GuhAdvancements.grant(p, "erfenis_gekleurde_wol");
        }
        if (b.vkInDorp() >= 40) {
            GuhAdvancements.grant(p, "erfenis_vadsmast");
        }
        if (!b.loges.isEmpty()) {
            GuhAdvancements.grant(p, "erfenis_pension");
        }
        // deco
        if (!b.deco.isEmpty()) {
            GuhAdvancements.grant(p, "erfenis_deco");
        }
        if (b.deco.stream().anyMatch(d -> d.id().equals("spuitguhfontein"))) {
            GuhAdvancements.grant(p, "erfenis_fontein");
        }
        if (b.vondsten.stream().anyMatch(v -> v.startsWith("deco_") || v.equals("trekguh"))) {
            GuhAdvancements.grant(p, "erfenis_vondst");
        }
        if (b.decoOpslag.keySet().stream().anyMatch(BEDANKJES::contains) || b.deco.stream().anyMatch(d -> BEDANKJES.contains(d.id()))) {
            GuhAdvancements.grant(p, "erfenis_bedankje");
        }
        // made and sent home (counted since the last look)
        Teller t = Data.get(s).van(id);
        boolean veranderd = t.tel(b);
        for (Map.Entry<Goed, String> e : GEMAAKT.entrySet()) {
            if (t.gemaakt(e.getKey()) >= 1) {
                GuhAdvancements.grant(p, e.getValue());
            }
        }
        zet(p, "erfenis_planken", (int) t.gemaakt(Goed.PLANK));
        zet(p, "erfenis_baksteen", (int) t.gemaakt(Goed.BAKSTEEN));
        zet(p, "erfenis_naar_huis", (int) t.naarHuis);
        if (t.naarHuis >= 1) {
            GuhAdvancements.grant(p, "erfenis_naar_huis");
        }
        if (veranderd) {
            Data.get(s).setDirty();
        }
    }

    private static void bestellingen(ServerPlayer p, MinecraftServer s, UUID id) {
        BestelData.Speler bs = BestelData.get(s).bestaand(id);
        if (bs == null) {
            return;
        }
        zet(p, "erfenis_bestellingen", bs.geleverd);
        zet(p, "erfenis_burenstempels", bs.stempels);
        if (bs.stempels > 0 || !bs.geholpen.isEmpty()) {
            GuhAdvancements.grant(p, "erfenis_handje");
        }
        if (onderweg(bs.ster)) {
            GuhAdvancements.grant(p, "erfenis_sterbestelling");
        }
        for (Bestelling k : bs.karren) {
            if (onderweg(k) && k.relatie > 0) {
                GuhAdvancements.grant(p, "erfenis_hoger_dorp");
            }
        }
    }

    /** A cart that delivered and drives home (not after a refusal). */
    private static boolean onderweg(Bestelling k) {
        return k != null && k.fase == Bestelling.TERUG && !k.geweigerd;
    }

    private static void sherlock(ServerPlayer p, MinecraftServer s, UUID id) {
        Voortgang v = SherlockData.als(s, id);
        if (v == null) {
            return;
        }
        for (Zaak z : Zaken.ALLE) {
            if (v.isOpgelost(z.id())) {
                GuhAdvancements.grant(p, z == Zaken.RECHTSZAAK ? "erfenis_bezwaar" : "erfenis_zaak_" + z.id());
            }
        }
    }

    /** Sets a statistic, never lower than it was (an FTB stat task only needs to see the goal once). */
    static void zet(ServerPlayer p, String naam, int waarde) {
        Stat<?> stat = Stats.CUSTOM.get(ErfenisFtbFeature.STAT.get(naam).get());
        if (waarde > p.getStats().getValue(stat)) {
            p.getStats().setValue(p, stat, waarde);
        }
    }

    // =====================================================================================================================
    // the counters (saved per player: data/guhs_erfenis_ftb.dat)
    // =====================================================================================================================

    /** Per player: what was there at the last look, and what has been made / sent home since the counting began. */
    static final class Teller {
        final Map<Goed, Double> laatst = new EnumMap<>(Goed.class);
        final Map<Goed, Double> gemaakt = new EnumMap<>(Goed.class);
        final Map<Goed, Double> tegoed = new EnumMap<>(Goed.class);
        long tegoedTijd = Long.MIN_VALUE;
        double naarHuis;

        double gemaakt(Goed g) {
            return gemaakt.getOrDefault(g, 0.0);
        }

        /** Counts what changed since the last look. Returns true when something was remembered anew. */
        boolean tel(Base b) {
            boolean veranderd = false;
            for (Goed g : Goed.values()) {
                double nu = b.goed(g);
                for (Gebouw gb : b.gebouwen) {
                    nu += gb.inBuffer(g);
                }
                Double was = laatst.put(g, nu);
                if (was == null || Math.abs(was - nu) > 1e-9) {
                    veranderd = true;
                }
                if (was != null && nu > was + 1e-9) {
                    gemaakt.merge(g, nu - was, Double::sum);
                }
            }
            // the Leverkist allowance only drops when something goes home (Base.naarHuis), and it grows by the hour
            long tijd = b.leverBijgewerkt;
            if (tijd != tegoedTijd) {
                if (tegoedTijd != Long.MIN_VALUE && tijd > tegoedTijd) {
                    double uren = (tijd - tegoedTijd) / 3_600_000.0;
                    for (Goed g : BOUWSPUL) {
                        Double was = tegoed.get(g);
                        if (was != null && g.capPerUur > 0) {
                            double verwacht = Math.min(g.capPerUur * 8.0, was + g.capPerUur * uren);
                            double weg = verwacht - b.leverTegoed.getOrDefault(g, 0.0);
                            if (weg > 0.5) {
                                naarHuis += Math.round(weg);
                            }
                        }
                    }
                }
                tegoedTijd = tijd;
                veranderd = true;
            }
            for (Goed g : BOUWSPUL) {
                tegoed.put(g, b.leverTegoed.getOrDefault(g, 0.0));
            }
            return veranderd;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.put("Laatst", kaart(laatst));
            t.put("Gemaakt", kaart(gemaakt));
            t.put("Tegoed", kaart(tegoed));
            t.putLong("TegoedTijd", tegoedTijd);
            t.putDouble("NaarHuis", naarHuis);
            return t;
        }

        static Teller load(CompoundTag t) {
            Teller r = new Teller();
            lees(t.getCompoundOrEmpty("Laatst"), r.laatst);
            lees(t.getCompoundOrEmpty("Gemaakt"), r.gemaakt);
            lees(t.getCompoundOrEmpty("Tegoed"), r.tegoed);
            r.tegoedTijd = t.getLongOr("TegoedTijd", Long.MIN_VALUE);
            r.naarHuis = t.getDoubleOr("NaarHuis", 0);
            return r;
        }

        private static CompoundTag kaart(Map<Goed, Double> m) {
            CompoundTag t = new CompoundTag();
            m.forEach((g, v) -> t.putDouble(g.id, v));
            return t;
        }

        private static void lees(CompoundTag t, Map<Goed, Double> m) {
            for (Goed g : Goed.values()) {
                if (t.contains(g.id)) {
                    m.put(g, t.getDoubleOr(g.id, 0));
                }
            }
        }
    }

    static final class Data extends SavedData {
        static final SavedDataType<Data> TYPE = GuhSavedData.tagType("erfenis_ftb", Data::new, Data::load, Data::save);
        private final Map<UUID, Teller> spelers = new HashMap<>();

        static Data get(MinecraftServer server) {
            return GuhSavedData.get(server.overworld(), TYPE, "guhs_erfenis_ftb");
        }

        Teller van(UUID speler) {
            return spelers.computeIfAbsent(speler, k -> new Teller());
        }

        private static Data load(CompoundTag t) {
            Data d = new Data();
            CompoundTag s = t.getCompoundOrEmpty("Spelers");
            for (String k : s.keySet()) {
                try {
                    d.spelers.put(UUID.fromString(k), Teller.load(s.getCompoundOrEmpty(k)));
                } catch (IllegalArgumentException e) {
                    // (not a uuid: skip)
                }
            }
            return d;
        }

        private CompoundTag save() {
            CompoundTag t = new CompoundTag();
            CompoundTag s = new CompoundTag();
            spelers.forEach((id, v) -> s.put(id.toString(), v.save()));
            t.put("Spelers", s);
            return t;
        }
    }
}
