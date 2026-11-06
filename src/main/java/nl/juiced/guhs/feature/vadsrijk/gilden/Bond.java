package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.opslag.Vak;

/**
 * U-GILDEN: one Knuffelbond (a guild). Plain data; {@link Bonden} (SavedData) keeps them all and {@link BondActies} changes
 * them. Members are CHARACTERS: the key is {@code Karakter.sleutel(p).tekst()} = "uuid/slot".
 */
public final class Bond {
    public static final int BANK_TABS = 5, BANK_VAKKEN = 45;

    /** One member (a character). */
    public static final class Lid {
        /** player name + character name (for lists while offline) */
        public String naam, karakter;
        public Rang rang;
        public long sinds, bijdrage, gezien;
        /** today's bond xp from this member (daily cap) and which day that was */
        public long dagXp;
        public long dag;

        Lid(String naam, String karakter, Rang rang, long sinds) {
            this.naam = naam;
            this.karakter = karakter;
            this.rang = rang;
            this.sinds = sinds;
            this.gezien = sinds;
        }
    }

    /** One banner layer: pattern id (vanilla, e.g. "cross") and colour. */
    public record Laag(String patroon, DyeColor kleur) {
    }

    public final int id;
    public String naam, tag;
    public long gemaakt;
    // banner + colour (the tag colour follows the banner's base colour)
    public DyeColor basis = DyeColor.PINK;
    public final List<Laag> lagen = new ArrayList<>();
    // recruiting
    public boolean werft = true, vrij = false;
    public String werfTekst = "";
    public int minLevel = 1;
    public final Map<String, Lid> leden = new LinkedHashMap<>();
    /** applications: character key -> "player (character) lv N" */
    public final Map<String, String> aanvragen = new LinkedHashMap<>();
    // progress
    public long xp;
    /** the plot cell in the valley (-1: none), castle level 0..5, the kas (brons) and the depot for the next level (material -> n) */
    public int kavel = -1, kasteel = 0;
    /** the castle level that stands in the world now (0: nothing stamped yet) */
    public int gebouwd = 0;
    public long kas;
    public final Map<String, Integer> depot = new LinkedHashMap<>();
    /** boss trophies: boss title key -> first time (ms), in order */
    public final Map<String, Long> trofeeen = new LinkedHashMap<>();
    /** the week tasks: week id, task ids, progress, done */
    public long week = -1;
    public final List<String> taken = new ArrayList<>();
    public final long[] voortgang = new long[3];
    public final boolean[] taakKlaar = new boolean[3];
    // the bank: raw tags until the first use (needs the server's registries), then live containers
    @Nullable
    private ListTag bankRuw = new ListTag();
    @Nullable
    private List<Vak> bank;
    /** items that could not be read (a removed mod): written back unchanged */
    private final ListTag onleesbaar = new ListTag();

    public Bond(int id, String naam, String tag) {
        this.id = id;
        this.naam = naam;
        this.tag = tag;
    }

    @Nullable
    public Lid lid(String sleutel) {
        return leden.get(sleutel);
    }

    @Nullable
    public String leider() {
        for (Map.Entry<String, Lid> e : leden.entrySet()) {
            if (e.getValue().rang == Rang.LEIDER) {
                return e.getKey();
            }
        }
        return null;
    }

    public int level() {
        return BondXp.levelVan(xp);
    }

    // ---- the bank ------------------------------------------------------------------------------------------------------------

    /** The live bank tabs (materialised on first use with these registries). */
    public List<Vak> bank(HolderLookup.Provider reg) {
        if (bank == null) {
            List<Vak> l = new ArrayList<>();
            for (int t = 0; t < BANK_TABS; t++) {
                l.add(new Vak(BANK_VAKKEN));
            }
            if (bankRuw != null) {
                for (int i = 0; i < bankRuw.size(); i++) {
                    CompoundTag c = bankRuw.getCompoundOrEmpty(i);
                    int t = c.getIntOr("Tab", 0), s = c.getIntOr("Vak", 0);
                    ItemStack st = EcoUtil.stack(reg, c.getCompoundOrEmpty("Item"));
                    if (st.isEmpty() || t < 0 || t >= BANK_TABS || s < 0 || s >= BANK_VAKKEN || !l.get(t).getItem(s).isEmpty()) {
                        onleesbaar.add(c.copy());   // (kept, never lost)
                    } else {
                        l.get(t).setItem(s, st);
                    }
                }
            }
            bank = l;
            bankRuw = null;
        }
        return bank;
    }

    public boolean bankGeladen() {
        return bank != null;
    }

    /** Is there anything in the bank (also unreadable items)? */
    public boolean bankLeeg(HolderLookup.Provider reg) {
        if (!onleesbaar.isEmpty()) {
            return false;
        }
        for (Vak v : bank(reg)) {
            if (!v.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // ---- saving --------------------------------------------------------------------------------------------------------------

    CompoundTag save(@Nullable HolderLookup.Provider reg) {
        CompoundTag t = new CompoundTag();
        t.putInt("Id", id);
        t.putString("Naam", naam);
        t.putString("Tag", tag);
        t.putLong("Gemaakt", gemaakt);
        t.putString("Basis", basis.getName());
        ListTag lg = new ListTag();
        for (Laag l : lagen) {
            CompoundTag c = new CompoundTag();
            c.putString("P", l.patroon());
            c.putString("K", l.kleur().getName());
            lg.add(c);
        }
        t.put("Lagen", lg);
        t.putBoolean("Werft", werft);
        t.putBoolean("Vrij", vrij);
        t.putString("WerfTekst", werfTekst);
        t.putInt("MinLevel", minLevel);
        ListTag ld = new ListTag();
        leden.forEach((k, l) -> {
            CompoundTag c = new CompoundTag();
            c.putString("K", k);
            c.putString("Naam", l.naam);
            c.putString("Kar", l.karakter);
            c.putString("Rang", l.rang.id);
            c.putLong("Sinds", l.sinds);
            c.putLong("Bijdrage", l.bijdrage);
            c.putLong("Gezien", l.gezien);
            c.putLong("DagXp", l.dagXp);
            c.putLong("Dag", l.dag);
            ld.add(c);
        });
        t.put("Leden", ld);
        CompoundTag aa = new CompoundTag();
        aanvragen.forEach(aa::putString);
        t.put("Aanvragen", aa);
        t.putLong("Xp", xp);
        t.putInt("Kavel", kavel);
        t.putInt("Kasteel", kasteel);
        t.putInt("Gebouwd", gebouwd);
        t.putLong("Kas", kas);
        CompoundTag dp = new CompoundTag();
        depot.forEach(dp::putInt);
        t.put("Depot", dp);
        ListTag tr = new ListTag();
        trofeeen.forEach((k, v) -> {
            CompoundTag c = new CompoundTag();
            c.putString("B", k);
            c.putLong("T", v);
            tr.add(c);
        });
        t.put("Trofeeen", tr);
        t.putLong("Week", week);
        t.putString("Taken", String.join(",", taken));
        t.putLongArray("Voortgang", voortgang.clone());
        t.putByteArray("Klaar", new byte[] {(byte) (taakKlaar[0] ? 1 : 0), (byte) (taakKlaar[1] ? 1 : 0), (byte) (taakKlaar[2] ? 1 : 0)});
        ListTag b;
        if (bank == null || reg == null) {
            b = bankRuw == null ? new ListTag() : bankRuw.copy();
        } else {
            b = new ListTag();
            for (int ti = 0; ti < bank.size(); ti++) {
                Vak v = bank.get(ti);
                for (int s = 0; s < v.getContainerSize(); s++) {
                    ItemStack st = v.getItem(s);
                    if (!st.isEmpty()) {
                        CompoundTag c = new CompoundTag();
                        c.putInt("Tab", ti);
                        c.putInt("Vak", s);
                        c.put("Item", EcoUtil.tag(reg, st));
                        b.add(c);
                    }
                }
            }
        }
        for (int i = 0; i < onleesbaar.size(); i++) {
            b.add(onleesbaar.getCompoundOrEmpty(i).copy());
        }
        t.put("Bank", b);
        return t;
    }

    static Bond load(CompoundTag t) {
        Bond b = new Bond(t.getIntOr("Id", 0), t.getStringOr("Naam", "?"), t.getStringOr("Tag", "?"));
        b.gemaakt = t.getLongOr("Gemaakt", 0);
        b.basis = kleur(t.getStringOr("Basis", "pink"));
        ListTag lg = t.getListOrEmpty("Lagen");
        for (int i = 0; i < lg.size(); i++) {
            CompoundTag c = lg.getCompoundOrEmpty(i);
            b.lagen.add(new Laag(c.getStringOr("P", "border"), kleur(c.getStringOr("K", "white"))));
        }
        b.werft = t.getBooleanOr("Werft", true);
        b.vrij = t.getBooleanOr("Vrij", false);
        b.werfTekst = t.getStringOr("WerfTekst", "");
        b.minLevel = t.getIntOr("MinLevel", 1);
        ListTag ld = t.getListOrEmpty("Leden");
        for (int i = 0; i < ld.size(); i++) {
            CompoundTag c = ld.getCompoundOrEmpty(i);
            Lid l = new Lid(c.getStringOr("Naam", "?"), c.getStringOr("Kar", ""), Rang.van(c.getStringOr("Rang", "rekruut")), c.getLongOr("Sinds", 0));
            l.bijdrage = c.getLongOr("Bijdrage", 0);
            l.gezien = c.getLongOr("Gezien", 0);
            l.dagXp = c.getLongOr("DagXp", 0);
            l.dag = c.getLongOr("Dag", 0);
            b.leden.put(c.getStringOr("K", "?"), l);
        }
        CompoundTag aa = t.getCompoundOrEmpty("Aanvragen");
        for (String k : aa.keySet()) {
            b.aanvragen.put(k, aa.getStringOr(k, "?"));
        }
        b.xp = t.getLongOr("Xp", 0);
        b.kavel = t.getIntOr("Kavel", -1);
        b.kasteel = t.getIntOr("Kasteel", 0);
        b.gebouwd = t.getIntOr("Gebouwd", 0);
        b.kas = t.getLongOr("Kas", 0);
        CompoundTag dp = t.getCompoundOrEmpty("Depot");
        for (String k : dp.keySet()) {
            b.depot.put(k, dp.getIntOr(k, 0));
        }
        ListTag tr = t.getListOrEmpty("Trofeeen");
        for (int i = 0; i < tr.size(); i++) {
            CompoundTag c = tr.getCompoundOrEmpty(i);
            b.trofeeen.put(c.getStringOr("B", "?"), c.getLongOr("T", 0));
        }
        b.week = t.getLongOr("Week", -1);
        String taken = t.getStringOr("Taken", "");
        if (!taken.isEmpty()) {
            b.taken.addAll(List.of(taken.split(",")));
        }
        long[] v = t.getLongArray("Voortgang").orElse(new long[3]);
        for (int i = 0; i < 3 && i < v.length; i++) {
            b.voortgang[i] = v[i];
        }
        byte[] k = t.getByteArray("Klaar").orElse(new byte[3]);
        for (int i = 0; i < 3 && i < k.length; i++) {
            b.taakKlaar[i] = k[i] != 0;
        }
        b.bankRuw = t.getListOrEmpty("Bank").copy();
        return b;
    }

    static DyeColor kleur(String naam) {
        DyeColor c = DyeColor.byName(naam, null);
        return c == null ? DyeColor.PINK : c;
    }
}
