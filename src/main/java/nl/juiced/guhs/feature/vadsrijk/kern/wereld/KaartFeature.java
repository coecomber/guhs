package nl.juiced.guhs.feature.vadsrijk.kern.wereld;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;

/**
 * Het Vadsrijk: the hand-built map gets into the world here (CONTRACT_VADSRIJK 10). The biome {@code guhs:vadsrijk} places this
 * feature once per chunk (placed feature {@code guhs:vr_kaart}, no modifiers). It finds the 64x320x64 tile
 * {@code guhs:vadsrijk/tegels/t_<tx>_<tz>} that holds this chunk and places only the part inside the chunk. No tile:
 * {@link Ondergrond#vul} (void, except kern's proeftuin).
 * <p>Deterministic, only new chunks, worldgen thread only. Entities in tiles are skipped (NPCs/mobs are code).
 */
public class KaartFeature extends Feature<NoneFeatureConfiguration> {
    public static final int TEGEL = 64, HOOGTE = 320;
    /** Tiles known to be missing (no file), so we don't ask the template manager 16 times per tile. */
    private static final Map<Long, Boolean> GEEN = new ConcurrentHashMap<>();

    public KaartFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public static Identifier tegelId(int tx, int tz) {
        return Guhs.id("vadsrijk/tegels/t_" + tx + "_" + tz);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ChunkPos cp = ChunkPos.containing(ctx.origin());
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        int tx = Math.floorDiv(x0, TEGEL), tz = Math.floorDiv(z0, TEGEL);
        long sleutel = ((long) tx << 32) | (tz & 0xFFFFFFFFL);
        StructureTemplate tegel = null;
        if (!GEEN.containsKey(sleutel)) {
            ServerLevel sl = level.getLevel();
            Optional<StructureTemplate> t = sl.getStructureManager().get(tegelId(tx, tz));
            if (t.isPresent()) {
                tegel = t.get();
            } else {
                GEEN.put(sleutel, Boolean.TRUE);
            }
        }
        if (tegel == null) {
            Ondergrond.vul(level, cp);
            return true;
        }
        BoundingBox chunk = new BoundingBox(x0, level.getMinY(), z0, x0 + 15, level.getMaxY(), z0 + 15);
        StructurePlaceSettings s = new StructurePlaceSettings().setBoundingBox(chunk).setIgnoreEntities(true).setKnownShape(true);
        BlockPos hoek = new BlockPos(tx * TEGEL, 0, tz * TEGEL);
        tegel.placeInWorld(level, hoek, hoek, s, ctx.random(), 2);
        return true;
    }
}
