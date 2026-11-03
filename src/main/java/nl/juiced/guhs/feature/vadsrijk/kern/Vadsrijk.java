package nl.juiced.guhs.feature.vadsrijk.kern;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.Guhs;

/** Het Vadsrijk: where it is. */
public final class Vadsrijk {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("vadsrijk"));
    /** The ground level of the map, the water level. */
    public static final int GROND = 64, WATER = 62;

    private Vadsrijk() {
    }

    public static boolean in(@Nullable Entity e) {
        return e != null && e.level().dimension() == DIM;
    }

    public static boolean in(@Nullable Level l) {
        return l != null && l.dimension() == DIM;
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(DIM);
    }
}
