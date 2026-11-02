package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * One NPC village on the world map: its id (also the core base id it becomes), name, owner, flag colour, cell (plot grid
 * of the dimension guhs:erfenis) and its building plan (which grows). The map's points are its Knusheid.
 */
public final class NpcDorp {
    public final UUID id;
    public String naam;
    public String eigenaar;
    public int kleur;
    public final int cx, cz;
    public final List<KaartGebouw> plan = new ArrayList<>();
    /** Real time (millis) it was founded / last grew (for the "nieuw!" mark on the map). */
    public long gesticht, gegroeid;

    public NpcDorp(UUID id, String naam, String eigenaar, int kleur, int cx, int cz) {
        this.id = id;
        this.naam = naam;
        this.eigenaar = eigenaar;
        this.kleur = kleur;
        this.cx = cx;
        this.cz = cz;
    }

    public int stadhuisLevel() {
        return NpcBouwer.stadhuisLevel(plan);
    }

    public int punten() {
        return NpcBouwer.knusheid(plan);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, id);
        t.putString("Naam", naam);
        t.putString("Eigenaar", eigenaar);
        t.putInt("Kleur", kleur);
        t.putInt("Cx", cx);
        t.putInt("Cz", cz);
        t.putLong("Gesticht", gesticht);
        t.putLong("Gegroeid", gegroeid);
        ListTag l = new ListTag();
        for (KaartGebouw g : plan) {
            l.add(g.save());
        }
        t.put("Plan", l);
        return t;
    }

    @javax.annotation.Nullable
    public static NpcDorp load(CompoundTag t) {
        UUID id = t.read("Id", UUIDUtil.CODEC).orElse(null);
        if (id == null) {
            return null;
        }
        NpcDorp d = new NpcDorp(id, t.getStringOr("Naam", "?"), t.getStringOr("Eigenaar", "?"), t.getIntOr("Kleur", 0xFFF58BB0),
                t.getIntOr("Cx", 0), t.getIntOr("Cz", 0));
        d.gesticht = t.getLongOr("Gesticht", 0L);
        d.gegroeid = t.getLongOr("Gegroeid", 0L);
        for (Tag e : t.getListOrEmpty("Plan")) {
            if (e instanceof CompoundTag c) {
                KaartGebouw g = KaartGebouw.load(c);
                if (g != null) {
                    d.plan.add(g);
                }
            }
        }
        return d;
    }
}
