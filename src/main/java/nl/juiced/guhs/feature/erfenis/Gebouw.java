package nl.juiced.guhs.feature.erfenis;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;

/**
 * De Erfenis (POC): one placed building of a {@link Base}. Plain data, the same class on server and client.
 * <ul>
 *   <li>{@code x, z}: its min corner on the plot (tiles 0..{@value Plot#MAAT}-1); it covers {@code type.maat} tiles each way.</li>
 *   <li>{@code level}: 0 only while its first build runs (then {@link Base#bouw} names it), else 1..type.topLevel().</li>
 *   <li>{@code rot}: 0..3 quarter turns (only the look turns: every footprint is square).</li>
 *   <li>{@code voorraad, bijgewerkt}: (molen) the Bouwspul inside and the real time (millis) it was counted up to.</li>
 * </ul>
 */
public final class Gebouw {
    public final int id;
    public final GebouwType type;
    public int level;
    public int x, z;
    public int rot;
    public double voorraad;
    public long bijgewerkt;

    public Gebouw(int id, GebouwType type, int level, int x, int z, int rot) {
        this.id = id;
        this.type = type;
        this.level = level;
        this.x = x;
        this.z = z;
        this.rot = rot & 3;
    }

    /** Does this building cover tile (tx, tz)? */
    public boolean bevat(int tx, int tz) {
        return tx >= x && tx < x + type.maat && tz >= z && tz < z + type.maat;
    }

    /** Does a footprint of {@code maat} at (ox, oz) overlap this building? */
    public boolean overlapt(int ox, int oz, int maat) {
        return ox < x + type.maat && ox + maat > x && oz < z + type.maat && oz + maat > z;
    }

    /** (molen) the Bouwspul inside at this real time: the stock plus what was made since, up to what it holds. */
    public double voorraadOp(long nu) {
        if (type != GebouwType.MOLEN || level <= 0) {
            return 0;
        }
        double erbij = Math.max(0, nu - bijgewerkt) / 60000.0 * GebouwType.productiePerMinuut(level);
        return Math.min(GebouwType.molenOpslag(level), voorraad + erbij);
    }

    /** (molen) counts the stock up to now (before the level or the stock changes). */
    public void telBij(long nu) {
        if (type == GebouwType.MOLEN) {
            voorraad = voorraadOp(nu);
        }
        bijgewerkt = nu;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Id", id);
        t.putString("Type", type.id);
        t.putInt("Level", level);
        t.putInt("X", x);
        t.putInt("Z", z);
        t.putInt("Rot", rot);
        t.putDouble("Voorraad", voorraad);
        t.putLong("Bijgewerkt", bijgewerkt);
        return t;
    }

    @Nullable
    public static Gebouw load(CompoundTag t) {
        GebouwType type = GebouwType.van(t.getStringOr("Type", ""));
        if (type == null) {
            return null;
        }
        Gebouw g = new Gebouw(t.getIntOr("Id", 0), type, t.getIntOr("Level", 1), t.getIntOr("X", 0), t.getIntOr("Z", 0), t.getIntOr("Rot", 0));
        g.voorraad = t.getDoubleOr("Voorraad", 0);
        g.bijgewerkt = t.getLongOr("Bijgewerkt", 0L);
        return g;
    }
}
