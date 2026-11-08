package nl.juiced.guhs.feature.vadsrijk.questlopen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijdierQuests;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;

/**
 * U-QUESTLOPEN: the main line of zones 1-6 as one chronological list of steps (data-driven from {@link Quests#alle()}: the main
 * quests in questlog order, which is the story order; a zone added later comes along by itself). Optional quests are slotted in
 * after the main quest that opens them:
 * <ul>
 *   <li>"advancements": the class quests of YOUR class (spec17_ / z3_adv35_ / z5_adv58_: the lv-17 spec choice, lv 35, lv 58);</li>
 *   <li>"rijdieren": the mount quests (soort RIJDIER) and the side quests that give a pet ({@code ..._huisdier:} action).</li>
 * </ul>
 * Whether a step is done is read from the real questlog, so the list always matches the character's progress.
 */
public final class QlStappen {
    /** The main quest whose step gives the first mount (the "@rijdier" gate of the mount quests opens after it). */
    static final String RIJDIER_MELD = "rijdier:starter";

    private QlStappen() {
    }

    /** The whole list for this class and these toggles. */
    public static List<QlStap> lijst(@Nullable String klasse, boolean advancements, boolean rijdieren) {
        List<Quest> hoofd = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (q.hoofd()) {
                hoofd.add(q);
            }
        }
        // the optional quests, each behind its anchor (the latest main quest it waits for)
        Map<String, List<Quest>> na = new LinkedHashMap<>();
        for (Quest q : Quests.alle().values()) {
            if (q.hoofd() || !(advancements && advancement(q, klasse) || rijdieren && rijdier(q))) {
                continue;
            }
            String anker = anker(q, hoofd);
            if (anker != null) {
                na.computeIfAbsent(anker, k -> new ArrayList<>()).add(q);
            }
        }
        List<QlStap> l = new ArrayList<>();
        l.add(new QlStap(0, 1, hoofd.isEmpty() ? "" : hoofd.get(0).id(), QlStap.Soort.OPENING, -1, 1, 1, false, false));
        for (int i = 0; i < hoofd.size(); i++) {
            Quest q = hoofd.get(i);
            int verwacht = i + 1 < hoofd.size() ? Math.max(q.lv(), hoofd.get(i + 1).lv()) : q.lv();
            voegToe(l, q, verwacht, false);
            for (Quest x : na.getOrDefault(q.id(), List.of())) {
                voegToe(l, x, Math.max(x.lv(), verwacht), true);
            }
        }
        return l;
    }

    private static void voegToe(List<QlStap> l, Quest q, int verwacht, boolean extra) {
        int zone = ZoneBoeken.zoneVan(q.id());
        boolean kl = q.klasse() != null;
        int lv = Math.min(Levels.MAX, q.lv());
        verwacht = Math.min(Levels.MAX, Math.max(lv, verwacht));
        if (!q.auto() || !q.hoofd()) {
            l.add(new QlStap(l.size(), zone, q.id(), QlStap.Soort.NEEM, -1, lv, verwacht, kl, extra));
        }
        for (int i = 0; i < q.doelen().size(); i++) {
            Quest.Doel d = q.doelen().get(i);
            int min = d.type() == Quest.Doel.Type.LEVEL ? Math.min(Levels.MAX, Math.max(lv, d.n())) : lv;
            l.add(new QlStap(l.size(), zone, q.id(), QlStap.Soort.DOEL, i, min, Math.max(min, verwacht), kl, extra));
        }
        l.add(new QlStap(l.size(), zone, q.id(), QlStap.Soort.LEVER, -1, lv, verwacht, kl, extra));
    }

    /** An advancement quest of this class (job advancements: lv 17 spec choice, lv 35, lv 58). */
    static boolean advancement(Quest q, @Nullable String klasse) {
        return q.soort() == Quest.Soort.KLASSE && klasse != null && klasse.equals(q.klasse()) && q.actieKlaar().startsWith("advancement:");
    }

    /** A mount quest, or a quest whose reward is a pet. */
    static boolean rijdier(Quest q) {
        return q.soort() == Quest.Soort.RIJDIER || q.actieKlaar().contains("huisdier:") || q.actieKlaar().startsWith("rijdier");
    }

    /** The main quest after which this optional quest can be done (null: it never opens on the main line). */
    @Nullable
    static String anker(Quest q, List<Quest> hoofd) {
        int beste = -1;
        for (String n : q.na()) {
            int i = -1;
            if (n.equals(RijdierQuests.POORT)) {
                for (int k = 0; k < hoofd.size(); k++) {
                    for (Quest.Doel d : hoofd.get(k).doelen()) {
                        if (d.type() == Quest.Doel.Type.MELD && d.doel().equals(RIJDIER_MELD)) {
                            i = k;
                        }
                    }
                }
            } else {
                for (int k = 0; k < hoofd.size(); k++) {
                    if (hoofd.get(k).id().equals(n)) {
                        i = k;
                    }
                }
                if (i < 0) {
                    Quest x = Quests.van(n);   // (behind another optional quest: that one's anchor)
                    String a = x == null || x.hoofd() ? null : anker(x, hoofd);
                    for (int k = 0; a != null && k < hoofd.size(); k++) {
                        if (hoofd.get(k).id().equals(a)) {
                            i = k;
                        }
                    }
                }
            }
            if (i < 0) {
                return null;
            }
            beste = Math.max(beste, i);
        }
        return beste < 0 ? null : hoofd.get(beste).id();
    }

    // ---- progress ------------------------------------------------------------------------------------------------------------------

    public static boolean klaar(ServerPlayer p, QlStap s) {
        Quest q = s.q();
        if (s.soort() == QlStap.Soort.OPENING) {
            return Spelers.data(p).gezien.contains("npc:opening") || q != null && Questlog.status(p, q.id()) != 0;
        }
        if (q == null) {
            return true;
        }
        return switch (s.soort()) {
            case NEEM -> Questlog.status(p, q.id()) != 0;
            case DOEL -> Questlog.klaar(p, q.id()) || Questlog.actief(p, q.id()) && Questlog.doelKlaar(p, q, s.doel());
            default -> Questlog.klaar(p, q.id());
        };
    }

    /** The first step that is not done yet (null: the whole main line is done). */
    @Nullable
    public static QlStap huidig(ServerPlayer p, List<QlStap> l) {
        for (QlStap s : l) {
            if (!klaar(p, s)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Sets the questlog so that exactly the steps before {@code tot} count as done for every quest IN THE LIST (taken / objectives
     * / handed in): used by "Terug", "Spring naar" backwards and "Reset". Rewards, story flags and items are NOT taken back.
     */
    public static void zetStand(ServerPlayer p, List<QlStap> l, int tot) {
        Map<String, List<QlStap>> perQuest = new LinkedHashMap<>();
        for (QlStap s : l) {
            if (s.soort() != QlStap.Soort.OPENING) {
                perQuest.computeIfAbsent(s.quest(), k -> new ArrayList<>()).add(s);
            }
        }
        CompoundTag data = Questlog.data(p);
        if (!(data.get("q") instanceof CompoundTag)) {
            data.put("q", new CompoundTag());
        }
        CompoundTag qs = (CompoundTag) data.get("q");
        for (var e : perQuest.entrySet()) {
            Quest q = Quests.van(e.getKey());
            List<QlStap> st = e.getValue();
            if (q == null || st.get(st.size() - 1).nr() < tot) {
                continue;   // (entirely before the cut: leave it as it is)
            }
            if (st.get(0).nr() >= tot) {
                qs.remove(q.id());
                continue;
            }
            int[] v = new int[q.doelen().size()];
            for (QlStap s : st) {
                if (s.soort() == QlStap.Soort.DOEL && s.nr() < tot) {
                    v[s.doel()] = q.doelen().get(s.doel()).n();
                }
            }
            CompoundTag t = new CompoundTag();
            t.putInt("s", Questlog.ACTIEF);
            t.putIntArray("v", v);
            qs.put(q.id(), t);
        }
        if (tot <= 0) {
            Spelers.data(p).gezien.remove("npc:opening");
        }
        nl.juiced.guhs.feature.vadsrijk.npc.NpcMarkers.vies(p);
        Questlog.kijkLive(p);
    }
}
