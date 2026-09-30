package nl.juiced.guhs.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.block.entity.FryingPanBlockEntity;
import nl.juiced.guhs.registry.ModItems;

/**
 * Guh frying pan. Right-click with Mika's vet to add fat (+64 fries, up to {@link #MAX_CHARGES}),
 * right-click with kaas knabbels to fry them into gefrituurde kaasknabbels. Empty hand shows how much fat is left.
 */
public class FryingPanBlock extends BaseEntityBlock {
    public static final MapCodec<FryingPanBlock> CODEC = simpleCodec(FryingPanBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    public static final int CHARGES_PER_VET = 64;
    public static final int MAX_CHARGES = 256;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 3, 15);

    public FryingPanBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILLED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILLED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FryingPanBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        boolean vet = stack.is(ModItems.MIKA_VET.get());
        boolean knabbels = stack.is(ModItems.KAAS_KNABBELS.get()) || stack.is(ModItems.GUH_VIS.get());
        if (!vet && !knabbels) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof FryingPanBlockEntity pan)) {
            return InteractionResult.SUCCESS;
        }

        if (vet) {
            if (pan.getCharges() + CHARGES_PER_VET > MAX_CHARGES) {
                player.sendOverlayMessage(Component.translatable("block.guhs.frying_pan.full"));
                return InteractionResult.CONSUME;
            }
            stack.consume(1, player);
            pan.setCharges(pan.getCharges() + CHARGES_PER_VET);
            level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
        } else {
            if (pan.getCharges() <= 0) {
                player.sendOverlayMessage(Component.translatable("block.guhs.frying_pan.no_fat"));
                return InteractionResult.CONSUME;
            }
            int fried = Math.min(stack.getCount(), pan.getCharges());
            // kaas knabbels -> fried kaas knabbels, guh fish -> fried guh fish
            net.minecraft.world.item.Item result = stack.is(ModItems.GUH_VIS.get()) ? ModItems.GEBAKKEN_GUH_VIS.get()
                    : ModItems.GEFRITUURDE_KAASKNABBELS.get();
            if (!player.getAbilities().instabuild) {
                stack.shrink(fried);
            }
            pan.setCharges(pan.getCharges() - fried);
            player.getInventory().placeItemBackInInventory(new ItemStack(result, fried));
            level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.6f, 1.6f);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 8, 0.25, 0.05, 0.25, 0.01);
            }
        }
        level.setBlock(pos, state.setValue(FILLED, pan.getCharges() > 0), 3);
        showCharges(player, pan);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FryingPanBlockEntity pan) {
            showCharges(player, pan);
        }
        return InteractionResult.SUCCESS;
    }

    private static void showCharges(Player player, FryingPanBlockEntity pan) {
        player.sendOverlayMessage(Component.translatable("block.guhs.frying_pan.charges", pan.getCharges(), MAX_CHARGES));
    }
}
