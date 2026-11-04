package nl.juiced.guhs.feature.vadsrijk.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * What the questlog screen (J) shows, built on the server (texts are lang keys, the client translates):
 * <pre>
 *   Actief  [{Id, Soort, Klaar, Bij (naam key of who to hand in to), Doelen [{K, V, N}]}]
 *   Af      [{Id, Soort}]
 *   Knuffels n, KnuffelsMax, Aanwijzingen [ids], AanwijzingenMax, Schatten [ids], SchattenMax
 *   Wezens  [{T: entity type id, N}]     Zone1 true/false
 * </pre>
 */
public final class NpcLog {
    private NpcLog() {
    }

    public static CompoundTag bouw(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        ListTag actief = new ListTag(), af = new ListTag();
        for (Quest q : Quests.alle().values()) {
            int s = Questlog.status(p, q.id());
            if (s == Questlog.ACTIEF) {
                CompoundTag a = new CompoundTag();
                a.putString("Id", q.id());
                a.putString("Soort", q.soort().name());
                a.putBoolean("Klaar", Questlog.allesKlaar(p, q));
                NpcDef bij = Npcs.van(q.inleveren());
                a.putString("Bij", bij == null ? "" : bij.naamKey());
                ListTag doelen = new ListTag();
                for (int i = 0; i < q.doelen().size(); i++) {
                    CompoundTag d = new CompoundTag();
                    d.putString("K", q.doelKey(i));
                    d.putInt("V", Questlog.waarde(p, q, i));
                    d.putInt("N", q.doelen().get(i).n());
                    doelen.add(d);
                }
                a.put("Doelen", doelen);
                QuestStap st = QuestStap.van(p, q);
                nl.juiced.guhs.taal.Tekst.put(a, "Wat", st.wat());
                nl.juiced.guhs.taal.Tekst.put(a, "Waar", st.waar());
                actief.add(a);
            } else if (s == Questlog.KLAAR) {
                CompoundTag a = new CompoundTag();
                a.putString("Id", q.id());
                a.putString("Soort", q.soort().name());
                af.add(a);
            }
        }
        // the main story: always on top, with its current step (also when no main quest is active) and its history
        QuestStap h = QuestStap.hoofdlijn(p);
        if (h != null) {
            CompoundTag s = new CompoundTag();
            s.putString("Id", h.quest());
            s.putBoolean("Actief", Questlog.actief(p, h.quest()));
            nl.juiced.guhs.taal.Tekst.put(s, "Wat", h.wat());
            nl.juiced.guhs.taal.Tekst.put(s, "Waar", h.waar());
            if (h.extra() != null) {
                nl.juiced.guhs.taal.Tekst.put(s, "Extra", h.extra());
            }
            t.put("Hoofd", s);
        }
        ListTag geschiedenis = new ListTag();
        for (Quest q : QuestStap.hoofdGedaan(p)) {
            geschiedenis.add(StringTag.valueOf(q.id()));
        }
        t.put("Geschiedenis", geschiedenis);
        t.putString("Pin", NpcVolg.pin(p));
        t.putBoolean("VolgUit", Questlog.vlag(p, "volg_uit"));
        t.putBoolean("StraalUit", Questlog.vlag(p, "straal_uit"));
        t.put("Actief", actief);
        t.put("Af", af);
        t.putInt("Knuffels", Questlog.map(p, "knuffels").size());
        t.putInt("KnuffelsMax", NpcPlekken.KNUFFEL.size());
        t.put("Aanwijzingen", ids(Questlog.map(p, "aanwijzingen")));
        t.putInt("AanwijzingenMax", NpcPlekken.AANWIJZING.size());
        t.put("Schatten", ids(Questlog.map(p, "schatten")));
        t.putInt("SchattenMax", NpcPlekken.SCHAT.size());
        ListTag wezens = new ListTag();
        CompoundTag w = Questlog.map(p, "wezens");
        for (String k : w.keySet()) {
            CompoundTag x = new CompoundTag();
            x.putString("T", k);
            x.putInt("N", w.getIntOr(k, 0));
            wezens.add(x);
        }
        t.put("Wezens", wezens);
        t.putBoolean("Zone1", NpcApi.zone1Klaar(p));
        return t;
    }

    private static ListTag ids(CompoundTag t) {
        ListTag l = new ListTag();
        for (String k : t.keySet()) {
            l.add(StringTag.valueOf(k));
        }
        return l;
    }
}
