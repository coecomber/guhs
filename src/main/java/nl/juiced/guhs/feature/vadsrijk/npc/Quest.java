package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * One quest of zone 1 (data: data/guhs/vadsrijk/npc/quests.json, written by tools/features/vadsrijk_npc.py, which also holds
 * every text). Texts by key: {@code vadsrijk.npc.q.<id>.titel / .tekst / .doel.<i> / .aanbod.<i> / .bezig / .klaar.<i> /
 * .praat.<npc>.<i>}; the speaker lists say who says page i ("npc" = the quest NPC, "jij" = the player, else an NPC id).
 */
public record Quest(String id, Soort soort, String gever, String inleveren, int lv, List<String> na, @Nullable String klasse, boolean auto,
        List<Doel> doelen, Beloning beloning, List<String> aanbod, List<String> klaar, Map<String, List<String>> praat, String actieAanname,
        String actieKlaar, List<Beloning.Ding> startItems) {

    public enum Soort {
        HOOFD, ZIJ, KLASSE, RIJDIER, GEHEIM;

        public static Soort van(String s) {
            for (Soort x : values()) {
                if (x.name().equalsIgnoreCase(s)) {
                    return x;
                }
            }
            return ZIJ;
        }
    }

    /**
     * What an objective counts. {@code doel}: an NPC id, "gebied:&lt;area&gt;", "poi:&lt;id&gt;", an entity/item id, "*"...
     * {@code plek} (zone 2, optional, "" = derive it from the type): where the tracker points, "npc:&lt;id&gt;", "poi:&lt;id&gt;" or
     * "gebied:&lt;area&gt;" ({@link QuestPlek}).
     */
    public record Doel(Type type, String doel, int n, String bron, double kans, String plek) {
        public Doel(Type type, String doel, int n, String bron, double kans) {
            this(type, doel, n, bron, kans, "");
        }

        public enum Type {
            /** talk to NPC doel */
            PRAAT,
            /** cheer up n mobs: doel = entity type id, "gebied:&lt;area&gt;" or "*" (Haken.VERSLAGEN) */
            OPVROLIJK,
            /** cheer up n elites (doel as OPVROLIJK) */
            ELITE,
            /** have n of item doel at turn-in; they drop for you from mobs in bron ("gebied:x", a type id or "*") with chance kans */
            VERZAMEL,
            /** reach: doel = area id (Haken.GEBIED_BETREDEN) or "poi:&lt;id&gt;" */
            GEBIED,
            /** light a campfire: doel = area id or "*" (Haken.KAMPVUUR) */
            KAMPVUUR,
            /** beat a boss: doel = entity type id or "gebied:&lt;area&gt;" (any VadsrijkBaas there) */
            BAAS,
            /** bring NPC doel (who follows you) to bron = "poi:&lt;id&gt;" */
            ESCORTE,
            /** find a secret: doel = "knuffel" / "aanwijzing" / "schat" (n of them, ever) or "poi:&lt;id&gt;" */
            GEHEIM,
            /** reach level n in the quest's class (or your current one) */
            LEVEL,
            /** hit the training dummies n times (target/hay blocks in Knuffelkamp) */
            POP,
            /** pick a class at the klasse-tempeltje (NpcApi.meld(p, "klasse", id) or the fallback screen) */
            KLASSE,
            /** any event another slice reports: NpcApi.meld(p, soort, doel) with doel = "soort:doel" */
            MELD;

            public static Type van(String s) {
                return valueOf(s.toUpperCase(java.util.Locale.ROOT));
            }
        }
    }

    public record Beloning(long xp, long munten, List<Ding> items, boolean wapen, String loot) {
        public record Ding(String item, int n) {
        }
    }

    public String sleutel() {
        return "vadsrijk.npc.q." + id;
    }

    public String titelKey() {
        return sleutel() + ".titel";
    }

    public String tekstKey() {
        return sleutel() + ".tekst";
    }

    public String doelKey(int i) {
        return sleutel() + ".doel." + i;
    }

    public boolean hoofd() {
        return soort == Soort.HOOFD;
    }
}
