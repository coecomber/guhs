package nl.juiced.guhs.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.BankSleutelItem;
import nl.juiced.guhs.feature.bank.BankUpgradeItem;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Bank Guh: right-click to open its stomach (storage with search, sort, filters and a crafting grid; at most 256 of one
 * kind of item until it got the upgrade). Breaking it keeps everything inside the item, the upgrade included.
 * <p>
 * bbq2: a click with the Bodemloos Buikje ({@code guhs:bank_upgrade}) upgrades it, a click with a Banksleutel
 * ({@code guhs:bank_sleutel}) makes the key remember this bank (for a Hapluikje). {@link #OPGEVOERD} mirrors the
 * upgrade of the block entity so the client can let an upgraded bank sparkle.
 */
public class BankGuhBlock extends BaseEntityBlock {
    public static final MapCodec<BankGuhBlock> CODEC = simpleCodec(BankGuhBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPGEVOERD = BooleanProperty.create("opgevoerd");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public BankGuhBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPGEVOERD, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPGEVOERD);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // face the player who placed it; a bank that had the upgrade still has it
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(OPGEVOERD, context.getItemInHand().getOrDefault(BankFeature.BANK_OPGEVOERD.get(), false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // drawn by GeckoLib (BankGuhRenderer)
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BankGuhBlockEntity(pos, state);
    }

    /** The upgrade and the link key do their thing on the bank; anything else in the hand just opens the stomach. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        boolean upgrade = stack.is(BankFeature.BANK_UPGRADE.get()), sleutel = stack.is(BankFeature.BANK_SLEUTEL.get());
        if (!upgrade && !sleutel) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof BankGuhBlockEntity bank) {
            if (upgrade) {
                BankUpgradeItem.opBank(stack, bank, speler);
            } else {
                BankSleutelItem.opBank(stack, bank, speler);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BankGuhBlockEntity bank) {
            player.openMenu(bank, pos);
            level.playSound(null, pos, ModSounds.GUH_AMBIENT.get(), SoundSource.BLOCKS, 0.6f, 1.2f);
        }
        return InteractionResult.SUCCESS;
    }

    /** An upgraded bank sparkles a little now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPGEVOERD) && random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.2, pos.getY() + 0.4 + random.nextDouble() * 1.3,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.2, 0.0, 0.01, 0.0);
        }
    }
}
