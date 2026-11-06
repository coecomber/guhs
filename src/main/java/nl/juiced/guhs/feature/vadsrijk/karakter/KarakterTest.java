package nl.juiced.guhs.feature.vadsrijk.karakter;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/** T-KARAKTER: checks used by {@code /guhs vadsrijk karakter migratie-test} and the gametests (non-destructive). */
final class KarakterTest {
    private KarakterTest() {
    }

    /**
     * Pretends the current data is an OLD save (no character list) and loads it into a fresh SpelerData: the migrated slot 0 must
     * hold exactly the same character (levels, coins, quests, inventory, extras). Also checks that splitting into character +
     * account and joining them again gives the same data back. Returns report lines ("OK ..." / "FOUT ...").
     */
    static List<String> migratie(ServerPlayer p) {
        List<String> r = new ArrayList<>();
        SpelerData d = Spelers.data(p);
        CompoundTag vol = Karakter.schrijf(p, d);
        CompoundTag oud = vol.copy();
        CompoundTag extra = oud.getCompoundOrEmpty("extra").copy();
        extra.remove(Karakter.LIJST);
        oud.put("extra", extra);
        SpelerData n = new SpelerData();
        Karakter.lees(p, n, oud);
        r.add(check(n.level() == d.level(), "generiek level " + n.level() + " = " + d.level()));
        r.add(check(n.xp.equals(d.xp), "klasse-xp gelijk " + n.xp));
        r.add(check(n.munten == d.munten, "Vadsmunten " + n.munten + " = " + d.munten));
        r.add(check(n.rijkInventaris.size() == d.rijkInventaris.size() && n.opgeborgen.size() == d.opgeborgen.size(),
                "inventarissen " + n.rijkInventaris.size() + "/" + n.opgeborgen.size()));
        r.add(check(n.extra("npc").equals(d.extra("npc")), "quests (extra npc) gelijk"));
        r.add(check(n.boom.equals(d.boom), "skillbomen gelijk"));
        CompoundTag lijst = Karakter.migreer(p.getGameProfile().name());
        r.add(check(lijst.getIntOr("actief", -1) == 0 && lijst.contains("s0") && !lijst.contains("s1") && Modus.vanSlot(0) == Modus.NORMAAL, "lijst: slot 0 (links, Normaal) actief, rest leeg"));
        // split + join = the same
        CompoundTag terug = Karakter.metAccount(Karakter.alleenKarakter(vol), vol);
        r.add(check(terug.equals(vol), "splitsen in karakter + account en weer samenvoegen geeft hetzelfde"));
        CompoundTag kar = Karakter.alleenKarakter(vol);
        boolean schoon = !kar.contains("opgeborgen") && !kar.contains("tips") && !kar.getCompoundOrEmpty("extra").contains(Karakter.LIJST)
                && !kar.getCompoundOrEmpty("extra").contains("huisdieren") && !kar.getCompoundOrEmpty("extra").getCompoundOrEmpty("economie").contains("rijdieren");
        r.add(check(schoon, "het karakterdeel bevat geen accountdata"));
        return r;
    }

    /**
     * U-KARAKTER: the v1 -> v2 re-ordering on hand-made v1 lists (no player needed). Returns report lines ("OK ..." / "FOUT ...").
     */
    static List<String> herschik() {
        List<String> r = new ArrayList<>();
        // A: v1 slot 0 Normaal, slot 1 Eénvads (active), slot 2 Knuffelmodus
        CompoundTag l = new CompoundTag();
        l.putInt("actief", 1);
        l.putInt("versie", 1);
        l.put("s0", v1("Anna", "normaal", false, true));
        l.put("s1", v1("Bram", "eenvads", false, false));
        l.put("s2", v1("Cor", "knuffel", false, true));
        Karakter.herschik(l);
        r.add(check(l.getIntOr("versie", 0) == Karakter.VERSIE, "versie " + l.getIntOr("versie", 0)));
        r.add(check(naam(l, 0).equals("Anna") && naam(l, 1).equals("Cor") && naam(l, 2).equals("Bram"), "A: Normaal links, Knuffel midden, Eenvads rechts"));
        r.add(check(l.getIntOr("actief", -1) == 2, "A: de actieve (Eenvads) is nu slot 2"));
        r.add(check(l.getCompoundOrEmpty("s1").contains("data") && l.getCompoundOrEmpty("s1").getStringOr("opslag", "").equals("3"),
                "A: Cor houdt zijn data en zijn knapzak-sleutel (3)"));
        r.add(check(l.getCompoundOrEmpty("s2").getStringOr("opslag", "").equals("2"), "A: Bram houdt knapzak-sleutel 2"));
        // B: two Normaal knights (active = slot 2) + a lowered one (begin eenvads, now knuffel) + nothing else
        l = new CompoundTag();
        l.putInt("actief", 2);
        l.put("s0", v1("Dirk", "normaal", false, true));
        l.put("s1", v1("Eva", "knuffel", false, true));
        l.put("s2", v1("Fien", "normaal", false, false));
        Karakter.herschik(l);
        r.add(check(naam(l, 0).equals("Fien") && l.getIntOr("actief", -1) == 0, "B: de actieve Normaal krijgt links en blijft actief"));
        r.add(check(naam(l, 1).equals("Eva") && !l.contains("s2"), "B: Knuffel midden, rechts leeg"));
        var archief = l.getListOrEmpty("archief");
        r.add(check(archief.size() == 1 && archief.getCompoundOrEmpty(0).getStringOr("naam", "").equals("Dirk") && archief.getCompoundOrEmpty(0).contains("data"),
                "B: de tweede Normaal gaat met al zijn data naar het archief"));
        // C: a retired Eénvads knight (v1: modus normaal + pensioen) goes right, as a statue
        l = new CompoundTag();
        l.putInt("actief", 0);
        l.put("s0", v1("Gijs", "normaal", false, false));
        l.put("s1", v1("Held", "normaal", true, true));
        Karakter.herschik(l);
        r.add(check(naam(l, 0).equals("Gijs") && naam(l, 2).equals("Held") && l.getCompoundOrEmpty("s2").getBooleanOr("pensioen", false)
                && !l.contains("archief"), "C: de gepensioneerde Eenvads staat rechts als beeld, de Normaal links"));
        // D: a fresh v2 list is left alone
        CompoundTag v2 = Karakter.migreer("Speler");
        CompoundTag kopie = v2.copy();
        r.add(check(v2.getIntOr("versie", 0) == Karakter.VERSIE && v2.equals(kopie) && naam(v2, 0).equals("Speler"), "D: nieuwe lijst = v2, links Normaal"));
        return r;
    }

    private static CompoundTag v1(String naam, String modus, boolean pensioen, boolean data) {
        CompoundTag s = new CompoundTag();
        s.putString("naam", naam);
        s.putString("modus", modus);
        s.putString("begin", pensioen ? "eenvads" : modus);
        if (pensioen) {
            s.putBoolean("pensioen", true);
        }
        if (data) {
            CompoundTag d = new CompoundTag();
            d.putLong("munten", 5);
            s.put("data", d);
        }
        return s;
    }

    private static String naam(CompoundTag l, int slot) {
        return l.getCompoundOrEmpty("s" + slot).getStringOr("naam", "-");
    }

    private static String check(boolean ok, String wat) {
        return (ok ? "OK   " : "FOUT ") + wat;
    }
}
