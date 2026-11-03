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
 * of the dimension guhs:erfenis), its building plan (which grows) and the player it follows (1.9 rule, see KaartLeven). The map's points are its Knusheid.
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
    /**
     * The real player whose Hoeve level this village keeps following (null: a POC village from before 1.9, dropped or adopted
     * by the migration) and how far off it stays: -1 (~1 lower), 0 (equal) or +1 (~1 higher).
     */
    @javax.annotation.Nullable
    public UUID volgt;
    public int afwijking;

    public NpcDorp(UUID id, String naam, String eigenaar, int kleur, int cx, int cz) {
        this.id = id;
        this.naam = naam;
        this.eigenaar = eigenaar;
        this.kleur = kleur;
        this.cx = cx;
        this.cz = cz;
    }

    public int hoeveLevel() {
        return NpcBouwer.hoeveLevel(plan);
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
        if (volgt != null) {
            t.store("Volgt", UUIDUtil.CODEC, volgt);
        }
        t.putInt("Afwijking", afwijking);
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
        d.volgt = t.read("Volgt", UUIDUtil.CODEC).orElse(null);
        d.afwijking = t.getIntOr("Afwijking", 0);
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
