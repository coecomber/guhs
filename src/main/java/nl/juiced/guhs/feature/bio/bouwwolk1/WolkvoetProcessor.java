package nl.juiced.guhs.feature.bio.bouwwolk1;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Processor {@code guhs:wolkenhoeder_hut_wolkvoet} (the processor list of the same name; all three templates of this
 * slice use it): the cloud "feet" under the lift and stream columns lie in the template's lowest layers, below the
 * height the terrain model gives the meadow under the anchor. The real meadow a few blocks further on may be a block
 * or two higher or lower, so a block of those layers ({@code onder_y}: template y below this) is only placed where the
 * world has air: the foot fills the gap down to a lower meadow and never cuts into a higher one. The terrain itself is
 * never changed (the model is the terrain).
 */
public class WolkvoetProcessor extends StructureProcessor {
    public static final MapCodec<WolkvoetProcessor> CODEC = Codec.INT.fieldOf("onder_y").xmap(WolkvoetProcessor::new, p -> p.onderY);
    private final int onderY;

    public WolkvoetProcessor(int onderY) {
        this.onderY = onderY;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, StructureTemplate.StructureBlockInfo original,
                                                             StructureTemplate.StructureBlockInfo current, StructurePlaceSettings settings) {
        if (original.pos().getY() >= onderY) {
            return current;
        }
        var box = settings.getBoundingBox();
        if (box != null && !box.isInside(current.pos())) {
            return current;             // (not placed in this pass anyway: do not ask the world outside the chunk being made)
        }
        return magHier(level.getBlockState(current.pos()).isAir()) ? current : null;
    }

    /** The rule: a foot block comes only where the world has air. */
    public static boolean magHier(boolean lucht) {
        return lucht;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return BouwWolk1Slice.WOLKVOET.get();
    }
}
