package nl.juiced.guhs.feature.bio.wereld;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Feature {@code guhs:bio_wereld_vulling}: everything of the three biomes' terrain that is not plain solid ground, placed
 * per chunk from the terrain model (the Guhmensie has no sea level, so water always comes from a feature): the rivers,
 * ponds and waterfalls of the Klaterdal ({@link DalVulling}), the lake and its beaches ({@link MeerVulling}), the clouds
 * and lift columns of the Wolkenweide ({@link WolkVulling}).
 * <p>
 * It runs once per chunk (placed feature without placement modifiers, in all three biomes; step "lakes", before every
 * building) and decides per column from the model, never from the biome or from blocks it finds. Each of the three
 * parts only writes inside the chunk it is called for, apart from the placeholder trees, which stay within a few blocks.
 */
public class BioVulling extends Feature<NoneFeatureConfiguration> {
    public BioVulling() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunk = ChunkPos.containing(context.origin());
        BioModel m = BioModel.van(level.getLevel().getChunkSource().randomState());
        Kaart k = m.kaart(chunk.x(), chunk.z());
        if (k.leeg) {
            return false;
        }
        int n = DalVulling.vul(level, m, k);
        n += MeerVulling.vul(level, m, k);
        n += WolkVulling.vul(level, m, k);
        return n > 0;
    }
}
