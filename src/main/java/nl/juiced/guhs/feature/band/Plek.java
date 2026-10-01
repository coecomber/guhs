package nl.juiced.guhs.feature.band;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/** Where a guh (or maatje) was last seen: what kind of place, the dimension, the position, a detail and the game time.
 *  1.2.0: the detail (a huisje, a player, a chest...) is a Component; saved before 1.2.0 it was a String (now a literal). */
public record Plek(PlekSoort soort, ResourceKey<Level> dim, BlockPos pos, net.minecraft.network.chat.Component detail, long tijd) {
    public static final Plek ONBEKEND = new Plek(PlekSoort.ONBEKEND, Level.OVERWORLD, BlockPos.ZERO, net.minecraft.network.chat.Component.empty(), 0);

    public Plek(PlekSoort soort, ResourceKey<Level> dim, BlockPos pos, String detail, long tijd) {
        this(soort, dim, pos, net.minecraft.network.chat.Component.literal(detail), tijd);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Soort", soort.id());
        t.putString("Dim", dim.identifier().toString());
        t.putLong("Pos", pos.asLong());
        nl.juiced.guhs.taal.Tekst.put(t, "Detail", detail);
        t.putLong("Tijd", tijd);
        return t;
    }

    public static Plek load(CompoundTag t) {
        if (!t.contains("Soort")) {
            return ONBEKEND;
        }
        Identifier dim = Identifier.tryParse(t.getStringOr("Dim", ""));
        return new Plek(PlekSoort.byId(t.getStringOr("Soort", "")),
                ResourceKey.create(Registries.DIMENSION, dim == null ? Level.OVERWORLD.identifier() : dim),
                BlockPos.of(t.getLongOr("Pos", 0L)), nl.juiced.guhs.taal.Tekst.get(t, "Detail"), t.getLongOr("Tijd", 0L));
    }

    /** Same place (ignoring the time)? */
    public boolean zelfde(Plek o) {
        return o != null && soort == o.soort && dim.equals(o.dim) && pos.equals(o.pos) && detail.equals(o.detail);
    }
}
