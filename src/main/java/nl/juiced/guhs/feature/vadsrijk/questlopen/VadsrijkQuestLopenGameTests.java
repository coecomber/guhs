package nl.juiced.guhs.feature.vadsrijk.questlopen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/** U-QUESTLOPEN: the step list (complete + chronological for zones 1-6), the level setting, the fast-forward with its flags. */
public class VadsrijkQuestLopenGameTests {
    /** Every main quest of zones 1-6 is in the list, with all its steps in order, zones never go back, "na" before the quest. */
    @GuhTest(template = "empty", batch = "vadsrijk_questlopen")
    public static void stappenVolledigEnOpVolgorde(GameTestHelper helper) {
        List<QlStap> l = QlStappen.lijst("knuffelridder", false, false);
        helper.assertTrue(l.get(0).soort() == QlStap.Soort.OPENING, "starts with the opening");
        Map<String, Integer> eerste = new HashMap<>();
        int zone = 1;
        for (QlStap s : l) {
            helper.assertTrue(s.nr() == l.indexOf(s), "nr = index at " + s);
            helper.assertTrue(s.zone() >= zone, "zone goes back at " + s);
            zone = s.zone();
            helper.assertFalse(s.extra(), "no extras with both toggles off: " + s);
            eerste.putIfAbsent(s.quest() + "#" + s.soort(), s.nr());
        }
        List<Integer> zones = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (!q.hoofd()) {
                continue;
            }
            int z = ZoneBoeken.zoneVan(q.id());
            if (!zones.contains(z)) {
                zones.add(z);
            }
            // NEEM (not for auto quests), every objective in order, then LEVER, all consecutive
            List<QlStap> van = l.stream().filter(s -> s.quest().equals(q.id()) && s.soort() != QlStap.Soort.OPENING).toList();
            int verwacht = (q.auto() ? 0 : 1) + q.doelen().size() + 1;
            helper.assertTrue(van.size() == verwacht, q.id() + ": " + van.size() + " steps, expected " + verwacht);
            for (int i = 1; i < van.size(); i++) {
                helper.assertTrue(van.get(i).nr() == van.get(i - 1).nr() + 1, q.id() + ": steps not consecutive");
            }
            helper.assertTrue(van.get(van.size() - 1).soort() == QlStap.Soort.LEVER, q.id() + ": ends with the hand-in");
            for (String na : q.na()) {
                Quest n = Quests.van(na);
                if (n != null && n.hoofd()) {
                    helper.assertTrue(l.stream().anyMatch(s -> s.quest().equals(na) && s.soort() == QlStap.Soort.LEVER && s.nr() < van.get(0).nr()),
                            q.id() + ": " + na + " must come first");
                }
                helper.assertTrue(van.get(0).minLv() == Math.min(nl.juiced.guhs.feature.vadsrijk.kern.Levels.MAX, q.lv()), q.id() + ": min level");
            }
        }
        helper.assertTrue(zones.equals(List.of(1, 2, 3, 4, 5, 6)), "zones 1-6 all present in order: " + zones);
        // the toggles add the advancement quests of the class and the mount/pet quests, each after its anchor
        List<QlStap> extra = QlStappen.lijst("knuffelridder", true, true);
        helper.assertTrue(extra.size() > l.size(), "toggles add steps");
        int spec = indexVan(extra, "spec17_knuffelridder"), hq09 = indexVan(extra, "hq09_druipgrotten");
        helper.assertTrue(spec > hq09 && hq09 >= 0, "spec17 after hq09 (" + spec + " > " + hq09 + ")");
        helper.assertTrue(indexVan(extra, "spec17_pluisjager") < 0, "only your own class's advancements");
        helper.assertTrue(indexVan(extra, "rijdier_hoedhupper") > indexVan(extra, "z3_hq03_pluimstal"), "mount quests after the first mount");
        helper.succeed();
    }

    private static int indexVan(List<QlStap> l, String quest) {
        for (QlStap s : l) {
            if (s.quest().equals(quest)) {
                return s.nr();
            }
        }
        return -1;
    }

    /** The level goes UP to at least the minimum (generic + current class; a class quest: that class too), never down. */
    @GuhTest(template = "empty", batch = "vadsrijk_questlopen")
    public static void levelZetten(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.zetLevel(p, 1);
        QlDoe.zetLevel(p, null, 35);
        helper.assertTrue(Spelers.level(p) >= 35, "generic level >= 35: " + Spelers.level(p));
        helper.assertTrue(Spelers.klasseLevel(p) >= 35, "class level >= 35: " + Spelers.klasseLevel(p));
        QlDoe.zetLevel(p, null, 20);
        helper.assertTrue(Spelers.level(p) >= 35, "never down: " + Spelers.level(p));
        QlDoe.zetLevel(p, "pluisjager", 40);
        helper.assertTrue(Spelers.level(p, "pluisjager") >= 40, "the quest's class: " + Spelers.level(p, "pluisjager"));
        // the step list's own numbers: the zone-6 finale needs its quest level
        List<QlStap> l = QlStappen.lijst(Spelers.data(p).klasse, false, false);
        QlStap laatste = l.get(l.size() - 1);
        Quest q = laatste.q();
        helper.assertTrue(q != null && laatste.minLv() == Math.min(nl.juiced.guhs.feature.vadsrijk.kern.Levels.MAX, q.lv()), "last step min level");
        helper.assertTrue(laatste.verwachtLv() >= laatste.minLv(), "expected >= min");
        Spelers.zetLevel(p, 1);
        helper.succeed();
    }

    /**
     * "Spring naar" the first step of zone 2: every zone-1 main quest is handed in through the real hooks, with the zone flag set
     * (zone1_klaar); then back to hq06: hq06 and later are open again, hq05 stays done.
     */
    @GuhTest(template = "empty", batch = "vadsrijk_questlopen", timeoutTicks = 600)
    public static void springNaarMetVlaggen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        Spelers.zetLevel(p, 1);
        List<QlStap> l = QlStappen.lijst(Spelers.data(p).klasse, false, false);
        int zone2 = l.stream().filter(s -> s.zone() == 2).findFirst().orElseThrow().nr();
        QuestLoper.springDirect(p, false, false, zone2);
        for (Quest q : Quests.alle().values()) {
            if (q.hoofd() && ZoneBoeken.zoneVan(q.id()) == 1) {
                helper.assertTrue(Questlog.klaar(p, q.id()), q.id() + " handed in");
            }
        }
        QlStap nu = QlStappen.huidig(p, l);
        helper.assertTrue(nu != null && nu.nr() == zone2, "now at the first zone-2 step: " + nu);
        helper.assertTrue(Questlog.vlag(p, "zone1_klaar"), "zone1_klaar set by the real end of zone 1");
        helper.assertTrue(Spelers.level(p) >= nu.minLv(), "level for the next step");
        int hq06 = l.stream().filter(s -> s.quest().equals("hq06_zwamzoet")).findFirst().orElseThrow().nr();
        QlStappen.zetStand(p, l, hq06);
        helper.assertTrue(Questlog.klaar(p, "hq05_klodderreus"), "hq05 stays done");
        helper.assertTrue(Questlog.status(p, "hq06_zwamzoet") == 0 && Questlog.status(p, "hq12_schaduw") == 0, "hq06+ open again");
        QlStap terug = QlStappen.huidig(p, l);
        helper.assertTrue(terug != null && terug.nr() == hq06, "current = hq06's first step: " + terug);
        Questlog.wisAlles(p);
        Spelers.zetLevel(p, 1);
        helper.succeed();
    }
}
