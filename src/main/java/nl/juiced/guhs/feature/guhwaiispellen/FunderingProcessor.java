package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * The foundation of the surf beach (processor {@code guhs:guhwaiispellen_fundering}, processor list of the same name): the
 * beach terrace sits on the island's shore, which drops towards the lagoon on one side (the template is placed unrotated,
 * the side is not known). Under every solid block of the template's bottom layer the column is filled with sandstone down to the ground or the lagoon floor (at most {@link #DIEPTE} blocks), so nothing ever hangs over air or
 * water: from the lagoon it looks like a sandy bank.
 */
public class FunderingProcessor extends StructureProcessor {
    public static final MapCodec<FunderingProcessor> CODEC = MapCodec.unit(FunderingProcessor::new);
    public static final DeferredRegister<StructureProcessorType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Guhs.MODID);
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<FunderingProcessor>> TYPE =
            TYPES.register("guhwaiispellen_fundering", () -> () -> CODEC);
    public static final int DIEPTE = 40;

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos offset, BlockPos pos,
                                                                         List<StructureTemplate.StructureBlockInfo> original,
                                                                         List<StructureTemplate.StructureBlockInfo> processed,
                                                                         StructurePlaceSettings settings) {
        int bodem = Integer.MAX_VALUE;
        for (StructureTemplate.StructureBlockInfo info : processed) {
            bodem = Math.min(bodem, info.pos().getY());
        }
        List<StructureTemplate.StructureBlockInfo> out = new ArrayList<>(processed);
        var box = settings.getBoundingBox();
        for (StructureTemplate.StructureBlockInfo info : processed) {
            if (info.pos().getY() != bodem || info.state().isAir() || (box != null && !box.isInside(info.pos()))) {
                continue;
            }
            BlockPos.MutableBlockPos p = info.pos().mutable().move(0, -1, 0);
            for (int k = 0; k < DIEPTE && p.getY() > level.getMinBuildHeight(); k++, p.move(0, -1, 0)) {
                BlockState onder = level.getBlockState(p);
                if (!onder.isAir() && onder.getFluidState().isEmpty() && !onder.canBeReplaced()) {
                    break;                                       // (the ground)
                }
                out.add(new StructureTemplate.StructureBlockInfo(p.immutable(), Blocks.SANDSTONE.defaultBlockState(), null));
            }
        }
        return out;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE.get();
    }
}
