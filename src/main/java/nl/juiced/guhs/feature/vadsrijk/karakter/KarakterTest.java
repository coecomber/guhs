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
        r.add(check(lijst.getIntOr("actief", -1) == 0 && lijst.contains("s0") && !lijst.contains("s1"), "lijst: slot 0 actief, rest leeg"));
        // split + join = the same
        CompoundTag terug = Karakter.metAccount(Karakter.alleenKarakter(vol), vol);
        r.add(check(terug.equals(vol), "splitsen in karakter + account en weer samenvoegen geeft hetzelfde"));
        CompoundTag kar = Karakter.alleenKarakter(vol);
        boolean schoon = !kar.contains("opgeborgen") && !kar.contains("tips") && !kar.getCompoundOrEmpty("extra").contains(Karakter.LIJST)
                && !kar.getCompoundOrEmpty("extra").contains("huisdieren") && !kar.getCompoundOrEmpty("extra").getCompoundOrEmpty("economie").contains("rijdieren");
        r.add(check(schoon, "het karakterdeel bevat geen accountdata"));
        return r;
    }

    private static String check(boolean ok, String wat) {
        return (ok ? "OK   " : "FOUT ") + wat;
    }
}
