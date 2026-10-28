package nl.juiced.guhs.feature.zingendeweide;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.blub.BlubTrekking;

/**
 * The five parts of De Zingende Weide: zones of the noise guhs:zingende_weide_deel, with the same thresholds as the surface
 * rule (tools/features/zingendeweide.py: DELEN). Each part has its own Blubs ({@link #blubSoort}).
 */
public enum WeideDeel {
    WOLKJESBOS(-10.0, -0.30, BlubTrekking.Soort.ZWAM),
    WIEGWEIDE(-0.30, -0.10, BlubTrekking.Soort.WEIDE),
    ZANGWEIDE(-0.10, 0.22, null),
    SCHILDJESZAND(0.22, 0.38, BlubTrekking.Soort.SCHILD),
    ZINGKLOOFJES(0.38, 10.0, BlubTrekking.Soort.RAVIJN);

    public static final ResourceKey<NormalNoise.NoiseParameters> RUIS = ResourceKey.create(Registries.NOISE, Guhs.id("zingende_weide_deel"));

    final double van, tot;
    /** The kind of Blub of this part (besides the blue ones that live everywhere); null: the usual draw. */
    @Nullable
    final BlubTrekking.Soort blub;

    WeideDeel(double van, double tot, @Nullable BlubTrekking.Soort blub) {
        this.van = van;
        this.tot = tot;
        this.blub = blub;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The part at (x, z) by the noise alone (also outside the biome). */
    public static WeideDeel van(ServerLevel level, int x, int z) {
        double v = level.getChunkSource().randomState().getOrCreateNoise(RUIS).getValue(x, 0, z);
        for (WeideDeel d : values()) {
            if (v >= d.van && v < d.tot) {
                return d;
            }
        }
        return ZANGWEIDE;
    }

    /** The part at this spot, or null outside De Zingende Weide. */
    @Nullable
    public static WeideDeel op(LevelAccessor level, BlockPos pos) {
        ServerLevel server = server(level);
        if (server == null || !level.getBiome(pos).is(ZingendeWeideFeature.ZINGENDE_WEIDE)) {
            return null;
        }
        return van(server, pos.getX(), pos.getZ());
    }

    /** The kind a wild Blub born here takes besides blue (WeideDeel.blub), or null: outside the meadow, or the Roze Zangweide. */
    @Nullable
    public static BlubTrekking.Soort blubSoort(LevelAccessor level, BlockPos pos) {
        WeideDeel d = op(level, pos);
        return d == null ? null : d.blub;
    }

    @Nullable
    static ServerLevel server(LevelAccessor level) {
        if (level instanceof ServerLevel s) {
            return s;
        }
        if (level instanceof ServerLevelAccessor a) {
            return a.getLevel();
        }
        return null;
    }
}
