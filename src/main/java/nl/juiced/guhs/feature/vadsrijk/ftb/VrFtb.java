package nl.juiced.guhs.feature.vadsrijk.ftb;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Y-FTB: the tasks of the compact FTB chapter "Het Vadsrijk" (tools/features/vadsrijk_y_ftb.py). Every task is a hidden advancement
 * {@code guhs:quest/vr_ftb_<x>}; this class grants them. An advancement belongs to the Minecraft player, not to a character, so
 * whatever character or game mode (Normaal, Knuffelmodus, Eénvads) did it, it counts once and stays (a retired Eénvads knight
 * keeps his trophies).
 * <ul>
 *   <li>{@code vr_ftb_rekruut}: the Knuffelkamp training is done ({@link #REKRUUT_QUEST}).</li>
 *   <li>{@code vr_ftb_zone<N>}: the last main quest of zone N is handed in ({@link #eindQuest}).</li>
 *   <li>{@code vr_ftb_raid}: the first clear of De Sterrendovende Toren (raid hook, or the raid's own "clear" flag).</li>
 *   <li>{@code vr_ftb_geheimen<N>}: every secret, lost knuffel and puzzle on the zone's Verkenningsboek page ({@link #GEHEIM_CATS}).</li>
 *   <li>{@code vr_ftb_poort<N>} (N = 2..6): the server opened zone N (zone gate). The chapter's zone-N quests hang behind it in FTB
 *       Quests (hide_until_deps_complete), so they only appear once the zone is open.</li>
 * </ul>
 * Checked on every hand-in (NpcApi.QUEST_KLAAR), on the raid clear, at login and every {@link #INTERVAL} ticks (secrets, the gate,
 * and anything a dev command or an older save already did).
 */
public final class VrFtb {
    public static final String REKRUUT_QUEST = "hq05_klodderreus";
    /** The categories of a zone page that count as "geheimpjes". */
    public static final Set<String> GEHEIM_CATS = Set.of("geheim", "knuffel", "puzzel");
    public static final int INTERVAL = 200;
    /** Gametests: how many advancements were granted by this class (new ones only). */
    public static int verleend;

    private VrFtb() {
    }

    // ---- the triggers -----------------------------------------------------------------------------------------------------------

    /** The last main quest of a zone (the main quest of that zone no other main quest of the zone comes after), or null. */
    @Nullable
    public static String eindQuest(int zone) {
        List<Quest> hoofd = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (q.hoofd() && ZoneBoeken.zoneVan(q.id()) == zone) {
                hoofd.add(q);
            }
        }
        String eind = null;
        for (Quest q : hoofd) {
            boolean later = false;
            for (Quest o : hoofd) {
                if (o != q && o.na().contains(q.id())) {
                    later = true;
                    break;
                }
            }
            if (!later) {
                eind = q.id();   // (the chains are linear: exactly one; on a tie the last one in questlog order)
            }
        }
        return eind;
    }

    public static boolean rekruut(ServerPlayer p) {
        return Questlog.klaar(p, REKRUUT_QUEST);
    }

    public static boolean zoneKlaar(ServerPlayer p, int zone) {
        String eind = eindQuest(zone);
        return eind != null && Questlog.klaar(p, eind);
    }

    public static boolean raidKlaar(ServerPlayer p) {
        return Spelers.data(p).extra("z6raid").getBooleanOr("clear", false);
    }

    /** {found, total} of the secret categories of zone N's Verkenningsboek page ({0, 0} when that page is not there). */
    public static int[] geheimen(ServerPlayer p, int zone) {
        Voltooiing.ZoneBoek z = Voltooiing.zone("zone" + zone);
        int k = 0, t = 0;
        if (z != null) {
            for (Voltooiing.Categorie c : Voltooiing.categorieen(z)) {
                if (!c.later() && GEHEIM_CATS.contains(c.id())) {
                    Voltooiing.Telling tel = Voltooiing.tel(p, c);
                    k += tel.klaar();
                    t += tel.totaal();
                }
            }
        }
        return new int[] {k, t};
    }

    public static boolean geheimenKlaar(ServerPlayer p, int zone) {
        int[] g = geheimen(p, zone);
        return g[1] > 0 && g[0] >= g[1];
    }

    // ---- granting ---------------------------------------------------------------------------------------------------------------

    /** Grants every vr_ftb advancement this player has earned; returns how many were new. */
    public static int controleer(ServerPlayer p) {
        int n = 0;
        if (rekruut(p)) {
            n += geef(p, "vr_ftb_rekruut");
        }
        for (int z = 1; z <= 6; z++) {
            if (zoneKlaar(p, z)) {
                n += geef(p, "vr_ftb_zone" + z);
            }
            if (geheimenKlaar(p, z)) {
                n += geef(p, "vr_ftb_geheimen" + z);
            }
        }
        if (raidKlaar(p)) {
            n += geef(p, "vr_ftb_raid");
        }
        n += poorten(p);
        return n;
    }

    /** The zone gate: "Zone N is open!" for every open zone N >= 2. Never taken back (a lowered gate hides nothing that was shown). */
    public static int poorten(ServerPlayer p) {
        int n = 0;
        for (int z = 2; z <= Math.min(6, ZonePoort.open()); z++) {
            n += geef(p, "vr_ftb_poort" + z);
        }
        return n;
    }

    /** After a hand-in: only the quests that can end something (cheap; the secrets wait for the interval). */
    static void questKlaar(ServerPlayer p, String quest) {
        if (quest.equals(REKRUUT_QUEST)) {
            geef(p, "vr_ftb_rekruut");
        }
        int z = ZoneBoeken.zoneVan(quest);
        if (quest.equals(eindQuest(z))) {
            geef(p, "vr_ftb_zone" + z);
        }
    }

    static void raidOpgevrolijkt(ServerPlayer p) {
        geef(p, "vr_ftb_raid");
    }

    /** 1 when this call granted it, else 0. */
    static int geef(ServerPlayer p, String naam) {
        if (heeft(p, naam)) {
            return 0;
        }
        GuhAdvancements.grant(p, naam);
        if (heeft(p, naam)) {
            verleend++;
            return 1;
        }
        return 0;
    }

    public static boolean heeft(ServerPlayer p, String naam) {
        AdvancementHolder h = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return h != null && p.getAdvancements().getOrStartProgress(h).isDone();
    }

    /** Dev/tests: takes one back. */
    public static void neemTerug(ServerPlayer p, String naam) {
        AdvancementHolder h = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        if (h != null) {
            List<String> gedaan = new ArrayList<>();
            p.getAdvancements().getOrStartProgress(h).getCompletedCriteria().forEach(gedaan::add);
            for (String c : gedaan) {
                p.getAdvancements().revoke(h, c);
            }
        }
    }

    /** Every advancement name of the chapter (the order of the quest book). */
    public static List<String> alle() {
        List<String> l = new ArrayList<>();
        l.add("vr_ftb_rekruut");
        for (int z = 1; z <= 6; z++) {
            l.add("vr_ftb_zone" + z);
        }
        l.add("vr_ftb_raid");
        for (int z = 1; z <= 6; z++) {
            l.add("vr_ftb_geheimen" + z);
        }
        for (int z = 2; z <= 6; z++) {
            l.add("vr_ftb_poort" + z);
        }
        return l;
    }
}
