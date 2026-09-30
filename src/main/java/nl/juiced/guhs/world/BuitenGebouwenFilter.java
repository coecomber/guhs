package nl.juiced.guhs.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.juiced.guhs.registry.ModStructureTypes;

/**
 * Placement filter guhs:buiten_gebouwen: no plants, trees or bushes in or on a building. The vegetation comes after the
 * buildings, and a flat roof looks just like ground to it; so a spot is skipped when it (or the block under it) lies in
 * a piece of any structure.
 */
public class BuitenGebouwenFilter extends PlacementFilter {
    public static final BuitenGebouwenFilter INSTANCE = new BuitenGebouwenFilter();
    public static final MapCodec<BuitenGebouwenFilter> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return !BouwRuimte.inBuilding(context.getLevel(), pos) && !BouwRuimte.inBuilding(context.getLevel(), pos.below());
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModStructureTypes.BUITEN_GEBOUWEN.get();
    }
}
