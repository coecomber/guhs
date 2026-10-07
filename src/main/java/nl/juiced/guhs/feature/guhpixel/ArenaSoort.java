package nl.juiced.guhs.feature.guhpixel;

import java.util.function.Consumer;

import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * A kind of arena: its template ({@code data/guhs/structure/guhpixel/<N>_<what>.nbt}), its size (at most 160 x 160 x 160),
 * where players start (template coordinates, feet) and how it is made ready for the next game: {@code herstempel = true}
 * clears the box and stamps the template again (only when the game changes many blocks), otherwise {@code herstel} undoes
 * what the game changed. Non-player entities in the box are always removed first.
 */
public record ArenaSoort(String id, Identifier template, Vec3i maat, Vec3 startLokaal, float startYaw, boolean herstempel, Consumer<Arena> herstel) {
    public ArenaSoort {
        if (maat.getX() > Guhpixel.ARENA_MAX || maat.getY() > Guhpixel.ARENA_MAX || maat.getZ() > Guhpixel.ARENA_MAX) {
            throw new IllegalArgumentException("arena " + id + " is larger than " + Guhpixel.ARENA_MAX + ": " + maat);
        }
        if (herstel == null) {
            herstel = a -> { };
        }
    }

    /** An arena that is simply cleaned of entities after a game. */
    public ArenaSoort(String id, Identifier template, Vec3i maat, Vec3 startLokaal, float startYaw) {
        this(id, template, maat, startLokaal, startYaw, false, a -> { });
    }
}
