package nl.juiced.guhs.feature.vadsrijk.questlopen;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;

/**
 * U-QUESTLOPEN: one step of the main line as the Questlopen screen shows it. {@code soort}: OPENING (the arrival film in the
 * Knuffelkamp, before the first quest), NEEM (take quest at its giver; auto quests have none), DOEL (objective {@code doel} of the
 * quest) or LEVER (hand it in). {@code minLv}: the level the step needs (the quest's level, or a LEVEL objective's), {@code
 * verwachtLv}: the level a normal player has around here (the next main quest's level). {@code klasseLv}: the level is a CLASS
 * level (advancement quests). {@code extra}: an optional quest (advancement / mount / pet) that is only in the list when its toggle
 * is on.
 */
public record QlStap(int nr, int zone, String quest, Soort soort, int doel, int minLv, int verwachtLv, boolean klasseLv, boolean extra) {
    public enum Soort {
        OPENING, NEEM, DOEL, LEVER
    }

    @Nullable
    public Quest q() {
        return Quests.van(quest);
    }

    @Nullable
    public Quest.Doel d() {
        Quest q = q();
        return q == null || soort != Soort.DOEL || doel < 0 || doel >= q.doelen().size() ? null : q.doelen().get(doel);
    }

    /** A boss fight: a BAAS objective, or a MELD "x:verslagen"/"x:opgevrolijkt" that points at a boss spot ("baas:..."). */
    public boolean baas() {
        Quest.Doel d = d();
        if (d == null) {
            return false;
        }
        if (d.type() == Quest.Doel.Type.BAAS) {
            return true;
        }
        return d.type() == Quest.Doel.Type.MELD && d.plek().startsWith("baas:") && (d.doel().endsWith(":verslagen") || d.doel().endsWith(":opgevrolijkt"));
    }
}
