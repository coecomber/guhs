package nl.juiced.guhs.feature.onderwater;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * A placement filter: only outside (the pieces of) one structure. The Diepe Guhzee's corals, kelp and seagrass grow
 * everywhere on its bottom, but not in the Guhbubbel: that one has its own reef, and its paths and doors stay free.
 * (Worldgen only: it asks the structures of the chunks being generated, like the game itself does for its pieces.)
 */
public class OutsideStructureFilter extends PlacementFilter {
    public static final MapCodec<OutsideStructureFilter> CODEC = ResourceKey.codec(Registries.STRUCTURE).fieldOf("structure")
            .xmap(OutsideStructureFilter::new, f -> f.structure);

    private final ResourceKey<Structure> structure;

    public OutsideStructureFilter(ResourceKey<Structure> structure) {
        this.structure = structure;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        if (!(context.getLevel() instanceof WorldGenRegion region)) {
            return true;
        }
        Structure target = region.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(structure);
        if (target == null) {
            return true;
        }
        var manager = region.getLevel().structureManager().forWorldGenRegion(region);
        return !manager.getStructureWithPieceAt(pos, target).isValid();
    }

    @Override
    public PlacementModifierType<?> type() {
        return OnderwaterFeature.OUTSIDE_STRUCTURE.get();
    }
}
