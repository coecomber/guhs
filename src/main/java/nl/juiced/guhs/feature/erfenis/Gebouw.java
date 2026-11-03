package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * De Erfenis: one placed building of a {@link Base}. Plain data, the same class on server and client.
 * <ul>
 *   <li>{@code x, z}: its min corner on the plot; it covers {@link #b()} x {@link #d()} tiles (the footprint turned by {@code rot}).</li>
 *   <li>{@code level}: 0 only while its first build runs, else 1..type.topLevel().</li>
 *   <li>{@code palet}: 1..5 (the STIJLGIDS palettes T1..T5); {@code paletGekozen}: the player painted it ("Verven"), else the
 *   palette follows the building's own shape tier when it goes up a level.</li>
 *   <li>{@code keuze}: the chosen recipe (index into {@link Productie#recepten}); {@code kleur}: the candle colour (0..15);
 *   {@code selectie}: the Bouwkeet blocks a Bouwblokkenwerkplaats makes (block ids; several = a random mix).</li>
 *   <li>{@code buffer}: what it made and you didn't collect yet (fractions too).</li>
 * </ul>
 */
public final class Gebouw {
    public final int id;
    public final GebouwType type;
    public int level;
    public int x, z;
    public int rot;
    public int palet = 1;
    public boolean paletGekozen;
    public int keuze;
    public int kleur;
    public final List<String> selectie = new ArrayList<>();
    public final Map<Goed, Double> buffer = new EnumMap<>(Goed.class);

    public Gebouw(int id, GebouwType type, int level, int x, int z, int rot) {
        this.id = id;
        this.type = type;
        this.level = level;
        this.x = x;
        this.z = z;
        this.rot = rot & 3;
    }

    /** Footprint along x (turned). */
    public int b() {
        return type.b(rot);
    }

    /** Footprint along z (turned). */
    public int d() {
        return type.d(rot);
    }

    public boolean bevat(int tx, int tz) {
        return tx >= x && tx < x + b() && tz >= z && tz < z + d();
    }

    /** Does a footprint of w x h at (ox, oz) overlap this building? */
    public boolean overlapt(int ox, int oz, int w, int h) {
        return ox < x + b() && ox + w > x && oz < z + d() && oz + h > z;
    }

    public double inBuffer(Goed g) {
        return buffer.getOrDefault(g, 0.0);
    }

    void bijBuffer(Goed g, double n) {
        buffer.merge(g, n, Double::sum);
    }

    /** Whole units waiting to be collected. */
    public int klaar() {
        int n = 0;
        for (double v : buffer.values()) {
            n += (int) Math.floor(v + 1e-9);
        }
        return n;
    }

    /** Is the basket full (its main product at the cap)? */
    public boolean vol() {
        Recept r = Productie.recept(this);
        return r != null && level > 0 && inBuffer(r.uit()) >= Productie.bufferCap(this) - 1e-6;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Id", id);
        t.putString("Type", type.id);
        t.putInt("Level", level);
        t.putInt("X", x);
        t.putInt("Z", z);
        t.putInt("Rot", rot);
        t.putInt("Palet", palet);
        t.putBoolean("PaletGekozen", paletGekozen);
        t.putInt("Keuze", keuze);
        t.putInt("Kleur", kleur);
        if (!selectie.isEmpty()) {
            ListTag l = new ListTag();
            for (String s : selectie) {
                l.add(StringTag.valueOf(s));
            }
            t.put("Selectie", l);
        }
        if (!buffer.isEmpty()) {
            CompoundTag b = new CompoundTag();
            buffer.forEach((g, v) -> b.putDouble(g.id, v));
            t.put("Buffer", b);
        }
        return t;
    }

    @Nullable
    public static Gebouw load(CompoundTag t) {
        GebouwType type = GebouwType.van(t.getStringOr("Type", ""));
        if (type == null) {
            return null;
        }
        Gebouw g = new Gebouw(t.getIntOr("Id", 0), type, t.getIntOr("Level", 1), t.getIntOr("X", 0), t.getIntOr("Z", 0), t.getIntOr("Rot", 0));
        g.level = Math.max(0, Math.min(type.topLevel(), g.level));
        g.palet = Math.max(1, Math.min(5, t.getIntOr("Palet", 1)));
        g.paletGekozen = t.getBooleanOr("PaletGekozen", false);
        g.keuze = t.getIntOr("Keuze", 0);
        g.kleur = Math.max(0, Math.min(16, t.getIntOr("Kleur", 0)));
        for (Tag e : t.getListOrEmpty("Selectie")) {
            e.asString().ifPresent(g.selectie::add);
        }
        CompoundTag b = t.getCompoundOrEmpty("Buffer");
        for (String k : b.keySet()) {
            Goed goed = Goed.van(k);
            if (goed != null) {
                g.buffer.put(goed, b.getDoubleOr(k, 0));
            }
        }
        return g;
    }
}
