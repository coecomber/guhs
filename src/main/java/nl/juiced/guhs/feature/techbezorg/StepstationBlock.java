package nl.juiced.guhs.feature.techbezorg;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;

/**
 * The Stepstation (bbq2): the home of one Bezorgguhtje. A guh machine ({@link MachineBlock}: it faces who placed it, has a
 * face, uses vadskracht); everything it does is in {@link StepstationBlockEntity}. A click opens its screen: the backpack
 * and the list of stops. Resources: tools/features/tech_bezorg.py.
 */
public class StepstationBlock extends MachineBlock {
    public static final MapCodec<StepstationBlock> CODEC = simpleCodec(StepstationBlock::new);

    public StepstationBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StepstationBlockEntity(pos, state);
    }

    /** The machine's own tick, and after it the station's: the guhtje is looked after also while there is no vadskracht. */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityTicker<T> machine = super.getTicker(level, state, type);
        if (level.isClientSide()) {
            return machine;
        }
        return (l, pos, s, be) -> {
            if (machine != null) {
                machine.tick(l, pos, s, be);
            }
            if (be instanceof StepstationBlockEntity station && !station.isRemoved()) {
                station.altijd();
            }
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof StepstationBlockEntity station) {
            station.geplaatst(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StepstationBlockEntity station && player instanceof ServerPlayer sp) {
            sp.openMenu(station, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
