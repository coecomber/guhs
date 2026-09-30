package nl.juiced.guhs.feature.band;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Where a guh (or maatje) was last seen: what kind of place, the dimension, the position, a detail and the game time. */
public record Plek(PlekSoort soort, ResourceKey<Level> dim, BlockPos pos, String detail, long tijd) {
    public static final Plek ONBEKEND = new Plek(PlekSoort.ONBEKEND, Level.OVERWORLD, BlockPos.ZERO, "", 0);

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Soort", soort.id());
        t.putString("Dim", dim.location().toString());
        t.putLong("Pos", pos.asLong());
        t.putString("Detail", detail);
        t.putLong("Tijd", tijd);
        return t;
    }

    public static Plek load(CompoundTag t) {
        if (!t.contains("Soort")) {
            return ONBEKEND;
        }
        ResourceLocation dim = ResourceLocation.tryParse(t.getString("Dim"));
        return new Plek(PlekSoort.byId(t.getString("Soort")),
                ResourceKey.create(Registries.DIMENSION, dim == null ? Level.OVERWORLD.location() : dim),
                BlockPos.of(t.getLong("Pos")), t.getString("Detail"), t.getLong("Tijd"));
    }

    /** Same place (ignoring the time)? */
    public boolean zelfde(Plek o) {
        return o != null && soort == o.soort && dim.equals(o.dim) && pos.equals(o.pos) && detail.equals(o.detail);
    }
}
