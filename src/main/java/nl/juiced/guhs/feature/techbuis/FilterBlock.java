package nl.juiced.guhs.feature.techbuis;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;

/**
 * Het Filterstuk ({@code guhs:knabbelbuis_filter}): a Richtingstuk with a little guh in it that knows what it likes. It
 * needs vadskracht ({@code VadsGetallen.BUISFILTER}), takes a mouthful at a time, and only lets through what is on its
 * list (or everything except that). Against a chest or a Bank Guh it can leave a number of each item behind ("bewaar
 * minstens"). A place behind a Filterstuk that asks for an item gets that item first: that is how you sort. Like every
 * guh machine it has a face ({@link #SNOET}): asleep without vadskracht, happy while it works, surprised when it cannot
 * get rid of its items. Use it to open its list; sneak + use with an empty hand turns it around.
 */
public class FilterBlock extends BuisStukBlock {
    public static final MapCodec<FilterBlock> CODEC = simpleCodec(FilterBlock::new);
    public static final EnumProperty<Snoet> SNOET = MachineBlock.SNOET;

    public FilterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SNOET, Snoet.SLAAPT));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SNOET);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilterBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
            draaiOm(level, pos, state, player);
        } else if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof FilterBlockEntity filter) {
            sp.openMenu(filter);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    // --- vadskracht: the Filterstuk is a knoop ---

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            VadsKracht.veranderd(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        VadsKracht.veranderd(level, pos);
    }
}
