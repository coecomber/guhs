package nl.juiced.guhs.feature.ringsausuman;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A station in the Toren van Sausuman where a player takes one ingredient for the Ringenbakker: the Deegkneder, the
 * Sauskraan or the Kaaskast ({@code soort}). The block never changes and never runs out: a click gives the quest item to a
 * player whose own questline asks for it, and again when they lost it ({@link Bakkerij#klikVoorraad}).
 */
public class VoorraadBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<VoorraadBlock> CODEC = simpleCodec(VoorraadBlock::new);
    public static final EnumProperty<Ingredient> SOORT = EnumProperty.create("soort", Ingredient.class);

    public VoorraadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SOORT, Ingredient.DEEG));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SOORT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Bakkerij.klikVoorraad(sp, pos, state.getValue(SOORT));
        }
        return InteractionResult.SUCCESS;
    }
}
