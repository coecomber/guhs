package nl.juiced.guhs.feature.vadsrijk.voltooiing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPlekken;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Categorie;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Ding;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Staat;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.ZoneBoek;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2Haken;
import nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2NpcPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;

/**
 * The pages of zone 1 (De Knuffelzwammen: Knuffelkamp, Zwamzoet and the land around) and zone 2 (De Splinterpieken), plus the
 * helpers a later zone builds its own page with. Zone membership follows the id conventions: quests "z&lt;N&gt;_..." and pet
 * bosses "guhs:vr_z&lt;N&gt;_..." belong to zone N, everything else to zone 1.
 */
public final class ZoneBoeken {
    public static final String T = "vadsrijk.voltooiing.";
    public static final List<String> GEBIEDEN_Z1 = List.of("knuffelkamp", "zwamzoet", "haven", "klodderweide", "sporenwoud", "druipgrotten", "zwamtempel");
    public static final List<String> GEBIEDEN_Z2 = List.of("z2_kandijkom_dok", "z2_kandijkom", "z2_galmhellingen", "z2_knuffelmeer", "z2_splinterkloof",
            "z2_spiegelburcht");

    public static final ZoneBoek ZONE1 = new ZoneBoek("zone1", T + "zone.zone1", "vr_kenner_van_zwamzoet", 5000, ZoneBoeken::zone1);
    public static final ZoneBoek ZONE2 = new ZoneBoek("zone2", T + "zone.zone2", "vr_kenner_van_de_splinterpieken", 25000, ZoneBoeken::zone2);

    private ZoneBoeken() {
    }

    static void registreer() {
        Voltooiing.registreer(ZONE1);
        Voltooiing.registreer(ZONE2);
    }

    // ---- the two pages ----------------------------------------------------------------------------------------------------------

    static List<Categorie> zone1() {
        List<Categorie> l = new ArrayList<>();
        l.add(new Categorie("hoofd", T + "cat.hoofd", "minecraft:writable_book", quests(1, Quest.Soort.HOOFD)));
        l.add(new Categorie("zij", T + "cat.zij", "minecraft:paper", quests(1, Quest.Soort.ZIJ)));
        l.add(new Categorie("uitkijk", T + "cat.uitkijk", "minecraft:spyglass", uitkijken(WereldPlekken.UITKIJKEN, GEBIEDEN_Z1)));
        l.add(new Categorie("kampvuur", T + "cat.kampvuur", "minecraft:campfire", kampvuren(WereldPlekken.KAMPVUREN, GEBIEDEN_Z1)));
        List<Ding> geheim = new ArrayList<>();
        geheim.addAll(geheimen("aanwijzingen", NpcPlekken.AANWIJZING, "vadsrijk.npc.aanwijzing.titel.", GEBIEDEN_Z1));
        geheim.addAll(geheimen("schatten", NpcPlekken.SCHAT, "vadsrijk.npc.schat.naam.", GEBIEDEN_Z1));
        l.add(new Categorie("geheim", T + "cat.geheim", "minecraft:chest", geheim));
        l.add(new Categorie("knuffel", T + "cat.knuffel", "minecraft:pink_wool", geheimen("knuffels", NpcPlekken.KNUFFEL, "vadsrijk.npc.knuffel.", GEBIEDEN_Z1)));
        l.add(new Categorie("baas", T + "cat.baas", "guhs:vr_kussenzwaard", List.of(
                baas("klodderreus", "entity.guhs.vr_klodderreus", "knuffelkamp", false, "hq05_klodderreus", "guhs:vr_klodderreus"),
                baas("koning_klodder", "entity.guhs.vr_baas_koning_klodder", "klodderweide", false, "z17_koning", "guhs:vr_baas_koning_klodder"),
                baas("opperzwam", "entity.guhs.vr_baas_opperzwam", "zwamtempel", false, "hq11_opperzwam", "guhs:vr_baas_opperzwam"),
                baas("mika_spion", "entity.guhs.vr_baas_mika_spion", "zwamzoet", true, "geheim_spion", "guhs:vr_baas_mika_spion"))));
        l.add(new Categorie("huisdier", T + "cat.huisdier", "minecraft:lead", huisdieren(1)));
        // J-INT: the Hoedhupper/Klodderros are gone (i-mounts); zone 1's mounts are the return quests' Knuffelhert + Wolkenschaapje.
        // Mounts only exist from zone 3 on, so this is a "later" category: shown, never counted (never blocks 100 %).
        l.add(new Categorie("rijdier", T + "cat.rijdier", "minecraft:saddle",
                rijdieren(RijdierSoort.WOLKENSCHAAPJE, RijdierSoort.KNUFFELHERT), true));
        l.add(new Categorie("gebieden", T + "cat.gebieden", "minecraft:map", gebieden(GEBIEDEN_Z1)));
        return l;
    }

    static List<Categorie> zone2() {
        List<Categorie> l = new ArrayList<>();
        l.add(new Categorie("hoofd", T + "cat.hoofd", "minecraft:writable_book", quests(2, Quest.Soort.HOOFD)));
        l.add(new Categorie("zij", T + "cat.zij", "minecraft:paper", quests(2, Quest.Soort.ZIJ)));
        l.add(new Categorie("uitkijk", T + "cat.uitkijk", "minecraft:spyglass", uitkijken(Z2WereldPlekken.UITKIJKEN, GEBIEDEN_Z2)));
        l.add(new Categorie("kampvuur", T + "cat.kampvuur", "minecraft:campfire", kampvuren(Z2WereldPlekken.KAMPVUREN, GEBIEDEN_Z2)));
        List<Ding> geheim = new ArrayList<>();
        geheim.addAll(geheimen(Z2Haken.AANWIJZINGEN, Z2NpcPlekken.AANWIJZING, "vadsrijk.z2npc.aanwijzing.titel.", GEBIEDEN_Z2));
        geheim.addAll(geheimen(Z2Haken.SCHATTEN, Z2NpcPlekken.SCHAT, "vadsrijk.z2npc.schat.naam.", GEBIEDEN_Z2));
        l.add(new Categorie("geheim", T + "cat.geheim", "minecraft:chest", geheim));
        l.add(new Categorie("knuffel", T + "cat.knuffel", "minecraft:pink_wool",
                geheimen(Z2Haken.KNUFFELS, Z2NpcPlekken.KNUFFEL, "vadsrijk.z2npc.knuffel.", GEBIEDEN_Z2)));
        l.add(new Categorie("baas", T + "cat.baas", "guhs:vr_kussenzwaard", List.of(
                baas("galmwolf", "entity.guhs.vr_z2_baas_galmwolf", "z2_galmhellingen", false, "z2_hq05_galmwolf", "guhs:vr_z2_baas_galmwolf"),
                baas("spiegelzucht", "entity.guhs.vr_z2_baas_spiegelzucht", "z2_spiegelburcht", false, "z2_hq11_spiegelzucht", "guhs:vr_z2_baas_spiegelzucht",
                        "guhs:vr_z2_baas_spiegelbeeld"),
                baas("boormeester", "entity.guhs.vr_z2_baas_boormeester", "z2_splinterkloof", true, "z2_geheim_boormeester",
                        "guhs:vr_z2_baas_boormeester"))));
        l.add(new Categorie("huisdier", T + "cat.huisdier", "minecraft:lead", huisdieren(2)));
        l.add(new Categorie("rijdier", T + "cat.rijdier", "minecraft:saddle", rijdieren(RijdierSoort.GUHWOLF), true));   // (J-INT: later, see zone 1)
        l.add(new Categorie("gebieden", T + "cat.gebieden", "minecraft:map", gebieden(GEBIEDEN_Z2)));
        return l;
    }

    // ---- helpers (public: zone 3+ builds its page with these) --------------------------------------------------------------------

    /**
     * The zone of a quest id: "z2_..." .. "z9_..." = that zone, anything else = 1. (Single digit on purpose: zone 1's own side
     * quests are called "z01_..".."z20_..".)
     */
    public static int zoneVan(String id) {
        if (id.length() > 3 && id.charAt(0) == 'z' && id.charAt(1) >= '2' && id.charAt(1) <= '9' && id.charAt(2) == '_') {
            return id.charAt(1) - '0';
        }
        return 1;
    }

    /** The quests of one kind of a zone, in questlog order. Main quests are secret until started; side quests show once available. */
    public static List<Ding> quests(int zone, Quest.Soort soort) {
        List<Ding> l = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (q.soort() != soort || zoneVan(q.id()) != zone) {
                continue;
            }
            boolean hoofd = soort == Quest.Soort.HOOFD;
            String hint = hoofd ? T + "hint.verhaal" : T + "hint.quest";
            String hintArg = hoofd || q.gever().isEmpty() ? "" : "vadsrijk.npc.naam." + q.gever();
            l.add(new Ding(q.id(), q.titelKey(), "", hint, hintArg, true, p -> questStaat(p, q), null));
        }
        return l;
    }

    static Staat questStaat(ServerPlayer p, Quest q) {
        int s = Questlog.status(p, q.id());
        return s == Questlog.KLAAR ? Staat.KLAAR : s == Questlog.ACTIEF ? Staat.BEZIG : Staat.NIETS;
    }

    /** Lookout needles (kern {@code gezien "vr_w_uitkijk_<id>"}). */
    public static List<Ding> uitkijken(List<WereldPlekken.Uitkijk> lijst, List<String> gebieden) {
        List<Ding> l = new ArrayList<>();
        for (WereldPlekken.Uitkijk u : lijst) {
            String sleutel = "vr_w_uitkijk_" + u.id();
            l.add(new Ding(u.id(), "vadsrijk.wereld.uitkijk." + u.id(), "", T + "hint.uitkijk", gebiedNaam(u.top(), gebieden), true,
                    p -> Spelers.data(p).gezien.contains(sleutel) ? Staat.KLAAR : Staat.NIETS, u.top()));
        }
        return l;
    }

    /** Campfires: lit/used by this player ({@link Voltooiing#kampvuur}) or their checkpoint. Named by area + number. */
    public static List<Ding> kampvuren(List<BlockPos> lijst, List<String> gebieden) {
        List<Ding> l = new ArrayList<>();
        java.util.Map<String, Integer> per = new java.util.HashMap<>();
        List<BlockPos> gesorteerd = new ArrayList<>(lijst);
        // (by area in the page's area order, then north to south: a stable, readable list)
        gesorteerd.sort(java.util.Comparator.<BlockPos>comparingInt(b -> {
            String g = gebiedId(b, gebieden);
            int i = g == null ? -1 : gebieden.indexOf(g);
            return i < 0 ? 99 : i;
        }).thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getX));
        for (BlockPos b : gesorteerd) {
            String g = gebiedId(b, gebieden);
            int n = per.merge(g == null ? "" : g, 1, Integer::sum);
            String naam = g == null ? T + "kampvuur.los" : "vadsrijk.kern.gebied." + g;
            l.add(new Ding("vuur_" + Voltooiing.sleutel(b), T + "kampvuur.naam", naam + "#" + n, "", "", false,
                    p -> Voltooiing.vuurAan(p, b) ? Staat.KLAAR : Staat.NIETS, b));
        }
        return l;
    }

    /** Secrets found by walking up to them ({@code SpelerData.extra("npc").<map>}): knuffels, clues, treasures. */
    public static List<Ding> geheimen(String map, Map<String, BlockPos> plekken, String naamPrefix, List<String> gebieden) {
        List<Ding> l = new ArrayList<>();
        for (Map.Entry<String, BlockPos> e : plekken.entrySet()) {
            String id = e.getKey();
            BlockPos b = e.getValue();
            l.add(new Ding(map + "_" + id, naamPrefix + id, "", T + "hint.ergens", gebiedNaam(b, gebieden), true,
                    p -> Questlog.map(p, map).getBooleanOr(id, false) ? Staat.KLAAR : Staat.NIETS, b));
        }
        return l;
    }

    /** A boss: the kern first-kill key of any of its entity types, or its quest done (the fight that ends the quest). */
    public static Ding baas(String id, String naamKey, String gebied, boolean geheim, @Nullable String quest, String... types) {
        return new Ding("baas_" + id, naamKey, "", T + "hint.baas", "vadsrijk.kern.gebied." + gebied, geheim, p -> {
            for (String t : types) {
                if (Spelers.data(p).gezien.contains("baaskill:" + t)) {
                    return Staat.KLAAR;
                }
            }
            if (quest != null) {
                int s = Questlog.status(p, quest);
                return s == Questlog.KLAAR ? Staat.KLAAR : s == Questlog.ACTIEF ? Staat.BEZIG : Staat.NIETS;
            }
            return Staat.NIETS;
        }, null);
    }

    /** The pets whose boss belongs to this zone ("guhs:vr_z2_..." = zone 2). */
    public static List<Ding> huisdieren(int zone) {
        List<Ding> l = new ArrayList<>();
        for (Huisdier h : Huisdieren.alle()) {
            String pad = h.baas().contains(":") ? h.baas().substring(h.baas().indexOf(':') + 1) : h.baas();
            int z = pad.startsWith("vr_") ? zoneVan(pad.substring(3)) : 1;
            if (z == zone) {
                l.add(new Ding("huisdier_" + h.id(), h.naamKey(), "", h.bronKey(), "", false, p -> Huisdieren.heeft(p, h.id()) ? Staat.KLAAR : Staat.NIETS,
                        null));
            }
        }
        return l;
    }

    public static List<Ding> rijdieren(RijdierSoort... soorten) {
        List<Ding> l = new ArrayList<>();
        for (RijdierSoort s : soorten) {
            String id = s.name().toLowerCase(java.util.Locale.ROOT);
            l.add(new Ding("rijdier_" + id, "vadsrijk.economie.rijdier." + id, "", T + "hint.rijdier." + id, "", false,
                    p -> Rijdieren.heeft(p, s) ? Staat.KLAAR : Staat.NIETS, null));
        }
        return l;
    }

    /**
     * Areas: walked into ({@code Haken.GEBIED_BETREDEN}) - or, for saves from before the book, anything done inside the area
     * (a lit campfire, a lookout, a secret) counts as having been there.
     */
    public static List<Ding> gebieden(List<String> ids) {
        List<Ding> l = new ArrayList<>();
        for (String id : ids) {
            Gebieden.Gebied g = Gebieden.van(id);
            l.add(new Ding(id, "vadsrijk.kern.gebied." + id, "", "", "", false, p -> bezochtAfgeleid(p, id, ids) ? Staat.KLAAR : Staat.NIETS,
                    g == null ? BlockPos.ZERO : g.midden(64)));
        }
        return l;
    }

    static boolean bezochtAfgeleid(ServerPlayer p, String gebied, List<String> lijst) {
        if (Voltooiing.bezocht(p, gebied)) {
            return true;
        }
        for (ZoneBoek z : Voltooiing.zones()) {
            for (Categorie c : Voltooiing.categorieen(z)) {
                if (c.id().equals("gebieden")) {
                    continue;
                }
                for (Ding d : c.dingen()) {
                    if (d.plek() != null && gebied.equals(gebiedId(d.plek(), lijst)) && Voltooiing.staat(p, d) == Staat.KLAAR) {
                        Voltooiing.markeerGebied(p, gebied);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** The area (from this page's list) a spot is in; null = none of them (e.g. the ring between). */
    @Nullable
    static String gebiedId(BlockPos b, List<String> gebieden) {
        Gebieden.Gebied op = Gebieden.op(b.getX(), b.getY(), b.getZ());   // (K-INT: 3D, zone 3 has layered areas)
        if (op != null && gebieden.contains(op.id())) {
            return op.id();
        }
        for (String id : gebieden) {   // (a small dungeon/arena box in front: fall back to the big area around it)
            Gebieden.Gebied g = Gebieden.van(id);
            if (g != null && g.bevat(b.getX(), b.getY(), b.getZ())) {
                return id;
            }
        }
        return null;
    }

    /** The name key of the area a spot is in (for "Ergens in ..."), or the zone's catch-all name. */
    static String gebiedNaam(BlockPos b, List<String> gebieden) {
        String g = gebiedId(b, gebieden);
        return g != null ? "vadsrijk.kern.gebied." + g : gebieden == GEBIEDEN_Z2 ? "vadsrijk.kern.gebied.z2_ring"
                : !gebieden.isEmpty() && gebieden.get(0).startsWith("z3_") ? "vadsrijk.kern.gebied.z3_ring" : "vadsrijk.kern.gebied.ring";
    }
}
