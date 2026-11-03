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
                actief.add(a);
            } else if (s == Questlog.KLAAR) {
                CompoundTag a = new CompoundTag();
                a.putString("Id", q.id());
                a.putString("Soort", q.soort().name());
                af.add(a);
            }
        }
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
