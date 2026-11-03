package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * One player's Sherlock progress (saved in {@link SherlockData}): the moments seen so far (what was built, the Hoeve level,
 * orders delivered, the first find), the solved cases with their boards (the Speurboekje), and the running case (its cards,
 * the red threads on the board, a card Vadson tinted red, whether the player guessed wrong).
 */
public final class Voortgang {
    public enum Status {
        /** Its moment has not come yet (or the case before it is not solved). */
        WACHT,
        /** Ready in the bureau's letterbox: open the bureau and start it. */
        KLAAR,
        BEZIG,
        OPGELOST
    }

    // --- the moments (only ever grow) ---
    public final Set<String> gebouwd = new LinkedHashSet<>();
    public int hoeve;
    public int bestellingen;
    /** The prologue: the rusty little key with a cheese charm (the first special find). */
    public boolean sleuteltje;
    /** Sherlock and Vadson moved into the Detectivebureau (it was built): the vergrootglas was handed out. */
    public boolean bureau;

    // --- the cases ---
    public final Set<String> opgelost = new LinkedHashSet<>();
    /** Solved in one go (the sticker "In één keer goed!"). */
    public final Set<String> eersteKeer = new LinkedHashSet<>();
    /** The boards of the solved cases (card to suspect), to read back in the Speurboekje. */
    public final Map<String, Map<String, String>> borden = new LinkedHashMap<>();
    /** Deco and reward flags received (each once). */
    public final Set<String> beloningen = new LinkedHashSet<>();

    // --- the running case ---
    @Nullable
    public String actief;
    public final Set<String> gevonden = new LinkedHashSet<>();
    public final Map<String, String> draadjes = new LinkedHashMap<>();
    @Nullable
    public String rood;
    public boolean foutGeraden;
    /** The courtroom: how many of Morris' three claims were answered with BEZWAAR (0..3). */
    public int ronde;

    public boolean isOpgelost(String zaakId) {
        return opgelost.contains(zaakId);
    }

    public Status status(Zaak z) {
        if (opgelost.contains(z.id())) {
            return Status.OPGELOST;
        }
        if (z.id().equals(actief)) {
            return Status.BEZIG;
        }
        Zaak vorige = Zaken.vorige(z);
        if ((vorige == null || opgelost.contains(vorige.id())) && z.trigger().test(this)) {
            return Status.KLAAR;
        }
        return Status.WACHT;
    }

    /** The first case that is not solved yet (null: the whole story is done). */
    @Nullable
    public Zaak huidige() {
        for (Zaak z : Zaken.ALLE) {
            if (!opgelost.contains(z.id())) {
                return z;
            }
        }
        return null;
    }

    public void wisZaak() {
        actief = null;
        gevonden.clear();
        draadjes.clear();
        rood = null;
        foutGeraden = false;
        ronde = 0;
    }

    // =====================================================================================================================

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.put("Gebouwd", lijst(gebouwd));
        t.putInt("Hoeve", hoeve);
        t.putInt("Bestellingen", bestellingen);
        t.putBoolean("Sleuteltje", sleuteltje);
        t.putBoolean("Bureau", bureau);
        t.put("Opgelost", lijst(opgelost));
        t.put("EersteKeer", lijst(eersteKeer));
        t.put("Beloningen", lijst(beloningen));
        CompoundTag b = new CompoundTag();
        borden.forEach((zaak, d) -> b.put(zaak, kaart(d)));
        t.put("Borden", b);
        if (actief != null) {
            t.putString("Actief", actief);
        }
        t.put("Gevonden", lijst(gevonden));
        t.put("Draadjes", kaart(draadjes));
        if (rood != null) {
            t.putString("Rood", rood);
        }
        t.putBoolean("Fout", foutGeraden);
        t.putInt("Ronde", ronde);
        return t;
    }

    public static Voortgang load(CompoundTag t) {
        Voortgang v = new Voortgang();
        lees(t.getListOrEmpty("Gebouwd"), v.gebouwd);
        v.hoeve = t.getIntOr("Hoeve", 0);
        v.bestellingen = t.getIntOr("Bestellingen", 0);
        v.sleuteltje = t.getBooleanOr("Sleuteltje", false);
        v.bureau = t.getBooleanOr("Bureau", false);
        lees(t.getListOrEmpty("Opgelost"), v.opgelost);
        lees(t.getListOrEmpty("EersteKeer"), v.eersteKeer);
        lees(t.getListOrEmpty("Beloningen"), v.beloningen);
        CompoundTag b = t.getCompoundOrEmpty("Borden");
        for (String zaak : b.keySet()) {
            v.borden.put(zaak, kaart(b.getCompoundOrEmpty(zaak)));
        }
        String a = t.getStringOr("Actief", "");
        v.actief = a.isEmpty() || Zaken.van(a) == null ? null : a;
        lees(t.getListOrEmpty("Gevonden"), v.gevonden);
        v.draadjes.putAll(kaart(t.getCompoundOrEmpty("Draadjes")));
        String r = t.getStringOr("Rood", "");
        v.rood = r.isEmpty() ? null : r;
        v.foutGeraden = t.getBooleanOr("Fout", false);
        v.ronde = t.getIntOr("Ronde", 0);
        return v;
    }

    private static ListTag lijst(Set<String> s) {
        ListTag l = new ListTag();
        for (String x : s) {
            l.add(StringTag.valueOf(x));
        }
        return l;
    }

    private static void lees(ListTag l, Set<String> naar) {
        for (Tag x : l) {
            x.asString().ifPresent(naar::add);
        }
    }

    private static CompoundTag kaart(Map<String, String> m) {
        CompoundTag c = new CompoundTag();
        m.forEach(c::putString);
        return c;
    }

    private static Map<String, String> kaart(CompoundTag c) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String k : c.keySet()) {
            m.put(k, c.getStringOr(k, ""));
        }
        return m;
    }
}
