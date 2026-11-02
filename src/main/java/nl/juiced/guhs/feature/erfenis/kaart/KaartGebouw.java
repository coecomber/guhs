package nl.juiced.guhs.feature.erfenis.kaart;

import net.minecraft.nbt.CompoundTag;

/**
 * One building of an NPC village's plan: type, level, min corner (x, z) on the 64x64 plot, quarter turns. The same fields as
 * the core's {@code Base.Plan(GebouwType type, int level, int x, int z, int rot)}.
 */
public record KaartGebouw(KaartGebouwType type, int level, int x, int z, int rot) {
    public boolean overlapt(int ox, int oz, int maat) {
        return ox < x + type.maat && ox + maat > x && oz < z + type.maat && oz + maat > z;
    }

    public KaartGebouw metLevel(int nieuw) {
        return new KaartGebouw(type, nieuw, x, z, rot);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("T", type.id);
        t.putInt("L", level);
        t.putInt("X", x);
        t.putInt("Z", z);
        t.putInt("R", rot);
        return t;
    }

    @javax.annotation.Nullable
    public static KaartGebouw load(CompoundTag t) {
        KaartGebouwType type = KaartGebouwType.van(t.getStringOr("T", ""));
        return type == null ? null : new KaartGebouw(type, t.getIntOr("L", 1), t.getIntOr("X", 0), t.getIntOr("Z", 0), t.getIntOr("R", 0));
    }
}
