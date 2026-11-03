package nl.juiced.guhs.feature.erfenis.kaart;

import net.minecraft.nbt.CompoundTag;

/**
 * One building of an NPC village's plan: type, level, min corner (x, z) on the plot, quarter turns. The same fields as the
 * core's {@code Base.Plan}. An odd turn swaps the footprint (non-square buildings only get 0 or 2, see {@link NpcBouwer}).
 */
public record KaartGebouw(KaartGebouwType type, int level, int x, int z, int rot) {
    public int breedte() {
        return rot % 2 == 0 ? type.breedte : type.diepte;
    }

    public int diepte() {
        return rot % 2 == 0 ? type.diepte : type.breedte;
    }

    public boolean overlapt(int ox, int oz, int w, int d) {
        return ox < x + breedte() && ox + w > x && oz < z + diepte() && oz + d > z;
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

    /** Null for an unknown type (a POC plan: stadhuis, muur, ... are dropped on load and the village is planned anew). */
    @javax.annotation.Nullable
    public static KaartGebouw load(CompoundTag t) {
        KaartGebouwType type = KaartGebouwType.van(t.getStringOr("T", ""));
        return type == null ? null : new KaartGebouw(type, t.getIntOr("L", 1), t.getIntOr("X", 0), t.getIntOr("Z", 0), t.getIntOr("R", 0));
    }
}
